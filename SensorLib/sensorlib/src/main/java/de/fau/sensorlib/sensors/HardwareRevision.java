package de.fau.sensorlib.sensors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class HardwareRevision {

    private String hwRevision;
    private String hwRevisionName;

    public HardwareRevision() {
        this("", "");
    }

    public HardwareRevision(String hwRevision) {
        this(hwRevision, "");
    }

    public HardwareRevision(String hwRevision, String hwRevisionName) {
        this.hwRevision = hwRevision;
        this.hwRevisionName = hwRevisionName;
    }

    public String getHwRevision() {
        return hwRevision;
    }

    public String getHwRevisionName() {
        return hwRevisionName;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof HardwareRevision) {
            HardwareRevision hw = (HardwareRevision) obj;
            return this.hwRevision.equals(hw.hwRevision);
        }
        return super.equals(obj);
    }

    @NonNull
    @Override
    public String toString() {
        return hwRevisionName.isEmpty() ? hwRevision : hwRevision + " (" + hwRevisionName + ")";
    }
}
