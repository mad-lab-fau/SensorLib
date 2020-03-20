package de.fau.sensorlib.sensors;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class NilsPodAlarm {

    private Calendar mStartAlarm;
    private Calendar mStopAlarm;

    private boolean mAlarmEnabled;

    private SimpleDateFormat mDf = new SimpleDateFormat("hh:mm", Locale.getDefault());

    public NilsPodAlarm() {
        this(Calendar.getInstance(), Calendar.getInstance(), false);
    }

    public NilsPodAlarm(Calendar startAlarm, Calendar stopAlarm, boolean alarmEnabled) {
        mStartAlarm = startAlarm;
        mStopAlarm = stopAlarm;
        mAlarmEnabled = alarmEnabled;
    }

    public NilsPodAlarm(int startHour, int startMinute, int stopHour, int stopMinute, boolean alarmEnabled) {
        setStartAlarm(startHour, startMinute);
        setStopAlarm(stopHour, stopMinute);
        setAlarmEnabled(alarmEnabled);
    }

    public void setStartAlarm(int startHour, int startMinute) {
        mStartAlarm = Calendar.getInstance();
        mStartAlarm.set(Calendar.HOUR_OF_DAY, startHour);
        mStartAlarm.set(Calendar.MINUTE, startMinute);
    }

    public Calendar getStartAlarm() {
        return mStartAlarm;
    }

    public String getStartAlarmString() {
        return mDf.format(mStartAlarm);
    }

    public int getStartHour() {
        return mStartAlarm.get(Calendar.HOUR_OF_DAY);
    }

    public int getStartMinute() {
        return mStartAlarm.get(Calendar.MINUTE);
    }

    public void setStopAlarm(int stopHour, int stopMinute) {
        mStopAlarm = Calendar.getInstance();
        mStopAlarm.set(Calendar.HOUR_OF_DAY, stopHour);
        mStopAlarm.set(Calendar.MINUTE, stopMinute);
    }

    public Calendar getStopAlarm() {
        return mStopAlarm;
    }

    public int getStopHour() {
        return mStopAlarm.get(Calendar.HOUR_OF_DAY);
    }

    public int getStopMinute() {
        return mStopAlarm.get(Calendar.MINUTE);
    }

    public String getStopAlarmString() {
        return mDf.format(mStopAlarm);
    }

    public boolean isAlarmEnabled() {
        return mAlarmEnabled;
    }

    public void setAlarmEnabled(boolean alarmEnabled) {
        mAlarmEnabled = alarmEnabled;
    }
}
