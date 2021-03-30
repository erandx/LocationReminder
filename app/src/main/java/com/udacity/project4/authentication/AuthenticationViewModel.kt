package com.udacity.project4.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.map

class AuthenticationViewModel: ViewModel() {

    enum class AuthenticationState{
        AUTHENTICATED, UNAUTHENTICATED}

    /**
     * authenticationState variable based on the FirebaseUserLiveData Object.
     * By creating this variable other Classes will be able to query it the user is logged in or not
     */
    val authenticationState = FirebaseUserLiveData().map{ firebaseUser ->
        if (firebaseUser !=null){
            AuthenticationState.AUTHENTICATED
        } else {
            AuthenticationState.UNAUTHENTICATED
        }
    }
}
