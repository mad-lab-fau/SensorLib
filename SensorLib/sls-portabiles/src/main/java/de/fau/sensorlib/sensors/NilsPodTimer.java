package de.fau.sensorlib.sensors;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class NilsPodTimer {

    private static final String TAG = NilsPodTimer.class.getSimpleName();

    private Calendar mTimerStart;
    private Calendar mTimerStop;

    private boolean mTimerEnabled;

    private SimpleDateFormat mTimerFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public NilsPodTimer() {
        this(Calendar.getInstance(), Calendar.getInstance(), false);
    }

    public NilsPodTimer(Calendar timerStart, Calendar timerStop, boolean timerEnabled) {
        mTimerStart = timerStart;
        mTimerStop = timerStop;
        mTimerEnabled = timerEnabled;
        mTimerStart.setTimeZone(TimeZone.getTimeZone("UTC"));
        mTimerStop.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public NilsPodTimer(int startHour, int startMinute, int stopHour, int stopMinute, boolean timerEnabled) {
        setStartTimer(startHour, startMinute);
        setStopTimer(stopHour, stopMinute);
        setTimerEnabled(timerEnabled);
    }

    public void setStartTimer(int startHour, int startMinute) {
        mTimerStart = Calendar.getInstance();
        mTimerStart.set(Calendar.HOUR_OF_DAY, startHour);
        mTimerStart.set(Calendar.MINUTE, startMinute);
        mTimerStart.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public Calendar getStartTimer() {
        return mTimerStart;
    }

    public String getStartTimerString() {
        return mTimerFormat.format(mTimerStart.getTime());
    }

    public int getStartHour() {
        return mTimerStart.get(Calendar.HOUR_OF_DAY);
    }

    public int getStartMinute() {
        return mTimerStart.get(Calendar.MINUTE);
    }

    public void setStopTimer(int stopHour, int stopMinute) {
        mTimerStop = Calendar.getInstance();
        mTimerStop.set(Calendar.HOUR_OF_DAY, stopHour);
        mTimerStop.set(Calendar.MINUTE, stopMinute);
        mTimerStop.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public Calendar getStopTimer() {
        return mTimerStop;
    }

    public int getStopHour() {
        return mTimerStop.get(Calendar.HOUR_OF_DAY);
    }

    public int getStopMinute() {
        return mTimerStop.get(Calendar.MINUTE);
    }

    public String getStopTimerString() {
        return mTimerFormat.format(mTimerStop.getTime());
    }

    public boolean isTimerEnabled() {
        return mTimerEnabled;
    }

    public void setTimerEnabled(boolean timerEnabled) {
        mTimerEnabled = timerEnabled;
    }
}
