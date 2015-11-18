package de.fu_berlin.inf.moto360;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

public class WatchDataReceiver extends WearableListenerService {
    public WatchDataReceiver() {
    }

    @Override
    public void onDestroy() {
        stopSelf();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Intent intent = new Intent("de.fu_berlin.inf.moto360");
        intent.putExtra("sensorData", messageEvent.getPath());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
