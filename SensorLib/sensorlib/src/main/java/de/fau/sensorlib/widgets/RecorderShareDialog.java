/*
 * Copyright (C) 2019 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */

package de.fau.sensorlib.widgets;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;

import java.io.File;

import de.fau.sensorlib.R;
import de.fau.sensorlib.sensors.AbstractSensor;

public class RecorderShareDialog extends AlertDialog {

    private static final String TAG = RecorderShareDialog.class.getSimpleName();

    private AbstractSensor mSensor;
    private String mFilename;


    public RecorderShareDialog(@NonNull Context context, AbstractSensor sensor, final String absolutePath, final String filename) {
        super(context);

        mSensor = sensor;
        mFilename = filename;

        setTitle("Share recording?");
        setMessage("Recording " + filename + " finished!");
        setCancelable(false);
        setButton(BUTTON_NEGATIVE, getContext().getResources().getString(R.string.share), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Uri uri = GenericFileProvider.getUriForFile(getContext(),
                        getContext().getApplicationContext().getPackageName() +
                                ".de.fau.sensorlib.sensors.logging.provider",
                        new File(absolutePath));
                sharingIntent.setType("application/octet-stream");
                sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);
                sharingIntent.putExtra(Intent.EXTRA_SUBJECT, filename);
                getContext().startActivity(Intent.createChooser(sharingIntent, "Share Session via..."));
            }
        });
        setButton(BUTTON_POSITIVE, getContext().getResources().getString(R.string.cancel), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismiss();
            }
        });


        show();
    }

}
