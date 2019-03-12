package de.fau.sensorlib.sensors.enums;

import java.util.Arrays;

public enum NilsPodRfGroup {
    SYNC_GROUP_0(0, 27, new byte[]{(byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF, (byte) 0x19}),
    SYNC_GROUP_1(1, 35, new byte[]{(byte) 0xAB, (byte) 0xCD, (byte) 0xEF, (byte) 0x12, (byte) 0x35}),
    SYNC_GROUP_2(2, 42, new byte[]{(byte) 0xEA, (byte) 0x43, (byte) 0xA7, (byte) 0x35, (byte) 0x42});

    private int syncGroup;
    private int rfChannel;
    private byte[] rfAddress;

    NilsPodRfGroup(int syncGroup, int rfChannel, byte[] rfAddress) {
        this.syncGroup = syncGroup;
        this.rfChannel = rfChannel;
        this.rfAddress = rfAddress;
    }


    @Override
    public String toString() {
        return "[" + syncGroup + "]: Channel " + rfChannel + " @ " + Arrays.toString(rfAddress);
    }
}
