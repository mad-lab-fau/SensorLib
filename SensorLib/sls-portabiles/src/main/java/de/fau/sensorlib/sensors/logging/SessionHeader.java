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

import java.util.Date;
import java.util.HashMap;

import de.fau.sensorlib.enums.HardwareSensor;
import de.fau.sensorlib.sensors.enums.NilsPodSyncGroup;
import de.fau.sensorlib.sensors.enums.NilsPodSyncRole;
import de.fau.sensorlib.sensors.enums.NilsPodTerminationSource;

public class SessionHeader {

    private static final String TAG = SessionHeader.class.getSimpleName();

    private String sensorName = "n/a";
    private String firmwareVersion = "0.0.0";
    private String modelNumber = "n/a";
    private int sampleSize;
    private double samplingRate;
    private HashMap<HardwareSensor, Boolean> enabledSensors = new HashMap<>();
    private NilsPodTerminationSource terminationSource =
            NilsPodTerminationSource.BLE;
    private NilsPodSyncRole syncRole = NilsPodSyncRole.SYNC_ROLE_SLAVE;
    private int syncDistance;
    private NilsPodSyncGroup syncGroup = NilsPodSyncGroup.SYNC_GROUP_0;
    private int accRange;
    private int gyroRange;
    private int sensorPosition;
    private int specialFunction;
    private String startDate = new Date(0).toString();
    private String endDate = new Date(0).toString();
    private int sessionSize;

    public int getSampleSize() {
        return sampleSize;
    }

    public void setSampleSize(int sampleSize) {
        this.sampleSize = sampleSize;
    }

    public HashMap<HardwareSensor, Boolean> getEnabledSensors() {
        return enabledSensors;
    }

    public void setEnabledSensors(HashMap<HardwareSensor, Boolean> enabledSensors) {
        this.enabledSensors = enabledSensors;
    }

    public double getSamplingRate() {
        return samplingRate;
    }

    public void setSamplingRate(double samplingRate) {
        this.samplingRate = samplingRate;
    }

    public NilsPodTerminationSource getTerminationSource() {
        return terminationSource;
    }

    public void setTerminationSource(NilsPodTerminationSource terminationSource) {
        this.terminationSource = terminationSource;
    }

    public NilsPodSyncRole getSyncRole() {
        return syncRole;
    }

    public void setSyncRole(NilsPodSyncRole syncRole) {
        this.syncRole = syncRole;
    }

    public int getSyncDistance() {
        return syncDistance;
    }

    public void setSyncDistance(int syncDistance) {
        this.syncDistance = syncDistance;
    }

    public NilsPodSyncGroup getRfGroup() {
        return syncGroup;
    }

    public void setSyncGroup(NilsPodSyncGroup syncGroup) {
        this.syncGroup = syncGroup;
    }

    public int getAccRange() {
        return accRange;
    }

    public void setAccRange(int accRange) {
        this.accRange = accRange;
    }

    public int getGyroRange() {
        return gyroRange;
    }

    public void setGyroRange(int gyroRange) {
        this.gyroRange = gyroRange;
    }

    public int getSensorPosition() {
        return sensorPosition;
    }

    public void setSensorPosition(int sensorPosition) {
        this.sensorPosition = sensorPosition;
    }

    public int getSpecialFunction() {
        return specialFunction;
    }

    public void setSpecialFunction(int specialFunction) {
        this.specialFunction = specialFunction;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public int getSessionSize() {
        return sessionSize;
    }

    public void setSessionSize(int sessionSize) {
        this.sessionSize = sessionSize;
    }

    public String getSensorName() {
        return sensorName;
    }

    public void setSensorName(String sensorName) {
        this.sensorName = sensorName;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }


    @Override
    public String toString() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this);
    }

    public String getModelNumber() {
        return modelNumber;
    }

    public void setModelNumber(String modelNumber) {
        this.modelNumber = modelNumber;
    }
}
