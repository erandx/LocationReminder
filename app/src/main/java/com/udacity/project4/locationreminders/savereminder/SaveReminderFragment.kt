package com.udacity.project4.locationreminders.savereminder

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
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
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.*

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var geofenceClient: GeofencingClient

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binding =
                DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

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
        geofenceClient = LocationServices.getGeofencingClient(requireActivity())

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

//            TODO: use the user entered reminder details to:
//             1) add a geofencing request
//             2) save the reminder to the local db
            val reminder = ReminderDataItem(title, description, location, latitude, longitude)

            if (_viewModel.validateEnteredData(reminder)) {
                _viewModel.validateAndSaveReminder(reminder)
                addGeofence(reminder)
            }
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

        geofenceClient.addGeofences(geofencingRequest, geofencePendingIntent)?.run {
            addOnCompleteListener {
                Log.e("Add Geofence", geofence.requestId)
            }
            addOnFailureListener {
                if (it.message != null) {
                    Log.w(TAG, it.message!!)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }
}
