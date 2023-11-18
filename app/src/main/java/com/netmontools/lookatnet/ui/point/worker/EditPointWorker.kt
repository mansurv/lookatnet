package com.netmontools.lookatnet.ui.point.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.netmontools.lookatnet.App
import com.netmontools.lookatnet.ui.point.model.DataModel

class EditPointWorker (context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): ListenableWorker.Result {
        val currentSnip = inputData.getString("snip")
        val currentTitle = inputData.getString("title")
        val currentId = inputData.getLong("id",0L)
        val currentLatitude = inputData.getDouble("latitude", 0.0)
        val currentLongitude = inputData.getDouble("longitude", 0.0)

        val db = App.instance.database
        val pointDao = db.dataModelDao()
        val old_point = pointDao.getById(currentId)
        val point = DataModel()
        point.bssid = currentSnip.toString()
        point.name = currentTitle.toString()
        point.lat = currentLatitude
        point.lon = currentLongitude

        pointDao.delete(old_point)
        pointDao.insert(point)

        return ListenableWorker.Result.success()
    }

    companion object {
        const val TAG = "EditPointWorker"
    }
}