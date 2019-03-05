/*
 * Copyright (C) 2019 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */

package de.fau.sensorlib.widgets;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.Objects;

import de.fau.sensorlib.Constants;
import de.fau.sensorlib.R;
import de.fau.sensorlib.sensors.AbstractSensor;
import de.fau.sensorlib.sensors.Loggable;

public class SensorActionDialog extends DialogFragment implements AdapterView.OnItemClickListener {

    private static final String TAG = SensorActionDialog.class.getSimpleName();

    // TODO hardcoded for now => find better solution
    private String[] values = new String[]{"Configure", "Start Logging", "Stop Logging", "Clear Flash", "Sensor Info"};

    private ListView mListView;
    private AbstractSensor mSensor;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.widget_sensor_action_dialog, container);

        mListView = rootView.findViewById(R.id.list_view);
        mListView.setAdapter(new ArrayAdapter<>(Objects.requireNonNull(getActivity()), android.R.layout.simple_expandable_list_item_1, values));
        mListView.setOnItemClickListener(this);
        mListView.post(new Runnable() {
            @Override
            public void run() {
                switch (Objects.requireNonNull(mSensor).getState()) {
                    case LOGGING:
                        // disable Start Logging, Clear Flash and Configure
                        mListView.getChildAt(1).setEnabled(false);
                        mListView.getChildAt(3).setEnabled(false);
                        mListView.getChildAt(0).setEnabled(false);
                        break;
                    case CONNECTED:
                        // disable Stop Logging
                        mListView.getChildAt(2).setEnabled(false);
                        break;

                }
            }
        });

        Bundle bundle = getArguments();
        if (bundle != null) {
            mSensor = (AbstractSensor) bundle.getSerializable(Constants.KEY_SENSOR);
        }

        return rootView;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        switch (mSensor.getState()) {
            case CONNECTED:
                if (position == 2) {
                    return;
                }
                break;
            case LOGGING:
                if (position == 0 || position == 1 || position == 3) {
                    return;
                }
                break;
        }

        switch (position) {
            case 1:
                if (mSensor instanceof Loggable) {
                    ((Loggable) mSensor).startLogging();
                }
                break;
            case 2:
                if (mSensor instanceof Loggable) {
                    ((Loggable) mSensor).stopLogging();
                }
                break;
            case 3:
                if (mSensor instanceof Loggable) {
                    ((Loggable) mSensor).clearSessions();
                }
                break;
            case 0:
                dismiss();
                return;
            case 4:
                showSensorInfoDialog();
                dismiss();
                return;
        }
        Toast.makeText(getActivity(), values[position] + " on " + mSensor.getDeviceName(), Toast.LENGTH_SHORT).show();
        dismiss();
    }

    private void showSensorInfoDialog() {
        Log.d(TAG, mSensor.toString());
        Bundle bundle = new Bundle();
        bundle.putSerializable(Constants.KEY_SENSOR, mSensor);

        SensorInfoDialog dialog = new SensorInfoDialog();
        dialog.setArguments(bundle);
        FragmentActivity activity = getActivity();
        if (activity != null) {
            FragmentManager fm = activity.getSupportFragmentManager();
            if (fm != null) {
                dialog.show(fm, "sensor_info");
            }
        }
    }
}
