package com.g1.booklog.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.g1.booklog.data.model.Book
import com.g1.booklog.data.repository.FirebaseRepository
import com.g1.booklog.data.repository.FriendInfo
import com.g1.booklog.data.repository.FriendRequest
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FriendViewModel(private val firebaseRepo: FirebaseRepository) : ViewModel() {

    val friends: StateFlow<List<FriendInfo>> = firebaseRepo.friendsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val friendRequests: StateFlow<List<FriendRequest>> = firebaseRepo.friendRequestsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _selectedFriend = MutableStateFlow<FriendInfo?>(null)
    val selectedFriend: StateFlow<FriendInfo?> = _selectedFriend.asStateFlow()

    private val _friendBooks = MutableStateFlow<List<Book>>(emptyList())
    val friendBooks: StateFlow<List<Book>> = _friendBooks.asStateFlow()

    init {
        viewModelScope.launch {
            try { firebaseRepo.refreshFriendsProfiles() }
            catch (_: Exception) {}
        }
    }

    fun refreshFriends() = viewModelScope.launch {
        try { firebaseRepo.refreshFriendsProfiles() }
        catch (_: Exception) {}
    }

    fun selectFriend(friend: FriendInfo) { _selectedFriend.value = friend }

    fun sendFriendRequest(email: String) = viewModelScope.launch {
        firebaseRepo.sendFriendRequest(email)
            .onSuccess { _message.value = "친구 요청을 보냈습니다" }
            .onFailure { _message.value = it.message }
    }

    fun acceptRequest(request: FriendRequest) = viewModelScope.launch {
        try { firebaseRepo.acceptFriendRequest(request) }
        catch (e: Exception) { _message.value = e.message }
    }

    fun declineRequest(requestId: String) = viewModelScope.launch {
        try { firebaseRepo.declineFriendRequest(requestId) }
        catch (e: Exception) { _message.value = e.message }
    }

    fun removeFriend(friendUid: String) = viewModelScope.launch {
        try { firebaseRepo.removeFriend(friendUid) }
        catch (e: Exception) { _message.value = e.message }
    }

    fun loadFriendBooks(friendUid: String) = viewModelScope.launch {
        try { _friendBooks.value = firebaseRepo.getFriendBooks(friendUid) }
        catch (e: Exception) { _message.value = e.message }
    }

    fun clearMessage() { _message.value = null }

    class Factory(private val firebaseRepo: FirebaseRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            FriendViewModel(firebaseRepo) as T
    }
}
