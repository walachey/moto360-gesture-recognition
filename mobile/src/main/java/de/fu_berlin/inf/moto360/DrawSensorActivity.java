package de.fu_berlin.inf.moto360;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;

public class DrawSensorActivity extends Activity {

    private View mView;
    private Paint mPaint;

    private int lHeight = 0;
    private int lWidth = 0;
    private int x_cur = 0;
    private int y_cur = 0;
    private int xx = -10;
    private int yy = -10;

    // Output file used for logging sensor data.
    // This is opened lazily when first receiving data.
    OutputStreamWriter sensorDataOutput = null;
    FileOutputStream sensorDataOutStream = null;

    // Make sure that the logged time starts at 0 by remembering when we started.
    long startingTime = 0;

    // The actual actor that reacts to the inputs.
    GestureRecognitionActor gestureRecognitionActor = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draw_sensor);

        startService(new Intent(this, WatchDataReceiver.class));

        mView = new DrawingView(this);
        LinearLayout layout = (LinearLayout) findViewById(R.id.myDrawing);//TODO add myDrawing to sources
        layout.addView(mView, new ViewGroup.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.MATCH_PARENT));

        lHeight = this.getResources().getDisplayMetrics().heightPixels;   //layout.getHeight();
//        lHeight = mView.getLayoutParams().height;
//        lHeight = mView.getHeight();

        lWidth =    this.getResources().getDisplayMetrics().widthPixels; //layout.getWidth();
 //       lWidth = mView.getLayoutParams().width;
   //     lWidth = mView.getMeasuredWidth();
        Log.d("HANDHELD", "onCreate: layer Height "+lHeight+" \tlayer Width "+ lWidth);
        initPaint();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sensorDataOutput != null) {
            try {
                sensorDataOutput.close();
            } catch (IOException e) {
                // I don't really care at this point.
            }
        }
        if (sensorDataOutStream != null)
            try {
                sensorDataOutStream.close();
            } catch (IOException e) {
                // As above.
            }
        stopService(new Intent(this, WatchDataReceiver.class));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

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

    private void initPaint() {
        mPaint = new Paint();
        mPaint.setDither(true);
        mPaint.setColor(0xFFFFFF00);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(8);
    }

    class DrawingView extends View {
        private Path mPath;
        private Bitmap mBitmap;
        private Canvas mCanvas;
        private ArrayList<PathWithPaint> _graphics1 = new ArrayList<>();
        private View view;

        public DrawingView(Context context) {
            super(context);
            mPath = new Path();
            mBitmap = Bitmap.createBitmap(820, 480, Bitmap.Config.ARGB_8888);
            mCanvas = new Canvas(mBitmap);
            this.setBackgroundColor(Color.BLACK);
            view = findViewById(R.id.myDrawing);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            PathWithPaint pwp = new PathWithPaint();
            mCanvas.drawPath(mPath, mPaint);
            mPath.lineTo(xx, yy); //TODO initialize floats for drawing
//            mPath.lineTo(lWidth / 2, lHeight/2 );
            pwp.setPath(mPath);
            pwp.setPaint(mPaint);
            _graphics1.add(pwp);
            invalidate();

            if(_graphics1.size() > 0) {
                canvas.drawPath(_graphics1.get(_graphics1.size() - 1).getPath(),
                                _graphics1.get(_graphics1.size() - 1).getPaint());
            }
        }
    }

    public void processData(String message) {
        // Parse into double array.
        final String[] splitSensorReadings = message.split("[\\[\\],\\s]");
        String[] cleanedSensorReadings = new String[8];
        final double[] wearableInput = new double[6];
        int wearableInputIndex = 0;
        final int wearableInputIndexOffset = 1;
        for (String valueString : splitSensorReadings) {
            if (valueString.isEmpty()) continue;
            cleanedSensorReadings[wearableInputIndex] = valueString;
            if (wearableInputIndex++ < wearableInputIndexOffset) continue;
            final int targetIndex = wearableInputIndex - wearableInputIndexOffset - 1;
            if (targetIndex >= wearableInput.length) continue;
            wearableInput[targetIndex] = Double.parseDouble(valueString);
        }

        // Open output file if not yet done.
        if (sensorDataOutput == null) {
            try {
                File logfile = new File("/sdcard/sensor_out.csv");
                logfile.delete();
                logfile.createNewFile();
                sensorDataOutStream = new FileOutputStream(logfile);
                sensorDataOutput = new OutputStreamWriter(sensorDataOutStream);
                sensorDataOutput.write("handheld,wear,x,y,z,phi_x,phi_y,phi_z,gyro_acc");
                for (String t : Arrays.asList("ma1", "ma2", "ma3")){
                    for (String f : Arrays.asList("x", "y", "z", "phi_x", "phi_y", "phi_z")){
                        sensorDataOutput.write("," + t + "_" + f);
                    }
                }
                sensorDataOutput.write("\n");
            } catch (IOException e) {
                Log.e("Exception", "Opening file failed: " + e.toString());
            }
        }

        // Not all data from the wearable are necessarily features.
        double [] rawFeatures = Arrays.copyOfRange(wearableInput, 0, 6);
        gestureRecognitionActor.input(rawFeatures);

        // If we have a logfile, write the output.
        if (sensorDataOutput != null) {
            if (startingTime == 0)
                startingTime = System.nanoTime();
            try {
                // Write additional mobile data first.
                sensorDataOutput.write("" + (System.nanoTime() - startingTime));
                // Then comes the original message of the wearable.
                for (String valueString : cleanedSensorReadings) {
                    sensorDataOutput.write(", " + valueString);
                }
                // And finally the transformed features.
                for (double feature : gestureRecognitionActor.lastInputFeatures) {
                    sensorDataOutput.write(", " + Double.toString(feature));
                }
                sensorDataOutput.write("\n");
                sensorDataOutput.flush();
            } catch (IOException e) {
                Log.e("Exception", "Writing to file failed: " + e.toString());
            }
        }

        int x = (int)(Math.round(10.0f * wearableInput[1]));
        int y = (int)(Math.round(10.0f * wearableInput[2]));


        xx += x;
        x_cur = x;
        if(xx < 0 || xx > lWidth) {xx = lWidth/2; yy = lHeight/2;}

        yy += y;
        y_cur = y;
        if(yy < 0 || yy > lHeight) {xx = lWidth/2; yy = lHeight/2;}
        //Log.d("HANDHELD", "processData: layer Height "+lHeight+"\tlayer Width "+ lWidth);
    }

    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("sensorData");
            processData(message);
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this)
                             .registerReceiver(messageReceiver, new IntentFilter("de.fu_berlin.inf.moto360"));
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
        super.onPause();
    }
}
