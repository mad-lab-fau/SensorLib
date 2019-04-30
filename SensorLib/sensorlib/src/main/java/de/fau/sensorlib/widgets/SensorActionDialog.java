/*
 * Copyright (C) 2019 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */

package de.fau.sensorlib.widgets;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.apache.commons.lang3.text.WordUtils;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Objects;

import de.fau.sensorlib.Constants;
import de.fau.sensorlib.R;
import de.fau.sensorlib.sensors.AbstractSensor;
import de.fau.sensorlib.sensors.Configurable;
import de.fau.sensorlib.sensors.Erasable;
import de.fau.sensorlib.sensors.Loggable;
import de.fau.sensorlib.sensors.Resettable;

public class SensorActionDialog extends DialogFragment implements SensorConfigListener {

    private static final String TAG = SensorActionDialog.class.getSimpleName();

    public enum SensorAction {
        CONFIGURE,
        DEFAULT_CONFIG,
        RESET,
        START_LOGGING,
        STOP_LOGGING,
        CLEAR_SESSIONS,
        FULL_ERASE,
        SENSOR_INFO;

        @Override
        public String toString() {
            return WordUtils.capitalizeFully(name().replace('_', ' '));
        }
    }

    private Context mContext;
    private RecyclerView mRecyclerView;
    private AbstractSensor mSensor;

    public static class SensorActionViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView mActionTextView;
        private ItemClickListener mListener;

        public SensorActionViewHolder(View itemView, ItemClickListener listener) {
            super(itemView);
            mActionTextView = itemView.findViewById(R.id.tv_sensor_action);
            mListener = listener;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            mListener.onItemClick(v, getAdapterPosition());
        }

        interface ItemClickListener {
            void onItemClick(View view, int position);
        }
    }


    public class SensorActionAdapter extends RecyclerView.Adapter<SensorActionViewHolder> implements SensorActionViewHolder.ItemClickListener {

        private SensorActionAdapter(Context context) {
            mContext = context;
        }

        @NonNull
        @Override
        public SensorActionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View layout = LayoutInflater.from(mContext).inflate(R.layout.item_sensor_action, parent, false);
            return new SensorActionViewHolder(layout, this);
        }

        @Override
        public void onBindViewHolder(@NonNull SensorActionViewHolder holder, int position) {
            holder.mActionTextView.setText(SensorAction.values()[position].toString());
        }

        @Override
        public int getItemCount() {
            return SensorAction.values().length;
        }

        @Override
        public void onItemClick(View view, int position) {
            switch (mSensor.getState()) {
                case CONNECTED:
                    for (SensorAction action : EnumSet.of(SensorAction.STOP_LOGGING)) {
                        if (position == action.ordinal()) {
                            return;
                        }
                    }
                    break;
                case LOGGING:
                    for (SensorAction action : EnumSet.of(SensorAction.CONFIGURE, SensorAction.START_LOGGING, SensorAction.CLEAR_SESSIONS, SensorAction.FULL_ERASE, SensorAction.RESET)) {
                        if (position == action.ordinal()) {
                            return;
                        }
                    }
                    break;
                case STREAMING:
                    for (SensorAction action : EnumSet.of(SensorAction.CONFIGURE, SensorAction.STOP_LOGGING, SensorAction.START_LOGGING, SensorAction.CLEAR_SESSIONS, SensorAction.FULL_ERASE)) {
                        if (position == action.ordinal()) {
                            // TODO Toasts for Arne
                            return;
                        }
                    }
                    break;

            }

            SensorAction action = SensorAction.values()[position];
            switch (action) {
                case CONFIGURE:
                    if (mSensor instanceof Configurable) {
                        showSensorConfigDialog();
                    }
                    return;
                case DEFAULT_CONFIG:
                    if (mSensor instanceof Configurable) {
                        ((Configurable) mSensor).setDefaultConfig();
                    } else {
                        return;
                    }
                    break;
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
                case CLEAR_SESSIONS:
                    if (mSensor instanceof Erasable) {
                        ((Erasable) mSensor).clearData();
                    } else {
                        return;
                    }
                    break;
                case FULL_ERASE:
                    if (mSensor instanceof Erasable) {
                        ((Erasable) mSensor).fullErase();
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
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.widget_sensor_action_dialog, container);

        mContext = getContext();

        mRecyclerView = rootView.findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        mRecyclerView.setAdapter(new SensorActionAdapter(mContext));

        Bundle bundle = getArguments();
        if (bundle != null) {
            mSensor = (AbstractSensor) bundle.getSerializable(Constants.KEY_SENSOR);
        }

        mRecyclerView.post(new Runnable() {
            @Override
            public void run() {
                switch (Objects.requireNonNull(mSensor).getState()) {
                    case LOGGING:
                        // disable Configure Start Logging and Clear Flash
                        for (SensorAction action : EnumSet.of(SensorAction.CONFIGURE, SensorAction.START_LOGGING, SensorAction.CLEAR_SESSIONS, SensorAction.FULL_ERASE)) {
                            setItemDisabled(mRecyclerView.getChildAt(action.ordinal()));
                        }
                        break;
                    case CONNECTED:
                        // disable Stop Logging
                        for (SensorAction action : EnumSet.of(SensorAction.STOP_LOGGING)) {
                            mRecyclerView.getChildAt(action.ordinal()).setEnabled(false);
                            setItemDisabled(mRecyclerView.getChildAt(action.ordinal()));
                        }
                        break;
                    case STREAMING:
                        // disable all Logging actions
                        for (SensorAction action : EnumSet.of(SensorAction.CONFIGURE, SensorAction.STOP_LOGGING, SensorAction.START_LOGGING, SensorAction.CLEAR_SESSIONS, SensorAction.FULL_ERASE)) {
                            setItemDisabled(mRecyclerView.getChildAt(action.ordinal()));
                        }
                        break;
                }

                if (!(mSensor instanceof Configurable)) {
                    for (SensorAction action : EnumSet.of(SensorAction.CONFIGURE, SensorAction.DEFAULT_CONFIG)) {
                        setItemDisabled(mRecyclerView.getChildAt(action.ordinal()));
                    }
                }

                if (!(mSensor instanceof Loggable)) {
                    for (SensorAction action : EnumSet.of(SensorAction.START_LOGGING, SensorAction.STOP_LOGGING, SensorAction.CLEAR_SESSIONS, SensorAction.FULL_ERASE)) {
                        setItemDisabled(mRecyclerView.getChildAt(action.ordinal()));
                    }
                }

                if (!(mSensor instanceof Resettable)) {
                    for (SensorAction action : EnumSet.of(SensorAction.RESET)) {
                        setItemDisabled(mRecyclerView.getChildAt(action.ordinal()));
                    }
                }
            }
        });

        return rootView;
    }

    public void setItemDisabled(View view) {
        view.findViewById(R.id.tv_sensor_action).setEnabled(false);
    }


    @Override
    public void onSensorConfigSelected(HashMap<String, Object> configMap) {
        Log.d(TAG, "onSensorConfigSelected: " + configMap);
        if (mSensor instanceof Configurable) {
            ((Configurable) mSensor).setConfigMap(configMap);
        }
    }


    private void showSensorConfigDialog() {
        Bundle bundle = new Bundle();
        bundle.putSerializable(Constants.KEY_SENSOR_CONFIG, ((Configurable) mSensor).getConfigMap());
        bundle.putSerializable(Constants.KEY_SENSOR_CONFIG_DEFAULT, ((Configurable) mSensor).getCurrentConfigMap());
        bundle.putString(Constants.KEY_SENSOR_NAME, mSensor.getDeviceName());

        SensorConfigDialog dialog = new SensorConfigDialog();
        dialog.setSensorConfigListener(this);
        dialog.setArguments(bundle);

        FragmentActivity activity = getActivity();
        if (activity != null) {
            FragmentManager fm = activity.getSupportFragmentManager();
            dialog.show(fm, "sensor_config");
        }
    }


    private void showSensorInfoDialog() {
        Bundle bundle = new Bundle();
        bundle.putSerializable(Constants.KEY_SENSOR, mSensor);

        SensorInfoDialog dialog = new SensorInfoDialog();
        dialog.setArguments(bundle);
        FragmentActivity activity = getActivity();
        if (activity != null) {
            FragmentManager fm = activity.getSupportFragmentManager();
            dialog.show(fm, "sensor_info");
        }
    }
}
