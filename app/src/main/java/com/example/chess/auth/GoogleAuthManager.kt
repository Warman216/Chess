package com.example.chess.auth

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GoogleUser(
    val id: String,
    val email: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val isSignedIn: Boolean = false
) {
    val initials: String
        get() {
            val name = displayName.trim()
            if (name.isEmpty()) return "?"
            val parts = name.split(" ")
            return if (parts.size > 1) {
                "${parts[0].firstOrNull()?.uppercaseChar() ?: ""}${parts[1].firstOrNull()?.uppercaseChar() ?: ""}"
            } else {
                "${name.firstOrNull()?.uppercaseChar() ?: "?"}"
            }
        }
}

class GoogleAuthManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("chess_google_auth_prefs", Context.MODE_PRIVATE)

    private val credentialManager = CredentialManager.create(context)

    private val _currentUser = MutableStateFlow(loadSavedUser())
    val currentUser: StateFlow<GoogleUser> = _currentUser.asStateFlow()

    private fun loadSavedUser(): GoogleUser {
        val isSignedIn = prefs.getBoolean(KEY_IS_SIGNED_IN, false)
        val id = prefs.getString(KEY_ID, "guest_player") ?: "guest_player"
        val email = prefs.getString(KEY_EMAIL, "guest@chess.ai") ?: "guest@chess.ai"
        val name = prefs.getString(KEY_NAME, "Guest Player") ?: "Guest Player"
        val avatar = prefs.getString(KEY_AVATAR, null)

        return GoogleUser(
            id = id,
            email = email,
            displayName = name,
            avatarUrl = avatar,
            isSignedIn = isSignedIn
        )
    }

    private fun saveUser(user: GoogleUser) {
        prefs.edit()
            .putBoolean(KEY_IS_SIGNED_IN, user.isSignedIn)
            .putString(KEY_ID, user.id)
            .putString(KEY_EMAIL, user.email)
            .putString(KEY_NAME, user.displayName)
            .putString(KEY_AVATAR, user.avatarUrl)
            .apply()
        _currentUser.value = user
    }

    fun signInWithCustomAccount(email: String, displayName: String) {
        val cleanEmail = email.trim()
        val name = if (displayName.isNotBlank()) displayName.trim() else cleanEmail.substringBefore("@")
        val user = GoogleUser(
            id = "google_" + cleanEmail.lowercase().hashCode().toString(),
            email = cleanEmail,
            displayName = name,
            avatarUrl = null,
            isSignedIn = true
        )
        saveUser(user)
    }

    fun signInWithGoogle(
        activity: Activity,
        scope: CoroutineScope,
        onSuccess: (GoogleUser) -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch(Dispatchers.Main) {
            try {
                // Prepare Google ID Option
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId("google-chess-client") // Placeholder client id
                    .setAutoSelectEnabled(true)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result: GetCredentialResponse = credentialManager.getCredential(
                    request = request,
                    context = activity
                )

                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val user = GoogleUser(
                        id = googleIdTokenCredential.id,
                        email = googleIdTokenCredential.id,
                        displayName = googleIdTokenCredential.displayName ?: googleIdTokenCredential.id.substringBefore("@"),
                        avatarUrl = googleIdTokenCredential.profilePictureUri?.toString(),
                        isSignedIn = true
                    )
                    saveUser(user)
                    onSuccess(user)
                } else {
                    onError("Unrecognized credential type")
                }
            } catch (e: GetCredentialException) {
                Log.w("GoogleAuthManager", "Credential manager sign-in failed: ${e.message}")
                onError(e.message ?: "Sign-in cancelled or unavailable")
            } catch (e: Exception) {
                Log.w("GoogleAuthManager", "Sign-in error: ${e.message}")
                onError(e.message ?: "Google Sign-in failed")
            }
        }
    }

    fun signOut() {
        prefs.edit().clear().apply()
        _currentUser.value = GoogleUser(
            id = "guest_player",
            email = "guest@chess.ai",
            displayName = "Guest Player",
            avatarUrl = null,
            isSignedIn = false
        )
    }

    companion object {
        private const val KEY_IS_SIGNED_IN = "is_signed_in"
        private const val KEY_ID = "user_id"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_NAME = "user_name"
        private const val KEY_AVATAR = "user_avatar"
    }
}
