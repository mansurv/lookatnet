package com.netmontools.lookatnet.ui.local;

import android.app.Application;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Environment;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.netmontools.lookatnet.App;
import com.netmontools.lookatnet.R;
import com.netmontools.lookatnet.ui.local.model.Folder;
import com.netmontools.lookatnet.ui.remote.model.RemoteFolder;
import com.netmontools.lookatnet.utils.SimpleUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class  LocalRepository {
    private String TAG = "LocalRepository";
    private static LiveData<List<Folder>> allPoints;
    private static MutableLiveData<List<Folder>> liveData;

    public static ArrayList<Folder> folders = new ArrayList<Folder>();
    public static ArrayList<RemoteFolder> remoteFolders = new ArrayList<RemoteFolder>();

    public static Drawable host_image, folder_image, file_image;
    public static String rootPath, previousPath, remoteRootPath, remotePreviousPath, remoteCurrentPath;

    public LocalRepository(Application application) {
        liveData = new MutableLiveData<>();
        host_image = ContextCompat.getDrawable(application, R.drawable.ic_desktop_windows_black_24dp);
        folder_image = ContextCompat.getDrawable(application, R.drawable.baseline_folder_yellow_24);
        file_image = ContextCompat.getDrawable(application, R.drawable.ic_file);

        new PopulateDbAsyncTask().execute();

        try {
            liveData.setValue(folders);
        } catch (NullPointerException npe) {
            npe.printStackTrace();
        }
        allPoints = liveData;
    }

    public void delete(Folder point) {
        new DeletePointAsyncTask().execute(point);
    }

    public void update(Folder point) {
        new UpdateListAsyncTask().execute(point);
    }

    public void check(Folder point) {
        new CheckPointAsyncTask().execute(point);
    }

    public LiveData<List<Folder>> getAll() {
        return allPoints;
    }

    private static class DeletePointAsyncTask extends AsyncTask<Folder, Void, Void> {
        @Override
        protected Void doInBackground(Folder... points) {

            return null;
        }
    }

    public static void imageSelector(File file) {
        String ext = SimpleUtils.getExtension(file.getName());
        switch (ext) {
            case "ai" : file_image = ContextCompat.getDrawable(App.instance, R.drawable.ic_ai); break;
            case "avi" : file_image = ContextCompat.getDrawable(App.instance, R.drawable.ic_avi); break;
            case "bmp" : file_image = ContextCompat.getDrawable(App.instance, R.drawable.ic_bmp); break;
            case "cdr" : file_image = ContextCompat.getDrawable(App.instance, R.drawable.ic_cdr); break;
            case "css" : file_image = ContextCompat.getDrawable(App.instance, R.drawable.ic_css); break;
            case "doc" : file_image = ContextCompat.getDrawable(App.instance, R.drawable.ic_doc); break;
            case "eps" : file_image = ContextCompat.getDrawable(App.instance, R.drawable.ic_eps); break;
            case "flv" : file_image = ContextCompat.getDrawable(App.instance, R.drawable.ic_flv); break;
            case "gif" : file_image = ContextCompat.getDrawable(App.instance, R.drawable.ic_gif); break;
            case "htm" : file_image = ContextCompat.getDrawable(App.instance, R.drawable.ic_html);
            case "html" : file_image = ContextCompat.getDrawable(App.instance, R.drawable.ic_html); break;
            case "iso" : file_image = ContextCompat.getDrawable(App.instance, R.drawable.ic_iso); break;
            case "js" : file_image = ContextCompat.getDrawable(App.instance, R.drawable.ic_js); break;
            case "jpg" : file_image = ContextCompat.getDrawable(App.instance, R.drawable.ic_jpg); break;
            case "mov" : file_image = ContextCompat.getDrawable(App.instance, R.drawable.ic_mov); break;
            case "mp3" : file_image = ContextCompat.getDrawable(App.instance, R.drawable.ic_mp3); break;
            case "mpg" : file_image = ContextCompat.getDrawable(App.instance, R.drawable.ic_mpg); break;
            case "pdf" : file_image = ContextCompat.getDrawable(App.instance, R.drawable.ic_pdf); break;
            case "php" : file_image = ContextCompat.getDrawable(App.instance, R.drawable.ic_php); break;
            case "png" : file_image = ContextCompat.getDrawable(App.instance, R.drawable.ic_png); break;
            case "ppt" : file_image = ContextCompat.getDrawable(App.instance, R.drawable.ic_ppt); break;
            case "ps" : file_image = ContextCompat.getDrawable(App.instance, R.drawable.ic_ps); break;
            case "psd" : file_image = ContextCompat.getDrawable(App.instance, R.drawable.ic_psd); break;
            case "raw" : file_image = ContextCompat.getDrawable(App.instance, R.drawable.ic_raw); break;
            case "svg" : file_image = ContextCompat.getDrawable(App.instance, R.drawable.ic_svg); break;
            case "tiff" : file_image = ContextCompat.getDrawable(App.instance, R.drawable.ic_tif);
            case "tif" : file_image = ContextCompat.getDrawable(App.instance, R.drawable.ic_tif); break;
            case "txt" : file_image = ContextCompat.getDrawable(App.instance, R.drawable.ic_txt); break;
            case "xls" : file_image = ContextCompat.getDrawable(App.instance, R.drawable.ic_xls); break;
            case "xml" : file_image = ContextCompat.getDrawable(App.instance, R.drawable.ic_xml); break;
            case "zip" : file_image = ContextCompat.getDrawable(App.instance, R.drawable.ic_zip); break;
            default : file_image = ContextCompat.getDrawable(App.instance, R.drawable.ic_file); break;
        }
    }

    private static class UpdateListAsyncTask extends AsyncTask<Folder, Void, Void> {
        @Override
        protected void onPreExecute() {
            folders.clear();
        }
        @Override
        protected Void doInBackground(Folder... points) {

            try {
                Folder fd, dir;
                File file = null;
                if (points[0].getPath() != null) {
                    file = new File(points[0].getPath());
                }
                if (file != null) {
                    previousPath = file.getPath();
                    if (file.exists()) {
                        if (file.isDirectory()) {
                            dir = new Folder();
                            dir.setName(file.getName());
                            dir.setPath(file.getPath());
                            dir.folders = new ArrayList<Folder>();

                            for (File f : Objects.requireNonNull(file.listFiles())) {
                                if (f.exists()) {
                                    fd = new Folder();
                                    fd.setName(f.getName());
                                    fd.setPath(f.getPath());
                                    if (f.isDirectory()) {
                                        fd.isFile = false;
                                        fd.setSize(SimpleUtils.getDirectorySize(f));
                                        fd.setImage(folder_image);
                                        fd.isImage = false;
                                        fd.isVideo = false;
                                    } else {
                                        fd.isFile = true;
                                        fd.setSize(f.length());
                                        imageSelector(f);
                                        fd.setImage(file_image);
                                        String ext = SimpleUtils.getExtension(f.getName());
                                        if(ext.equalsIgnoreCase("jpg") ||
                                                ext.equalsIgnoreCase("jpeg") ||
                                                ext.equalsIgnoreCase("png") ||
                                                ext.equalsIgnoreCase("webp") ||
                                                ext.equalsIgnoreCase("bmp")) {
                                            fd.isImage = true;
                                            fd.isVideo = false;
                                        } else if (ext.equalsIgnoreCase("mp4") ||
                                                ext.equalsIgnoreCase("avi") ||
                                                ext.equalsIgnoreCase("mkv")) {
                                            fd.isImage = false;
                                            fd.isVideo = true;
                                        } else {
                                            fd.isImage = false;
                                            fd.isVideo = false;
                                        }
                                    }
                                    folders.add(fd);
                                    dir.addFolder(fd);
                                }
                            }
                        }
                    }
                }
            } catch (NullPointerException npe) {
                npe.printStackTrace();
                npe.getMessage();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            liveData.setValue(folders);
            allPoints = liveData;
        }
    }

    private static class CheckPointAsyncTask extends AsyncTask<Folder, Void, Void> {
        @Override
        protected Void doInBackground(Folder... points) {
            if (points[0].isChecked) {
                points[0].isChecked = false;
            } else {
                points[0].isChecked = true;
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            liveData.setValue(App.folders);
            allPoints = liveData;
        }
    }

    private static class PopulateDbAsyncTask extends AsyncTask<Void, Void, String> {

        @Override
        protected void onPreExecute() {
            folders.clear();
        }
        @Override
        protected String doInBackground(Void... voids) {

            try {
                Folder fd, dir;
                File file = new File(Environment.getExternalStorageDirectory().getPath());
                if (file.exists()) {
                    rootPath = file.getPath();
                    dir = new Folder();
                    dir.setName(file.getName());
                    dir.setPath(file.getPath());
                    dir.folders = new ArrayList<Folder>();

                    for (File f : Objects.requireNonNull(file.listFiles())) {
                        if (f.exists()) {
                            fd = new Folder();
                            if (f.isDirectory()) {
                                App.imageSelector(f);
                                fd.setImage(App.folder_image);
                                fd.setName(f.getName());
                                fd.setPath(f.getPath());
                                fd.isFile = false;
                                fd.isChecked = false;
                                fd.isImage = false;
                                fd.isVideo = false;
                                //fd.setSize(SimpleUtils.getDirectorySize(f));
                                fd.setSize(0L);
                                folders.add(fd);
                                dir.folders.add(fd);
                            }
                        }
                    }
                    for (File f : Objects.requireNonNull(file.listFiles())) {
                        if (f.exists()) {
                            fd = new Folder();
                            if (f.isFile()) {
                                fd.setName(f.getName());
                                fd.setPath(f.getPath());
                                fd.isFile = true;
                                fd.isChecked = false;
                                fd.setSize(f.length());
                                App.imageSelector(f);
                                fd.setImage(App.file_image);

                                    /*String ext = SimpleUtils.getExtension(f.getName());
                                    if(ext.equalsIgnoreCase("jpg") ||
                                            ext.equalsIgnoreCase("png") ||
                                            ext.equalsIgnoreCase("webp") ||
                                            ext.equalsIgnoreCase("bmp")) {
                                        fd.isImage = true;
                                        fd.isVideo = false;
                                    } else if (ext.equalsIgnoreCase("mp4") ||
                                            ext.equalsIgnoreCase("avi") ||
                                            ext.equalsIgnoreCase("mkv")) {
                                        fd.isImage = false;
                                        fd.isVideo = true;
                                    } else {
                                        fd.isImage = false;
                                        fd.isVideo = false;
                                    }*/
                                folders.add(fd);
                                dir.folders.add(fd);
                            }
                        }
                    }
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
            }//2 catch
            return "";
        }//doInBackground
    }
}
