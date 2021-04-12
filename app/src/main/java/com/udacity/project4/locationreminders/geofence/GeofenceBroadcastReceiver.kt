package com.udacity.project4.locationreminders.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.concurrent.TimeUnit
import com.udacity.project4.locationreminders.geofence.GeofenceTransitionsJobIntentService.Companion.enqueueWork

/**
 * Triggered by the Geofence.  Since we can have many Geofences at once, we pull the request
 * ID from the first Geofence, and locate it within the cached data in our Room DB
 *
 * Or users can add the reminders and then close the app, So our app has to run in the background
 * and handle the geofencing in the background.
 * To do that you can use https://developer.android.com/reference/android/support/v4/app/JobIntentService to do that.
 *
 */

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // implement the onReceive method to receive the geofencing events at the background
        enqueueWork(context, intent)

    }

    companion object {
        internal const val ACTION_GEOFENCE_EVENT = "com.udacity.project4.ACTION_GEOFENCE_EVENT"
        const val GEO_FENCE_METERS_RADIUS = 500f

        /** Used to set an Expiration time for geoFence. HOURS, DAYS etc. After this amount of time,
         * Location Services stops tracking the geofence. We set it to expire in 24 hours.
         */
        val GEOFENCE_EXPIRATION_IN_MILLISECONDS: Long = TimeUnit.HOURS.toHours(24)
    }
}