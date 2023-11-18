package com.netmontools.lookatnet.ui.remote.workers;

import static androidx.work.ListenableWorker.Result.failure;
import static androidx.work.ListenableWorker.Result.success;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.netmontools.lookatnet.App;
import com.netmontools.lookatnet.AppDatabase;
import com.netmontools.lookatnet.BuildConfig;
import com.netmontools.lookatnet.ui.remote.model.RemoteModel;
import com.netmontools.lookatnet.ui.remote.model.RemoteModelDao;
import com.netmontools.lookatnet.utils.LogSystem;

public class LoginWorker extends Worker {

    public LoginWorker(Context context, WorkerParameters params) {
        super(context, params);
    }

    private static final String TAG = "LoginWorker";
    public AppDatabase db;

    @SuppressLint("WrongThread")
    @NonNull
    @Override
    public Result doWork() {
        String address = getInputData().getString("address");
        String bssid = getInputData().getString("bssid");
        String currentUser = getInputData().getString("user");
        String currentPass = getInputData().getString("pass");
        String user, pass;
        db = App.getInstance().getDatabase();
        RemoteModelDao remoteDao = db.remoteModelDao();
        RemoteModel remote;
        remote = remoteDao.getByAddrAndBssid(address, bssid);
        if  (remote != null) {
            if (currentUser.equalsIgnoreCase("") || currentPass.equalsIgnoreCase("")) {
                user = remote.getLogin();
                if(user == null)  user = "";
                pass = remote.getPass();
                if(pass == null) pass = "";


                Data outputData = new Data.Builder()
                        .putString("user", user)
                        .putString("pass", pass)
                        .build();

                return success(outputData);
            } else {
                remote.setLogin(currentUser);
                remote.setPass(currentPass);
                remoteDao.update(remote);
                Data outputData = new Data.Builder()
                        .putString("user", currentUser)
                        .putString("pass", currentPass)
                        .build();

                return success(outputData);
            }
        } else {
            if (BuildConfig.USE_LOG) {
                LogSystem.logInFile(TAG, "\r\n Remote do not init!");
            }
            return failure();
        }

        //return success();
    }
}
