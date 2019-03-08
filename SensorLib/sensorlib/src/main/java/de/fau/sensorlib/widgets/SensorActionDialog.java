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

import org.apache.commons.lang3.text.WordUtils;

import java.util.EnumSet;
import java.util.Objects;

import de.fau.sensorlib.Constants;
import de.fau.sensorlib.R;
import de.fau.sensorlib.sensors.AbstractSensor;
import de.fau.sensorlib.sensors.Loggable;
import de.fau.sensorlib.sensors.Resettable;

public class SensorActionDialog extends DialogFragment implements AdapterView.OnItemClickListener {

    private static final String TAG = SensorActionDialog.class.getSimpleName();

    public enum SensorAction {
        CONFIGURE,
        RESET,
        START_LOGGING,
        STOP_LOGGING,
        CLEAR_FLASH,
        SENSOR_INFO;

        @Override
        public String toString() {
            return WordUtils.capitalizeFully(name().replace('_', ' '));
        }
    }

    private ListView mListView;
    private AbstractSensor mSensor;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.widget_sensor_action_dialog, container);

        mListView = rootView.findViewById(R.id.list_view);

        mListView.setAdapter(new ArrayAdapter<>(Objects.requireNonNull(getActivity()), android.R.layout.simple_expandable_list_item_1, SensorAction.values()));
        mListView.setOnItemClickListener(this);

        Bundle bundle = getArguments();
        if (bundle != null) {
            mSensor = (AbstractSensor) bundle.getSerializable(Constants.KEY_SENSOR);
        }

        mListView.post(new Runnable() {
            @Override
            public void run() {
                switch (Objects.requireNonNull(mSensor).getState()) {
                    case LOGGING:
                        // disable Configure Start Logging and Clear Flash
                        for (SensorAction action : EnumSet.of(SensorAction.CONFIGURE, SensorAction.START_LOGGING, SensorAction.CLEAR_FLASH)) {
                            mListView.getChildAt(action.ordinal()).setEnabled(false);
                        }
                        break;
                    case CONNECTED:
                        // disable Stop Logging
                        for (SensorAction action : EnumSet.of(SensorAction.STOP_LOGGING)) {
                            mListView.getChildAt(action.ordinal()).setEnabled(false);
                        }
                        break;
                }

                if (!(mSensor instanceof Loggable)) {
                    for (SensorAction action : EnumSet.of(SensorAction.START_LOGGING, SensorAction.STOP_LOGGING, SensorAction.CLEAR_FLASH)) {
                        mListView.getChildAt(action.ordinal()).setEnabled(false);
                    }
                }

                if (!(mSensor instanceof Resettable)) {
                    for (SensorAction action : EnumSet.of(SensorAction.RESET)) {
                        mListView.getChildAt(action.ordinal()).setEnabled(false);
                    }
                }
            }
        });

        return rootView;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        switch (mSensor.getState()) {
            case CONNECTED:
                for (SensorAction action : EnumSet.of(SensorAction.STOP_LOGGING)) {
                    if (position == action.ordinal()) {
                        return;
                    }
                }
                break;
            case LOGGING:
                for (SensorAction action : EnumSet.of(SensorAction.CONFIGURE, SensorAction.START_LOGGING, SensorAction.CLEAR_FLASH, SensorAction.RESET)) {
                    if (position == action.ordinal()) {
                        return;
                    }
                }
                break;
        }

        SensorAction action = SensorAction.values()[position];
        switch (action) {
            case CONFIGURE:
                dismiss();
                return;
            case RESET:
                if (mSensor instanceof Resettable) {
                    ((Resettable) mSensor).reset();
                } else {
                    return;
                }
                break;
            case START_LOGGING:
                if (mSensor instanceof Loggable) {
                    ((Loggable) mSensor).startLogging();
                } else {
                    return;
                }
                break;
            case STOP_LOGGING:
                if (mSensor instanceof Loggable) {
                    ((Loggable) mSensor).stopLogging();
                } else {
                    return;
                }
                break;
            case CLEAR_FLASH:
                if (mSensor instanceof Loggable) {
                    ((Loggable) mSensor).clearSessions();
                } else {
                    return;
                }
                break;
            case SENSOR_INFO:
                showSensorInfoDialog();
                dismiss();
                return;
        }
        Toast.makeText(getActivity(), SensorAction.values()[position] + " on " + mSensor.getDeviceName(), Toast.LENGTH_SHORT).show();
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
