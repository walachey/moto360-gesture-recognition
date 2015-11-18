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

import java.util.ArrayList;


public class DrawSensorActivity extends Activity {

    private View mView;
    private Paint mPaint;

    private int lHeight = 0;
    private int lWidth = 0;
    private int x_cur = 0;
    private int y_cur = 0;
    private int xx = 20;
    private int yy = 20;

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
        String s[] = message.substring(1, message.length() - 1).split(",");
        int x = Math.round(Float.parseFloat(s[0]));
        int y = Math.round(Float.parseFloat(s[1]));

        if(x != x_cur) {
            xx += x;
            x_cur = x;
            if(xx < 0 || xx > lWidth) {xx = lWidth/2; yy = lHeight/2;}
        }
        if(y != y_cur) {
            yy += y;
            y_cur = y;
            if(yy < 0 || yy > lHeight) {xx = lWidth/2; yy = lHeight/2;}
        }
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
