/*
 * Copyright (C) 2018 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import de.fau.sensorlib.R;
import de.fau.sensorlib.SensorEventGenerator;
import de.fau.sensorlib.SensorEventListener;
import de.fau.sensorlib.enums.SensorMessage;
import de.fau.sensorlib.enums.SensorState;
import de.fau.sensorlib.sensors.AbstractSensor;

/**
 * Provides a UI element with buttons to connect/disconnect sensors, and to start/stop streaming. This Widget implements the {@link SensorEventListener},
 * so can subscribe to the {@link SensorEventGenerator}.
 */
public class StreamingFooter extends RelativeLayout implements View.OnClickListener, SensorEventListener {

    private Context mContext;

    private ImageButton mFab;
    private Button mStartStopButton;
    private Button mDisconnectButton;
    private boolean mFabOpen;

    private OnStreamingFooterClickListener mListener;

    private Animation mAnimLeftClose;
    private Animation mAnimRightClose;
    private Animation mAnimFabPressed;
    private Animation.AnimationListener mAnimLeftCloseListener;
    private Animation.AnimationListener mAnimRightCloseListener;

    private Animation mAnimLeftOpen;
    private Animation mAnimRightOpen;
    private Animation mAnimFabNotPressed;

    private SensorState mState = SensorState.UNDEFINED;


    public StreamingFooter(Context context) {
        this(context, null);
    }

    public StreamingFooter(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public StreamingFooter(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.widget_streaming_footer, this);
        mContext = context;

        mFab = findViewById(R.id.fab);
        mStartStopButton = findViewById(R.id.button_start_stop);
        mDisconnectButton = findViewById(R.id.button_disconnect);

        mFab.setOnClickListener(this);
        mStartStopButton.setOnClickListener(this);
        mDisconnectButton.setOnClickListener(this);

        // load animations for closing Start/Stop and Disconnect Buttons and animate FAB
        mAnimLeftClose = AnimationUtils.loadAnimation(mContext, R.anim.view_start_stop_close);
        mAnimRightClose = AnimationUtils.loadAnimation(mContext, R.anim.view_disconnnect_close);
        mAnimFabPressed = AnimationUtils.loadAnimation(mContext, R.anim.fab_pressed);

        mAnimLeftOpen = AnimationUtils.loadAnimation(mContext, R.anim.view_start_stop_open);
        mAnimRightOpen = AnimationUtils.loadAnimation(mContext, R.anim.view_disconnect_open);
        mAnimFabNotPressed = AnimationUtils.loadAnimation(mContext, R.anim.fab_not_pressed);

        mAnimLeftCloseListener = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mStartStopButton.setVisibility(View.INVISIBLE);
                mStartStopButton.setEnabled(false);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        };
        mAnimRightCloseListener = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mDisconnectButton.setVisibility(View.INVISIBLE);
                mDisconnectButton.setEnabled(false);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        };
    }

    /**
     * Sets the listener for UI clicks on the streaming footer.
     *
     * @param listener the listener for UI clicks
     */
    public void setOnStreamingFooterClickListener(OnStreamingFooterClickListener listener) {
        mListener = listener;
    }

    private void animateFAB() {
        if (mFabOpen) {
            mAnimLeftClose.setAnimationListener(mAnimLeftCloseListener);
            mAnimRightClose.setAnimationListener(mAnimRightCloseListener);
            mStartStopButton.startAnimation(mAnimLeftClose);
            mDisconnectButton.startAnimation(mAnimRightClose);
            mFab.startAnimation(mAnimFabPressed);
        } else {
            mStartStopButton.setVisibility(View.VISIBLE);
            mDisconnectButton.setVisibility(View.VISIBLE);
            if (mState.ordinal() >= SensorState.CONNECTED.ordinal()) {
                mStartStopButton.setEnabled(true);
            }
            mDisconnectButton.setEnabled(true);
            mStartStopButton.startAnimation(mAnimLeftOpen);
            mDisconnectButton.startAnimation(mAnimRightOpen);
            mFab.startAnimation(mAnimFabNotPressed);
            mFab.setImageResource(R.drawable.ic_stop);
        }
        if (mListener != null) {
            mListener.onFabClicked(mFabOpen);
        }
        mFabOpen = !mFabOpen;
    }

    private void onStartStopButtonPressed() {
        if (!mStartStopButton.isEnabled()) {
            return;
        }

        if (mListener != null) {
            mListener.onStartStopButtonClicked();
        }
    }

    private void onDisconnectButtonPressed() {
        if (!mDisconnectButton.isEnabled()) {
            return;
        }
        // start animations
        mStartStopButton.startAnimation(mAnimLeftClose);
        mDisconnectButton.startAnimation(mAnimRightClose);
        reset();

        if (mListener != null) {
            mListener.onDisconnectButtonClicked();
        }
    }

    /**
     * Resets the streaming footer, i.e. closes it.
     */
    public void reset() {
        mFab.performClick();
        mFab.setImageResource(R.drawable.ic_play);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.fab) {
            animateFAB();
        } else if (id == R.id.button_start_stop) {
            onStartStopButtonPressed();
        } else if (id == R.id.button_disconnect) {
            onDisconnectButtonPressed();
        }
    }


    @Override
    public void onSensorStateChange(AbstractSensor sensor, SensorState state) {
        switch (state) {
            case STREAMING:
                if (mFabOpen) {
                    mFab.performClick();
                    mStartStopButton.setText(R.string.stop);
                }
                break;
            case CONNECTED:
                if (sensor == null) {
                    mStartStopButton.setEnabled(true);
                    mStartStopButton.setText(R.string.start);
                }
                break;
            case CONNECTION_LOST:
                mFab.performClick();
                mDisconnectButton.performClick();
                break;
            case DISCONNECTED:
                mFab.setImageResource(R.drawable.ic_play);
                break;
        }
        mState = state;
    }

    @Override
    public void onSensorMessage(AbstractSensor sensor, SensorMessage messsage) {

    }
}
