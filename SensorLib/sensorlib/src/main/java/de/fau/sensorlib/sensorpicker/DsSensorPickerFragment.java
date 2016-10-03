/**
 * Copyright (C) 2016 Digital Sports Group, Friedrich-Alexander University Erlangen-Nuremberg (FAU).
 * <p/>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.sensorpicker;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.apache.commons.lang3.text.WordUtils;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import de.fau.sensorlib.DsSensor.HardwareSensor;
import de.fau.sensorlib.DsSensorManager;
import de.fau.sensorlib.KnownSensor;
import de.fau.sensorlib.R;
import de.fau.sensorlib.SensorFoundCallback;
import de.fau.sensorlib.SensorInfo;

/**
 * The {@link DsSensorPickerFragment} is a {@link DialogFragment} that discovers and lists all currently available sensors.
 */
public class DsSensorPickerFragment extends DialogFragment {

    private static final String TAG = DsSensorPickerFragment.class.getSimpleName();

    private static final String KEY_SENSOR_RSSI = "key_sensor_rssi";
    private static final String KEY_SENSOR_ADDRESS = "key_sensor_address";
    private static final String KEY_SENSOR_NAME = "key_sensor_name";
    private static final String KEY_KNOWN_SENSOR = "key_known_sensor";

    private Context mContext;
    private ProgressBar mProgressBar;
    private TextView mProgressBarTextView;
    private DsRecyclerAdapter mAdapter;
    private SensorFoundCallback mSensorFoundCallback;

    private BluetoothAdapter mBluetoothAdapter;

    public class DsRecyclerAdapter extends RecyclerView.Adapter<DsViewHolder> implements DsViewHolder.ItemClickListener {

        private List<Bundle> mDataset;

        @Override
        public void onItemClick(View view, int position) {
            KnownSensor sensor = (KnownSensor) mDataset.get(position).getSerializable(KEY_SENSOR_ADDRESS);
            String name = mDataset.get(position).getString(KEY_SENSOR_NAME);
            String address = mDataset.get(position).getString(KEY_SENSOR_ADDRESS);
            mSensorFoundCallback.onKnownSensorFound(new SensorInfo(name, address, sensor));
            createSelectSensorDialog(sensor);
            //mSensorsSelectedCallback.onSensorsSelected()
        }

        DsRecyclerAdapter() {
            mDataset = new ArrayList<>(1);
        }

        @Override
        public DsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            return new DsViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recyclerview, parent, false), this);
        }

        @Override
        public void onBindViewHolder(final DsViewHolder holder, int position) {
            final KnownSensor sensor = (KnownSensor) mDataset.get(position).getSerializable(KEY_SENSOR_ADDRESS);
            String name = mDataset.get(position).getString(KEY_SENSOR_NAME);
            String address = mDataset.get(position).getString(KEY_SENSOR_ADDRESS);
            int rssi = mDataset.get(position).getInt(KEY_SENSOR_RSSI);
            if (sensor == null) {
                return;
            }
            Log.d(TAG, name + ", " + address);
            holder.mSensorNameTextView.setText(name);
            holder.mSensorInformationTextView.setText(address);
            holder.mSensorRssi.setText(mContext.getString(R.string.placeholder_rssi, rssi));
            if (sensor.hasBatteryMeasurement()) {
                holder.mBatteryImageView.setImageResource(R.drawable.ic_battery_available);
                holder.mBatteryImageView.setColorFilter(ContextCompat.getColor(mContext, R.color.sensor_available));
            }
            // wait until Adapter has filled the GridView
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    for (HardwareSensor hw : sensor.getAvailableSensors()) {
                        ((TextView) holder.mSensorGridView.getChildAt(hw.ordinal())).setTextColor(ContextCompat.getColor(mContext, R.color.sensor_available));
                    }
                }
            }, 100);
        }


        @Override
        public int getItemCount() {
            return (mDataset == null) ? 0 : mDataset.size();
        }


        public void addAt(int position, Bundle element) {
            if (!mDataset.contains(element)) {
                mDataset.add(position, element);
                notifyItemInserted(position);
                notifyItemRangeChanged(position, mDataset.size() - position);
            }
        }

        public void add(Bundle element) {
            addAt(mDataset.size(), element);
        }


        public void removeAt(int position) {
            mDataset.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, mDataset.size() - position);
        }


        public void remove() {
            removeAt(mDataset.size() - 1);
        }
    }

    private void createSelectSensorDialog(final KnownSensor sensor) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

        final EnumSet<HardwareSensor> selectedSensors = EnumSet.noneOf(HardwareSensor.class);

        EnumSet<HardwareSensor> availableSensors = sensor.getAvailableSensors();
        final HardwareSensor[] availableSensorsArray = new HardwareSensor[availableSensors.size()];
        availableSensors.toArray(availableSensorsArray);

        String[] sensorNames = new String[availableSensorsArray.length];
        for (int i = 0; i < sensorNames.length; i++) {
            sensorNames[i] = WordUtils.capitalizeFully(availableSensorsArray[i].name().replace('_', ' '));
        }
        builder.setTitle(R.string.string_select_sensors);

        builder.setMultiChoiceItems(sensorNames, null, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                if (isChecked) {
                    selectedSensors.add(availableSensorsArray[which]);
                } else {
                    selectedSensors.remove(availableSensorsArray[which]);
                }
            }
        });

        builder.setPositiveButton(R.string.string_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mSensorFoundCallback.onSensorsSelected(selectedSensors);
                DsSensorPickerFragment.this.dismiss();
            }
        });
        builder.setNegativeButton(R.string.string_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.show();
    }

    public static class DsViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView mSensorNameTextView;
        TextView mSensorInformationTextView;
        TextView mSensorRssi;
        ImageView mBatteryImageView;
        GridView mSensorGridView;
        ItemClickListener mItemClickListener;

        DsViewHolder(View itemView, ItemClickListener listener) {
            super(itemView);
            mSensorNameTextView = (TextView) itemView.findViewById(R.id.tv_sensor_name);
            mSensorInformationTextView = (TextView) itemView.findViewById(R.id.tv_sensor_address);
            mSensorRssi = (TextView) itemView.findViewById(R.id.tv_sensor_rssi);
            mBatteryImageView = (ImageView) itemView.findViewById(R.id.iv_battery);
            mSensorGridView = (GridView) itemView.findViewById(R.id.gv_sensors);
            mSensorGridView.setAdapter(new DsGridAdapter(itemView.getContext()));
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

    public static class DsGridAdapter extends BaseAdapter {

        private Context mContext;

        private HardwareSensor[] mAllSensors = HardwareSensor.values();

        DsGridAdapter(Context context) {
            this.mContext = context;
        }

        @Override
        public int getCount() {
            return mAllSensors.length;
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
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.layout_dialog_sensor_picker, container);

        mContext = rootView.getContext();
        mAdapter = new DsRecyclerAdapter();
        getDialog().setTitle("DsSensorPickerFragment");

        DsRecyclerView recyclerView = (DsRecyclerView) rootView.findViewById(R.id.recyclerview);
        recyclerView.setEmptyView(rootView.findViewById(android.R.id.empty));
        recyclerView.setAdapter(mAdapter);
        mProgressBar = (ProgressBar) rootView.findViewById(R.id.progress_bar);
        mProgressBarTextView = (TextView) rootView.findViewById(R.id.tv_discovering);

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        mContext.registerReceiver(mSensorDiscoveryReceiver, filter);

        return rootView;
    }

    public void setSensorFoundCallback(SensorFoundCallback callback) {
        mSensorFoundCallback = callback;
    }

    public void startSensorDiscovery() {
        try {
            DsSensorManager.checkBtLePermissions(getActivity(), true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Add internal sensor (can always be selected)
        Bundle internalSensor = new Bundle();
        internalSensor.putSerializable(KEY_SENSOR_ADDRESS, KnownSensor.inferSensorClass("Internal", ""));
        mAdapter.add(internalSensor);

        // Start with BLE scan...
        try {
            DsSensorManager.searchBleDevices(getActivity(), new SensorFoundCallback() {
                @Override
                public boolean onKnownSensorFound(SensorInfo sensor) {
                    return true;
                }

                @Override
                public boolean onKnownSensorFound(SensorInfo sensor, int rssi) {
                    Bundle bundle = new Bundle();
                    bundle.putString(KEY_SENSOR_ADDRESS, sensor.getDeviceAddress());
                    bundle.putString(KEY_SENSOR_NAME, sensor.getName());
                    bundle.putSerializable(KEY_KNOWN_SENSOR, sensor.getDeviceClass());
                    bundle.putInt(KEY_SENSOR_RSSI, rssi);
                    mAdapter.add(bundle);
                    return true;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    // ...stop BLE Scan...
                    DsSensorManager.cancelRunningScans();
                    // ... and start with the discovery of "classic" Bluetooth devices,
                    // because BLE devices don't have to be in "discoverable" state
                    mBluetoothAdapter.startDiscovery();
                }
            }, 5000);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        try {
            DsSensorManager.cancelRunningScans();
            mBluetoothAdapter.cancelDiscovery();
            mContext.unregisterReceiver(mSensorDiscoveryReceiver);
        } catch (IllegalArgumentException ignored) {
            // In case BroadcastReceiver for Bluetooth discovery is
            // already unregistered: ignore exception...
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        startSensorDiscovery();
    }

    private final BroadcastReceiver mSensorDiscoveryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, (short) 0);
                KnownSensor sensor = KnownSensor.inferSensorClass(device.getName(), device.getAddress());
                Log.d(TAG, "New device: " + device.getName());
                if (sensor != null) {
                    // Add the sensor to the RecyclerAdapter
                    Bundle bundle = new Bundle();
                    bundle.putSerializable(KEY_SENSOR_ADDRESS, sensor);
                    bundle.putInt(KEY_SENSOR_RSSI, rssi);
                    mAdapter.add(bundle);
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                try {
                    mContext.unregisterReceiver(mSensorDiscoveryReceiver);
                    mProgressBar.setVisibility(View.GONE);
                    mProgressBarTextView.setText(getResources().getString(R.string.string_results));
                } catch (IllegalArgumentException ignored) {
                    // In case BroadcastReceiver for Bluetooth discovery is
                    // already unregistered: ignore exception...
                }
            }
        }
    };

}
