/*
 * Copyright (C) 2018 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import de.fau.sensorlib.R;
import de.fau.sensorlib.SensorEventGenerator;
import de.fau.sensorlib.SensorEventListener;
import de.fau.sensorlib.enums.SensorState;
import de.fau.sensorlib.sensors.AbstractSensor;

/**
 * Provides a status bar that displays the current sensor state. This Widget implements the {@link SensorEventListener},
 * so can subscribe to the {@link SensorEventGenerator}.
 */
public class StatusBar extends RelativeLayout implements SensorEventListener {

    private SensorState mState;
    private Context mContext;
    private TextView mStateTextView;
    private String mSensorName;

    public StatusBar(Context context) {
        this(context, null, -1);
    }

    public StatusBar(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public StatusBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.widget_status_bar, this);

        mContext = context;
        mStateTextView = findViewById(R.id.tv_status);

        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.StatusBar);
        if (attributes != null) {
            try {
                mSensorName = attributes.getString(R.styleable.StatusBar_sensor_name);
                mSensorName = (mSensorName == null) ? "Sensor" : mSensorName;
                setState(SensorState.values()[attributes.getInteger(R.styleable.StatusBar_state, 0)]);

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                attributes.recycle();
            }
        }

    }

    /**
     * Returns the current connection state.
     *
     * @return The current connection state
     */
    public SensorState getState() {
        return mState;
    }

    /**
     * Sets the current connection state of the StatusBar.
     *
     * @param status The current connection state
     */
    public void setState(SensorState status) {
        switch (status) {
            case CONNECTION_LOST:
                // fall through
            case DISCONNECTED:
                setBackgroundColor(ContextCompat.getColor(mContext, R.color.status_bar_disconnected));
                mStateTextView.setText(getResources().getString(R.string.status_bar_disconnected, mSensorName).toUpperCase());
                break;
            case CONNECTING:
                setBackgroundColor(ContextCompat.getColor(mContext, R.color.status_bar_connecting));
                mStateTextView.setText(getResources().getString(R.string.status_bar_connecting, mSensorName).toUpperCase());
                break;
            case CONNECTED:
                setBackgroundColor(ContextCompat.getColor(mContext, R.color.status_bar_connected));
                mStateTextView.setText(getResources().getString(R.string.status_bar_connected, mSensorName).toUpperCase());
                break;
            case STREAMING:
                setBackgroundColor(ContextCompat.getColor(mContext, R.color.status_bar_streaming));
                mStateTextView.setText(getResources().getString(R.string.status_bar_streaming, mSensorName).toUpperCase());
                break;
            case LOGGING:
                setBackgroundColor(ContextCompat.getColor(mContext, R.color.status_bar_logging));
                mStateTextView.setText(getResources().getString(R.string.status_bar_loggging, mSensorName).toUpperCase());
                break;
            case SIMULATING:
                setBackgroundColor(ContextCompat.getColor(mContext, R.color.status_bar_simulating));
                mStateTextView.setText(getResources().getString(R.string.status_bar_simulating, mSensorName).toUpperCase());
                break;
            case UPGRADING_FIRMWARE:
                setBackgroundColor(ContextCompat.getColor(mContext, R.color.status_bar_upgrading_firmware));
                mStateTextView.setText(getResources().getString(R.string.status_bar_upgrading_firmware, mSensorName).toUpperCase());
                break;
            case DOWNLOADING:
                setBackgroundColor(ContextCompat.getColor(mContext, R.color.status_bar_downloading));
                mStateTextView.setText(getResources().getString(R.string.status_bar_downloading, mSensorName).toUpperCase());
                break;
            case UNDEFINED:
                // fall through
            case INITIALIZED:
                // fall through
            default:
                setBackgroundColor(ContextCompat.getColor(mContext, android.R.color.transparent));
                mStateTextView.setText("");
                break;
        }
        mState = status;
    }

    @Override
    public void onSensorStateChange(AbstractSensor sensor, SensorState state) {
        if (sensor == null) {
            setState(state);
        }
    }
}
