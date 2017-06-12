package com.zugaldia.robocar.mobile.controller;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.zugaldia.robocar.mobile.R;
import com.zugaldia.robocar.mobile.client.RobocarRestClient;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

public class TiltControllerActivity extends AppCompatActivity implements SensorEventListener, CompoundButton.OnCheckedChangeListener, NavigationView.OnNavigationItemSelectedListener {

    private SensorManager mSensorManager;
    Sensor mAccelerometer;
    Sensor mMagnetometer;
    float[] mGravity;
    float[] mGeomagnetic;

    @BindView(R.id.pitchTextView)
    TextView mPitchTextView;

    @BindView(R.id.rollTextView)
    TextView mRollTextView;

    @BindView(R.id.leftSpeedTextView)
    TextView mLeftSpeedTextView;

    @BindView(R.id.rightSpeedTextView)
    TextView mRightSpeedTextView;


    @BindView(R.id.toggleButton)
    ToggleButton mToggleButton;

    @BindView(R.id.drawer_layout)
    DrawerLayout drawer;

    private String mBaseUrl;

    int SPEED_HI = 255;
    int SPEED_LOW = 95;

    int mLastLeftSpeed;
    int mLastRightSpeed;
    int mLastRoll;
    int mLastPitch;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_tilt_controller);
        ButterKnife.bind(this);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED);

        mToggleButton.setOnCheckedChangeListener(this);

        initNavigationDrawer();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = event.values;
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED)
            mGeomagnetic = event.values;
        if ((mGravity == null) || (mGeomagnetic == null))
            return;

        float[] R = new float[9];
        float[] I = new float[9];
        if (!SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic))
            return;

        float[] orientation = new float[3];
        SensorManager.getOrientation(R, orientation);

        if (orientation == null)
            return;

        double rollAngle = orientation[1] * 180 / Math.PI;
        double pitchAngle = orientation[2] * 180 / Math.PI;

        if(notWithinAngleTolerance((int)rollAngle, mLastRoll))
            this.mRollTextView.setText(String.format("%.0f", (rollAngle)));
        if(notWithinAngleTolerance((int)pitchAngle,mLastPitch))
            this.mPitchTextView.setText(String.format("%.0f", (pitchAngle)));

        mLastPitch = (int)pitchAngle;
        mLastRoll = (int) rollAngle;

        calculateAndSetSpeed(rollAngle,pitchAngle);
    }

    private void calculateAndSetSpeed(double rollAngle,double pitchAngle){

        boolean isForward = pitchAngle > -75 && pitchAngle < 15;
        boolean isBackward = pitchAngle < -100;
        boolean isForwardOrBackward = isForward || isBackward;
        boolean isLeftTurn = rollAngle > 15;
        boolean isRightTurn = rollAngle < -15;


        if(!isForwardOrBackward && isLeftTurn) {
            if(mLastLeftSpeed!=-255 && mLastRightSpeed!=255)
                setSpeed(-255, 255);
            return;
        }
        if(!isForwardOrBackward && isRightTurn) {
            if(mLastLeftSpeed!=255 && mLastRightSpeed!=-255)
                setSpeed(255, -255);
            return;
        }

       if(!isForwardOrBackward) {
           if(mLastLeftSpeed!=0 || mLastRightSpeed!=0)
               setSpeed(0, 0);
           return;
       }

       if(isForward ){
           if(!isLeftTurn && !isRightTurn) {
               SpeedValueInterpolator svi = new SpeedValueInterpolator()
                       .setValueRange(-75, -15)
                       .setSpeedRange(SPEED_LOW, SPEED_HI)
                       .setSpeedStep(-1);
               float speed = svi.getSpeedForValue((float) pitchAngle);

               setSpeedIfNecessary((int) speed, (int) speed);
           }
           else if (isLeftTurn || isRightTurn){
               double rollAngleAbsolute = Math.abs(rollAngle);

               SpeedValueInterpolator svi = new SpeedValueInterpolator()
                       .setValueRange(15,75)
                       .setSpeedRange(SPEED_LOW,SPEED_HI)
                       .setSpeedStep(1);
               float speed = 255 - svi.getSpeedForValue((float)rollAngleAbsolute);

               if(isLeftTurn)
                   setSpeedIfNecessary((int)speed,255);
               else
                   setSpeedIfNecessary(255,(int)speed);
           }
       }

        if(isBackward){
            if(!isLeftTurn && !isRightTurn) {
                SpeedValueInterpolator svi = new SpeedValueInterpolator()
                        .setValueRange(-100, -135)
                        .setSpeedRange(SPEED_LOW, SPEED_HI)
                        .setSpeedStep(1);
                float speed = -svi.getSpeedForValue((float) pitchAngle);
                setSpeedIfNecessary((int) speed, (int) speed);
            }
            else if(isLeftTurn || isRightTurn){
                double rollAngleAbsolute = Math.abs(rollAngle);

                SpeedValueInterpolator svi = new SpeedValueInterpolator()
                        .setValueRange(15,75)
                        .setSpeedRange(SPEED_LOW,SPEED_HI)
                        .setSpeedStep(1);
                float speed = -255 + svi.getSpeedForValue((float)rollAngleAbsolute);

                if(isLeftTurn)
                    setSpeedIfNecessary((int)speed,-255);
                else
                    setSpeedIfNecessary(-255,(int)speed);
            }
        }
    }

    private int speedReactionTolerance = 16;
    private int angleReactionTolerance = 5;
    private boolean withinSpeedTolerance(int a, int b){
        return Math.abs(a-b) < speedReactionTolerance;
    }
    private boolean withinAngleTolerance(int a, int b){
        return Math.abs(a-b) < angleReactionTolerance;
    }
    private boolean notWithinSpeedTolerance (int a, int b){
        return !withinSpeedTolerance(a,b);
    }
    private boolean notWithinAngleTolerance (int a, int b){
        return !withinAngleTolerance(a,b);
    }


    private void setSpeedIfNecessary(int left, int right) {
        boolean setLeft = notWithinSpeedTolerance((int) left, mLastLeftSpeed);
        boolean setRight = notWithinSpeedTolerance((int) right, mLastRightSpeed);
        if (setLeft || setRight)
            setSpeed(setLeft ? left : null, setRight ? right : null);
    }


    private void setSpeed(Integer left, Integer right) {

        Integer leftSpeed = left==null || left==mLastLeftSpeed? null:left;
        Integer rightSpeed = right==null || right==mLastRightSpeed? null:right;

            if(leftSpeed!=null) mLeftSpeedTextView.setText(""+left);
            if(rightSpeed!=null) mRightSpeedTextView.setText(""+right);

        try {
            com.zugaldia.robocar.mobile.client.RobocarClient service= new RobocarRestClient("http://"+this.mBaseUrl);
            service.setSpeed(leftSpeed, rightSpeed);
            if(left!=null) mLastLeftSpeed = left;
            if(right!=null) mLastRightSpeed = right;
        }catch(Exception e) {
            Toast.makeText(this, "Unable to communicate with Robocar: "+e.getMessage(), Toast.LENGTH_SHORT).show();
            Timber.d(e.getMessage());
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
        this.mToggleButton.setChecked(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mToggleButton.setChecked(false);

        SharedPreferences sp = getSharedPreferences("ui-resources", 0);
        SharedPreferences.Editor spe = sp.edit();
        mBaseUrl = sp.getString("webserviceUrl", "");
        mLastLeftSpeed =0;
        mLastRightSpeed = 0;
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        if(buttonView==mToggleButton) {
            if(isChecked){
                mLastLeftSpeed=0;
                mLastRightSpeed = 0;

                mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
                mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_UI);
            }
            else{
                mSensorManager.unregisterListener(this);
                setSpeed(0,0);
            }

        }
    }

    private void initNavigationDrawer() {
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        drawer.closeDrawer(GravityCompat.START);

        int id = item.getItemId();

        new IntentRouter(this)
                .navigateFrom(R.id.tilt_controller_activity)
                .to(id);

        return true;
    }
}
