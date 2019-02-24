/*
 * Copyright (C) 2018 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.widgets;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import de.fau.sensorlib.Constants;
import de.fau.sensorlib.R;
import de.fau.sensorlib.SensorEventGenerator;
import de.fau.sensorlib.SensorEventListener;
import de.fau.sensorlib.enums.SensorMessage;
import de.fau.sensorlib.enums.SensorState;
import de.fau.sensorlib.sensors.AbstractSensor;

/**
 * Provides a grid layout that displays sensor information. This Widget implements the
 * {@link SensorEventListener}, so it can subscribe to the {@link SensorEventGenerator}.
 */
public class SensorInfoBar extends RecyclerView implements SensorEventListener {

    private static final String TAG = SensorInfoBar.class.getSimpleName();

    private SensorInfoGridAdapter mAdapter;

    public SensorInfoBar(Context context) {
        this(context, null);
    }

    public SensorInfoBar(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public SensorInfoBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayoutManager(new GridLayoutManager(getContext(), 2));
        addItemDecoration(new ItemSpacing(getContext()));
        mAdapter = new SensorInfoGridAdapter(context);
        setAdapter(mAdapter);
    }

    private AppCompatActivity getActivity() {
        Context context = getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof AppCompatActivity) {
                return (AppCompatActivity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    @Override
    public void onSensorStateChange(AbstractSensor sensor, SensorState state) {
        switch (state) {
            case CONNECTED:
                if (sensor != null) {
                    mAdapter.addSensor(sensor);
                }
                break;
            case CONNECTION_LOST:
                // fall through
            case DISCONNECTED:
                mAdapter.clear();
                break;
        }
    }

    @Override
    public void onSensorMessage(AbstractSensor sensor, SensorMessage message) {
        switch (message) {
            case BATTERY_LEVEL_CHANGED:
                mAdapter.updateBatteryLevel(sensor);
                break;
        }
    }


    private class SensorInfoGridAdapter extends Adapter<SensorInfoViewHolder> implements SensorInfoViewHolder.ItemClickListener {

        private Context mContext;
        private ArrayList<AbstractSensor> mAttachedSensors;

        private SensorInfoGridAdapter(Context context) {
            mContext = context;
            mAttachedSensors = new ArrayList<>();
        }

        private void updateBatteryLevel(AbstractSensor sensor) {
            int idx = mAttachedSensors.indexOf(sensor);
            if (idx != -1) {
                mAttachedSensors.set(idx, sensor);
                notifyDataSetChanged();
            }
        }

        private void addSensor(AbstractSensor sensor) {
            mAttachedSensors.add(sensor);
            notifyDataSetChanged();
        }

        private void clear() {
            mAttachedSensors.clear();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public SensorInfoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View layout = LayoutInflater.from(mContext).inflate(R.layout.item_sensor_info_bar, parent, false);
            return new SensorInfoViewHolder(mContext, layout, this);
        }

        @Override
        public void onBindViewHolder(@NonNull SensorInfoViewHolder holder, int position) {
            holder.setSensorName(mAttachedSensors.get(position).getDeviceName());
            holder.updateBatteryLevel(mAttachedSensors.get(position).getBatteryLevel());
        }

        @Override
        public void onItemClick(View view, int position) {
            SensorInfoViewHolder viewHolder = (SensorInfoViewHolder) findViewHolderForAdapterPosition(position);
        }

        @Override
        public void onLongItemClick(View view, int position) {
            AbstractSensor sensor = mAttachedSensors.get(position);

            Bundle bundle = new Bundle();
            bundle.putString(Constants.KEY_SENSOR_NAME, sensor.getName());
            bundle.putString(Constants.KEY_SENSOR_ADDRESS, sensor.getDeviceAddress());
            bundle.putString(Constants.KEY_MANUFACTURER, sensor.getManufacturer());
            bundle.putString(Constants.KEY_FIRMWARE_REVISION, sensor.getFirmwareRevision());

            SensorInfoDialog dialog = new SensorInfoDialog();
            dialog.setArguments(bundle);
            AppCompatActivity activity = getActivity();
            if (activity != null) {
                FragmentManager fm = activity.getSupportFragmentManager();
                if (fm != null) {
                    dialog.show(fm, "sensor_info");
                }
            }
        }

        @Override
        public int getItemCount() {
            return mAttachedSensors.size();
        }

    }

    private static class SensorInfoViewHolder extends ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        private Context mContext;
        private TextView mSensorNameTextView;
        private TextView mBatteryLevelTextView;
        private ItemClickListener mItemClickListener;

        private SensorInfoViewHolder(Context context, View itemView, ItemClickListener listener) {
            super(itemView);
            mContext = context;
            mSensorNameTextView = itemView.findViewById(R.id.tv_sensor_name);
            mBatteryLevelTextView = itemView.findViewById(R.id.tv_battery_level);
            mItemClickListener = listener;
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        public void setSensorName(String sensorName) {
            mSensorNameTextView.setText(sensorName);
            TextViewCompat.setAutoSizeTextTypeWithDefaults(mSensorNameTextView, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);
        }

        public void updateBatteryLevel(int batteryLevel) {
            mBatteryLevelTextView.setText(mContext.getString(R.string.placeholder_battery_level, batteryLevel));
        }

        @Override
        public void onClick(View v) {
            mItemClickListener.onItemClick(v, getAdapterPosition());
        }

        @Override
        public boolean onLongClick(View v) {
            mItemClickListener.onLongItemClick(v, getAdapterPosition());
            return true;
        }

        interface ItemClickListener {
            void onItemClick(View view, int position);

            void onLongItemClick(View view, int position);
        }
    }

    private static class ItemSpacing extends ItemDecoration {

        private int spacing;

        private ItemSpacing(Context context) {
            spacing = context.getResources().getDimensionPixelSize(R.dimen.grid_spacing);
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, State state) {
            outRect.set(spacing, spacing, spacing, spacing);
        }
    }
}