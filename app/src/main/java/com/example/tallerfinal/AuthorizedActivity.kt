package com.example.tallerfinal

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth


open class AuthorizedActivity : AppCompatActivity() {
    protected var auth: FirebaseAuth = Firebase.auth
    protected var currentUser = auth.currentUser



    override fun onStart() {
        super.onStart()
        if(auth.currentUser == null){
            logout()
        }
    }

    override fun onResume() {
        super.onResume()
        if(auth.currentUser == null){
            logout()
        }
    }

    protected fun logout(){
        auth.signOut()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }
}