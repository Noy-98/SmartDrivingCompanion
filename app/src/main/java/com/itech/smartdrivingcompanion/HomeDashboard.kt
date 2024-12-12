package com.itech.smartdrivingcompanion

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HomeDashboard : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home_dashboard)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNavigationView.selectedItemId = R.id.home
        bottomNavigationView.setOnItemSelectedListener { item: MenuItem ->
            if (item.itemId == R.id.home) {
                return@setOnItemSelectedListener true
            } else if (item.itemId == R.id.profile) {
                startActivity(Intent(applicationContext, ViewProfileDashboard::class.java))
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                finish()
                return@setOnItemSelectedListener true
            } else if (item.itemId == R.id.logout) {
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                return@setOnItemSelectedListener true
            }
            false
        }

        val mapsLink = findViewById<CardView>(R.id.mapsLink)
        val alertLink = findViewById<CardView>(R.id.alertLink)

        mapsLink.setOnClickListener {
            val intent = Intent (this, MapsDashboard::class.java)
            startActivity(intent)
            finish()
        }

        alertLink.setOnClickListener {
            val intent = Intent (this, AlertDashboard::class.java)
            startActivity(intent)
            finish()
        }

        loadUsersProfile()
    }

    private fun loadUsersProfile() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null){
            val uid = currentUser.uid
            val usersReference = FirebaseDatabase.getInstance().getReference("UsersTbl/$uid")

            usersReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val users = snapshot.getValue(UsersDBStructure::class.java)
                        if (users != null) {

                            findViewById<TextView>(R.id.fname).text = users.first_name
                            findViewById<TextView>(R.id.lname).text = users.last_name
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@HomeDashboard, "Failed to load profile", Toast.LENGTH_SHORT).show()
                }

            })
        }
    }
}