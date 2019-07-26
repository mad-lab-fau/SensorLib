/*
 * Copyright (C) 2018 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.widgets;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.collection.ArraySet;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import de.fau.sensorlib.BleSensorManager;
import de.fau.sensorlib.Constants;
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

    private Context mContext;
    private ProgressBar mProgressBar;
    private TextView mProgressTextView;
    private RecyclerView mRecyclerView;
    private SensorPickerRecyclerAdapter mAdapter;
    private SensorFoundCallback mSensorFoundCallback;

    private boolean mAddByRssi = true;

    private EnumSet<HardwareSensor> mHwSensorFilter = EnumSet.noneOf(HardwareSensor.class);
    private EnumSet<KnownSensor> mSensorFilter = EnumSet.noneOf(KnownSensor.class);
    private ArrayList<String> mLastConnectedSensors = new ArrayList<>();
    private ArrayList<Bundle> mFoundSensors = new ArrayList<>();
    private ArrayList<Bundle> mSelectedSensors = new ArrayList<>();

    private DialogInterface.OnDismissListener mDialogDismissCallback;

    @Override
    public void onClick(View view) {
        int i = view.getId();
        if (i == R.id.button_cancel) {
            BleSensorManager.cancelRunningScans();
            dismiss();
        } else if (i == R.id.button_ok) {
            // Save selected sensors to Shared Preferences to display them at the top the next time
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
            List<String> sensorNames = new ArrayList<>();
            for (Bundle item : mSelectedSensors) {
                KnownSensor sensor = (KnownSensor) item.getSerializable(Constants.KEY_KNOWN_SENSOR);
                String name = item.getString(Constants.KEY_SENSOR_NAME);
                String address = item.getString(Constants.KEY_SENSOR_ADDRESS);
                mSensorFoundCallback.onKnownSensorFound(new SensorInfo(name, address, sensor));
                sensorNames.add(name);
            }
            sharedPreferences.edit().putStringSet(Constants.KEY_SENSOR_LIST, new ArraySet<>(sensorNames)).apply();
            dismiss();
        }
    }


    private class SensorPickerRecyclerAdapter extends Adapter<SensorPickerViewHolder> implements SensorPickerViewHolder.ItemClickListener {

        private SensorPickerRecyclerAdapter() {
            mFoundSensors = new ArrayList<>(0);
        }

        @NonNull
        @Override
        public SensorPickerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View layout = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sensor_picker, parent, false);
            return new SensorPickerViewHolder(layout, this);
        }

        @Override
        public void onBindViewHolder(@NonNull final SensorPickerViewHolder holder, int position) {
            final KnownSensor sensor = (KnownSensor) mFoundSensors.get(position).getSerializable(Constants.KEY_KNOWN_SENSOR);
            if (sensor == null) {
                return;
            }

            String name = mFoundSensors.get(position).getString(Constants.KEY_SENSOR_NAME);
            String address = mFoundSensors.get(position).getString(Constants.KEY_SENSOR_ADDRESS);
            int rssi = mFoundSensors.get(position).getInt(Constants.KEY_SENSOR_RSSI);
            holder.mSensorNameTextView.setText(name);

            holder.mSensorInformationTextView.setText(address);
            holder.mSensorRssi.setText(mContext.getString(R.string.placeholder_rssi, rssi));

            // highlight last connected sensors
            if (mLastConnectedSensors.contains(mFoundSensors.get(position).getString(Constants.KEY_SENSOR_NAME))) {
                holder.mSensorNameTextView.setTypeface(null, Typeface.ITALIC);
                holder.mRecentlyConnectedTextView.setVisibility(View.VISIBLE);
            }

            holder.mGridAdapter.setSensorsAvailable(sensor.getAvailableSensors());
        }

        @Override
        public void onViewAttachedToWindow(@NonNull SensorPickerViewHolder holder) {
            super.onViewAttachedToWindow(holder);
        }

        @Override
        public void onItemClick(View view, int position) {
            SensorPickerViewHolder viewHolder = (SensorPickerViewHolder) mRecyclerView.findViewHolderForAdapterPosition(position);
            if (viewHolder == null) {
                return;
            }

            viewHolder.mCheckBox.setChecked(!viewHolder.mCheckBox.isChecked());

            Bundle bundle = mFoundSensors.get(position);
            if (mSelectedSensors.contains(bundle)) {
                viewHolder.mCheckBox.setChecked(false);
                mSelectedSensors.remove(bundle);
            } else {
                viewHolder.mCheckBox.setChecked(true);
                mSelectedSensors.add(bundle);
            }
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

        public void addByRssi(Bundle element) {
            int rssi = element.getInt(Constants.KEY_SENSOR_RSSI);
            // skip first sensor (internal sensor)
            for (int i = 1; i < mFoundSensors.size(); i++) {
                // skip "last connected" sensors because they should be at the front
                if (mLastConnectedSensors.contains(mFoundSensors.get(i).getString(Constants.KEY_SENSOR_NAME))) {
                    continue;
                }
                int rssiTmp = mFoundSensors.get(i).getInt(Constants.KEY_SENSOR_RSSI);
                if (rssi > rssiTmp) {
                    // add at right position
                    addAt(i, element);
                    break;
                }
            }
            // add at end
            add(element);
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
        private TextView mRecentlyConnectedTextView;
        private TextView mSensorInformationTextView;
        private TextView mSensorRssi;
        private GridView mSensorGridView;
        private CheckBox mCheckBox;
        private HardwareSensorGridAdapter mGridAdapter;
        private ItemClickListener mItemClickListener;

        private SensorPickerViewHolder(View itemView, ItemClickListener listener) {
            super(itemView);

            mSensorNameTextView = itemView.findViewById(R.id.tv_sensor_name);
            mRecentlyConnectedTextView = itemView.findViewById(R.id.tv_recently);
            mSensorInformationTextView = itemView.findViewById(R.id.tv_sensor_address);
            mSensorRssi = itemView.findViewById(R.id.tv_sensor_rssi);
            mCheckBox = itemView.findViewById(R.id.checkbox);
            mCheckBox.setOnClickListener(this);
            mSensorGridView = itemView.findViewById(R.id.gv_sensors);
            mGridAdapter = new HardwareSensorGridAdapter(itemView.getContext());
            mSensorGridView.setAdapter(mGridAdapter);
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
    private static class HardwareSensorGridAdapter extends BaseAdapter {

        private Context mContext;
        private EnumSet<HardwareSensor> mAvailableSensors;

        HardwareSensorGridAdapter(Context context) {
            mContext = context;
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
            if (mAvailableSensors != null && mAvailableSensors.contains(HardwareSensor.values()[position])) {
                tv.setTextColor(ContextCompat.getColor(mContext, R.color.sensor_available));
            } else {
                tv.setTextColor(ContextCompat.getColor(mContext, R.color.sensor_not_available));
            }
            tv.setBackgroundColor(ContextCompat.getColor(mContext, android.R.color.white));
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            return tv;
        }

        public void setSensorsAvailable(EnumSet<HardwareSensor> availableSensors) {
            mAvailableSensors = availableSensors;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.widget_sensor_picker, container);

        mContext = rootView.getContext();
        mAdapter = new SensorPickerRecyclerAdapter();

        if (getDialog() != null) {
            getDialog().setTitle("SensorPicker");
        }

        setCancelable(false);
        Button cancelButton = rootView.findViewById(R.id.button_cancel);
        cancelButton.setOnClickListener(this);
        Button okButton = rootView.findViewById(R.id.button_ok);
        okButton.setOnClickListener(this);

        mRecyclerView = rootView.findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        mRecyclerView.setItemViewCacheSize(100);

        mRecyclerView.setAdapter(mAdapter);
        mProgressBar = rootView.findViewById(R.id.progress_bar);
        mProgressTextView = rootView.findViewById(R.id.tv_scanning);

        Set<String> sensors = PreferenceManager.getDefaultSharedPreferences(getContext()).getStringSet(Constants.KEY_SENSOR_LIST, null);
        if (sensors != null) {
            mLastConnectedSensors.addAll(sensors);
        }

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
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

    public void setSensorFoundCallback(SensorFoundCallback callback) {
        mSensorFoundCallback = callback;
    }

    public void setDialogDismissCallback(DialogInterface.OnDismissListener callback) {
        mDialogDismissCallback = callback;
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
                internalSensor.putString(Constants.KEY_SENSOR_NAME, InternalSensor.INTERNAL_SENSOR_NAME);
                internalSensor.putString(Constants.KEY_SENSOR_ADDRESS, InternalSensor.INTERNAL_SENSOR_ADDRESS);
                internalSensor.putSerializable(Constants.KEY_KNOWN_SENSOR, KnownSensor.INTERNAL);
                internalSensor.putInt(Constants.KEY_SENSOR_RSSI, 0);
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
                                bundle.putString(Constants.KEY_SENSOR_ADDRESS, sensor.getDeviceAddress());
                                bundle.putString(Constants.KEY_SENSOR_NAME, sensor.getDeviceName());
                                bundle.putSerializable(Constants.KEY_KNOWN_SENSOR, sensor.getDeviceClass());
                                bundle.putInt(Constants.KEY_SENSOR_RSSI, rssi);
                                if (mLastConnectedSensors.contains(sensor.getDeviceName())) {
                                    // add after internal sensor
                                    mAdapter.addAt(1, bundle);
                                } else {
                                    if (mAddByRssi) {
                                        mAdapter.addByRssi(bundle);
                                    } else {
                                        mAdapter.add(bundle);
                                    }
                                }
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
        new Handler().postDelayed(() -> {
            // ...stop BLE Scan
            if (BleSensorManager.isScanning()) {
                BleSensorManager.cancelRunningScans();
            }
            if (isAdded()) {
                mProgressBar.setVisibility(View.GONE);
                mProgressTextView.setText(getString(R.string.string_scan_results));
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
