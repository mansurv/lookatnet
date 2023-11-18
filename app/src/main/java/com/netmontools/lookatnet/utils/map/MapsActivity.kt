package com.netmontools.lookatnet.utils.map

import android.content.SharedPreferences
import android.database.Cursor
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import androidx.loader.content.Loader
import androidx.preference.PreferenceManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.netmontools.lookatnet.BuildConfig
import com.netmontools.lookatnet.MainViewModel
import com.netmontools.lookatnet.R
import com.netmontools.lookatnet.utils.AddPointFragment
import com.netmontools.lookatnet.utils.AddPointFragment.Companion.newInstance
import com.netmontools.lookatnet.utils.EditPointFragment
import com.netmontools.lookatnet.utils.EditPointFragment.Companion.newInstance
import com.netmontools.lookatnet.utils.LogSystem

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {
    private var mGoogleMap: GoogleMap? = null
    private var isSatelliteMode = false
    private var isSchemeMode = false
    private var isHybridMode = false
    var mapFragment: SupportMapFragment? = null
    var sp: SharedPreferences? = null
    var argId: Long = -1
    var argLatitude = 0.0
    var argLongitude = 0.0
    var argBssid: String? = null
    var argSsid: String? = null
    lateinit var appCompatActivity: AppCompatActivity
    lateinit var appBar: ActionBar
    private lateinit var mainViewModel: MainViewModel
    private val loader: Loader<Cursor>? = null
    private val cursor: Cursor? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)
        mapFragment!!.setHasOptionsMenu(true)
        sp = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        isSatelliteMode = intent.getBooleanExtra(SATELLITE, false)
        isSchemeMode = intent.getBooleanExtra(SCHEME, false)
        isHybridMode = intent.getBooleanExtra(HYBRID, false)
        argId = intent.getLongExtra(ID, -1)
        argLatitude = intent.getDoubleExtra(LATITUDE, 0.0)
        argLongitude = intent.getDoubleExtra(LONGITUDE, 0.0)
        argBssid = intent.getStringExtra(BSSID)
        argSsid = intent.getStringExtra(SSID)
    }


    override fun onResume() {
        super.onResume()
        isSatelliteMode = sp!!.getBoolean("isSatellite_mode", false)
        isSchemeMode = sp!!.getBoolean("isScheme_mode", false)
        isHybridMode = sp!!.getBoolean("isHybrid_mode", false)


    }

    override fun onMapReady(googleMap: GoogleMap) {
        mGoogleMap = googleMap
        if (isSatelliteMode) {
            googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
        } else if (isSchemeMode) {
            googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        } else if (isHybridMode) {
            googleMap.mapType = GoogleMap.MAP_TYPE_HYBRID
        }

        mainViewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        mainViewModel.updateActionBarTitle("My places map")

        mGoogleMap!!.uiSettings.isMapToolbarEnabled = false
        if (argLatitude == 0.0) {
        } else {
            val latLngBuilder = LatLngBounds.Builder()
            val latLng = LatLng(argLatitude, argLongitude)
            mGoogleMap!!.addMarker(
                MarkerOptions().position(latLng).title(argSsid).snippet(argBssid).draggable(true)
            )
            latLngBuilder.include(latLng)
            val display = this.windowManager.defaultDisplay
            // construct a movement instruction for the map camera
            val movement = CameraUpdateFactory.newLatLngBounds(
                latLngBuilder.build(),
                display.width, display.height, 16
            )
            mGoogleMap!!.moveCamera(movement)
            mGoogleMap!!.setOnMapLongClickListener { latLng ->
                mGoogleMap!!.addMarker(
                    MarkerOptions().position(latLng).title("Marker in this position").snippet("")
                        .draggable(true)
                )
                mGoogleMap!!.moveCamera(CameraUpdateFactory.newLatLng(latLng))
                val dialog = newInstance(latLng.longitude, latLng.latitude)
                dialog.show(supportFragmentManager, AddPointFragment.TAG)
            }
            mGoogleMap!!.setOnMarkerDragListener(object : OnMarkerDragListener {
                override fun onMarkerDragStart(arg0: Marker) {}
                override fun onMarkerDragEnd(arg0: Marker) {
                    val dialog = newInstance(
                        argId, arg0.position.longitude, arg0.position.latitude,
                        argSsid!!,
                        argBssid!!
                    )
                    dialog.show(supportFragmentManager, EditPointFragment.TAG)
                    if (BuildConfig.USE_LOG) {
                        LogSystem.logInFile(
                            TAG, """ Location:
  ${arg0.position.longitude}, ${arg0.position.latitude}"""
                        )
                    }
                    mGoogleMap!!.animateCamera(CameraUpdateFactory.newLatLng(arg0.position))
                    //updateLocation(arg0.getPosition().longitude, arg0.getPosition().latitude);
                }

                override fun onMarkerDrag(arg0: Marker) {}
            })
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.activity_maps, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        if (menu != null) {
            menu.findItem(R.id.action_satellite_mode).setChecked(isSatelliteMode)
            menu.findItem(R.id.action_scheme_mode).setChecked(isSchemeMode)
            menu.findItem(R.id.action_hybrid_mode).setChecked(isHybridMode)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {

           R.id.action_satellite_mode -> {

                item.setChecked(true)
                isSatelliteMode = true
                mGoogleMap!!.mapType  = GoogleMap.MAP_TYPE_SATELLITE
                isSchemeMode = false
                isHybridMode = false
               sp?.edit()?.putBoolean("isScheme_mode", isSchemeMode)?.apply()
               sp?.edit()?.putBoolean("isHybrid_mode", isHybridMode)?.apply()
               sp?.edit()?.putBoolean("isSatellite_mode", isSatelliteMode)?.apply()
                true
            }
            R.id.action_scheme_mode -> {

                item.setChecked(true)
                isSchemeMode = true
                mGoogleMap!!.mapType = GoogleMap.MAP_TYPE_NORMAL
                isSatelliteMode = false
                isHybridMode = false
                sp?.edit()?.putBoolean("isScheme_mode", isSchemeMode)?.apply()
                sp?.edit()?.putBoolean("isHybrid_mode", isHybridMode)?.apply()
                sp?.edit()?.putBoolean("isSatellite_mode", isSatelliteMode)?.apply()
                true
            }
            R.id.action_hybrid_mode -> {

                item.setChecked(true)
                isHybridMode = true
                mGoogleMap!!.mapType = GoogleMap.MAP_TYPE_HYBRID
                isSatelliteMode = false
                isSchemeMode = false
                sp?.edit()?.putBoolean("isScheme_mode", isSchemeMode)?.apply()
                sp?.edit()?.putBoolean("isHybrid_mode", isHybridMode)?.apply()
                sp?.edit()?.putBoolean("isSatellite_mode", isSatelliteMode)?.apply()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /*private fun updateUI() {
        if (mGoogleMap == null) return

        // also create a LatLngBounds so we can zoom to fit
        val latLngBuilder = LatLngBounds.Builder()
        // iterate over the locations
        val latIndex = mapCursor!!.getColumnIndex("latitude")
        val longIndex = mapCursor!!.getColumnIndex("longitude")
        val ssidIndex = mapCursor!!.getColumnIndex("ssid")
        val bssidIndex = mapCursor!!.getColumnIndex("bssid")
        mapCursor!!.moveToFirst()
        while (!mapCursor!!.isAfterLast) {
            argLatitude = mapCursor!!.getDouble(latIndex)
            argLongitude = mapCursor!!.getDouble(longIndex)
            if (argLatitude == 0.0 || argLongitude == 0.0) {
                if (mapCursor!!.count == 1) return
                mapCursor!!.moveToNext()
            }
            argSsid = mapCursor!!.getString(ssidIndex)
            argBssid = mapCursor!!.getString(bssidIndex)
            val latLng = LatLng(argLatitude, argLongitude)
            val netMarkerOptions = MarkerOptions()
                .position(latLng)
                .title(argSsid)
                .snippet(argBssid)
            mGoogleMap!!.addMarker(netMarkerOptions)
            latLngBuilder.include(latLng)
            mapCursor!!.moveToNext()
        }
        mapCursor!!.close()
        // make the map zoom to show the track, with some padding
        // use the size of the current display in pixels as a bounding box
        val display = this.windowManager.defaultDisplay
        // construct a movement instruction for the map camera
        val movement = CameraUpdateFactory.newLatLngBounds(
            latLngBuilder.build(),
            display.width, display.height, 10
        )
        mGoogleMap!!.moveCamera(movement)
    }*/

    companion object {
        private const val TAG = "MapsActivity"
        const val SATELLITE = "com.netmontools.lookatnet.SATELLITE"
        const val SCHEME = "com.netmontools.lookatnet.SCHEME"
        const val HYBRID = "com.netmontools.lookatnet.HYBRID"
        const val LATITUDE = "com.netmontools.lookatnet.LATITUDE"
        const val LONGITUDE = "com.netmontools.lookatnet.LONGITUDE"
        const val BSSID = "com.netmontools.lookatnet.BSSID"
        const val SSID = "com.netmontools.lookatnet.SSID"
        const val ID = "com.netmontools.lookatnet.ID"
        var mapCursor: Cursor? = null
    }
}