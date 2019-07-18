/*
 * Copyright (C) 2019 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */

package de.fau.sensorlib.widgets;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import java.io.File;

import de.fau.sensorlib.R;

public class RecorderShareDialog extends AlertDialog {

    private static final String TAG = RecorderShareDialog.class.getSimpleName();


    public RecorderShareDialog(@NonNull Context context, final String absolutePath, final String filename) {
        super(context);

        setTitle("Share recording?");
        setMessage("Recording " + filename + " finished!");
        setCancelable(false);
        setButton(BUTTON_NEGATIVE, getContext().getResources().getString(R.string.share), (dialog, which) -> {
            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
            sharingIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri uri = GenericFileProvider.getUriForFile(getContext(),
                    getContext().getApplicationContext().getPackageName() +
                            ".de.fau.sensorlib.sensors.logging.provider",
                    new File(absolutePath));
            sharingIntent.setType("application/octet-stream");
            sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, filename);
            getContext().startActivity(Intent.createChooser(sharingIntent, "Share Session via..."));
        });
        setButton(BUTTON_POSITIVE, getContext().getResources().getString(R.string.cancel), (dialog, which) -> dismiss());

        show();
    }

}
