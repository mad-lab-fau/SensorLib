/*
 * Copyright (C) 2019 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */

package de.fau.sensorlib.sensors.logging;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import de.fau.sensorlib.SensorException;
import de.fau.sensorlib.sensors.AbstractSensor;

public class SessionDownloadChecker {

    public enum SessionDownloadFlag {
        DOWNLOAD_SUCCESS,
        NOT_DOWNLOADED,
        DOWNLOAD_FAILED
    }

    private static final String TAG = SessionDownloadChecker.class.getSimpleName();

    private Context mContext;

    /**
     * Directory name where data will be stored on the external storage
     */
    private static final String DIR_NAME = "SensorLibRecordings/NilsPodSessionDownloads";
    private File mPath;
    private String mAbsoluteDirPath = "";

    private AbstractSensor mSensor;
    private ArrayList<Session> mSessionList;
    private ArrayList<SessionDownloadFlag> mAlreadyDownloadedList;
    private ArrayList<String> mFileList;

    public SessionDownloadChecker(Context context) throws SensorException {
        this(context, null);
    }

    public SessionDownloadChecker(Context context, AbstractSensor sensor) throws SensorException {
        mContext = context;
        mSensor = sensor;
        mSessionList = new ArrayList<>();
        mAlreadyDownloadedList = new ArrayList<>();

        if (checkPermissions()) {
            File directory = getDirectory();
            if (directory != null) {
                mPath = directory;
                listFiles();
            }
        } else {
            throw new SensorException(SensorException.SensorExceptionType.permissionsMissing);
        }

        Log.d(TAG, getClass().getSimpleName() + " checking on folder \"" + DIR_NAME + "\"");
    }

    /**
     * Checks if permissions to read and write external storage have been granted by the user
     *
     * @return true if permissions have been granted, false otherwise
     */
    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED;
    }

    private File getRootDirectory() {
        boolean storageWritable;
        String state;
        File root;

        root = mContext.getExternalFilesDir(null);
        // try to write on SD card
        state = Environment.getExternalStorageState();
        switch (state) {
            case Environment.MEDIA_MOUNTED:
                // media readable and writable
                storageWritable = true;
                break;
            case Environment.MEDIA_MOUNTED_READ_ONLY:
                // media only readable
                storageWritable = false;
                Log.e(TAG, "SD card only readable!");
                break;
            default:
                // not readable or writable
                storageWritable = false;
                Log.e(TAG, "SD card not readable and writable!");
                break;
        }

        if (!storageWritable) {
            // try to write on external storage
            root = ContextCompat.getDataDir(mContext);
            if (root == null || !root.canWrite()) {
                Log.e(TAG, "External storage not readable and writable!");
            }
        }
        return root;
    }

    private File getDirectory() {
        boolean fileCreated;
        File directory;
        File root = getRootDirectory();

        if (root != null) {
            try {
                // create directory
                directory = new File(root, DIR_NAME);
                fileCreated = directory.mkdirs();

                if (!fileCreated) {
                    fileCreated = directory.exists();
                    if (!fileCreated) {
                        Log.e(TAG, "Directory could not be created!");
                        return null;
                    } else {
                        mAbsoluteDirPath = directory.getAbsolutePath();
                        Log.i(TAG, "Working directory is " + directory.getAbsolutePath());
                    }
                } else {
                    Log.i(TAG, "Directory created at " + directory.getAbsolutePath());
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception on dir and file create!", e);
                return null;
            }
            return directory;
        } else {
            return null;
        }
    }

    private void listFiles() {
        if (mPath == null) {
            return;
        }
        // list files
        String[] files;
        if (mSensor == null) {
            files = mPath.list();
        } else {
            files = mPath.list((dir, name) -> name.contains(mSensor.getDeviceName()));
        }

        if (files != null) {
            mFileList = new ArrayList<>(Arrays.asList(files));
        } else {
            Toast.makeText(mContext, "Path " + mPath.getAbsolutePath() +
                    " is no valid directory or I/O error occurred!", Toast.LENGTH_SHORT).show();
        }
    }

    public void addSessions(ArrayList<Session> sessionList) {
        mSessionList = sessionList;
        mAlreadyDownloadedList = new ArrayList<>();

        // reload file list
        listFiles();

        for (Session session : mSessionList) {
            mAlreadyDownloadedList.add(SessionDownloadFlag.NOT_DOWNLOADED);
            for (String filename : mFileList) {
                if (filename.contains(session.getSessionStartString())) {
                    File f = new File(getAbsolutePathForSession(session));
                    if (f.length() != session.getSessionSize()) {
                        mAlreadyDownloadedList.set(mSessionList.indexOf(session), SessionDownloadFlag.DOWNLOAD_FAILED);
                    } else {
                        mAlreadyDownloadedList.set(mSessionList.indexOf(session), SessionDownloadFlag.DOWNLOAD_SUCCESS);
                    }
                    break;
                }
            }
        }
    }

    public void setSessionDownloaded(Session session) {
        File f = new File(getAbsolutePathForSession(session));
        if (f.length() == session.getSessionSize()) {
            mAlreadyDownloadedList.set(mSessionList.indexOf(session), SessionDownloadFlag.DOWNLOAD_SUCCESS);
        } else {
            mAlreadyDownloadedList.set(mSessionList.indexOf(session), SessionDownloadFlag.DOWNLOAD_FAILED);
        }
    }

    public void clear() {
        mSessionList.clear();
        mAlreadyDownloadedList.clear();
    }

    public SessionDownloadFlag getSessionDownloadStatus(Session session) {
        return mAlreadyDownloadedList.get(mSessionList.indexOf(session));
    }


    public String getFullDirectoryPath() {
        return mAbsoluteDirPath;
    }

    public String getAbsolutePathForSession(Session session) {
        // reload file list
        listFiles();

        for (String filename : mFileList) {
            if (filename.contains(session.getSessionStartString())) {
                return getFullDirectoryPath() + "/" + filename;
            }
        }

        return null;
    }


    public String getFileNameForSession(Session session) {
        // reload file list
        listFiles();

        if (getSessionDownloadStatus(session) == SessionDownloadFlag.DOWNLOAD_SUCCESS) {
            for (String filename : mFileList) {
                if (filename.contains(session.getSessionStartString())) {
                    return filename;
                }
            }
        }
        // this should never happen
        throw new IllegalArgumentException("Session " + session + " not downloaded yet!");

    }
}
