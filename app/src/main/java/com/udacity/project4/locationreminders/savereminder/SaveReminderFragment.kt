package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.authentication.AuthenticationActivity.Companion.TAG
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver.Companion.ACTION_GEOFENCE_EVENT
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver.Companion.GEOFENCE_EXPIRATION_IN_MILLISECONDS
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver.Companion.GEO_FENCE_METERS_RADIUS
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.savereminder.selectreminderlocation.*
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.BuildConfig
import org.koin.android.ext.android.inject
import java.util.*

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var geofenceClient: GeofencingClient

    private val runningQorLater =
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(requireActivity(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        geofenceClient = LocationServices.getGeofencingClient(activity!!)

        checkPermissionsAndStartGeofence()

        binding.viewModel = _viewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }
        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value
//            Completed: use the user entered reminder details to:
//             1) add a geofencing request
//             2) save the reminder to the local db
            val reminder = ReminderDataItem(title, description, location, latitude, longitude)

            if (_viewModel.validateEnteredData(reminder)) {
                _viewModel.validateAndSaveReminder(reminder)
                addGeofence(reminder)
            }
        }
    }

    private fun checkPermissionsAndStartGeofence() {
        if (foregroundAndBackgroundLocationPermissionApproved()) {
            checkDeviceLocationSettings()
        } else {
            requestForegroundAndBackgroundPermissions()
        }
    }

    @SuppressLint("MissingPermission")
    private fun addGeofence(reminder: ReminderDataItem) {
        //Build the Geofence Object
        val geofence = Geofence.Builder()
            //Set the request ID, string to identify the geofence.
            .setRequestId(reminder.id)
            //Set the Circular Region of this Geofence
            .setCircularRegion(reminder.latitude!!, reminder.longitude!!, GEO_FENCE_METERS_RADIUS)
            //Set the Expiration Duration of this Geofence. Automatically removed after a period of time
            // if we want to never Expire we use Geofence.NEVER_EXPIRE
            .setExpirationDuration(GEOFENCE_EXPIRATION_IN_MILLISECONDS)
            // Alerts are only generated for this transition. We can track Entry, Dwell, Exit.
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        //Build the Geofence request
        val geofencingRequest = GeofencingRequest.Builder()
            /** INITIAL_TRIGGER ENTER flag indicates that geofencing service should trigger a
             * GEOFENCE_TRANSITION_ENTER notification when the geofence is added, and if the device
             * is already inside that geofence
             */
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            //Add the geofences to be monitored by the geofence service
            .addGeofence(geofence)
            .build()

        //First we remove any existing Geofences that uses PendingIntent
        geofenceClient.removeGeofences(geofencePendingIntent)?.run {
            //Add new geofence regardless of success/failure removal
            addOnCompleteListener {
                geofenceClient.addGeofences(geofencingRequest, geofencePendingIntent)?.run {
                    addOnSuccessListener {
                        _viewModel.saveReminder(reminder)
                        Log.e("Add Geofence", geofence.requestId)
                    }
                    addOnFailureListener {
                        if (it.message != null) {
                            Log.w(TAG, it.message!!)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    /*
    * Determines if the App has the appropriate permission across Android Q and all other versions.
     */
    @TargetApi(Build.VERSION_CODES.Q)
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
                ))
        val backgroundLocationApproved =
            if (runningQorLater) {
                PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
            } else {
                true
            }
        return foregroundLocationApproved && backgroundLocationApproved
    }

    /**
     * Request ACCESS_FINE_LOCATION and on Android 10+ (Q) ACCESS_BACKGROUND_PERMISSION
     */
    @TargetApi(29)
    private fun requestForegroundAndBackgroundPermissions() {
        //Check if the permission have been already approved, if so we don't ask again and return
        if (foregroundAndBackgroundLocationPermissionApproved()) {
            checkDeviceLocationSettings()
            return
        }
        var permissionArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val resultCode = when {
            runningQorLater -> {
                permissionArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else -> REQUEST_FOREGROUND_ONLY_PERMISSION_REQUEST_CODE
        }
        Log.d(TAG, "Request Foreground only location permission")
        ActivityCompat.requestPermissions(requireActivity(), permissionArray, resultCode)
    }

    //We check if the user has their Location Device Enabled, if not we ask to turn it ON
    //using the Location request
    @SuppressLint("MissingPermission")
    private fun checkDeviceLocationSettings(resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        //LocationServices to get the Settings Client and create a val
        // called locationSettingsResponseTask to check the location settings
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask = settingsClient.checkLocationSettings(builder.build())

        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    exception.startResolutionForResult(
                        requireActivity(),
                        REQUEST_TURN_LOCATION_ON
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error getting location Settings" + sendEx.message)
                }
            } else {
                Snackbar.make(
                    binding.saveReminderLayout,
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettings()
                }.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                Log.v(TAG, "Location ON")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == REQUEST_TURN_LOCATION_ON) {
            checkDeviceLocationSettings(false)
        }
    }
}

private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
private const val REQUEST_FOREGROUND_ONLY_PERMISSION_REQUEST_CODE = 34
private const val REQUEST_TURN_LOCATION_ON = 29
