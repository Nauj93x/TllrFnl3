package com.example.tallerfinal

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.tallerfinal.databinding.ActivityMainBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var auth: FirebaseAuth = Firebase.auth
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityMainBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.button.setOnClickListener {
            login()
        }

        binding.enlaceRegistro.setOnClickListener{
            val intent = Intent(this, RegistrarseActivity::class.java )
            startActivity(intent)
        }

    }

    private fun login() {
        auth.signInWithEmailAndPassword(
            binding.correoForm.text.toString(),
            binding.contraForm.text.toString()
        ).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                startActivity(Intent(this@MainActivity, MenuPrincipal::class.java))
                finish()

            } else {
                // If sign in fails, display a message to the user.
                task.exception?.localizedMessage?.let {
                    Toast.makeText(applicationContext, "Fallo de inicio de sesión", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }}