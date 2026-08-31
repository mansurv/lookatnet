package com.netmontools.lookatnet.ui.remote.repository

import android.app.Application
import androidx.lifecycle.LiveData
import com.netmontools.lookatnet.App
import com.netmontools.lookatnet.ui.remote.model.RemoteModel
import com.netmontools.lookatnet.ui.remote.model.RemoteModelDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RemoteRepository(application: Application) {
    private val remoteDao: RemoteModelDao
    val all: LiveData<List<RemoteModel>>
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    suspend fun insert(point: RemoteModel?) = withContext(ioDispatcher) {
        point?.let { remoteDao.insert(it) }
    }

    suspend fun update(point: RemoteModel?) = withContext(ioDispatcher) {
        point?.let { remoteDao.update(it) }
    }

    suspend fun delete(point: RemoteModel?) = withContext(ioDispatcher) {
        point?.let { remoteDao.delete(it) }
    }


    init {
        val db = App.instance.database
        remoteDao = db.remoteModelDao()
        all = remoteDao.all
    }
}
