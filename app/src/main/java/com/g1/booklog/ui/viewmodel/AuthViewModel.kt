package com.g1.booklog.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.g1.booklog.data.repository.FirebaseRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val user: FirebaseUser) : AuthState()
}

class AuthViewModel(
    private val firebaseRepo: FirebaseRepository,
    val webClientId: String
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _nickname = MutableStateFlow("")
    val nickname: StateFlow<String> = _nickname.asStateFlow()

    private val _customPhotoUrl = MutableStateFlow("")
    val customPhotoUrl: StateFlow<String> = _customPhotoUrl.asStateFlow()

    init {
        _authState.value = if (auth.currentUser != null)
            AuthState.Authenticated(auth.currentUser!!)
        else
            AuthState.Unauthenticated
        auth.addAuthStateListener { fa ->
            _authState.value = if (fa.currentUser != null)
                AuthState.Authenticated(fa.currentUser!!)
            else
                AuthState.Unauthenticated
        }
        if (auth.currentUser != null) loadProfile()
    }

    private fun loadProfile() = viewModelScope.launch {
        _nickname.value = firebaseRepo.getMyNickname()
        _customPhotoUrl.value = firebaseRepo.getMyPhotoUrl()
    }

    fun uploadProfileImage(context: android.content.Context, uri: Uri) = viewModelScope.launch {
        try {
            _message.value = "업로드 중..."
            val url = firebaseRepo.uploadProfileImage(context, uri)
            _customPhotoUrl.value = url
            _message.value = "프로필 사진이 변경되었습니다"
        } catch (e: Exception) {
            _message.value = "업로드 실패: ${e.message ?: e.toString()}"
        }
    }

    fun updateNickname(nickname: String) = viewModelScope.launch {
        try {
            firebaseRepo.updateNickname(nickname)
            _nickname.value = nickname
            _message.value = "별명이 저장되었습니다"
        } catch (e: Exception) {
            _message.value = "저장 실패: ${e.localizedMessage}"
        }
    }

    fun getSignInIntent(context: Context): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso).signInIntent
    }

    fun handleSignInResult(data: Intent?) = viewModelScope.launch {
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data)
                .getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            auth.signInWithCredential(credential).await()
            firebaseRepo.saveUserProfile()
            loadProfile()
        } catch (e: Exception) {
            _message.value = "로그인 실패: ${e.localizedMessage ?: "다시 시도해주세요"}"
        }
    }

    fun signOut(context: Context) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso).signOut()
        auth.signOut()
        _nickname.value = ""
        _customPhotoUrl.value = ""
    }

    fun clearMessage() { _message.value = null }

    class Factory(
        private val firebaseRepo: FirebaseRepository,
        private val webClientId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AuthViewModel(firebaseRepo, webClientId) as T
    }
}
