package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import com.google.android.gms.maps.CameraUpdateFactory
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.authentication.AuthenticationActivity.Companion.TAG
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.BuildConfig
import org.koin.android.ext.android.inject
import org.koin.core.logger.KOIN_TAG

//OnMapReadyCallback Interface must be implemented that interfaes with onMapReady method.
class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private lateinit var map: GoogleMap

    private val runningQorLater =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)
        /**
         * SupportMapFragment is a way to get GoogleMap into the Application
         * When we reference a fragment inside an Activity we use supportFragmentManger
         * When we reference a fragment inside a parent Fragment we use childFragmentManager
         * getMapAsync is also called to prepare Google Map. When this finishes the onMapReady is called
         */
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity!!)

        return binding.root
    }

    private fun onLocationSelected(poi: PointOfInterest) {
        /** When the user confirms on the selected location,
         *  send back the selected location details to the view model
         *  and navigate back to the previous fragment to save the reminder and add the geofence
         */
        val latLng = poi.latLng
        _viewModel.latitude.value = latLng.latitude
        _viewModel.longitude.value = latLng.longitude
        _viewModel.selectedPOI.value = poi
        _viewModel.reminderSelectedLocationStr.value = poi.name
        _viewModel.navigationCommand.postValue(NavigationCommand.Back)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    //If Location Permission has been granted we Zoom In
    @SuppressLint("MissingPermission")
    private fun enableLocation() {
        if (foregroundAndBackgroundLocationPermissionApproved()) {
            map.isMyLocationEnabled = true
            fusedLocationProviderClient.lastLocation.addOnSuccessListener(activity!!) { location ->
                if (location != null) {
                    val userLatLng = LatLng(location.latitude, location.longitude)
                    val zoomLevel = 15f
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, zoomLevel))
                }
            }
        } else {
            _viewModel.showErrorMessage.postValue(getString(R.string.err_select_location))
            ActivityCompat.requestPermissions(
                requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_TURN_LOCATION_ON
            )
        }
    }

    //Change the Map Type based on User selection
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        enableLocation()

        setPoiClick(map)

        setMapStyle(map)
    }

    //POI (Point of Interest) that clicks and sets a Pin displaying POI name.
    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            binding.saveLocationButton.visibility = View.VISIBLE

            val poiMarket = map.addMarker(
                MarkerOptions().position(poi.latLng)
                    .title(poi.name)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN))
            )
            val zoom = 16f
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(poi.latLng, zoom))
            poiMarket.showInfoWindow()

            binding.saveLocationButton.setOnClickListener {
                onLocationSelected(poi)
            }
        }
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            //Customize the Base Map Style using a JSON object defined in the raw res file.
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style)
            )
            if (!success) {
                Log.e(TAG, "Style Parsing Failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find Style. Error", e)
        }
    }

    //    /*
//    * Determines if the App has the appropriate permission across Android Q and all other versions.
//     */
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

    //     * In all cases we need to have location permission. On Android 10+ Q we need
//     * background permission as well.
//     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        Log.d(TAG, "onRequestPermissionResult")
        if (
            grantResults.isEmpty() ||
            grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
            (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                    grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] ==
                    PackageManager.PERMISSION_DENIED)
        ) {
            Snackbar.make(
                binding.mapLayout,
                R.string.permission_denied_explanation,
                Snackbar.LENGTH_LONG
            )
                .setAction(R.string.settings) {
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }.show()
        } else {
            enableLocation()
        }

    }
}

private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
private const val LOCATION_PERMISSION_INDEX = 0
private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1
private const val REQUEST_TURN_LOCATION_ON = 29

