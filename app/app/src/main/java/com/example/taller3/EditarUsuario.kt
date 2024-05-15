package com.example.taller3

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.taller3.databinding.ActivityEditarUsuarioBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database

class EditarUsuario : AuthorizedActivity() {

    private lateinit var binding: ActivityEditarUsuarioBinding
    private val database = Firebase.database
    private var user: User? = null
    private val messageRef = database.getReference("messages")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEditarUsuarioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.i("Current user", "${currentUser?.displayName}")
        Log.i("Current User", "${currentUser?.email}")

        val userid = currentUser?.uid
        val myRef = database.getReference("messages/users/$userid")

        Log.i("MyRef", "$myRef")

        myRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Check if the dataSnapshot exists and contains data
                if (dataSnapshot.exists()) {
                    // dataSnapshot.getValue(User::class.java) will parse the dataSnapshot into a User object if you have a User class defined
                    user = dataSnapshot.getValue(User::class.java)
                    // Accessing the nombre and apellido fields from the user object
                    val nombre = user?.nombre
                    val apellido = user?.apellido

                    // Do something with the retrieved nombre and apellido
                    Log.i("User Info", "Nombre: $nombre, Apellido: $apellido")
                } else {
                    Log.e("User Info", "User data does not exist")
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("Firebase Error", "Error fetching user data: ${databaseError.message}")
            }
        })

        binding.buttonNombre.setOnClickListener(){

            val nuevoNombre = binding.nombreForm.text.toString()

            // Verificar que el nombre no esté vacío
            if (nuevoNombre.isNotEmpty()) {

                // Actualizar el nombre del usuario en la base de datos
                myRef.child("nombre").setValue(nuevoNombre)
                    .addOnSuccessListener {

                        Log.i("Editar Usuario", "Nombre actualizado exitosamente a: $nuevoNombre")
                        Toast.makeText(this, "Nombre actualizado exitosamente a: $nuevoNombre", Toast.LENGTH_SHORT).show()

                        user?.nombre = nuevoNombre

                    }
                    .addOnFailureListener { e ->
                        // Error al actualizar el nombre
                        Log.e("Editar Usuario", "Error al actualizar el nombre: $e")
                    }
            } else {
                // El campo del nombre está vacío, muestra un mensaje de error
                Toast.makeText(this, "Por favor ingresa un nuevo nombre", Toast.LENGTH_SHORT).show()
            }
        }
        //lo mismo para apellido
        binding.buttonApellido.setOnClickListener(){

            val nuevoNombre = binding.apellidoForm.text.toString()


            if (nuevoNombre.isNotEmpty()) {


                myRef.child("apellido").setValue(nuevoNombre)
                    .addOnSuccessListener {

                        Log.i("Editar Usuario", "apellido actualizado exitosamente a: $nuevoNombre")
                        Toast.makeText(this, "apellido actualizado exitosamente a: $nuevoNombre", Toast.LENGTH_SHORT).show()

                        user?.apellido = nuevoNombre

                    }
                    .addOnFailureListener { e ->
                        // Error al actualizar el nombre
                        Log.e("Editar Usuario", "Error al actualizar el apellido: $e")
                    }
            } else {
                // El campo del nombre está vacío, muestra un mensaje de error
                Toast.makeText(this, "Por favor ingresa un nuevo apellido", Toast.LENGTH_SHORT).show()
            }
        }

        binding.aceptar.setOnClickListener(){

            startActivity(Intent(this, MenuPrincipal::class.java))
        }


    }
}