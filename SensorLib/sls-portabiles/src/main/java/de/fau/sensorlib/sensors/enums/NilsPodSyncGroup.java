package de.fau.sensorlib.sensors.enums;

import java.util.Arrays;

public enum NilsPodSyncGroup {
    SYNC_GROUP_0(0, 27, new byte[]{(byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF, (byte) 0x19}),
    SYNC_GROUP_1(1, 35, new byte[]{(byte) 0xAB, (byte) 0xCD, (byte) 0xEF, (byte) 0x12, (byte) 0x35}),
    SYNC_GROUP_2(2, 42, new byte[]{(byte) 0xEA, (byte) 0x43, (byte) 0xA7, (byte) 0x35, (byte) 0x42}),
    SYNC_GROUP_3(3, 31, new byte[]{(byte) 0xDF, (byte) 0xAA, (byte) 0x12, (byte) 0x7C, (byte) 0x1B}),
    SYNC_GROUP_4(4, 29, new byte[]{(byte) 0xA1, (byte) 0xDB, (byte) 0xD7, (byte) 0x14, (byte) 0x9C}),
    SYNC_GROUP_5(5, 33, new byte[]{(byte) 0x9E, (byte) 0xE6, (byte) 0x5B, (byte) 0x85, (byte) 0xC2}),
    SYNC_GROUP_6(6, 37, new byte[]{(byte) 0x72, (byte) 0xB4, (byte) 0x5A, (byte) 0xCC, (byte) 0x62}),
    SYNC_GROUP_7(7, 39, new byte[]{(byte) 0xFE, (byte) 0x47, (byte) 0xA2, (byte) 0xD4, (byte) 0x1C}),
    SYNC_GROUP_8(8, 41, new byte[]{(byte) 0x3D, (byte) 0xFC, (byte) 0xD0, (byte) 0x3C, (byte) 0xE7}),
    SYNC_GROUP_9(9, 43, new byte[]{(byte) 0x7C, (byte) 0x6F, (byte) 0xE0, (byte) 0x2B, (byte) 0x9F});

    private int syncGroup;
    private int rfChannel;
    private byte[] rfAddress;

    NilsPodSyncGroup(int syncGroup, int rfChannel, byte[] rfAddress) {
        this.syncGroup = syncGroup;
        this.rfChannel = rfChannel;
        this.rfAddress = rfAddress;
    }


    @Override
    public String toString() {
        return "[" + syncGroup + "]: Channel " + rfChannel + " @ " + Arrays.toString(rfAddress);
    }
}
