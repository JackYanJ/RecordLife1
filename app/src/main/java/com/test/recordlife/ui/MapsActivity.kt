package com.test.recordlife.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatSpinner
import androidx.core.content.ContextCompat
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.test.recordlife.Const
import com.test.recordlife.R
import com.test.recordlife.databinding.ActivityMapsBinding
import com.test.recordlife.models.LandmarkRecord
import com.test.recordlife.models.UserRecord
import com.test.recordlife.util.PermissionUtils.PermissionDeniedDialog.Companion.newInstance
import com.test.recordlife.util.PermissionUtils.isPermissionGranted
import com.test.recordlife.util.PermissionUtils.requestPermission
import java.io.IOException
import java.util.*
import kotlin.collections.HashMap


class MapsActivity : BaseActivity(), GoogleMap.OnMyLocationButtonClickListener,
    GoogleMap.OnMyLocationClickListener, OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding

    private var cameraPosition: CameraPosition? = null

    // The entry point to the Fused Location Provider.
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    // A default location (Sydney, Australia) and default zoom to use when location permission is
    // not granted.
    private val defaultLocation = LatLng(-33.8523341, 151.2106085)
    private var locationPermissionGranted = false

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private var lastKnownLocation: Location? = null

    /**
     * Flag indicating whether a requested permission has been denied after returning in
     * [.onRequestPermissionsResult].
     */
    private var permissionDenied = false

    private var mText: String = ""

    private var mLocation: String = ""

    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // [START_EXCLUDE silent]
        // Retrieve location and camera position from saved instance state.
        // [START maps_current_place_on_create_save_instance_state]
        if (savedInstanceState != null) {
            lastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION)
            cameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION)
        }
        // [END maps_current_place_on_create_save_instance_state]
        // [END_EXCLUDE]

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Construct a FusedLocationProviderClient.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        databaseReference = Firebase.database("https://recordlife-ad18b-default-rtdb.asia-southeast1.firebasedatabase.app/").reference

        basicListen()

    }
    // [END maps_current_place_on_create]

    /**
     * Saves the state of the map when the activity is paused.
     */
    // [START maps_current_place_on_save_instance_state]
    override fun onSaveInstanceState(outState: Bundle) {
        mMap.let { map ->
            outState.putParcelable(KEY_CAMERA_POSITION, map.cameraPosition)
            outState.putParcelable(KEY_LOCATION, lastKnownLocation)
        }
        super.onSaveInstanceState(outState)
    }
    // [END maps_current_place_on_save_instance_state]

    /**
     * Sets up the options menu.
     * @param menu The options menu.
     * @return Boolean.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.extra_operate_menu, menu)
        return true
    }

    /**
     * Handles a click on the menu option to get a place.
     * @param item The menu item to handle.
     * @return Boolean.
     */
    // [START maps_current_place_on_options_item_selected]
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.option_add_note -> addNote()
            R.id.option_search -> search()
            R.id.option_my_notes -> checkMyNotes()
            R.id.option_sign_out -> signOut()
        }
        return true
    }

    private fun checkMyNotes() {
        if (FirebaseAuth.getInstance().currentUser == null){
            Toast.makeText(this, " user data expire, please sign in again", Toast.LENGTH_SHORT).show()
            signOut()
        }
        ListActivity.start(this@MapsActivity, FirebaseAuth.getInstance().currentUser!!.uid, Const.ListType.LIST_MINE)
    }

    private fun search() {
        SearchActivity.start(this)
    }

    private fun signOut() {
        AuthUI.getInstance()
            .signOut(this)
            .addOnCompleteListener {
                SplashActivity.start(this)
                finish()
            }
    }

    private fun addNote() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.option_add_notes))
        // I'm using fragment here so I'm using getView() to provide ViewGroup
        // but you can provide here any other instance of ViewGroup from your Fragment / Activity
        // I'm using fragment here so I'm using getView() to provide ViewGroup
        // but you can provide here any other instance of ViewGroup from your Fragment / Activity
        val viewInflated: View = LayoutInflater.from(this)
            .inflate(R.layout.dialog_edit_text, null, false)
        // Set up the input
        val input = viewInflated.findViewById<View>(R.id.input) as AppCompatEditText
        // Set up the spinner
        val spinner = viewInflated.findViewById<View>(R.id.spinner) as AppCompatSpinner

        builder.setView(viewInflated)

        if (lastKnownLocation == null){
            Toast.makeText(this, "Request location failed, please click your location", Toast.LENGTH_SHORT).show()
            return
        }
        getLocation(
            lastKnownLocation!!.latitude,
            lastKnownLocation!!.longitude, spinner
        )

        // Set up the buttons

        // Set up the buttons
        builder.setPositiveButton(
            android.R.string.ok
        ) { dialog, _ ->
            dialog.dismiss()

            insertNote(input)
        }
        builder.setNegativeButton(
            android.R.string.cancel
        ) { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    private fun insertNote(input: EditText) {
        mText = input.text.toString()
        if (lastKnownLocation != null) {
            var name = ""
            if (TextUtils.isEmpty(FirebaseAuth.getInstance().currentUser?.displayName)) {
                if (!TextUtils.isEmpty(FirebaseAuth.getInstance().currentUser?.email)) {
                    name = FirebaseAuth.getInstance().currentUser?.email.toString()
                } else if (!TextUtils.isEmpty(FirebaseAuth.getInstance().currentUser?.phoneNumber)) {
                    name = FirebaseAuth.getInstance().currentUser?.phoneNumber.toString()
                }
            } else {
                name = FirebaseAuth.getInstance().currentUser?.displayName.toString()
            }
            //add notes data to server
            if (TextUtils.isEmpty(mLocation)){
                Toast.makeText(this, "Request location failed", Toast.LENGTH_SHORT).show()
                return
            }
            databaseReference.child("locations").child(
                mLocation
            )
                .child("Latlng")
                .setValue(lastKnownLocation!!.latitude.toString() + "," + lastKnownLocation!!.longitude.toString())
            databaseReference.child("locations").child(
                mLocation
            )
                .child("record").push().setValue(
                    LandmarkRecord(
                        name,
                        FirebaseAuth.getInstance().currentUser?.uid,
                        mText,
                        System.currentTimeMillis(),
                        mLocation
                    )
                ).addOnSuccessListener {
                    Toast.makeText(this, " add note successfully", Toast.LENGTH_SHORT).show()
                }

            databaseReference.child("users")
                .child(FirebaseAuth.getInstance().currentUser?.uid.toString()).push().setValue(
                    UserRecord(
                        mText,
                        name,
                        System.currentTimeMillis(),
                        lastKnownLocation!!.latitude,
                        lastKnownLocation!!.longitude,
                        mLocation
                    )
                )
        } else {
            Toast.makeText(this, "Request location failed", Toast.LENGTH_SHORT).show()
        }
    }
    // [END maps_current_place_on_options_item_selected]

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    // [START maps_current_place_on_map_ready]
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // [START_EXCLUDE]
        // [START map_current_place_set_info_window_adapter]
        // Use a custom info window adapter to handle multiple lines of text in the
        // info window contents.
        this.mMap.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
            // Return null here, so that getInfoContents() is called next.
            override fun getInfoWindow(arg0: Marker): View? {
                return null
            }

            override fun getInfoContents(marker: Marker): View {
                // Inflate the layouts for the info window, title and snippet.
                val infoWindow = layoutInflater.inflate(
                    R.layout.custom_info_contents,
                    findViewById<FrameLayout>(R.id.map), false
                )
                val title = infoWindow.findViewById<TextView>(R.id.title)
                title.text = marker.title
                val snippet = infoWindow.findViewById<TextView>(R.id.snippet)
                snippet.text = marker.snippet
                return infoWindow
            }
        })

        // [END map_current_place_set_info_window_adapter]

        mMap.setOnMarkerClickListener(object : GoogleMap.OnMarkerClickListener{
            override fun onMarkerClick(p0: Marker): Boolean {
                p0.showInfoWindow()
                ListActivity.start(this@MapsActivity, p0.position.latitude.toString() +","+p0.position.longitude.toString(), Const.ListType.LIST_OTHER_USER)
                return false
            }
        })

        enableMyLocation()

        // Add a marker in Sydney and move the camera
//        val sydney = LatLng(-34.0, 151.0)
//        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))

        googleMap.setOnMyLocationButtonClickListener(this)
        googleMap.setOnMyLocationClickListener(this)
    }
    // [END maps_current_place_on_map_ready]

    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    private fun enableMyLocation() {
        if (!::mMap.isInitialized) return
        // [START maps_check_location_permission]
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
            locationPermissionGranted = true

            // Get the current location of the device and set the position of the map.
            getDeviceLocation()
        } else {
            // Permission to access the location is missing. Show rationale and request permission
            requestPermission(
                this, LOCATION_PERMISSION_REQUEST_CODE,
                Manifest.permission.ACCESS_FINE_LOCATION, true
            )
        }
        // [END maps_check_location_permission]
    }

    override fun onMyLocationButtonClick(): Boolean {

        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false
    }

    override fun onMyLocationClick(location: Location) {
        lastKnownLocation = location
    }

    // [START maps_check_location_permission_result]
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return
        }
        if (isPermissionGranted(
                permissions,
                grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation()
            locationPermissionGranted = false
            when (requestCode) {
                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {

                    // If request is cancelled, the result arrays are empty.
                    if (grantResults.isNotEmpty() &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED
                    ) {
                        locationPermissionGranted = true
                        // Get the current location of the device and set the position of the map.
                        getDeviceLocation()
                    }
                }
            }
        } else {
            // Permission was denied. Display an error message
            // [START_EXCLUDE]
            // Display the missing permission error dialog when the fragments resume.
            permissionDenied = true
            // [END_EXCLUDE]
        }
    }

    // [END maps_check_location_permission_result]
    override fun onResumeFragments() {
        super.onResumeFragments()
        if (permissionDenied) {
            // Permission was not granted, display error dialog.
            showMissingPermissionError()
            permissionDenied = false
        }
    }

    /**
     * Displays a dialog with error message explaining that the location permission is missing.
     */
    private fun showMissingPermissionError() {
        newInstance(true).show(supportFragmentManager, "dialog")
    }

// [END maps_current_place_on_map_ready]

    /**
     * Gets the current location of the device, and positions the map's camera.
     */
    // [START maps_current_place_get_device_location]
    private fun getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (locationPermissionGranted) {
                val locationResult = fusedLocationProviderClient.lastLocation
                locationResult.addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Set the map's camera position to the current location of the device.
                        lastKnownLocation = task.result
                        if (lastKnownLocation != null) {
                            mMap.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(
                                        lastKnownLocation!!.latitude,
                                        lastKnownLocation!!.longitude
                                    ), DEFAULT_ZOOM.toFloat()
                                )
                            )
                        }
                    } else {
                        mMap.moveCamera(
                            CameraUpdateFactory
                                .newLatLngZoom(defaultLocation, DEFAULT_ZOOM.toFloat())
                        )
                        mMap.uiSettings.isMyLocationButtonEnabled = false
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }
    // [END maps_current_place_get_device_location]

    companion object {

        fun start(context: Context) {
            context.startActivity(Intent(context, MapsActivity::class.java))
        }

        /**
         * Request code for location permission request.
         *
         * @see .onRequestPermissionsResult
         */
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1

        private const val DEFAULT_ZOOM = 15
        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1

        // Keys for storing activity state.
        // [START maps_current_place_state_keys]
        private const val KEY_CAMERA_POSITION = "camera_position"
        private const val KEY_LOCATION = "location"
        // [END maps_current_place_state_keys]

    }

//    private fun getLocation(lat: Double, lng: Double): String {
//        val geocoder = Geocoder(this@MapsActivity, Locale.getDefault())
//        var add = ""
//        try {
//            val addresses: List<Address> = geocoder.getFromLocation(lat, lng, 1)
//            val obj: Address = addresses[0]
//            add = obj.getAddressLine(0)
//            Log.e("IGA", "Address$add")
//        } catch (e: IOException) {
//            e.printStackTrace()
//            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
//        }
//
//        return add
//    }

    private fun getLocation(lat: Double, lng: Double, spinner: AppCompatSpinner) {
        val geocoder = Geocoder(this@MapsActivity, Locale.getDefault())
        try {
            val addresses: List<Address> = geocoder.getFromLocation(lat, lng, 1)
            val newAddress = addresses.map { it.getAddressLine(0) }

            val adapter: SpinnerAdapter =
                ArrayAdapter(this, android.R.layout.simple_spinner_item, newAddress)
            spinner.adapter = adapter

            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    mLocation = newAddress[position]
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {

                }

            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }

    }

    private fun basicListen() {
        // [START basic_listen]
        // Get a reference to Messages and attach a listener
        var locationRef = this.databaseReference.child("locations")
        val locationListener = object : ValueEventListener {

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // New data at this path. This method will be called after every change in the
                // data at this path or a subpath.

                Log.d("TAG", "Number of messages: ${dataSnapshot.childrenCount}")
                dataSnapshot.children.forEach { child ->
                    // Extract Message object from the DataSnapshot
                    val result : HashMap<String, Any> = child.value as HashMap<String, Any>

                    val latLng : String = result["Latlng"] as String
                    if (result["record"] != null){

                        val record = result["record"] as HashMap<String, Any>

                        var title = ""
                        var snippet = ""

                        val record1 = record.entries.last().value as HashMap<String, Any>

                        title = record1.getValue("userName") as String
                        snippet = record1.getValue("text") as String

                        mMap.addMarker(MarkerOptions()
                            .title(title)
                            .snippet(snippet)
                            .position(LatLng(latLng.split(",")[0].toDouble(), latLng.split(",")[1].toDouble())))
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Could not successfully listen for data, log the error
                Log.e("TAG", "messages:onCancelled: ${error.message}")
            }
        }
        locationRef.addValueEventListener(locationListener)
        // [END basic_listen]
    }
}