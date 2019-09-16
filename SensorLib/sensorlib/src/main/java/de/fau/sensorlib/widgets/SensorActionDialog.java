/*
 * Copyright (C) 2019 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */

package de.fau.sensorlib.widgets;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Objects;

import de.fau.sensorlib.Constants;
import de.fau.sensorlib.R;
import de.fau.sensorlib.enums.SensorAction;
import de.fau.sensorlib.sensors.AbstractSensor;
import de.fau.sensorlib.sensors.Configurable;
import de.fau.sensorlib.sensors.Erasable;
import de.fau.sensorlib.sensors.Loggable;
import de.fau.sensorlib.sensors.Resettable;
import de.fau.sensorlib.widgets.config.SensorConfigDialog;
import de.fau.sensorlib.widgets.config.SensorConfigSelectedListener;

public class SensorActionDialog extends DialogFragment implements SensorConfigSelectedListener, View.OnClickListener {

    private static final String TAG = SensorActionDialog.class.getSimpleName();

    private Context mContext;
    private RecyclerView mRecyclerView;
    private AbstractSensor mSensor;

    private DialogInterface.OnDismissListener mDialogDismissCallback;

    private SensorActionCallback mSensorActionCallback;


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
        public void onItemClick(View view, final int position) {
            switch (mSensor.getState()) {
                case CONNECTED:
                    for (SensorAction action : EnumSet.of(SensorAction.STOP_LOGGING)) {
                        if (position == action.ordinal()) {
                            Toast.makeText(getContext(), "Sensor is currently not logging.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    break;
                case LOGGING:
                    for (SensorAction action : EnumSet.of(SensorAction.CONFIGURE, SensorAction.START_LOGGING, SensorAction.CLEAR_SESSIONS, SensorAction.FULL_ERASE, SensorAction.RESET)) {
                        if (position == action.ordinal()) {
                            Toast.makeText(getContext(), "Sensor is currently logging. Stop logging first!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    break;
                case STREAMING:
                    for (SensorAction action : EnumSet.of(SensorAction.CONFIGURE, SensorAction.START_LOGGING, SensorAction.STOP_LOGGING, SensorAction.CLEAR_SESSIONS, SensorAction.FULL_ERASE)) {
                        if (position == action.ordinal()) {
                            Toast.makeText(getContext(), "Sensor is currently streaming. Stop streaming first!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    break;

            }

            SensorAction action = SensorAction.values()[position];

            // needs to be declared, otherwise app will crash when trying to call getActivity() in
            // OnClickListener of AlertDialog (SensorActionDialog has already been dismissed by then)
            final Activity activity = getActivity();

            switch (action) {
                case CONFIGURE:
                    if (mSensor instanceof Configurable) {
                        showSensorConfigDialog();
                    }
                    break;
                case DEFAULT_CONFIG:
                    if (mSensor instanceof Configurable) {
                        createAlertDialog("Setting default config of " + mSensor.getDeviceName() + "?",
                                (dialog, which) -> {
                                    mSensorActionCallback.onSensorActionSelected(mSensor, action);
                                    Toast.makeText(activity, SensorAction.values()[position] + " on " + mSensor.getDeviceName(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        return;
                    }
                    break;
                case RESET:
                    if (mSensor instanceof Resettable) {
                        createAlertDialog("Resetting " + mSensor.getDeviceName() + "?", (dialog, which) -> {
                            mSensorActionCallback.onSensorActionSelected(mSensor, action);
                            Toast.makeText(activity, SensorAction.values()[position] + " on " + mSensor.getDeviceName(), Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        return;
                    }
                    break;
                case START_LOGGING:
                case STOP_LOGGING:
                    if (mSensor instanceof Loggable) {
                        mSensorActionCallback.onSensorActionSelected(mSensor, action);
                        Toast.makeText(activity, SensorAction.values()[position] + " on " + mSensor.getDeviceName(), Toast.LENGTH_SHORT).show();
                    } else {
                        return;
                    }
                    break;
                case CLEAR_SESSIONS:
                    if (mSensor instanceof Erasable) {
                        createAlertDialog("Clearing sessions of " + mSensor.getDeviceName() + "?",
                                (dialog, which) -> {
                                    mSensorActionCallback.onSensorActionSelected(mSensor, action);
                                    Toast.makeText(activity, SensorAction.values()[position] + " on " + mSensor.getDeviceName(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        return;
                    }
                    break;
                case FULL_ERASE:
                    if (mSensor instanceof Erasable) {
                        createAlertDialog("Erasing full storage of " + mSensor.getDeviceName() + "?",
                                (dialog, which) -> {
                                    mSensorActionCallback.onSensorActionSelected(mSensor, action);
                                    Toast.makeText(activity, SensorAction.values()[position] + " on " + mSensor.getDeviceName(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        return;
                    }
                    break;
                case SENSOR_INFO:
                    showSensorInfoDialog();
                    break;
            }
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

        Button okButton = rootView.findViewById(R.id.button_ok);
        okButton.setOnClickListener(this);

        Bundle bundle = getArguments();
        if (bundle != null) {
            mSensor = (AbstractSensor) bundle.getSerializable(Constants.KEY_SENSOR);
        }

        mRecyclerView.post(() -> {
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
        });

        return rootView;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.button_ok) {
            dismiss();
        }
    }

    private void setItemDisabled(View view) {
        view.findViewById(R.id.tv_sensor_action).setEnabled(false);
    }

    public void setSensorActionCallback(SensorActionCallback callback) {
        mSensorActionCallback = callback;
    }

    public void setDialogDismissCallback(DialogInterface.OnDismissListener callback) {
        mDialogDismissCallback = callback;
    }

    @Override
    public void onSensorConfigSelected(HashMap<String, Object> configMap) {
        mSensorActionCallback.onSensorActionSelected(mSensor, SensorAction.CONFIGURE, configMap);
    }

    private void createAlertDialog(String message, DialogInterface.OnClickListener okListener, DialogInterface.OnClickListener cancelListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
                .setMessage(message)
                .setPositiveButton(getString(R.string.ok), okListener);
        if (cancelListener != null) {
            builder.setNegativeButton(getString(R.string.cancel), cancelListener);
        } else {
            builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dismiss());
        }
        builder.show();
    }

    private void createAlertDialog(String message, DialogInterface.OnClickListener okListener) {
        createAlertDialog(message, okListener, null);
    }


    private void showSensorConfigDialog() {
        Bundle bundle = new Bundle();
        bundle.putSerializable(Constants.KEY_SENSOR_CONFIG, ((Configurable) mSensor).getConfigItemMap());
        bundle.putSerializable(Constants.KEY_SENSOR_CONFIG_DEFAULT, ((Configurable) mSensor).getCurrentConfig());
        bundle.putString(Constants.KEY_SENSOR_NAME, mSensor.getDeviceName());

        SensorConfigDialog dialog = new SensorConfigDialog();
        dialog.setSensorConfigSelectedListener(this);
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

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (mDialogDismissCallback != null) {
            mDialogDismissCallback.onDismiss(dialog);
        } else {
            Log.e(TAG, "No OnDismissListener attached to " + getClass().getSimpleName() + "!");
        }
    }
}
