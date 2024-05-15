package com.example.tallerfinal

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tallerfinal.databinding.ActivityListaUsuariosBinding
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database

class ListaUsuarios : AppCompatActivity() {

    private lateinit var binding: ActivityListaUsuariosBinding

    private val database = Firebase.database


    private val messageRef = database.getReference("messages/users")
    override fun onCreate(savedInstanceState: Bundle?) {

        val userListwithUID = mutableListOf<Pair<String, User>>()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityListaUsuariosBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val listView: ListView = findViewById(R.id.usuariosDisponibles)

        messageRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userList = mutableListOf<String>()
                for (userSnapshot in snapshot.children) {
                    val uid = userSnapshot.key
                    val user = userSnapshot.getValue(User::class.java)
                    user?.let {
                        // Verificar si el usuario está disponible antes de agregarlo a la lista
                        if (user.disponible) {
                            // Agregar el nombre del usuario a la lista
                            userList.add("${user.nombre} ${user.apellido}")
                            userListwithUID.add(uid!! to user)
                        }
                    }
                }
                // Crear un adaptador para el ListView y establecerlo
                val adapter = ArrayAdapter(this@ListaUsuarios, android.R.layout.simple_list_item_1, userList)
                listView.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ListaUsuarios, "Error al cancelar la lectura de la base de datos", Toast.LENGTH_SHORT).show()
            }
        })

        listView.setOnItemClickListener { parent, view, position, id ->
            val (selectedUid, selectedUser) = userListwithUID[position] // UID and User


            messageRef.child(selectedUid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val latitude = snapshot.child("latitud").value as Double // Retrieve latitude
                    val longitude = snapshot.child("longitud").value as Double // Retrieve longitude
                    val nombre = snapshot.child("nombre").value as String
                    val apellido = snapshot.child("apellido").value as String
                    // Obtenet latitude y longitud
                    Log.d("SelectedUserLocation", "Latitude: $latitude, Longitude: $longitude")

                    val intent = Intent(this@ListaUsuarios, UserMapActivity::class.java)

                    intent.putExtra("latitude", latitude)
                    intent.putExtra("longitude", longitude)
                    intent.putExtra("nombre", nombre)
                    intent.putExtra("nombre", apellido)
                    intent.putExtra("uid", selectedUid)

                    startActivity(intent)

                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ListaUsuarios, "Error al cancelar la lectura de la base de datos", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }


}