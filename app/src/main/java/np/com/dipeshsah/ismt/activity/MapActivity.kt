package np.com.dipeshsah.ismt.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.PlacesStatusCodes
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.button.MaterialButton
import np.com.dipeshsah.ismt.R

class MapActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var map: GoogleMap
    private var selectedMarker: Marker? = null
    private var selectedLocation: LatLng? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var placeClient: PlacesClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1000
    private lateinit var productId: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        Places.initialize(applicationContext, "AIzaSyDxn7kIBwJOYXBf34Z3_dhfjJ8IBCVjGgY")
        placeClient = Places.createClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as? SupportMapFragment
        mapFragment?.getMapAsync(this@MapActivity)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val selectLocationButton = findViewById<Button>(R.id.selectLocationButton)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val cancelLocationButton = findViewById<MaterialButton>(R.id.tb_cancelMap)

        intent.hasExtra("productId").let {
            productId = intent.getStringExtra("productId").toString()
        }

        selectLocationButton.setOnClickListener {
            selectedLocation?.let {
                val intent = Intent().apply {
                    putExtra("latitude", it.latitude)
                    putExtra("longitude", it.longitude)
                    putExtra("productId", productId)
                }
                setResult(RESULT_OK, intent)
                finish()
            }
        }

        cancelLocationButton.setOnClickListener {
            finish()
        }

        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            getCurrentLocation()
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                Log.i("Location", location.toString())
                location?.let {
                    val currentLatLng = LatLng(it.latitude, it.longitude)
                    map.isMyLocationEnabled = true
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                    fetchNearbyToyStores(currentLatLng)

                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchNearbyToyStores(currentLatLng: LatLng) {
        val request = FindCurrentPlaceRequest.newInstance(
            listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.TYPES)
        )
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        val placeResponse = placeClient.findCurrentPlace(request)
        placeResponse.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val response: FindCurrentPlaceResponse = task.result
                for (placeLikelihood in response.placeLikelihoods) {
                    val place = placeLikelihood.place
                    if (place.types?.contains(Place.Type.STORE) == true) {
                        val markerOptions = MarkerOptions().position(place.latLng!!).title(place.name)
                        map.addMarker(markerOptions)
                    }
                }
            } else {
                val exception = task.exception
                if (exception is com.google.android.gms.common.api.ApiException) {
                    val statusCode = exception.statusCode
                    if (statusCode == PlacesStatusCodes.NOT_FOUND) {
                        Toast.makeText(this, "No toy stores found nearby", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }


    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        // Enable UI settings
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = true
        map.uiSettings.isScrollGesturesEnabled = true
        map.uiSettings.isZoomGesturesEnabled = true
        map.uiSettings.isTiltGesturesEnabled = true
        map.uiSettings.isRotateGesturesEnabled = true
       // enable search
        map.uiSettings.isMapToolbarEnabled = true

        val previousLatitude = intent.getDoubleExtra("latitude", 0.0)
        val previousLongitude = intent.getDoubleExtra("longitude", 0.0)

        if (previousLatitude != 0.0 && previousLongitude != 0.0) {
            selectedLocation = LatLng(previousLatitude, previousLongitude)
            selectedMarker = map.addMarker(MarkerOptions().position(selectedLocation!!).title("Selected Location"))
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLocation!!, 15f))
        }else{
            checkLocationPermission()
        }

        map.setOnMapClickListener { latLng ->
            selectedLocation = latLng
            selectedMarker?.remove()
            selectedMarker = map.addMarker(MarkerOptions().position(latLng).title("Selected Location"))
        }
    }
}