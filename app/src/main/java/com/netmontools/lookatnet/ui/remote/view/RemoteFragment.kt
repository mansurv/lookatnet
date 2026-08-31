package com.netmontools.lookatnet.ui.remote.view

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.netmontools.lookatnet.App
import com.netmontools.lookatnet.R
import com.netmontools.lookatnet.ui.remote.viewmodel.RemoteViewModel
import com.netmontools.lookatnet.ui.remote.workers.RemoteWorker

class RemoteFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var remoteRefreshLayout: SwipeRefreshLayout
    private lateinit var remoteViewModel: RemoteViewModel
    private lateinit var adapter: RemoteAdapter

    private var currentBssid: String? = null
    private var broadcastIP = 0
    private var subnetIP = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        //remoteViewModel.cleanupOrphans()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_remote, container, false)

        remoteViewModel = ViewModelProvider.AndroidViewModelFactory(App.getInstance()).create(RemoteViewModel::class.java)

        remoteRefreshLayout = root.findViewById(R.id.remote_refresh_layout)
        remoteRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )

        recyclerView = root.findViewById(R.id.remote_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.setHasFixedSize(true)
        adapter = RemoteAdapter()
        recyclerView.adapter = adapter

        remoteViewModel.allRemotes.observe(viewLifecycleOwner, Observer { points ->
            adapter.setHosts(points)
        })

        setupSwipeRefresh()
        setupItemTouchHelper()

        return root
    }

    private fun setupSwipeRefresh() {
        remoteRefreshLayout.setOnRefreshListener {
            if (isNetworkConnected()) {
                val myRemoteData = Data.Builder()
                    .putString("bssid", currentBssid)
                    .putInt("subnet", subnetIP)
                    .putInt("broadcast", broadcastIP)
                    .build()

                val scanWorkRequest = OneTimeWorkRequest.Builder(RemoteWorker::class.java)
                    .setInputData(myRemoteData)
                    .addTag("myRemoteTag")
                    .build()

                val wm = WorkManager.getInstance(App.instance)
                wm.enqueue(scanWorkRequest)

                val startTime = System.currentTimeMillis()
                wm.getWorkInfoByIdLiveData(scanWorkRequest.id)
                    .observe(viewLifecycleOwner, Observer { workStatus ->
                        if (workStatus != null && workStatus.state.isFinished) {
                            val endTime = System.currentTimeMillis()
                            val hostsCount = workStatus.outputData.getInt("count", 0)
                            remoteRefreshLayout.isRefreshing = false
                            showScanResultSnackbar(endTime - startTime, hostsCount)
                        }
                    })
            } else {
                remoteRefreshLayout.isRefreshing = false
                Snackbar.make(recyclerView, R.string.cantrefresh, BaseTransientBottomBar.LENGTH_INDEFINITE)
                    .setAction(R.string.action_setup) {
                        startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                    }
                    .show()
            }
        }
    }

    private fun showScanResultSnackbar(traceTimeMs: Long, hostsCount: Int) {
        val (seconds, minutes) = formatDuration(traceTimeMs)
        val s = getString(R.string.found_hosts)
        val s1 = " "

        val message = when {
            minutes > 0 ->
                s + s1 + hostsCount + s1 + getString(R.string.for_time) + s1 +
                    minutes + s1 + getString(R.string.time_min) + s1 +
                    seconds + s1 + getString(R.string.time_s)
            seconds > 0 ->
                s + s1 + hostsCount + s1 + getString(R.string.for_time) + s1 +
                    seconds + s1 + getString(R.string.time_s)
            else ->
                s + s1 + hostsCount + s1 + getString(R.string.for_time) + s1 +
                    traceTimeMs + s1 + getString(R.string.time_ms)
        }

        Snackbar.make(recyclerView, message, BaseTransientBottomBar.LENGTH_LONG).show()
    }

    private fun formatDuration(ms: Long): Pair<Long, Long> {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return seconds to minutes
    }

    private fun setupItemTouchHelper() {
        val callback = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    remoteViewModel.requestDelete(position)
                }
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(recyclerView)
    }

    private fun isNetworkConnected(): Boolean {
        val cm = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cm.getNetworkCapabilities(cm.activeNetwork)
        } else {
            return false
        }

        if (capabilities == null) return false
        if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return false

        val wifiManager = App.instance.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        currentBssid = wifiInfo?.bssid

        val dhcpInfo = wifiManager.dhcpInfo
        if (dhcpInfo != null) {
            var netMask: Int = dhcpInfo.netmask
            val serverIP: Int = dhcpInfo.serverAddress
            val ip = dhcpInfo.ipAddress

            if (serverIP != 0 && netMask == 0) {
                netMask = 16777215 // /24
            }

            if (netMask != 0) {
                subnetIP = ip and netMask
                broadcastIP = subnetIP or netMask.inv()
            }
        }

        return true
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: android.view.MenuInflater) {
        inflater.inflate(R.menu.fragment_remote, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add_new_smb_server -> {
                item.isChecked = !item.isChecked
                true
            }
            R.id.action_edit_smb_server -> {
                item.isChecked = !item.isChecked
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDetach() {
        super.onDetach()
    }
}
