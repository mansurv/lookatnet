package com.netmontools.lookatnet.utils

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.netmontools.lookatnet.App
import com.netmontools.lookatnet.R
import com.netmontools.lookatnet.ui.point.worker.AddPointWorker
import com.netmontools.lookatnet.ui.point.worker.EditPointWorker

class EditPointFragment  : DialogFragment(), DialogInterface.OnClickListener {
    lateinit var v: View
    private lateinit var longitudeBox: EditText
    private lateinit var latitudeBox: EditText
    private lateinit var titleBox: EditText
    private lateinit var snipBox: EditText
    private var longitude: Double = 0.0
    private var latitude: Double = 0.0
    private var id: Long = 0

            @SuppressLint("CutPasteId")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        v = requireActivity().layoutInflater
            .inflate(R.layout.fragment_add_point, null)
        titleBox = v.findViewById(R.id.title) as EditText
        //title = requireArguments().getString(TITLE).toString()
        titleBox.setText(requireArguments().getString(TITLE).toString())
        //snip = requireArguments().getString(SNIPPED).toString()
        snipBox = v.findViewById(R.id.snip) as EditText
        snipBox.setText(requireArguments().getString(SNIPPED).toString())
        longitudeBox = v.findViewById(R.id.lon) as EditText
        latitudeBox = v.findViewById(R.id.lat) as EditText
        longitude = requireArguments().getDouble(LONGITUDE)
        latitude = requireArguments().getDouble(LATITUDE)
        longitudeBox.setText(requireArguments().getDouble(LONGITUDE).toString())
        latitudeBox.setText(requireArguments().getDouble(LATITUDE).toString())
                id = requireArguments().getLong(ID,0L)
        return AlertDialog.Builder(requireActivity())
            .setView(v)
            .setTitle(R.string.dialog_edit_point_title)
            .setPositiveButton(android.R.string.ok, this)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        val editPointWorkRequest: OneTimeWorkRequest
        val myData = Data.Builder()
            .putLong("id",id)
            .putString("title", titleBox.text.toString())
            .putString("snip", snipBox.text.toString())
            .putDouble("latitude", latitude)
            .putDouble("longitude", longitude)
            .build()
        editPointWorkRequest = OneTimeWorkRequest.Builder(EditPointWorker::class.java)
            .setInputData(myData)
            .addTag("myEditPointTag")
            .build()
        val wm = WorkManager.getInstance(App.instance)
        wm.enqueue(editPointWorkRequest)
    }

    companion object {
        private const val ID = "current_id"
        private const val LONGITUDE = "current_longitude"
        private const val LATITUDE = "current_latitude"
        private const val TITLE = "current_title"
        private const val SNIPPED = "current_snipped"
        const val TAG = "confirm edit_point"
        @JvmStatic
        fun newInstance( id: Long, longitude: Double, latitude: Double, title: String, snipped: String): EditPointFragment {
            val args = Bundle()
            args.putLong(ID, id)
            args.putDouble(LONGITUDE, longitude)
            args.putDouble(LATITUDE, latitude)
            args.putString(TITLE, title )
            args.putString(SNIPPED, snipped)
            val fragment = EditPointFragment()
            fragment.arguments = args
            return fragment
        }
    }
}