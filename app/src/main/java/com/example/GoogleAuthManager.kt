package com.example

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

object GoogleAuthManager {
    private const val TAG = "GoogleAuthManager"
    
    // Web Client ID from google-services.json (client_type = 3)
    const val WEB_CLIENT_ID = "777263370542-6f3cqhlm2f7jdu0a6v1nr3f0kd1sllmf.apps.googleusercontent.com"

    private fun getFirebaseAuth(context: Context? = null): FirebaseAuth? {
        return try {
            if (context != null) {
                try {
                    if (FirebaseApp.getApps(context).isEmpty()) {
                        FirebaseApp.initializeApp(context)
                    }
                } catch (e: Throwable) {
                    Log.d(TAG, "FirebaseApp.initializeApp error: ${e.message}")
                }
            }
            try {
                FirebaseAuth.getInstance()
            } catch (e: Throwable) {
                null
            }
        } catch (e: Throwable) {
            Log.d(TAG, "FirebaseAuth not available: ${e.message}")
            null
        }
    }

    private val auth: FirebaseAuth?
        get() = getFirebaseAuth()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        try {
            _currentUser.value = firebaseAuth.currentUser
        } catch (e: Throwable) {
            Log.d(TAG, "AuthStateListener error: ${e.message}")
        }
    }

    fun init(context: Context? = null) {
        try {
            val firebaseAuth = getFirebaseAuth(context)
            if (firebaseAuth != null) {
                _currentUser.value = firebaseAuth.currentUser
                firebaseAuth.addAuthStateListener(authListener)
            }
        } catch (e: Throwable) {
            Log.d(TAG, "Failed to initialize authListener: ${e.message}")
        }
    }

    suspend fun signInWithGoogle(
        context: Context,
        onSuccess: (FirebaseUser) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val activity = context.findActivity() ?: (context as? Activity)
            val authContext = activity ?: context

            val firebaseAuth = getFirebaseAuth(authContext)
            if (firebaseAuth == null) {
                onError("Firebase Auth is not available on this device")
                return
            }

            if (activity == null) {
                onError("Activity context is required for Google Sign-In")
                return
            }

            val credentialManager = try {
                CredentialManager.create(activity)
            } catch (e: Throwable) {
                Log.e(TAG, "CredentialManager.create error", e)
                onError("Credential Manager initialization failed: ${e.message}")
                return
            }
            
            val rawNonce = UUID.randomUUID().toString()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(rawNonce.toByteArray())
            val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(WEB_CLIENT_ID)
                .setNonce(hashedNonce)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = try {
                credentialManager.getCredential(context = activity, request = request)
            } catch (e: GetCredentialCancellationException) {
                Log.d(TAG, "User cancelled Google Sign-In")
                onError("Sign-In cancelled by user")
                return
            } catch (e: GetCredentialException) {
                Log.e(TAG, "GetCredentialException: ${e.message}", e)
                onError(e.localizedMessage ?: "Google Credentials error: ${e.javaClass.simpleName}")
                return
            } catch (e: Throwable) {
                Log.e(TAG, "getCredential failed: ${e.message}", e)
                onError(e.localizedMessage ?: "Google Sign-In prompt failed")
                return
            }

            val credential = result.credential

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()
                
                val user = authResult.user
                if (user != null) {
                    _currentUser.value = user
                    onSuccess(user)
                } else {
                    onError("Google authentication failed: User is null")
                }
            } else {
                onError("Unexpected credential type received")
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Sign in failed: ${e.message}", e)
            onError(e.localizedMessage ?: "Google Sign-In failed")
        }
    }

    fun signOut(context: Context, onComplete: (() -> Unit)? = null) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                getFirebaseAuth(context)?.signOut()
                val activity = context.findActivity() ?: (context as? Activity)
                if (activity != null) {
                    val credentialManager = CredentialManager.create(activity)
                    credentialManager.clearCredentialState(
                        androidx.credentials.ClearCredentialStateRequest()
                    )
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error signing out: ${e.message}")
            } finally {
                _currentUser.value = null
                onComplete?.invoke()
            }
        }
    }
}
