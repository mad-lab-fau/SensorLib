/*
 * Copyright (C) 2019 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.sensors.logging;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.Nullable;

public class SessionDownloadService extends Service {

    private static final String TAG = SessionDownloadService.class.getSimpleName();

    private IBinder mBinder = new SessionDownloadServiceBinder();


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * Inner class representing the Binder between {@link SessionDownloadService}
     * and {@link android.app.Activity}
     */
    public class SessionDownloadServiceBinder extends Binder {

        public SessionDownloadServiceBinder getService() {
            return SessionDownloadServiceBinder.this;
        }
    }
}
