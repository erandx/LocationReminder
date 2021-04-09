package com.udacity.project4.utils

import androidx.test.espresso.idling.CountingIdlingResource

object EspressoIdlingResource {

    private const val GLOBAL = "GLOBAL"

    @JvmField
    val countingIdlingResource = CountingIdlingResource(GLOBAL)

    fun increment(){
        countingIdlingResource.increment()
    }

    fun decrement(){
        if (!countingIdlingResource.isIdleNow){
            countingIdlingResource.decrement()
        }
    }

    inline fun <T> wrapEspressoIdlingResource(function: () -> T) : T{
        // Espresso does not work well with coroutines yet. See
        // https://github.com/Kotlin/kotlinx.coroutines/issues/982
        EspressoIdlingResource.increment() //Set App as busy

        return try {
            function()
        }
        finally {
            EspressoIdlingResource.decrement() //Set app as Idle
        }
    }
}