package de.fu_berlin.inf.moto360;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;



public class MainActivity extends Activity implements SensorEventListener,
        MessageApi.MessageListener, GoogleApiClient.ConnectionCallbacks {

    private static final String LOG = "WATCH";
    private static final String LIST_SENSORS = "/list_sensors";
    private static final String SET_DELTA = "/set_delta";
    private static final String SENSOR_DATA = "/sensor_data";

    private SensorManager   sensorManager;
    private Sensor          accelerometer;
    private Sensor          gyroscope;

    private GoogleApiClient googleApiClient;

    private TextView mTextView;
    private ArrayAdapter<String> mAdapter;

    private double delta = 0.2;
    private boolean trigger = true;

    // This is a short time integral over the gyroscope measurements.
    // The data is sent once as new accelerometer input arrives (the acc. is the leading sensor).
    double[] gyroscope_state = {0., 0., 0., 0.};

    // Startup nanoTime() to keep the absolute values tighter together.
    long startupNanoTime = 0;
    // The device will vibrate every second to easier allow for synchronized movement recording.
    long lastVibrateNanoTime = 0;
    Vibrator vibratorDevice = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
            }
        });

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);

        // We need the gyroscope. It's probably better for gestures as the acceleration is inaccurate and biased.
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);

        if(accelerometer != null) {
            Log.v(LOG, ": Accelerometer registered.");
            Toast.makeText(getApplicationContext(),
                    "onCreate called", Toast.LENGTH_LONG).show();
        } else {
            Log.e(LOG, ": Registering accelerometer failed.");
        }

        vibratorDevice = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        googleApiClient = new GoogleApiClient.Builder(this).addApi(Wearable.API).build();
//        googleApiClient.connect();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(googleApiClient != null) {
            googleApiClient.connect();
        }
//        Toast.makeText(getApplicationContext(),
//                "onStart called", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Wearable.MessageApi.addListener(googleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Wearable.MessageApi.removeListener(googleApiClient,this);
        googleApiClient.unregisterConnectionCallbacks(this);
        googleApiClient.disconnect();
        sensorManager.unregisterListener(this, accelerometer);
    }

    // Saves the gyroscope readings, integrating over them if previous values are available.
    void pushGyroscopeReadings(double[] values) {
        for (int i = 0; i < 3; ++i)
            gyroscope_state[i] += values[i];
        gyroscope_state[3] = values[3];
    }

    // Retrieves the (possibly integrated) last gyroscope readings and resets the integral.
    double[] popGyroscopeReadings() {
        double[] values = gyroscope_state;
        gyroscope_state = new double[] {0., 0., 0., 0.};
        return values;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // The handheld device is not currently requesting data from us?
        if (!trigger) return;

        // The accelerometer is the leading sensor and will trigger a new message.
        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {

            // This will hold the output data as a serializable list.
            int dataOffset = 0;
            double[] sensorReadings = new double[7];

            // Get the time in nano seconds since the app started and add it to the data.
            if (startupNanoTime == 0)
                startupNanoTime = System.nanoTime();
            final long timePassed = System.nanoTime() - startupNanoTime;

            // The watch will vibrate regularly as a cheap metronome.
            final long vibrateTimePassed = timePassed - lastVibrateNanoTime;
            final long vibrateMillisecondsPassed = vibrateTimePassed / 1000 / 1000;
            final long timeBetweenVibrations = 1000;
            if (vibrateMillisecondsPassed >= timeBetweenVibrations) {
                // Is this the first ring? Then we need to just initialize the value.
                if (lastVibrateNanoTime == 0) {
                    lastVibrateNanoTime = timePassed;
                }
                else // Otherwise, we add the vibration delay to not introduce an offset that accumulates over time.
                {
                    lastVibrateNanoTime += timeBetweenVibrations * 1000 * 1000;
                }
                vibratorDevice.vibrate(100);
            }

            // Now actually apply the filtering to the data.
            for (int i = 0; i < 3; ++i)
                sensorReadings[dataOffset++] = event.values[i];

            // Append the last gyroscope readings integral to the output.
            double[] lastGyroscopeReadings = popGyroscopeReadings();
            for (int i = 0; i < 4; ++i)
                sensorReadings[dataOffset++] = lastGyroscopeReadings[i];

            // Send the filtered and concatenated data to the handheld device.
            sendMessageToHandheld("" + timePassed + ", " + Arrays.toString(sensorReadings));

            return;
        }

        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            final float [] R = event.values;
            // Append the gyroscope accuracy state.
            double [] allData = {R[0], R[1], R[2], (double)event.accuracy};
            // Save the values for when the accelerometer reports back next.
            pushGyroscopeReadings(allData);
            return;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //TODO: Not needed, leave blank
    }

    public void sendMessageToHandheld(String msg) {
        if(googleApiClient == null) return;

        final String message = msg;

        // use the api client to send the accelerometer values to our handheld
        final PendingResult<NodeApi.GetConnectedNodesResult> nodes = Wearable.NodeApi.getConnectedNodes(googleApiClient);
        nodes.setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult result) {
                final List<Node> nodes = result.getNodes();
                if (nodes != null) {
                    for (int i = 0; i < nodes.size(); i++) {
                        final Node node = nodes.get(i);
                        Wearable.MessageApi.sendMessage(googleApiClient, node.getId(), message, null);
                    }
                }
            }
        });
    }

    private void sendSensorList() {

        List<Sensor> deviceSensors = null;
        deviceSensors = sensorManager.getSensorList(Sensor.TYPE_ALL);

        sendMessageToHandheld(String.valueOf(deviceSensors.toString()));
    }

    @Override
    public void onMessageReceived(final MessageEvent messageEvent) {

        Log.d(LOG, "MESSAGE RECIEVED from " + messageEvent.getSourceNodeId() + " : " + messageEvent.getData().toString());
        Toast.makeText(getApplicationContext(),
                "onMessageReceived called", Toast.LENGTH_LONG).show();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (messageEvent.getPath().equalsIgnoreCase(LIST_SENSORS)) {

                    mAdapter.add(new String(messageEvent.getData()));
                    mAdapter.notifyDataSetChanged();
                }
                else if(messageEvent.getPath().equalsIgnoreCase(SET_DELTA)){
                    delta = Double.valueOf(String.valueOf(messageEvent.getData()));
                }
                else if(messageEvent.getPath().equalsIgnoreCase(SENSOR_DATA)){
                    trigger = true;
                }
            }
        });

    }


}
