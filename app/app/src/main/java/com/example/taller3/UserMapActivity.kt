package com.example.taller3

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.taller3.databinding.ActivityUserMapBinding
import com.example.taller3.utils.Alerts
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database

class UserMapActivity : AuthorizedActivity(), MapsFragment.RouteFetchListener {

    private lateinit var binding: ActivityUserMapBinding
    private val database = Firebase.database
    var TAG = UserMapActivity::class.java.name
    private var userLatLng: LatLng? = null

    private var alerts: Alerts = Alerts(this)
    private val PERM_LOCATION_CODE = 101
    private lateinit var currentLocation: Location
    private lateinit var fragment: MapsFragment
    private val messageRef = database.getReference("messages/users")
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityUserMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupLocation()
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                startLocationUpdates()
            }

            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                alerts.indefiniteSnackbar(
                    binding.root,
                    "El permiso de Localizacion es necesario para usar esta actividad 😭"
                )

            }

            else -> {
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    PERM_LOCATION_CODE
                )
            }

        }

        val latitude = intent.getDoubleExtra("latitude", 0.0)
        val longitude = intent.getDoubleExtra("longitude", 0.0)
        var nombre = intent.getStringExtra("nombre")

        var apellido = intent.getStringExtra("apellido")

        var uid = intent.getStringExtra("uid")

        fragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as MapsFragment


        if (latitude != 0.0 && longitude != 0.0) {

            val userLatLng = LatLng(latitude, longitude)
            Log.d("UserMapActivity", "Latitude: $latitude, Longitude: $longitude")
            fragment.addStore(this@UserMapActivity, userLatLng, "usuario ha encontrar", "$nombre $apellido")
            if(uid != null){
            startDisponibleUserUpdates(uid)}
        } else {


            Log.e("UserMapActivity", "Latitude and/or longitude not found in intent extras")
        }


        binding.switchFollowDog.setOnCheckedChangeListener { _, isChecked ->
            fragment.moveCamera = isChecked
            val currentUser = FirebaseAuth.getInstance().currentUser
            val username = currentUser?.displayName ?: "Usuario desconocido"
            Toast.makeText(this, "Usuario autenticado: $username", Toast.LENGTH_SHORT).show()

            val userid = currentUser?.uid
            val myRef = database.getReference("messages/users/$userid")
            val childUpdates = hashMapOf<String, Any>(
                "disponible" to isChecked
            )
            myRef.updateChildren(childUpdates)
        }



    }

    private fun setupLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).apply {
            setMinUpdateDistanceMeters(5F)
            setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            setWaitForAccurateLocation(true)
        }.build()
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.locations.forEach { location ->
                    Log.i(TAG, "onLocationResult: $location") // Aquí se muestra en el log la ubicación actual
                    // Aquí puedes realizar cualquier acción con la ubicación actual
                    fragment.moveDog(location)
                    currentLocation = location
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    val username = currentUser?.displayName ?: "Usuario desconocido"

                    val userid = currentUser?.uid
                    val myRef = database.getReference("messages/users/$userid")
                    val childUpdates = hashMapOf<String, Any>(
                        "latitud" to location.latitude,
                        "longitud" to location.longitude
                    )

                    myRef.updateChildren(childUpdates)
                        .addOnSuccessListener {
                            // Update successfully completed
                            Log.d("FirebaseUpdate", "Location updated successfully")
                        }
                        .addOnFailureListener { e ->
                            // Update failed, log the error
                            Log.e("FirebaseUpdate", "Error updating location: ${e.message}", e)
                        }

                    myRef.updateChildren(childUpdates)


                }
            }
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERM_LOCATION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationUpdates()
                } else {
                    alerts.shortSimpleSnackbar(
                        binding.root,
                        "Me acaban de negar los permisos de Localizacion 😭"
                    )

                }
            }
        }
    }




    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest, locationCallback, Looper.getMainLooper()
            )

        }
    }

    private fun startDisponibleUserUpdates(uid: String){

        messageRef.child(uid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val latitude = snapshot.child("latitud").value as Double // Retrieve latitude
                val longitude = snapshot.child("longitud").value as Double // Retrieve longitude

                // Obtenet latitude y longitud
                Log.d("SelectedUserLocation", "Latitude: $latitude, Longitude: $longitude")

                val location = Location("provider")
                location.latitude = latitude
                location.longitude = longitude

                fragment.moveUser(location)


            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@UserMapActivity, "Error al cancelar la lectura de la base de datos", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onRouteFetched(routeCoordinates: List<Pair<Double, Double>>) {
        Log.i("RouteCoordinates", "Number of coordinates: ${routeCoordinates.size}")
        for ((index, coordinate) in routeCoordinates.withIndex()) {
            Log.i("RouteCoordinates", "Coordinate $index: (${coordinate.first}, ${coordinate.second})")
        }
    }

    open fun getCurrentLocation(callback: (Location) -> Unit) {
        // Check for location permissions before requesting location updates

        // Request last known location from FusedLocationProviderClient
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                // Check if location is not null
                if (location != null) {
                    // Pass the location to the callback function
                    callback(location)
                } else {
                    // Handle case where last known location is not available
                    // You may need to request location updates or handle this case differently based on your requirements
                }
            }
    }


}