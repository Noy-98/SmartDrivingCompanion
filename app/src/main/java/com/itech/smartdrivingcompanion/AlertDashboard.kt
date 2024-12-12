package com.itech.smartdrivingcompanion

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.telephony.SmsManager
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AlertDashboard : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var distanceValueText: TextView
    private val recipientPhoneNumber = "+639206862167"
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_alert_dashboard)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val back = findViewById<FloatingActionButton>(R.id.go_back_bttn)
        distanceValueText = findViewById(R.id.distanceValue)

        // Firebase database reference
        database = FirebaseDatabase.getInstance().reference

        // Check SMS permissions at runtime
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), 1)
        }

        back.setOnClickListener {
            val intent = Intent (this, HomeDashboard::class.java)
            startActivity(intent)
            finish()
        }

        listenForSensorUpdates()
    }

    private fun listenForSensorUpdates() {
        // Reference to sensor1 and locTracker
        val sensorRef = database.child("CarDevice").child("CarDistanceSensor").child("sensor1")
        val gpsRef = database.child("CarDevice").child("GpsTracking").child("locTracker")

        var gpsLocation: String? = null

        gpsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                gpsLocation = snapshot.getValue(String::class.java)
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        sensorRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sensorValue = snapshot.getValue(Int::class.java) ?: return
                updateUI(sensorValue, gpsLocation)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun updateUI(sensorValue: Int, gpsLocation: String?) {
        distanceValueText.text = sensorValue.toString()

        // Update text color based on the value
        when (sensorValue) {
            in 60 .. Int.MAX_VALUE -> distanceValueText.setTextColor(
                resources.getColor(
                    android.R.color.black,
                    theme
                )
            )
            in 45 .. 59 -> distanceValueText.setTextColor(
                resources.getColor(
                    android.R.color.holo_orange_dark,
                    theme
                )
            ) // Orange
            in 0..58 -> {
                distanceValueText.setTextColor(
                    resources.getColor(
                        android.R.color.holo_red_dark,
                        theme
                    )
                ) // Red
                gpsLocation?.let { location ->
                    sendSMS(
                        recipientPhoneNumber,
                        "The Driver has an accident at $location. Can you come ASAP!"
                    )
                }
            }
        }
        // Trigger warning dialog and sound for sensor values in range 4 to 7
        if (sensorValue in 45..59 || sensorValue in 0.. 58) {
            showAlertDialog()
            startWarningSound()
        } else {
            stopWarningSound()  // Stop sound if sensor value is lower than 4
        }
    }

    private fun startWarningSound() {
        // Play warning sound only when needed
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.warning_sound) // Place a `warning_sound.mp3` in res/raw
            mediaPlayer?.start()
        }
    }

    private fun stopWarningSound() {
        // Stop warning sound when it is no longer needed
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
                it.release()  // Release resources
                mediaPlayer = null
            }
        }
    }

    private fun sendSMS(recipientNumber: String, message: String) {
        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(recipientNumber, null, message, null, null)
            Log.d("SMS", "SMS sent to $recipientNumber with message: $message")
            Toast.makeText(this, "SMS sent successfully!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("SMS", "Failed to send SMS: ${e.message}")
            Toast.makeText(this, "Failed to send SMS. Please check permissions or load.", Toast.LENGTH_LONG).show()
        }
    }

    private fun showAlertDialog() {
        // Check if the activity is still in a valid state before showing the dialog
        if (!isFinishing && !isDestroyed) {
            // Play warning sound
            val mediaPlayer = MediaPlayer.create(this, R.raw.warning_sound) // Place a `warning_sound.mp3` in res/raw
            mediaPlayer.start()

            // Display alert dialog
            val dialogBuilder = AlertDialog.Builder(this)
                .setTitle("Warning!")
                .setMessage("Don't get too close...")
                .setCancelable(false)
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                    mediaPlayer.stop()
                }
            dialogBuilder.create().show()
        }
    }

    override fun onStop() {
        super.onStop()
        stopWarningSound()  // Ensure the sound stops when the activity is stopped
    }

    override fun onDestroy() {
        super.onDestroy()
        stopWarningSound()  // Clean up sound when activity is destroyed
    }
}