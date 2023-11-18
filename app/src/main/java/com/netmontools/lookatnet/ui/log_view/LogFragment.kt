package com.netmontools.lookatnet.ui.log_view

import android.os.Bundle
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.netmontools.lookatnet.App
import com.netmontools.lookatnet.MainViewModel
import com.netmontools.lookatnet.R
import java.io.File
import java.io.InputStream
import java.util.concurrent.TimeUnit

class LogFragment : Fragment() {
    private var logViewModel: LogViewModel? = null
    private val mMenuItemRead: MenuItem? = null
    var textView: TextView? = null
    lateinit var appCompatActivity: AppCompatActivity
    lateinit var appBar: ActionBar
    private lateinit var mainViewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appCompatActivity = activity as AppCompatActivity
        appBar = appCompatActivity.supportActionBar as ActionBar
        appBar.setTitle("Log file")
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        logViewModel = ViewModelProviders.of(this).get(LogViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_log, container, false)
        textView = root.findViewById(R.id.text_log)

        openFile("log.dat")
        /*logViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });*/return root
    }

    override fun onResume() {
        appBar.setTitle("Log file")
        super.onResume()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity?.run {
            mainViewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        } ?: throw Throwable("invalid activity")
        mainViewModel.updateActionBarTitle("Remote files")
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.activity_log, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        try {
            TimeUnit.MILLISECONDS.sleep(50)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        return when (item.itemId) {
            android.R.id.home ->                 //onBackPressed();
                true
            R.id.action_read -> {

                openFile("log1.dat")
                appBar.setTitle("Previous log")
                true
            }
            else -> false
        }
    }

    private fun openFile(fileName: String) {
        try {
            val builder = StringBuilder()
            val inputStream: InputStream = File(App.instance.applicationContext.filesDir.toString() + "/log.dat").inputStream()
            val inputString = inputStream.bufferedReader().use { it.readText() }

            builder.append(inputString)
            inputStream.close()
            textView?.setText(builder.toString())

        } catch (t: Throwable) {
            Toast.makeText(
                App.instance,
                "Exception: $t", Toast.LENGTH_LONG
            ).show()
        }
    }
}