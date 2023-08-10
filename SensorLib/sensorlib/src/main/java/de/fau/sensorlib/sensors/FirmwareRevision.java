package de.fau.sensorlib.sensors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;
import java.util.regex.PatternSyntaxException;

public final class FirmwareRevision implements Comparable<FirmwareRevision> {

    private static final String TAG = FirmwareRevision.class.getSimpleName();

    public int majorVersion = 0;
    public int minorVersion = 0;
    public int patchVersion = 0;

    public int versionCode = 0;

    public FirmwareRevision() {
        this(0, 0, 0);
    }

    public FirmwareRevision(String fwVersion) {
        parseFirmwareVersion(fwVersion);
    }

    public FirmwareRevision(int majorVersion, int minorVersion, int patchVersion) {
        // Development firmware versions are counted from the "end"
        this.majorVersion = majorVersion;
        if (this.majorVersion >= Byte.MAX_VALUE) {
            this.majorVersion = 255 - this.majorVersion;
        }
        this.minorVersion = minorVersion;
        this.patchVersion = patchVersion;
        this.versionCode = getVersionCode(majorVersion, minorVersion, patchVersion);
    }

    public void parseFirmwareVersion(String fwVersion) {
        try {
            String[] split = fwVersion.split("\\.");
            majorVersion = Integer.parseInt(split[0]);
            if (this.majorVersion >= Byte.MAX_VALUE) {
                this.majorVersion = 255 - this.majorVersion;
            }
            minorVersion = Integer.parseInt(split[1]);
            patchVersion = Integer.parseInt(split[2]);
        } catch (PatternSyntaxException | NumberFormatException e) {
            majorVersion = 0;
            minorVersion = 0;
            patchVersion = 0;
        } finally {
            versionCode = getVersionCode(majorVersion, minorVersion, patchVersion);
        }
    }

    public static int getVersionCode(int majorVersion, int minorVersion, int patchVersion) {
        return majorVersion * 10000 + minorVersion * 100 + patchVersion;
    }


    public static int getVersionCode(FirmwareRevision fwVersion) {
        return getVersionCode(fwVersion.majorVersion, fwVersion.minorVersion, fwVersion.patchVersion);
    }


    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof FirmwareRevision) {
            FirmwareRevision fw = (FirmwareRevision) obj;
            return this.versionCode == fw.versionCode;
        }

        return super.equals(obj);
    }

    @Override
    public int compareTo(FirmwareRevision other) {
        return Integer.compare(versionCode, other.versionCode);
    }

    /**
     * Checks whether this firmware revision is the same or newer than the specified other.
     *
     * @param other The other firmware revision to check against
     * @return <code>true</code> if this firmware revision is the same or newer
     * than the specified other,<code>false</code> otherwise
     */
    public boolean isAtLeast(FirmwareRevision other) {
        return compareTo(other) >= 0;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(
                Locale.getDefault(),
                "%d.%d.%d", majorVersion, minorVersion, patchVersion
        );
    }
}
