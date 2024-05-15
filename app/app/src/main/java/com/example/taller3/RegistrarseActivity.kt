package com.example.taller3




import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import com.example.taller3.databinding.ActivityRegistrarseBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.database.database

import com.google.android.gms.location.LocationRequest
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.taller3.utils.Alerts
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority


class RegistrarseActivity:AppCompatActivity() {

    private lateinit var binding: ActivityRegistrarseBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private val PERM_LOCATION_CODE = 101
    private var alerts: Alerts = Alerts(this)
    private lateinit var currentLocation: Location

    private val database = Firebase.database
    private val messageRef = database.getReference("messages")




    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityRegistrarseBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

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


        binding.button.setOnClickListener {
            signUp()
        }
    }

    private fun setupLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = com.google.android.gms.location.LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).apply {
            setMinUpdateDistanceMeters(5F)
            setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            setWaitForAccurateLocation(true)
        }.build()
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.locations.forEach { location ->
                    currentLocation = location
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



    private fun signUp() {
        auth.createUserWithEmailAndPassword(
            binding.correoForm.text.toString(),
            binding.contraForm.text.toString()
        ).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                // Sign in success, update UI with the signed-in user's information
                val user = auth.currentUser
                val profileUpdates = userProfileChangeRequest {
                    displayName = binding.nombreForm.text.toString()
                }
                user?.updateProfile(profileUpdates)
                    ?.addOnCompleteListener { profileUpdateTask ->
                        if (profileUpdateTask.isSuccessful) {
                            // Update successful, proceed to the next activity
                            val userid = Firebase.auth.currentUser?.uid
                            val identificacion = binding.latitudForm.text.toString()
                            val usuario = User(binding.nombreForm.text.toString(),binding.apellidoForm.text.toString(),identificacion, currentLocation.latitude,currentLocation.longitude,false)
                            if (userid != null) {
                                messageRef.child("users").child(userid).setValue(usuario).addOnSuccessListener {
                                    Toast.makeText(this, "Usuario guardado correctamente", Toast.LENGTH_SHORT).show()
                                }.addOnFailureListener{
                                    Toast.makeText(this, "Error al guardar el usuario", Toast.LENGTH_SHORT).show()
                                }


                            }
                            Toast.makeText(applicationContext, "Usuario registrado", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@RegistrarseActivity, MenuPrincipal::class.java))
                        } else {
                            // Update failed, display a message to the user
                            Toast.makeText(applicationContext, "Error al actualizar el perfil del usuario", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                // If sign in fails, display a message to the user.
                task.exception?.localizedMessage?.let {
                    val errorMessage = task.exception?.localizedMessage ?: "Error desconocido"
                    Toast.makeText(applicationContext, "Fallo al registrar usuario: $errorMessage", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}