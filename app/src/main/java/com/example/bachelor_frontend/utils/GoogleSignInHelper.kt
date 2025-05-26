package com.example.bachelor_frontend.utils

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks

/**
 * Utility class for handling Google Sign-In
 */
class GoogleSignInHelper(private val context: Context) {
    private val googleSignInClient: GoogleSignInClient

    init {
        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("YOUR_WEB_CLIENT_ID_HERE") // Replace with your Web Client ID from Firebase console
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    /**
     * Get the sign-in intent for starting the Google sign-in flow
     */
    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    /**
     * Handle the sign-in result from the activity result
     */
    fun handleSignInResult(data: Intent?): Task<GoogleSignInAccount> {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            // Google Sign In was successful
            val account = task.getResult(ApiException::class.java)
            return Tasks.forResult(account)
        } catch (e: ApiException) {
            // Google Sign In failed
            return Tasks.forException(e)
        }
    }

    /**
     * Sign out from Google
     */
    fun signOut() {
        googleSignInClient.signOut()
    }
}