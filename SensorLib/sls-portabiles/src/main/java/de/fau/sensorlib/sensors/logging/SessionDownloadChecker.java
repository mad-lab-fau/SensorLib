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
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import de.fau.sensorlib.SensorException;
import de.fau.sensorlib.sensors.AbstractSensor;

public class SessionDownloadChecker {

    private static final String TAG = SessionDownloadChecker.class.getSimpleName();

    private Context mContext;

    /**
     * Directory name where data will be stored on the external storage
     */
    private String mDirName = "SensorLibRecordings/NilsPodSessionDownloads";

    private boolean mStorageReadable;
    private boolean mDirectoryCreated;

    private AbstractSensor mSensor;
    private ArrayList<Session> mSessionList;
    private ArrayList<Boolean> mAlreadyDownloadedList;
    private ArrayList<String> mFileList;

    public SessionDownloadChecker(Context context, AbstractSensor sensor) throws SensorException {
        mContext = context;
        mSensor = sensor;
        mSessionList = new ArrayList<>();
        mAlreadyDownloadedList = new ArrayList<>();

        if (checkPermissions()) {
            listFiles();
        } else {
            throw new SensorException(SensorException.SensorExceptionType.permissionsMissing);
        }

        Log.d(TAG, getClass().getSimpleName() + " checking on folder \"" + mDirName + "\"");
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

    private void listFiles() {
        String state;
        File root = null;
        File path;

        // try to write on SD card
        state = Environment.getExternalStorageState();
        switch (state) {
            case Environment.MEDIA_MOUNTED:
                // media readable and writable
                // fall through
            case Environment.MEDIA_MOUNTED_READ_ONLY:
                // media only readable
                mStorageReadable = true;
                root = Environment.getExternalStorageDirectory();
                break;
            default:
                // not readable or writable
                mStorageReadable = false;
                Log.e(TAG, "SD card not readable and writable!");
                break;
        }

        if (!mStorageReadable) {
            // try to write on external storage
            root = Environment.getDataDirectory();
            if (root.canRead()) {
                mStorageReadable = true;
            } else {
                Log.e(TAG, "External storage not readable!");
                Toast.makeText(mContext, "External storage not readable!", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        try {
            // create directory
            path = new File(root, mDirName);
            mDirectoryCreated = path.mkdirs();
            if (!mDirectoryCreated) {
                mDirectoryCreated = path.exists();
                if (!mDirectoryCreated) {
                    Log.e(TAG, "Directory could not be created!");
                    return;
                } else {
                    Log.i(TAG, "Working directory is " + path.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception on directory create!", e);
            mDirectoryCreated = false;
            return;
        }

        // list files
        String[] files = path.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.contains(mSensor.getDeviceName());
            }
        });
        mFileList = new ArrayList<>(Arrays.asList(files));

        Log.e(TAG, "file list: " + mFileList);
    }

    public void addSessions(ArrayList<Session> sessionList) {
        mSessionList = sessionList;
        for (Session session : mSessionList) {
            mAlreadyDownloadedList.add(false);
            String dateString = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(session.getStartDate());
            for (String filename : mFileList) {
                if (filename.contains(dateString)) {
                    mAlreadyDownloadedList.set(mSessionList.indexOf(session), true);
                    break;
                }
            }
        }
    }

    public void clear() {
        mSessionList.clear();
        mAlreadyDownloadedList.clear();
    }

    public boolean checkSessionAlreadyDownloaded(Session session) {
        return mAlreadyDownloadedList.get(mSessionList.indexOf(session));
    }
}
