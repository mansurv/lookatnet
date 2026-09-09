package com.netmontools.lookatnet

import android.Manifest
import android.annotation.TargetApi
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.telephony.TelephonyManager
import android.view.Menu
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.preference.PreferenceManager
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.netmontools.lookatnet.utils.LogSystem
import com.netmontools.lookatnet.utils.SimpleUtils
import java.io.File
import java.io.FileFilter
import java.util.regex.Pattern

class MainActivity : AppCompatActivity() {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var viewModel: MainViewModel
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navView = findViewById<BottomNavigationView>(R.id.nav_view)
        
        // Получаем навигационный контроллер
        navController = this.findNavController(R.id.nav_host_fragment)
        
        // Настройка BottomNavigationView с навигационным контроллером
        NavigationUI.setupWithNavController(navView, navController)

        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkStoragePermission()
            checkLocationPermission()
            checkStatePermission()
        }

        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        viewModel.title.observe(this, {
            supportActionBar?.title = it
        })
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        if (savedInstanceState == null) {
            SimpleUtils.renameTarget(
                getApplicationContext().getFilesDir().toString() + "/log.dat",
                "log1.dat"
            )

            var numCpuCores = sp.getInt("cpu_cores", 0)
            if (numCpuCores == 0) {
                numCpuCores = numCores
                sp.edit().putString("model", Build.MODEL).apply()
                sp.edit().putInt("version", Build.VERSION.SDK_INT).apply()
                sp.edit().putString("proc", Build.HARDWARE).apply()
                sp.edit().putInt("cpu_cores", numCpuCores).apply()
            }
            LogSystem.logInFile(
                TAG, """
                  Device: ${sp.getString("model", "")}     
                  SDK version: ${sp.getInt("version", 0)}
                  Processor: ${sp.getString("proc", "")}
                  Cpu cores: $numCpuCores"""
            )

            /*if (telephonyManager != null) {
                if (BuildConfig.USE_LOG) {
                    LogSystem.logInFile(
                        TAG, """
                     Network type: ${getNetworkType(this)}
                     Phone type: ${telephonyManager!!.phoneType}"""
                    )
                }
            }*/
            if (checkPlayServices()) {
                LogSystem.logInFile(TAG, "\r\n Google Play Service availiable ")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED
        ) {
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_STORAGE_PERMISSION
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkStatePermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_PHONE_STATE),
                REQUEST_STATE_PERMISSION
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_STORAGE_PERMISSION -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        LogSystem.logInFile(TAG, "\r\n  Storage permission granted")

                }
            }
            REQUEST_LOCATION_PERMISSION -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                        LogSystem.logInFile(TAG, "\r\n  Location permission granted")

                }
            }
            REQUEST_STATE_PERMISSION -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                        LogSystem.logInFile(TAG, "\r\n  State permission granted")

                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar если это есть.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    // Правильная обработка кнопки Back для двухуровневой навигации
    override fun onBackPressed() {
        // Проверяем, можем ли мы вернуться назад в навигационном графе (между вкладками)
        if (navController.popBackStack()) {
            return  // Есть предыдущая вкладка - возвращаемся к ней
        }
        
        // Если мы в корне навигации (на nav_local) - закрываем приложение
        super.onBackPressed()
    }

    // Этот метод вызывается BottomNavigationView при нажатии на пункты меню
    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun checkPlayServices(): Boolean {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = apiAvailability.isGooglePlayServicesAvailable(this)
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                    ?.show()
            } else {

                    LogSystem.logInFile(TAG, "\r\n This device is not supported")
            }
            return false
        }
        return true
    }

    private val numCores: Int
        get() {
            class CpuFilter : FileFilter {
                override fun accept(pathname: File): Boolean {
                    return if (Pattern.matches("cpu[0-9]", pathname.name)) {
                        true
                    } else false
                }
            }
            return try {
                val dir = File("/sys/devices/system/cpu/")
                val files = dir.listFiles(CpuFilter())
                files!!.size
            } catch (e: Exception) {
                e.printStackTrace()
                1
            }
        }

    // Удалён метод onKeyDown - он не нужен для современной навигации

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_STORAGE_PERMISSION = 101
        private const val REQUEST_LOCATION_PERMISSION = 102
        private const val REQUEST_STATE_PERMISSION = 103
        private const val PLAY_SERVICES_RESOLUTION_REQUEST = 9000

        //lateinit var extSDPath: Array<String?>
        //lateinit var remSDPath: Array<String?>

        //var isCancellable = false
        var telephonyManager: TelephonyManager? = null
        var locationManager: LocationManager? = null
        var database: AppDatabase? = null
        fun getNetworkType(context: Context?): String {
            if (telephonyManager != null) {
                if (ActivityCompat.checkSelfPermission(App.instance, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                }
                val networkType = telephonyManager!!.networkType
                when (networkType) {
                    TelephonyManager.NETWORK_TYPE_GSM -> return "GSM"
                    TelephonyManager.NETWORK_TYPE_1xRTT -> return "1xRTT (2G)"
                    TelephonyManager.NETWORK_TYPE_CDMA -> return "CDMA (2G)"
                    TelephonyManager.NETWORK_TYPE_EDGE -> return "EDGE (2.75G)"
                    TelephonyManager.NETWORK_TYPE_EVDO_0 -> return "EVDO rev. 0 (3G)"
                    TelephonyManager.NETWORK_TYPE_EVDO_A -> return "EVDO rev. A (3G)"
                    TelephonyManager.NETWORK_TYPE_EVDO_B -> return "EVDO rev. B (3G)"
                    TelephonyManager.NETWORK_TYPE_GPRS -> return "GPRS (2.5G)"
                    TelephonyManager.NETWORK_TYPE_HSDPA -> return "HSDPA (3G+)"
                    TelephonyManager.NETWORK_TYPE_HSPA -> return "HSPA (3G+)"
                    TelephonyManager.NETWORK_TYPE_EHRPD -> return "HSPA+ (3G++)"
                    TelephonyManager.NETWORK_TYPE_HSPAP -> return "HSPA+ (3G++)"
                    TelephonyManager.NETWORK_TYPE_TD_SCDMA -> return "TD_SCDMA (3G++)"
                    TelephonyManager.NETWORK_TYPE_HSUPA -> return "HSUPA (3G+)"
                    TelephonyManager.NETWORK_TYPE_IDEN -> return "iDen (2G)"
                    TelephonyManager.NETWORK_TYPE_LTE -> return "LTE (4G)"
                    TelephonyManager.NETWORK_TYPE_UMTS -> return "UMTS (3G)"
                    TelephonyManager.NETWORK_TYPE_UNKNOWN -> return "Unknown"
                }
                return "New type of network"
            }
            return "Do not access to telephony service"
        }
    }
}