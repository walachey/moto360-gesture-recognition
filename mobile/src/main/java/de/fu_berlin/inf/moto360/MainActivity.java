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
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.net.DatagramPacket;
import java.nio.charset.Charset;

import de.fu_berlin.inf.moto360.util.UDPInterface;

import static java.lang.System.out;


public class MainActivity extends Activity implements SensorEventListener {

    private static final String TAG = "HANDHELD";
    private SensorManager mSensorManager;
    private Sensor mSensor;
    float[] sensorValues = new float[]{(float) 0.0, (float) 0.0, (float) 0.0};
    float x,y,z;

    TextView tvX;
    TextView tvY;
    TextView tvZ;
    TextView mtvX, mtvY, mtvZ;
    TextView gest1tv;
    EditText et;
    EditText ip_field;
    int gest1Counter = 0;

    File file;
    FileOutputStream stream = null;
    private Context context = null;
    public static boolean trigger = false;
    private boolean state1 = false, state2 = false, state3 = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set (hardcoded) target port for UDP interface.
        UDPInterface.getInstance().setPort(3012);

        startService(new Intent(this, WatchDataReceiver.class));

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        tvX= (TextView) findViewById(R.id.axisX);
        tvY= (TextView) findViewById(R.id.axisY);
        tvZ= (TextView) findViewById(R.id.axisZ);
        mtvX = (TextView) findViewById(R.id.textView2X);
        mtvY = (TextView) findViewById(R.id.textView2Y);
        mtvZ = (TextView) findViewById(R.id.textView2Z);
        gest1tv = (TextView) findViewById(R.id.textGestute1);
        et = (EditText) findViewById(R.id.deltaTextField);
        et.setText("0.2");
        ip_field = (EditText) findViewById(R.id.ip_field);

        final Button ip_button = (Button) findViewById(R.id.ip_button);
        ip_button.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                final String IPString = ip_field.getText().toString();
                UDPInterface.getInstance().setTarget(IPString);
                Toast.makeText(getApplicationContext(),
                        "IP set to: " + IPString,
                        Toast.LENGTH_SHORT).show();
                Log.d("ip_field edited", IPString);
                Log.d( "ip set to", ip_field.getText().toString());
            }
        });

        final Button left_button = (Button) findViewById(R.id.left_button);
        left_button.setOnClickListener(new View.OnClickListener() {
           public void onClick(View view) {
               UDPInterface.getInstance().send("left");
               Toast.makeText(getApplicationContext(),
                       "left ", Toast.LENGTH_SHORT).show();
           }
        });

        final Button right_button = (Button) findViewById(R.id.right_button);
        right_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                UDPInterface.getInstance().send("right");
                Toast.makeText(getApplicationContext(),
                        "right ", Toast.LENGTH_SHORT).show();
            }
        });

        Button setDelta = (Button) findViewById(R.id.setDeltaButton);
        setDelta.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view){
                Toast.makeText(getApplicationContext(),
                        "set delta to " + et.getText().toString() , Toast.LENGTH_LONG).show();
                Log.d("EditText", et.getText().toString());
                //send delta to the watch method

            }
        });


        Button stopButton = (Button) findViewById(R.id.stopButton);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPause();
            }
        });

        Button startButton = (Button) findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mtvX.setText(Float.toString(sensorValues[0]).substring(0,6));
                mtvY.setText(Float.toString(sensorValues[1]).substring(0,6));
                mtvZ.setText(Float.toString(sensorValues[2]).substring(0,6));

                onResume();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("sensorData");

            Log.d("HANDHELD", "Main Activity messageReceiver " + message);
        }
    };


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private BroadcastReceiver messageReceiver2 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String message = intent.getStringExtra("sensorData");
            String s[] = message.split(",");

            x = Float.parseFloat(s[0]);
            y = Float.parseFloat(s[1]);
            z = Float.parseFloat(s[2]);

            mtvX.setText(String.format("%.2f", x));
            mtvY.setText(String.format("%.2f", y));
            mtvZ.setText(String.format("%.2f", z));
        }

    };
    
    public void send_udp_packet( String ip_address, int port, String message){
        byte[] buffer = message.getBytes(Charset.forName("UTF-8"));
        try {
            InetAddress address = InetAddress.getByName(ip_address);

            DatagramPacket packet = new DatagramPacket(
                    buffer, buffer.length, address, port
            );
            DatagramSocket datagramSocket = new DatagramSocket();
            datagramSocket.send(packet);
            datagramSocket.close();
            Log.d( "packet send", message + "send to " + presentation_device_ip);
        }
        catch (SocketException e) {
            System.out.println("something went wrong with the socket :( \n");
            e.printStackTrace(System.out);
        } catch (UnknownHostException e) {
            System.out.println("something went wrong with the host :( \n");
            e.printStackTrace(System.out);
        } catch (IOException e) {
            System.out.println("something went wrong with the IO :( \n");
            e.printStackTrace(System.out);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        sensorValues[0] = sensorEvent.values[0];
        sensorValues[1] = sensorEvent.values[1];
        sensorValues[2] = sensorEvent.values[2];

        //tvX.setText(Float.toString(sensorValues[0]).substring(0,4));
        //tvY.setText(Float.toString(sensorValues[1]).substring(0,4));
        //tvZ.setText(Float.toString(sensorValues[2]).substring(0,4));



        if (trigger){
            gest1tv.setText(Integer.toString(gest1Counter));
            gestureRecognition(sensorEvent.values);
        }

        //Log.d(TAG, "\tX: " + sensorValues[0] + "\tY: " + sensorValues[1] + "\tZ: " + sensorValues[2] + "\ttimestamp: " + sensorEvent.timestamp);
    }

    private void gestureRecognition(float[] sensorValues){

        if(sensorValues[1]<sensorValues[2]){
            state1 = true;
        }
        if(state1 && sensorValues[2]<sensorValues[1]){
            state2 = true;
        }
        if (state1 && state2 && sensorValues[1]<sensorValues[2]){
            gest1Counter++;
            Toast.makeText(getApplicationContext(),
                    Integer.toString(gest1Counter), Toast.LENGTH_SHORT).show();
            gest1tv.setText(Integer.toString(gest1Counter));
            state1 = false; state2 = false; state3 = false;
        }
    }


    public void startGestureRecognition(View view){
        trigger= true;
        Toast.makeText(getApplicationContext(),
                "trigger: true", Toast.LENGTH_SHORT).show();

    }

    public void stopGestureRecognition(View view){
        trigger = false;
        Toast.makeText(getApplicationContext(),
                "trigger: false", Toast.LENGTH_SHORT).show();
        gest1Counter = 0;
        gest1tv.setText("Gesture 1");
    }


    public void listSensors(View view) {
        Intent intent = new Intent(this, SensorikTestActivity.class);
        startActivity(intent);
    }


    public void drawSensors(View view) {
        Intent intent = new Intent(this, DrawSensorActivity.class);
        startActivity(intent);
    }

    @Override
    public void onStop(){
        super.onStop();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        mSensorManager.unregisterListener(this);
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

}
