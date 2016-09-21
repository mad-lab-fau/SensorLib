/**
 * Copyright (C) 2015-2016 Digital Sports Group, Friedrich-Alexander University Erlangen-Nuremberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.sensors;

import android.net.Uri;
import android.util.Log;

import com.google.android.gms.wearable.Channel;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Smart Watch listener.
 */
public class SmartWatchListener extends WearableListenerService {

    // Log tag
    private final String TAG = "SW/";


    /**
     * Method onCreate
     */
    @Override
    public void onCreate() {
        super.onCreate();
    }


    /**
     * Called whenever the Wearable is attached to the mobile.
     *
     * @param peer connected node
     */
    @Override
    public void onPeerConnected(Node peer) {
        Log.i(TAG, "Connected: " + peer.getDisplayName() + " (" + peer.getId() + ")");
        super.onPeerConnected(peer);
        SmartWatch.getInstance().sendConnectedWear();
    }


    /**
     * Called whenever the Wearable is detached from the mobile.
     *
     * @param peer disconnected node
     */
    @Override
    public void onPeerDisconnected(Node peer) {
        Log.i(TAG, "Disconnected: " + peer.getDisplayName() + " (" + peer.getId() + ")");
        super.onPeerDisconnected(peer);
        SmartWatch.getInstance().sendDisconnectedWear();
    }

    @Override
    public void onChannelOpened(Channel channel) {
        super.onChannelOpened(channel);
    }

    /**
     * Called whenever a new message sent by the Wearable.
     *
     * @param dataEvents buffer with transmitted data
     */
    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {

        Log.i(TAG, "onDataChanged()");

        for (DataEvent dataEvent : dataEvents) {
            if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
                DataItem dataItem = dataEvent.getDataItem();
                Uri uri = dataItem.getUri();
                String path = uri.getPath();

                if (path.startsWith("/sensors/")) {
                    if (SmartWatch.getInstance() != null) {
                        SmartWatch.getInstance().unpackSensorData(DataMapItem.fromDataItem(dataItem).getDataMap());
                    }
                }
            }
        }
    }

}
