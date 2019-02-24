/*
 * Copyright (C) 2018 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.widgets;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Objects;

import de.fau.sensorlib.BleSensorManager;
import de.fau.sensorlib.R;
import de.fau.sensorlib.SensorFoundCallback;
import de.fau.sensorlib.SensorInfo;
import de.fau.sensorlib.enums.HardwareSensor;
import de.fau.sensorlib.enums.KnownSensor;
import de.fau.sensorlib.sensors.InternalSensor;

/**
 * {@link DialogFragment} that discovers and lists all currently available sensors.
 */
public class SensorPickerDialog extends DialogFragment implements View.OnClickListener {

    private static final String TAG = SensorPickerDialog.class.getSimpleName();

    // Keys for getting sensor information from Bundle
    private static final String KEY_SENSOR_RSSI = "SENSOR_RSSI";
    private static final String KEY_SENSOR_ADDRESS = "SENSOR_ADDRESS";
    private static final String KEY_SENSOR_NAME = "SENSOR_NAME";
    private static final String KEY_KNOWN_SENSOR = "KNOWN_SENSOR";

    private Context mContext;
    private ProgressBar mProgressBar;
    private TextView mProgressTextView;
    private RecyclerView mRecyclerView;
    private Button mCancelButton;
    private Button mOkButton;
    private SensorPickerRecyclerAdapter mAdapter;
    private SensorFoundCallback mSensorFoundCallback;
    private BluetoothAdapter mBluetoothAdapter;

    private EnumSet<HardwareSensor> mHwSensorFilter = EnumSet.noneOf(HardwareSensor.class);
    private EnumSet<KnownSensor> mSensorFilter = EnumSet.noneOf(KnownSensor.class);
    private ArrayList<Bundle> mFoundSensors = new ArrayList<>();
    private ArrayList<Bundle> mSelectedSensors = new ArrayList<>(2);

    @Override
    public void onClick(View view) {
        int i = view.getId();
        if (i == R.id.button_cancel) {
            BleSensorManager.cancelRunningScans();
            dismiss();
        } else if (i == R.id.button_ok) {
            for (Bundle item : mSelectedSensors) {
                KnownSensor sensor = (KnownSensor) item.getSerializable(KEY_KNOWN_SENSOR);
                String name = item.getString(KEY_SENSOR_NAME);
                String address = item.getString(KEY_SENSOR_ADDRESS);
                mSensorFoundCallback.onKnownSensorFound(new SensorInfo(name, address, sensor));
            }
            dismiss();
        }
    }


    private class SensorPickerRecyclerAdapter extends Adapter<SensorPickerViewHolder> implements SensorPickerViewHolder.ItemClickListener {

        SensorPickerRecyclerAdapter() {
            mFoundSensors = new ArrayList<>(0);
        }

        @Override
        public SensorPickerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View layout = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sensor_picker, parent, false);
            return new SensorPickerViewHolder(layout, this);
        }

        @Override
        public void onBindViewHolder(@NonNull final SensorPickerViewHolder holder, int position) {
            final KnownSensor sensor = (KnownSensor) mFoundSensors.get(position).getSerializable(KEY_KNOWN_SENSOR);
            if (sensor == null) {
                return;
            }
            String name = mFoundSensors.get(position).getString(KEY_SENSOR_NAME);
            String address = mFoundSensors.get(position).getString(KEY_SENSOR_ADDRESS);
            int rssi = mFoundSensors.get(position).getInt(KEY_SENSOR_RSSI);
            holder.mSensorNameTextView.setText(name);
            holder.mSensorInformationTextView.setText(address);
            holder.mSensorRssi.setText(mContext.getString(R.string.placeholder_rssi, rssi));
            // if battery measurement is available for sensor
            if (sensor.hasBatteryMeasurement()) {
                holder.mBatteryImageView.setImageResource(R.drawable.ic_battery_available);
                holder.mBatteryImageView.setColorFilter(ContextCompat.getColor(mContext, R.color.sensor_available));
            }

            holder.mSensorGridView.post(new Runnable() {
                @Override
                public void run() {
                    for (final HardwareSensor hw : sensor.getAvailableSensors()) {
                        ((TextView) holder.mSensorGridView.getChildAt(hw.ordinal())).setTextColor(ContextCompat.getColor(mContext, R.color.sensor_available));
                    }
                }
            });
        }

        @Override
        public void onItemClick(View view, int position) {

            SensorPickerViewHolder viewHolder = (SensorPickerViewHolder) mRecyclerView.findViewHolderForAdapterPosition(position);
            viewHolder.mCheckBox.setChecked(!viewHolder.mCheckBox.isChecked());

            Bundle bundle = mFoundSensors.get(position);
            if (mSelectedSensors.contains(bundle)) {
                viewHolder.mCheckBox.setChecked(false);
                mSelectedSensors.remove(bundle);
            } else {
                viewHolder.mCheckBox.setChecked(true);
                mSelectedSensors.add(bundle);
            }

            //mSensorFoundCallback.onKnownSensorFound(new SensorInfo(name, address, sensor));
            //createSelectSensorDialog(sensor);
            //SensorPickerFragment.this.dismiss();
        }


        @Override
        public int getItemCount() {
            return (mFoundSensors == null) ? 0 : mFoundSensors.size();
        }

        /**
         * Adds element at the specified position and
         * notifies the {@link SensorPickerRecyclerAdapter} that the underlying list has changed.
         *
         * @param position Insert position
         * @param element  Sensor element as {@link Bundle}
         */
        public void addAt(int position, Bundle element) {
            if (!mFoundSensors.contains(element)) {
                mFoundSensors.add(position, element);
                notifyItemInserted(position);
                notifyItemRangeChanged(position, mFoundSensors.size() - position - 1);
            }
        }

        /**
         * Adds element to the end of the list and
         * notifies the {@link SensorPickerRecyclerAdapter} that the underlying list has changed.
         *
         * @param element Sensor element as {@link Bundle}
         */
        public void add(Bundle element) {
            addAt(mFoundSensors.size(), element);
        }


        /**
         * Removes element at the specified position and
         * notifies the {@link SensorPickerRecyclerAdapter} that the underlying list has changed.
         *
         * @param position Position to remove
         */
        public void removeAt(int position) {
            mFoundSensors.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, mFoundSensors.size() - position);
        }

        /**
         * Removes element of the end of the list and
         * notifies the {@link SensorPickerRecyclerAdapter} that the underlying list has changed.
         */
        public void remove() {
            removeAt(mFoundSensors.size() - 1);
        }
    }

//    /**
//     * Creates a Dialog to choose from the desired {@link HardwareSensor}
//     * for the selected sensor device.
//     *
//     * @param sensor Selected Sensor
//     */
//    private void createSelectSensorDialog(final KnownSensor sensor) {
//        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
//
//        final EnumSet<AbstractSensor.HardwareSensor> selectedSensors = EnumSet.noneOf(HardwareSensor.class);
//
//        // Get available HardwareSensors for the selected sensor device
//        EnumSet<HardwareSensor> availableSensors = sensor.getAvailableSensors();
//        final HardwareSensor[] availableSensorsArray = new HardwareSensor[availableSensors.size()];
//        availableSensors.toArray(availableSensorsArray);
//
//        String[] sensorNames = new String[availableSensorsArray.length];
//        for (int i = 0; i < sensorNames.length; i++) {
//            // Reformat names of Hardware Sensors with WordUtils (e.g. HEART_RATE => Heart Rate)
//            sensorNames[i] = WordUtils.capitalizeFully(availableSensorsArray[i].name().replace('_', ' '));
//        }
//
//        builder.setTitle(R.string.string_select_sensors);
//        builder.setMultiChoiceItems(sensorNames, null, new DialogInterface.OnMultiChoiceClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
//                if (isChecked) {
//                    selectedSensors.add(availableSensorsArray[which]);
//                } else {
//                    selectedSensors.remove(availableSensorsArray[which]);
//                }
//            }
//        });
//        builder.setPositiveButton(R.string.string_ok, new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                // notify Callback with the selected hardware sensors and close sensor picker
//                mSensorFoundCallback.onSensorsSelected(selectedSensors);
//                SensorPickerFragment.this.dismiss();
//            }
//        });
//        builder.setNegativeButton(R.string.string_cancel, new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                dialog.dismiss();
//            }
//        });
//
//        builder.show();
//    }

    private static class SensorPickerViewHolder extends ViewHolder implements View.OnClickListener {

        private TextView mSensorNameTextView;
        private TextView mSensorInformationTextView;
        private TextView mSensorRssi;
        private ImageView mBatteryImageView;
        private GridView mSensorGridView;
        private CheckBox mCheckBox;
        private ItemClickListener mItemClickListener;

        private SensorPickerViewHolder(View itemView, ItemClickListener listener) {
            super(itemView);
            mSensorNameTextView = itemView.findViewById(R.id.tv_sensor_name);
            mSensorInformationTextView = itemView.findViewById(R.id.tv_sensor_address);
            mSensorRssi = itemView.findViewById(R.id.tv_sensor_rssi);
            mBatteryImageView = itemView.findViewById(R.id.iv_battery);
            mCheckBox = itemView.findViewById(R.id.checkbox);
            mCheckBox.setOnClickListener(this);
            mSensorGridView = itemView.findViewById(R.id.gv_sensors);
            mSensorGridView.setAdapter(new SensorPickerGridAdapter(itemView.getContext()));
            mItemClickListener = listener;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            mItemClickListener.onItemClick(v, getAdapterPosition());
        }

        interface ItemClickListener {
            void onItemClick(View view, int position);
        }
    }

    /**
     * Adapter for GridView that lists all hardware sensors
     * and highlights the available hardware sensors.
     */
    private static class SensorPickerGridAdapter extends BaseAdapter {

        private Context mContext;

        SensorPickerGridAdapter(Context context) {
            this.mContext = context;
        }

        @Override
        public int getCount() {
            return HardwareSensor.values().length;
        }

        @Override
        public Object getItem(int position) {
            return HardwareSensor.values()[position];
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView tv = new TextView(mContext);
            tv.setLayoutParams(new GridView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            tv.setText(((HardwareSensor) getItem(position)).getShortDescription());
            tv.setTextColor(ContextCompat.getColor(mContext, R.color.sensor_not_available));
            tv.setBackgroundColor(ContextCompat.getColor(mContext, android.R.color.white));
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            return tv;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.widget_sensor_picker, container);

        mContext = rootView.getContext();
        mAdapter = new SensorPickerRecyclerAdapter();

        getDialog().setTitle("SensorPicker");

        setCancelable(false);
        mCancelButton = rootView.findViewById(R.id.button_cancel);
        mCancelButton.setOnClickListener(this);
        mOkButton = rootView.findViewById(R.id.button_ok);
        mOkButton.setOnClickListener(this);

        mRecyclerView = rootView.findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        mRecyclerView.setAdapter(mAdapter);
        mProgressBar = rootView.findViewById(R.id.progress_bar);
        mProgressTextView = rootView.findViewById(R.id.tv_scanning);

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        Objects.requireNonNull(getDialog().getWindow()).setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        Activity activity = getActivity();
        if (activity instanceof DialogInterface.OnDismissListener) {
            ((DialogInterface.OnDismissListener) activity).onDismiss(dialog);
        }
    }

    public void setSensorFoundCallback(SensorFoundCallback callback) {
        mSensorFoundCallback = callback;
    }

    public void setHardwareSensorFilter(EnumSet<HardwareSensor> filter) {
        mHwSensorFilter.addAll(filter);
    }

    public void setHardwareSensorFilter(HardwareSensor filter) {
        setHardwareSensorFilter(EnumSet.of(filter));
    }

    public void setSensorFilter(KnownSensor sensor) {
        setSensorFilter(EnumSet.of(sensor));
    }

    public void setSensorFilter(EnumSet<KnownSensor> sensors) {
        mSensorFilter.addAll(sensors);
    }

    public void clearHardwareSensorFilter() {
        mHwSensorFilter.clear();
    }

    public void clearSensorFilter() {
        mSensorFilter.clear();
    }


    /**
     * Starts scanning for available sensors.
     */
    private void startSensorScan() {
        try {
            if ((mHwSensorFilter.isEmpty() && mSensorFilter.isEmpty()) || (KnownSensor.INTERNAL.getAvailableSensors().containsAll(mHwSensorFilter) && mSensorFilter.contains(KnownSensor.INTERNAL))) {
                // Add internal sensor (can always be selected)
                Bundle internalSensor = new Bundle();
                internalSensor.putString(KEY_SENSOR_NAME, InternalSensor.INTERNAL_SENSOR_NAME);
                internalSensor.putString(KEY_SENSOR_ADDRESS, InternalSensor.INTERNAL_SENSOR_ADDRESS);
                internalSensor.putSerializable(KEY_KNOWN_SENSOR, KnownSensor.INTERNAL);
                internalSensor.putInt(KEY_SENSOR_RSSI, 0);
                mAdapter.add(internalSensor);
            }

            // Check for (and if needed request) necessary BLE permissions
            if (BleSensorManager.enableBluetooth(getActivity()) == BleSensorManager.BT_ENABLED) {
                if (BleSensorManager.checkBtLePermissions(getActivity(), true) == BleSensorManager.PERMISSIONS_GRANTED) {
                    // Start with BLE scan...
                    BleSensorManager.searchBleDevices(new SensorFoundCallback() {
                        @Override
                        public boolean onKnownSensorFound(SensorInfo sensor) {
                            return true;
                        }

                        @Override
                        public boolean onKnownSensorFound(SensorInfo sensor, int rssi) {
                            if ((mHwSensorFilter.isEmpty() && mSensorFilter.isEmpty()) ||
                                    (sensor.getDeviceClass().getAvailableSensors().containsAll(mHwSensorFilter)
                                            && mSensorFilter.contains(sensor.getDeviceClass()))) {
                                Bundle bundle = new Bundle();
                                bundle.putString(KEY_SENSOR_ADDRESS, sensor.getDeviceAddress());
                                bundle.putString(KEY_SENSOR_NAME, sensor.getName());
                                bundle.putSerializable(KEY_KNOWN_SENSOR, sensor.getDeviceClass());
                                bundle.putInt(KEY_SENSOR_RSSI, rssi);
                                mAdapter.add(bundle);
                                mAdapter.notifyDataSetChanged();
                            }
                            return true;
                        }
                    });
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // TODO Do Bluetooth Classic Discovery
    }

    private void stopSensorScan() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // ...stop BLE Scan
                if (BleSensorManager.isScanning()) {
                    BleSensorManager.cancelRunningScans();
                }
                if (isAdded()) {
                    mProgressBar.setVisibility(View.GONE);
                    mProgressTextView.setText(getString(R.string.string_scan_results));
                    mRecyclerView.getAdapter().notifyDataSetChanged();

                }
            }
        }, BleSensorManager.SCAN_PERIOD);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (BleSensorManager.isScanning()) {
            BleSensorManager.cancelRunningScans();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        startSensorScan();
        stopSensorScan();
    }

    public void show(Activity activity) {
        if (activity instanceof AppCompatActivity) {
            super.show((((AppCompatActivity) activity).getSupportFragmentManager()), "sensor_picker");
        }
    }
}
