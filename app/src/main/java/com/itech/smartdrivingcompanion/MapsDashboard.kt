package com.itech.smartdrivingcompanion

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MapsDashboard : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        private const val MAPVIEW_BUNDLE_KEY = "MapViewBundleKey"
    }

    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference
    private var locationMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_maps_dashboard)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().reference

        // Initialize MapView
        mapView = findViewById(R.id.maps)
        val mapViewBundle = savedInstanceState?.getBundle(MAPVIEW_BUNDLE_KEY)
        mapView.onCreate(mapViewBundle)
        mapView.getMapAsync(this)

        val back = findViewById<FloatingActionButton>(R.id.go_back_bttn)
        back.setOnClickListener {
            val intent = Intent (this, HomeDashboard::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        fetchCarLocation()
    }

    private fun fetchCarLocation() {
        val locTrackerRef = databaseReference.child("CarDevice").child("GpsTracking").child("locTracker")

        locTrackerRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val locUrl = snapshot.getValue(String::class.java)
                locUrl?.let {
                    val latLng = extractLatLngFromUrl(it)
                    if (latLng != null) {
                        updateMarker(latLng)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error
                error.toException().printStackTrace()
            }
        })
    }

    private fun updateMarker(location: LatLng) {
        if (locationMarker == null) {
            // Add a new marker
            locationMarker = googleMap.addMarker(
                MarkerOptions()
                    .position(location)
                    .title("Car Location")
            )
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
        } else {
            // Update the position of the existing marker
            locationMarker?.position = location
            googleMap.animateCamera(CameraUpdateFactory.newLatLng(location))
        }
    }

    private fun extractLatLngFromUrl(url: String): LatLng? {
        return try {
            val uri = Uri.parse(url)
            val latLng = uri.getQueryParameter("q")?.split(",")
            if (latLng != null && latLng.size == 2) {
                LatLng(latLng[0].toDouble(), latLng[1].toDouble())
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    override fun onStop() {
        mapView.onStop()
        super.onStop()
    }

    override fun onDestroy() {
        mapView.onDestroy()
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}