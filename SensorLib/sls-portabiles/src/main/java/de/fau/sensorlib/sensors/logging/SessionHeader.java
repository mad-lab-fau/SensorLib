/*
 * Copyright (C) 2019 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */

package de.fau.sensorlib.sensors.logging;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;

import de.fau.sensorlib.enums.HardwareSensor;
import de.fau.sensorlib.sensors.enums.NilsPodSensorPosition;
import de.fau.sensorlib.sensors.enums.NilsPodSyncRole;
import de.fau.sensorlib.sensors.enums.NilsPodTerminationSource;

public class SessionHeader {

    private static final String TAG = SessionHeader.class.getSimpleName();

    private ArrayList<HardwareSensor> enabled_sensors = new ArrayList<>();

    private boolean motion_interrupt_enabled;
    private boolean dock_mode_enabled;
    private NilsPodSensorPosition sensor_position;
    private NilsPodTerminationSource session_termination = NilsPodTerminationSource.BLE;
    private int sample_size;

    private double sampling_rate_hz;
    private int acc_range_g;
    private int gyro_range_dps;

    private NilsPodSyncRole sync_role = NilsPodSyncRole.SYNC_ROLE_SLAVE;
    private int sync_distance_ms;
    private String sync_address = "n/a";
    private int sync_channel;
    private int sync_index_start;
    private int sync_index_end;

    private String version_firmware = "n/a";
    private String version_hardware = "n/a";
    private String mac_address = "n/a";

    private int utc_start;
    private int utc_stop;

    private byte[] custom_meta_data;

    private int num_samples;


    public int getSampleSize() {
        return sample_size;
    }

    public void setSampleSize(int sampleSize) {
        this.sample_size = sampleSize;
    }

    public ArrayList<HardwareSensor> getEnabledSensors() {
        return enabled_sensors;
    }

    public void setEnabledSensors(ArrayList<HardwareSensor> enabledSensors) {
        this.enabled_sensors = enabledSensors;
    }

    public double getSamplingRate() {
        return sampling_rate_hz;
    }

    public void setSamplingRate(double samplingRate) {
        this.sampling_rate_hz = samplingRate;
    }

    public NilsPodTerminationSource getTerminationSource() {
        return session_termination;
    }

    public void setTerminationSource(NilsPodTerminationSource terminationSource) {
        this.session_termination = terminationSource;
    }

    public NilsPodSyncRole getSyncRole() {
        return sync_role;
    }

    public void setSyncRole(NilsPodSyncRole syncRole) {
        this.sync_role = syncRole;
    }

    public int getSyncDistance() {
        return sync_distance_ms;
    }

    public void setSyncDistance(int syncDistance) {
        this.sync_distance_ms = syncDistance;
    }

    public void setSyncIndex(int syncIndexStart, int syncIndexEnd) {
        this.sync_index_start = syncIndexStart;
        this.sync_index_end = syncIndexEnd;
    }

    public int getSyncIndexStart() {
        return sync_index_start;
    }

    public int getSyncIndexEnd() {
        return sync_index_end;
    }

    public void setSyncAddress(String syncAddress, int syncChannel) {
        this.sync_address = syncAddress;
        this.sync_channel = syncChannel;
    }

    public String getSyncAddress() {
        return sync_address;
    }

    public int getSyncChannel() {
        return sync_channel;
    }

    public void setCustomMetaData(byte[] customMetaData) {
        this.custom_meta_data = customMetaData;
    }

    public byte[] getCustomMetaData() {
        return custom_meta_data;
    }

    public int getAccRange() {
        return acc_range_g;
    }

    public void setAccRange(int accRange) {
        this.acc_range_g = accRange;
    }

    public int getGyroRange() {
        return gyro_range_dps;
    }

    public void setGyroRange(int gyroRange) {
        this.gyro_range_dps = gyroRange;
    }

    public NilsPodSensorPosition getSensorPosition() {
        return sensor_position;
    }

    public void setSensorPosition(NilsPodSensorPosition sensorPosition) {
        this.sensor_position = sensorPosition;
    }

    public boolean getDockModeEnabled() {
        return dock_mode_enabled;
    }

    public void setDockModeEnabled(boolean enabled) {
        this.dock_mode_enabled = enabled;
    }

    public void setMotionInterruptEnabled(boolean enabled) {
        this.motion_interrupt_enabled = enabled;
    }

    public boolean wasMotionInterruptEnabled() {
        return motion_interrupt_enabled;
    }

    public int getStartDate() {
        return utc_start;
    }

    public void setStartTime(int startDate) {
        this.utc_start = startDate;
    }

    public int getEndDate() {
        return utc_stop;
    }

    public void setEndTime(int endDate) {
        this.utc_stop = endDate;
    }

    public int getSessionSize() {
        return num_samples;
    }

    public void setSessionSize(int sessionSize) {
        this.num_samples = sessionSize;
    }

    public String getFirmwareVersion() {
        return version_firmware;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.version_firmware = firmwareVersion;
    }

    public void setHardwareVersion(String hardwareVersion) {
        this.version_hardware = hardwareVersion;
    }

    public String getHardwareVersion() {
        return version_hardware;
    }

    public void setMacAddress(String macAddress) {
        this.mac_address = macAddress;
    }

    public String getMacAddress() {
        return mac_address;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }


    @Override
    public String toString() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this);
    }
}
