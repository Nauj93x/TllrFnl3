package com.example.tallerfinal

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.tallerfinal.adapters.CustomInfoWindowAdapter
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONObject
import okhttp3.Request
import java.io.IOException

class MapsFragment:  Fragment(), SensorEventListener {

    private lateinit var gMap: GoogleMap
    private var pendingCameraUpdate: Pair<LatLng, Float>? = null
    val sydney = LatLng(-34.0, 151.0)
    private var dogMarker: Marker? = null
    private var selectedUserMarkerOptions: MarkerOptions? = null
    val polylineList = mutableListOf<Polyline>()
    var gMapPolyLine: Polyline? = null
    var zoomLevel = 15.0f
    private var destinationLocation: LatLng? = null
    var markerLocation: Marker? = null
    var moveCamera = true
    private val callback = OnMapReadyCallback { googleMap ->
        /**
         * Manipulates the map once available.
         * This callback is triggered when the map is ready to be used.
         * This is where we can add markers or lines, add listeners or move the camera.
         * In this case, we just add a marker near Sydney, Australia.
         * If Google Play services is not installed on the device, the user will be prompted to
         * install it inside the SupportMapFragment. This method will only be triggered once the
         * user has installed Google Play services and returned to the app.
         */
        /**
         * Manipulates the map once available.
         * This callback is triggered when the map is ready to be used.
         * This is where we can add markers or lines, add listeners or move the camera.
         * In this case, we just add a marker near Sydney, Australia.
         * If Google Play services is not installed on the device, the user will be prompted to
         * install it inside the SupportMapFragment. This method will only be triggered once the
         * user has installed Google Play services and returned to the app.
         */


        gMap = googleMap

        gMap.uiSettings.isZoomControlsEnabled = false
        gMap.uiSettings.isCompassEnabled = true



        gMap.setMapStyle(
            context?.let { MapStyleOptions.loadRawResourceStyle(it, R.raw.map_day) })

        dogMarker = gMap.addMarker(
            MarkerOptions().position(sydney).title("Hey Dog!")
                .icon(context?.let { bitmapDescriptorFromVector(it, R.drawable.baseline_location_pin_24) })
        )!!

        pendingCameraUpdate?.let { (latLng, zoomLevel) ->
            gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,zoomLevel))
            dogMarker?.position = latLng
            dogMarker?.zIndex = 10.0f
            pendingCameraUpdate = null // Clear pending camera update
        }

        if(selectedUserMarkerOptions != null){

            selectedUserMarkerOptions = selectedUserMarkerOptions?.icon(bitmapDescriptorFromVector(this,R.drawable.baseline_location_on_24))

            // Add the marker with the updated drawable
            selectedUserMarkerOptions?.let { updatedMarkerOptions ->
                markerLocation =  gMap.addMarker(updatedMarkerOptions)
            }
            selectedUserMarkerOptions = null
        }




    }


    fun updateCamera(cameraUpdate: CameraUpdate) {
        if(this::gMap.isInitialized) {
            gMap.animateCamera(cameraUpdate)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_maps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)

    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (this::gMap.isInitialized) {
            if (event!!.values[0] > 100) {
                gMap.setMapStyle(
                    context?.let { MapStyleOptions.loadRawResourceStyle(it, R.raw.map_day) })
            } else {
                gMap.setMapStyle(
                    context?.let { MapStyleOptions.loadRawResourceStyle(it, R.raw.map_night) })
            }
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        //Do Nothing
    }

    private fun bitmapDescriptorFromVector(fragment: Fragment, @DrawableRes vectorResId: Int): BitmapDescriptor? {
        return ContextCompat.getDrawable(fragment.requireContext(), vectorResId)?.run {
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
            val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
            draw(Canvas(bitmap))
            BitmapDescriptorFactory.fromBitmap(bitmap)
        }
    }


    //From https://stackoverflow.com/questions/42365658/custom-marker-in-google-maps-in-android-with-vector-asset-icon
    private fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
        return ContextCompat.getDrawable(context, vectorResId)?.run {
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
            val bitmap =
                Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
            draw(Canvas(bitmap))
            BitmapDescriptorFactory.fromBitmap(bitmap)
        }
    }

    fun addStore(context: Context, location: LatLng, title: String, address: String) {
        if (this::gMap.isInitialized){

            val infoWindowAdapter = CustomInfoWindowAdapter(context)
            gMap.setInfoWindowAdapter(infoWindowAdapter)

            val markerOptions = MarkerOptions()
                .position(location)
                .title(title)
                .snippet(address)

            // Set the address as the snippet
            gMap.addMarker(markerOptions)
            fetchRouteToDestination(location.latitude, location.longitude)}else{

            val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
            mapFragment?.getMapAsync(callback)

            selectedUserMarkerOptions = MarkerOptions()
                .position(location)
                .title(title)
                .snippet(address)


            fetchRouteToDestination(location.latitude, location.longitude)

        }
    }

    fun moveUser(location: Location){

        markerLocation?.position = LatLng(location.latitude,location.longitude)
        fetchRouteToDestination(location.latitude, location.longitude)

    }

    fun moveDog(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        dogMarker?.position = latLng
        dogMarker?.zIndex = 10.0f
        if (moveCamera) {
            if(this::gMap.isInitialized) {
                gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel))

            }else{

                Log.i("latLng", "$latLng.latitude")
                Log.i("latLng", "${latLng.longitude}")

                pendingCameraUpdate = Pair(latLng, zoomLevel)

                val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
                mapFragment?.getMapAsync(callback)

            }
        }
    }


    suspend fun fetchRoute(startLat: Double, startLon: Double, endLat: Double, endLon: Double, listener: RouteFetchListener): List<Pair<Double, Double>>? {

        return withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val url = "http://router.project-osrm.org/route/v1/driving/$startLon,$startLat;$endLon,$endLat?geometries=geojson"
                val request = Request.Builder()
                    .url(url)
                    .build()

                val response = client.newCall(request).execute()
                val jsonResponse = response.body?.string()

                if (jsonResponse != null) {
                    Log.i("HTTP Response", jsonResponse)
                }

                val routeCoordinates = mutableListOf<Pair<Double, Double>>()

                if (jsonResponse != null) {
                    val json = JSONObject(jsonResponse)
                    val routes = json.getJSONArray("routes")

                    if (routes.length() > 0) {
                        val route = routes.getJSONObject(0)
                        val geometry = route.getJSONObject("geometry")
                        val coordinates = geometry.getJSONArray("coordinates")

                        for (i in 0 until coordinates.length()) {
                            val coord = coordinates.getJSONArray(i)
                            val lon = coord.getDouble(0)
                            val lat = coord.getDouble(1)
                            routeCoordinates.add(Pair(lat, lon))
                        }
                    }
                }
                listener.onRouteFetched(routeCoordinates)
                routeCoordinates // Return the route coordinates
            } catch (e: IOException) {
                // Handle network error
                Log.e("RouteCoordinates", "Failed to fetch route coordinates ", e)
                e.printStackTrace()
                null // Return null in case of error
            }
        }

    }

    private fun fetchRouteToDestination(destinationLat: Double, destinationLon: Double) {
        CoroutineScope(Dispatchers.Main).launch {
            val activity = requireActivity() as? RouteFetchListener
            if (activity != null) {
                // Get user's current location from the Maps Activity
                (requireActivity() as? UserMapActivity)?.getCurrentLocation { userLocation ->
                    // Call fetchRoute and pass user's location and destination
                    CoroutineScope(Dispatchers.Main).launch {
                        val routeCoordinates = fetchRoute(
                            userLocation.latitude,
                            userLocation.longitude,
                            destinationLat,
                            destinationLon,
                            activity
                        )

                        if(gMapPolyLine != null){
                            gMapPolyLine?.remove()
                        }

                        if (routeCoordinates != null) {
                            Log.i("RouteCoordinates", "Number of coordinates: ${routeCoordinates.size}")
                            for ((index, coordinate) in routeCoordinates.withIndex()) {
                                Log.i("RouteCoordinates", "Coordinate $index: (${coordinate.first}, ${coordinate.second})")
                            }

                            if (routeCoordinates != null) {
                                // Convertir routeCoordinates a una lista de LatLng
                                val latLngList = mutableListOf<LatLng>()
                                for (coordinate in routeCoordinates) {
                                    val latLng = LatLng(coordinate.first, coordinate.second)
                                    latLngList.add(latLng)
                                }

                                // Log de las coordenadas convertidas
                                Log.i("RouteCoordinates", "Number of coordinates: ${latLngList.size}")
                                for ((index, latLng) in latLngList.withIndex()) {
                                    Log.i("RouteCoordinates", "Coordinate $index: (${latLng.latitude}, ${latLng.longitude})")
                                }

                                if (polylineList.isNotEmpty()) {
                                    // Remover la polilínea existente
                                    val existingPolyline = polylineList.removeAt(0)

                                    existingPolyline?.points?.clear()

                                    for (polyline in polylineList) {
                                        polyline.remove()
                                    }
                                    polylineList.clear()
                                    existingPolyline.isVisible = false
                                    existingPolyline?.points?.let { points ->
                                        if (points.isNotEmpty()) {
                                            points.forEachIndexed { index, point ->
                                                Log.i("Point poly $index", "$point")
                                            }
                                        } else {
                                            Log.i("Existing Polyline", "No points remaining.")
                                        }
                                    }





                                }

                                // Crear PolylineOptions
                                val polylineOptions = PolylineOptions().apply {
                                    color(ContextCompat.getColor(requireContext(), R.color.Verde3))
                                    width(10f)
                                    addAll(latLngList)
                                }

                                // Añadir polyline al mapa
                                val polyline = gMap.addPolyline(polylineOptions)
                                polylineList.add(polyline)
                                gMapPolyLine = gMap.addPolyline(polylineOptions)
                            }

                        } else {
                            Log.e("RouteCoordinates", "Failed to fetch route coordinates ")
                        }
                    }

                    Log.i("userLocation", "${userLocation.latitude}")
                    Log.i("userLocation", "${userLocation.longitude}")
                }
            } else {
                Log.e(ContentValues.TAG, "Activity does not implement RouteFetchListener")
            }
        }
    }

    interface RouteFetchListener {
        fun onRouteFetched(routeCoordinates: List<Pair<Double, Double>>)
    }


}