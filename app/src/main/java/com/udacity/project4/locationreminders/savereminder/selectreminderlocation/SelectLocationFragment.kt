package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.content.res.Resources
import com.google.android.gms.maps.CameraUpdateFactory
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.databinding.DataBindingUtil
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
import org.koin.android.ext.android.inject
import org.koin.core.logger.KOIN_TAG

//OnMapReadyCallback Interface must be implemented that interfaes with onMapReady method.
class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var map: GoogleMap


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
     * getMapAsync is also call to prepare Google Map. When this finishes the onMapReady is called
     */
            val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
//        TODO: zoom to the user location after taking his permission
//        TODO: put a marker to location that the user selected


//        TODO: call this function after the user confirms on the selected location
        onLocationSelected()

        return binding.root
    }


    private fun onLocationSelected() {
        //        TODO: When the user confirms on the selected location,
        //         send back the selected location details to the view model
        //         and navigate back to the previous fragment to save the reminder and add the geofence
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
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

        val latitude = 42.3142647
        val longitude = -71.1103688
        val zoomLevel = 15f
        val cityLatLog = LatLng(latitude,longitude)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(cityLatLog,zoomLevel))
        map.addMarker(MarkerOptions().position(cityLatLog))

        setPoiClick(map)
        setMapStyle(map)

    }

    //POI (Point of Interest) that clicks and sets a Pin dispaying POI name.
    private fun setPoiClick(map: GoogleMap){
        map.setOnPoiClickListener { poi ->
        val poiMarket = map.addMarker(
            MarkerOptions().position(poi.latLng)
                            .title(poi.name)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
        )
            poiMarket.showInfoWindow()
        }
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
        //Customize the Base Map Style using a JSON object defined in the raw res file.
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style)
            )
            if (!success){
                Log.e(TAG, "Style Parsing Failed.")
            }
        }
        catch (e: Resources.NotFoundException){
            Log.e(TAG, "Can't find Style. Error", e)
        }

    }


}
