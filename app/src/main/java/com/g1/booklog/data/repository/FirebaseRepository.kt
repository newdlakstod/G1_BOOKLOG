package com.g1.booklog.data.repository

import android.content.Context
import android.net.Uri
import com.g1.booklog.data.model.Book
import com.g1.booklog.data.model.BookGenre
import com.g1.booklog.data.model.ReadingStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

data class FriendInfo(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val nickname: String = "",
    val photoUrl: String = ""
) {
    fun visibleName() = nickname.ifBlank { displayName.ifBlank { email } }
}

data class FriendRequest(
    val requestId: String = "",
    val fromUid: String = "",
    val fromEmail: String = "",
    val fromDisplayName: String = "",
    val fromNickname: String = "",
    val fromPhotoUrl: String = "",
    val timestamp: Long = 0L
) {
    fun visibleName() = fromNickname.ifBlank { fromDisplayName.ifBlank { fromEmail } }
}

private const val CLOUDINARY_CLOUD_NAME = "davu2oxxc"
private const val CLOUDINARY_UPLOAD_PRESET = "saomgqjf"

class FirebaseRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val currentUid get() = auth.currentUser?.uid

    private fun userDoc(uid: String) = db.collection("users").document(uid)
    private fun booksCol(uid: String) = userDoc(uid).collection("books")
    private fun friendsCol(uid: String) = userDoc(uid).collection("friends")
    private fun requestsCol(uid: String) =
        db.collection("friendRequests").document(uid).collection("received")

    suspend fun saveUserProfile(nickname: String? = null) {
        val user = auth.currentUser ?: return
        val existing = userDoc(user.uid).get().await()
        val currentNickname = existing.getString("nickname") ?: ""
        val currentPhotoUrl = existing.getString("photoUrl") ?: ""
        userDoc(user.uid).set(
            mapOf(
                "email" to (user.email ?: ""),
                "displayName" to (user.displayName ?: ""),
                "nickname" to (nickname ?: currentNickname),
                "photoUrl" to currentPhotoUrl.ifEmpty { user.photoUrl?.toString() ?: "" }
            )
        ).await()
    }

    suspend fun updateNickname(nickname: String) {
        val uid = currentUid ?: return
        userDoc(uid).update("nickname", nickname).await()

        val myFriends = friendsCol(uid).get().await()
        if (myFriends.documents.isNotEmpty()) {
            val batch = db.batch()
            myFriends.documents.forEach { friendDoc ->
                batch.update(friendsCol(friendDoc.id).document(uid), "nickname", nickname)
            }
            batch.commit().await()
        }
    }

    suspend fun refreshFriendsProfiles() {
        val uid = currentUid ?: return
        val friendDocs = friendsCol(uid).get().await()
        if (friendDocs.isEmpty) return
        val batch = db.batch()
        friendDocs.documents.forEach { friendDoc ->
            val profile = userDoc(friendDoc.id).get().await()
            if (profile.exists()) {
                batch.update(
                    friendsCol(uid).document(friendDoc.id),
                    mapOf(
                        "nickname"    to (profile.getString("nickname") ?: ""),
                        "photoUrl"    to (profile.getString("photoUrl") ?: ""),
                        "displayName" to (profile.getString("displayName") ?: "")
                    )
                )
            }
        }
        batch.commit().await()
    }

    suspend fun getMyNickname(): String {
        val uid = currentUid ?: return ""
        return userDoc(uid).get().await().getString("nickname") ?: ""
    }

    suspend fun getMyPhotoUrl(): String {
        val uid = currentUid ?: return ""
        return userDoc(uid).get().await().getString("photoUrl") ?: ""
    }

    suspend fun uploadProfileImage(context: Context, uri: Uri): String {
        val uid = currentUid ?: throw Exception("로그인이 필요합니다")

        val url = withContext(Dispatchers.IO) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw Exception("이미지를 읽을 수 없습니다")

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "profile.jpg", bytes.toRequestBody("image/jpeg".toMediaTypeOrNull()))
                .addFormDataPart("upload_preset", CLOUDINARY_UPLOAD_PRESET)
                .build()

            val request = Request.Builder()
                .url("https://api.cloudinary.com/v1_1/$CLOUDINARY_CLOUD_NAME/image/upload")
                .post(requestBody)
                .build()

            val response = OkHttpClient().newCall(request).execute()
            val body = response.body?.string() ?: throw Exception("응답이 없습니다")
            if (!response.isSuccessful) throw Exception("업로드 실패 (${response.code}): $body")

            val json = JsonParser.parseString(body).asJsonObject
            if (json.has("error")) {
                throw Exception("Cloudinary 오류: ${json.getAsJsonObject("error").get("message").asString}")
            }
            json.get("secure_url")?.asString ?: throw Exception("URL 없음: $body")
        }

        userDoc(uid).update("photoUrl", url).await()

        // 내 사진이 저장된 친구들의 friends 컬렉션도 업데이트
        val myFriends = friendsCol(uid).get().await()
        if (myFriends.documents.isNotEmpty()) {
            val batch = db.batch()
            myFriends.documents.forEach { friendDoc ->
                batch.update(friendsCol(friendDoc.id).document(uid), "photoUrl", url)
            }
            batch.commit().await()
        }

        return url
    }

    suspend fun uploadBooks(books: List<Book>) {
        val uid = currentUid ?: return
        val col = booksCol(uid)
        val batch = db.batch()
        col.get().await().documents.forEach { batch.delete(it.reference) }
        books.forEach { book ->
            batch.set(col.document(book.id.toString()), bookToMap(book))
        }
        batch.commit().await()
    }

    suspend fun downloadBooks(): List<Book> {
        val uid = currentUid ?: return emptyList()
        return booksCol(uid).get().await().documents.mapNotNull { mapToBook(it.data) }
    }

    fun friendsFlow(): Flow<List<FriendInfo>> = callbackFlow {
        val uid = currentUid ?: run { trySend(emptyList()); close(); return@callbackFlow }
        val listener = friendsCol(uid).addSnapshotListener { snap, _ ->
            trySend(snap?.documents?.map { doc ->
                FriendInfo(
                    uid = doc.id,
                    email = doc.getString("email") ?: "",
                    displayName = doc.getString("displayName") ?: "",
                    nickname = doc.getString("nickname") ?: "",
                    photoUrl = doc.getString("photoUrl") ?: ""
                )
            } ?: emptyList())
        }
        awaitClose { listener.remove() }
    }

    fun friendRequestsFlow(): Flow<List<FriendRequest>> = callbackFlow {
        val uid = currentUid ?: run { trySend(emptyList()); close(); return@callbackFlow }
        val listener = requestsCol(uid).addSnapshotListener { snap, _ ->
            trySend(snap?.documents?.map { doc ->
                FriendRequest(
                    requestId = doc.id,
                    fromUid = doc.getString("fromUid") ?: "",
                    fromEmail = doc.getString("fromEmail") ?: "",
                    fromDisplayName = doc.getString("fromDisplayName") ?: "",
                    fromNickname = doc.getString("fromNickname") ?: "",
                    fromPhotoUrl = doc.getString("fromPhotoUrl") ?: "",
                    timestamp = doc.getLong("timestamp") ?: 0L
                )
            } ?: emptyList())
        }
        awaitClose { listener.remove() }
    }

    suspend fun sendFriendRequest(targetEmail: String): Result<Unit> {
        return try {
            val me = auth.currentUser ?: return Result.failure(Exception("로그인이 필요합니다"))
            val userSnap = db.collection("users")
                .whereEqualTo("email", targetEmail)
                .get().await()
            if (userSnap.isEmpty) return Result.failure(Exception("해당 이메일의 사용자를 찾을 수 없습니다"))
            val targetUid = userSnap.documents.first().id
            if (targetUid == me.uid) return Result.failure(Exception("자기 자신에게는 요청을 보낼 수 없습니다"))
            if (friendsCol(me.uid).document(targetUid).get().await().exists())
                return Result.failure(Exception("이미 친구입니다"))

            val myProfile = userDoc(me.uid).get().await()
            requestsCol(targetUid).add(
                mapOf(
                    "fromUid" to me.uid,
                    "fromEmail" to (me.email ?: ""),
                    "fromDisplayName" to (me.displayName ?: ""),
                    "fromNickname" to (myProfile.getString("nickname") ?: ""),
                    "fromPhotoUrl" to (me.photoUrl?.toString() ?: ""),
                    "timestamp" to System.currentTimeMillis()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptFriendRequest(request: FriendRequest) {
        val me = auth.currentUser ?: return
        val myProfile = userDoc(me.uid).get().await()
        val batch = db.batch()
        batch.set(
            friendsCol(me.uid).document(request.fromUid),
            mapOf(
                "email" to request.fromEmail,
                "displayName" to request.fromDisplayName,
                "nickname" to request.fromNickname,
                "photoUrl" to request.fromPhotoUrl,
                "addedAt" to System.currentTimeMillis()
            )
        )
        batch.set(
            friendsCol(request.fromUid).document(me.uid),
            mapOf(
                "email" to (me.email ?: ""),
                "displayName" to (me.displayName ?: ""),
                "nickname" to (myProfile.getString("nickname") ?: ""),
                "photoUrl" to (me.photoUrl?.toString() ?: ""),
                "addedAt" to System.currentTimeMillis()
            )
        )
        batch.delete(requestsCol(me.uid).document(request.requestId))
        batch.commit().await()
    }

    suspend fun declineFriendRequest(requestId: String) {
        val uid = currentUid ?: return
        requestsCol(uid).document(requestId).delete().await()
    }

    suspend fun removeFriend(friendUid: String) {
        val me = auth.currentUser ?: return
        val batch = db.batch()
        batch.delete(friendsCol(me.uid).document(friendUid))
        batch.delete(friendsCol(friendUid).document(me.uid))
        batch.commit().await()
    }

    suspend fun getFriendBooks(friendUid: String): List<Book> =
        booksCol(friendUid).get().await().documents.mapNotNull { mapToBook(it.data) }

    private fun bookToMap(book: Book): Map<String, Any?> = mapOf(
        "title" to book.title,
        "author" to book.author,
        "publisher" to book.publisher,
        "publishYear" to book.publishYear,
        "totalPages" to book.totalPages,
        "genre" to book.genre.name,
        "status" to book.status.name,
        "coverImageUrl" to book.coverImageUrl,
        "isbn" to book.isbn,
        "rating" to book.rating,
        "review" to book.review,
        "memo" to book.memo,
        "currentPage" to book.currentPage,
        "startDate" to book.startDate,
        "endDate" to book.endDate,
        "createdAt" to book.createdAt,
        "updatedAt" to book.updatedAt,
        "orderIndex" to book.orderIndex,
        "highlights" to book.highlights
    )

    private fun mapToBook(data: Map<String, Any?>?): Book? {
        if (data == null) return null
        return try {
            Book(
                id = 0,
                title = data["title"] as? String ?: return null,
                author = data["author"] as? String ?: "",
                publisher = data["publisher"] as? String ?: "",
                publishYear = (data["publishYear"] as? Long)?.toInt(),
                totalPages = (data["totalPages"] as? Long)?.toInt(),
                genre = BookGenre.entries.find { it.name == data["genre"] } ?: BookGenre.OTHER,
                status = ReadingStatus.entries.find { it.name == data["status"] } ?: ReadingStatus.WANT_TO_READ,
                coverImageUrl = data["coverImageUrl"] as? String ?: "",
                isbn = data["isbn"] as? String ?: "",
                rating = (data["rating"] as? Double)?.toFloat() ?: 0f,
                review = data["review"] as? String ?: "",
                memo = data["memo"] as? String ?: "",
                currentPage = (data["currentPage"] as? Long)?.toInt() ?: 0,
                startDate = data["startDate"] as? Long,
                endDate = data["endDate"] as? Long,
                createdAt = data["createdAt"] as? Long ?: System.currentTimeMillis(),
                updatedAt = data["updatedAt"] as? Long ?: System.currentTimeMillis(),
                orderIndex = (data["orderIndex"] as? Long)?.toInt() ?: 0,
                highlights = data["highlights"] as? String ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }
}
