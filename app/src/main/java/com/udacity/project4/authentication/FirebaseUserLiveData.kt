package com.udacity.project4.authentication

import androidx.lifecycle.LiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

/**
 * This Class Observers the Firebase User. If there is no Registered User Firebase should be null.
 */
class FirebaseUserLiveData:  LiveData<FirebaseUser?>(){

    private val firebaseAuth = FirebaseAuth.getInstance()

    private val authenticationStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
    //Use the Firebase Instance instantiated at the beginning of the Class to get an Entry point
    //into the Firebase Authentications SKD the App is using.
    //Update the current User logged in into our App.
    value = firebaseAuth.currentUser

    }

    //When this Object has an Active Observer, start Observing the User the FirebaseAuth state to see
    // if there is currently a logged in User.
    override fun onActive() {
        firebaseAuth.addAuthStateListener(authenticationStateListener)
    }

    //When this Object has an Inactive Observer, stop Observing the FirebaseAuth state to prevent
    //Memory leaks.
    override fun onInactive() {
        firebaseAuth.addAuthStateListener(authenticationStateListener)
    }
}