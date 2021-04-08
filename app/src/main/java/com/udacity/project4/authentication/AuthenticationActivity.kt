package com.udacity.project4.authentication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityAuthenticationBinding
import com.udacity.project4.locationreminders.RemindersActivity

/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */
class AuthenticationActivity : AppCompatActivity() {
    //We get a reference to the ViewModel scoped to this Fragment
    private val viewModel by viewModels<AuthenticationViewModel>()

    companion object {
        val TAG = AuthenticationActivity::class.java.simpleName
        const val SIGN_IN_RESULT_CODE = 1001
    }

    private lateinit var binding: ActivityAuthenticationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_authentication)

//          If the user was authenticated, send him to RemindersActivity
//        observeAuthenticationState()

        //Call launchSignInFlow when authenticationButton is pressed
        binding.authenticationButton.setOnClickListener {
            launchSignInFlow()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SIGN_IN_RESULT_CODE) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK) {
                //User successfully logged in.
                Log.i(TAG, "Successfully logged in ${FirebaseAuth.getInstance().currentUser?.displayName}")
                observeAuthenticationState()

                Toast.makeText(this, "Sign In Successful", Toast.LENGTH_SHORT).show()
            } else {
                //Sign In failed.
                Log.i(TAG, "Sign in Failed ${response?.error?.errorCode}")
                Toast.makeText(this, "Login unsuccessful", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Observe the Authentication State and change the UI accordingly
     */
    private fun observeAuthenticationState() {
        //Use the Authentication used form the AuthenticationViewModel to update the UI
        viewModel.authenticationState.observe(this, Observer { authenticationState ->
            if (authenticationState == AuthenticationViewModel.AuthenticationState.AUTHENTICATED)
                startActivity(Intent(this, RemindersActivity::class.java))
            finish()
        })
    }

    private fun launchSignInFlow() {
        // Give users the option to sign in / register with their email or Google account.
        // If users choose to register with their email, they will need to create a password as well.
        val providers = arrayListOf(
                AuthUI.IdpConfig.EmailBuilder().build(), AuthUI.IdpConfig.GoogleBuilder().build()
                //Here we can provide more ways to Authenticate users like PhoneBuilder() or FaceBookBuilder()
        )
        //Create and Launch Sign-In Intent
        //We listen to the Response with the SIGN_IN_RESULT_CODE
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setLogo(R.drawable.ic_pin)
                        .setAvailableProviders(providers).build(),
                AuthenticationActivity.SIGN_IN_RESULT_CODE
        )
    }
}
