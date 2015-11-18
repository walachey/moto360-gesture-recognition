package de.fu_berlin.inf.moto360;


import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

public class HandheldDataReceiver extends WearableListenerService {

    private static final String TAG = "WATCH LISTENER SERVICE";
    private static final String SENSORS_LIST_PATH = "/list-sensors";
    private static final String DATA_ITEM_RECEIVED_PATH = "/data-item-received";

    public HandheldDataReceiver() {
    }

    @Override
    public void onDestroy() {
        stopSelf();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG, "onMessageReceived was called");
        Toast.makeText(getApplicationContext(),
                "onMessafeReceived from ListenerService was called", Toast.LENGTH_LONG).show();
        Intent intent = new Intent("de.fu_berlin.inf.moto360");
        if (messageEvent.getPath().equalsIgnoreCase(SENSORS_LIST_PATH)) {
            intent.putExtra("sensorsList", messageEvent.getPath());
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
