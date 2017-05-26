package com.zugaldia.robocar.mobile.controller;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zugaldia.robocar.mobile.R;
import com.zugaldia.robocar.software.webserver.models.RobocarSpeed;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnTouch;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ControllerActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    @BindView(R.id.drawer_layout)
    DrawerLayout drawer;

    @BindView(R.id.leftButton)
    Button leftButton;

    @BindView(R.id.rightButton)
    Button rightButton;

    @BindView(R.id.leftSpeedTextView)
    TextView leftSpeedTextView;

    @BindView(R.id.rightSpeedTextView)
    TextView rightSpeedTextView;

    @BindView(R.id.upArrowButton)
    ImageButton upArrowButton;

    @BindView(R.id.downArrowButton)
    ImageButton downArrowButton;

    @BindView(R.id.leftArrowButton)
    ImageButton leftArrowButton;

    @BindView(R.id.rightArrowButton)
    ImageButton rightArrowButton;

    @BindView(R.id.webserviceUrlTextView)
    TextView webserviceUrlTextView;

    @BindView(R.id.joystickButton)
    ImageButton joystickButton;

    int lastLeftSpeed = 0;
    int lastRightSpeed = 0;

    int SPEED_FULL = 255;
    int SPEED_LOW = 95;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_controller);
        ButterKnife.bind(this);
        initNavigationDrawer();
    }

    @OnTouch({
            R.id.upArrowButton,
            R.id.downArrowButton,
            R.id.leftArrowButton,
            R.id.rightArrowButton,
            R.id.leftButton,
            R.id.rightButton,
            R.id.joystickButton,
    })
    public boolean onTouch(View v, MotionEvent event) {
        if (v == upArrowButton || v == downArrowButton || v == leftArrowButton || v == rightArrowButton)
            return handleArrowButtonEvent(v, event);
        if (v == leftButton || v == rightButton)
            return handleSlideButtonEvent(v, event);
        if(v==joystickButton)
            return handleJoystickButtonEvent(v,event);
        return false;
    }

    public boolean handleJoystickButtonEvent(View v, MotionEvent event){
        boolean buttonPressed = (event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN;
        boolean buttonReleased = (event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP;
        if(buttonPressed){
            leftArrowButton.setVisibility(View.GONE);
            rightArrowButton.setVisibility(View.GONE);
            upArrowButton.setVisibility(View.GONE);
            downArrowButton.setVisibility(View.GONE);

            joystickButton.setBackgroundResource(R.drawable.button_joystick);
        }

        if(buttonReleased){
            leftArrowButton.setVisibility(View.VISIBLE);
            rightArrowButton.setVisibility(View.VISIBLE);
            upArrowButton.setVisibility(View.VISIBLE);
            downArrowButton.setVisibility(View.VISIBLE);
            setSpeed(0,0);
            joystickButton.setBackgroundResource(0);
            return true;
        }

        float circleRadius = joystickButton.getMeasuredWidth()/2;

        float xSigned = event.getX() - joystickButton.getMeasuredWidth() /2f ;
        float ySigned = joystickButton.getMeasuredHeight() / 2f - event.getY();
        float xUnsigned = Math.abs(xSigned);
        float yUnsigned = Math.abs(ySigned);

        double touchDistanceToCenter = Math.sqrt(xSigned * xSigned + ySigned * ySigned);
        if(touchDistanceToCenter<circleRadius*.8){
            setSpeed(0,0);
            return true;
        }

        float ySign = ySigned > 0 ? 1 : -1;
        float xSign = xSigned > 0 ? 1 : -1;

        boolean isForward = false;
        boolean isBackward = false;
        boolean isLeft=false;
        boolean isRight=false;

        if(ySigned>0) {
            isForward = true;
            isBackward=false;
        }
        else{
            isForward = false;
            isBackward = true;
        }
        if(xSigned>0){
            isRight = true;
            isLeft = false;
        }
        else{
            isRight = false;
            isLeft = true;
        }

        SpeedValueInterpolator svi = new SpeedValueInterpolator()
                .setValueRange(1.2f,6)
                .setSpeedRange(SPEED_LOW,SPEED_FULL)
                .setSpeedStep(10);

        boolean sameDirection = yUnsigned > xUnsigned;

        float m = sameDirection ? yUnsigned / (xUnsigned<1e-6f? 1e-6f:xUnsigned): xUnsigned/(yUnsigned<1e-6f?1e-6f:yUnsigned);
        float lowSpeed = svi.getSpeedForValue(m);

        if(isForward) {
            if (sameDirection) {
                if (isLeft)
                    setSpeed((int) (ySign * lowSpeed), (int) ySign * SPEED_FULL);
                else
                    setSpeed((int) (ySign * SPEED_FULL), (int) (ySign * lowSpeed));
            } else {
                if (isLeft)
                    setSpeed((int) (-ySign * lowSpeed), (int) ySign * SPEED_FULL);
                else
                    setSpeed((int) (ySign * SPEED_FULL), (int) (-ySign * lowSpeed));
            }
        }
        if(isBackward) {
            if (sameDirection) {
                if (isLeft)
                    setSpeed((int) ySign * SPEED_FULL,(int) (ySign * lowSpeed));
                else
                    setSpeed((int) (ySign * lowSpeed),(int) (ySign * SPEED_FULL));
            } else {
                if (isLeft)
                    setSpeed( (int) ySign * SPEED_FULL,(int) (-ySign * lowSpeed));
                else
                    setSpeed( (int) (-ySign * lowSpeed), (int) (ySign * SPEED_FULL));
            }
        }

        return true;
    }

    boolean isUpArrowPressed = false;
    boolean isDownArrowPressed = false;
    boolean isLeftArrowPressed = false;
    boolean isRightArrowPressed = false;

    private boolean isArrowButtonPressed(boolean currentPressedState, MotionEvent event){
        boolean isPressed = currentPressedState;
        if((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN) {
            isPressed = true;
        }
        if((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
            isPressed = false;
        }
        return isPressed;
    }

    private void setArrowButtonStates(View v, MotionEvent event){

        if(v == this.upArrowButton)
            this.isUpArrowPressed = isArrowButtonPressed(this.isUpArrowPressed,event);

        if(v == this.downArrowButton)
            this.isDownArrowPressed = isArrowButtonPressed(this.isDownArrowPressed,event);

        if(v == this.leftArrowButton)
            this.isLeftArrowPressed = isArrowButtonPressed(this.isLeftArrowPressed,event);

        if(v == this.rightArrowButton)
            this.isRightArrowPressed = isArrowButtonPressed(this.isRightArrowPressed,event);
    }

    public boolean handleArrowButtonEvent(View v, MotionEvent event) {

        setArrowButtonStates(v, event);

        boolean allReleased = !isUpArrowPressed && !isDownArrowPressed && !isLeftArrowPressed && !isRightArrowPressed;

        int leftSpeed = 0;
        int rightSpeed = 0;

        if (allReleased) {
            leftSpeed = 0;
            rightSpeed = 0;
        }

        if(isUpArrowPressed){
            leftSpeed = isLeftArrowPressed?SPEED_LOW:SPEED_FULL;
            rightSpeed = isRightArrowPressed?SPEED_LOW:SPEED_FULL;
        }
        else if(isDownArrowPressed){
            leftSpeed = isLeftArrowPressed?-SPEED_LOW:-SPEED_FULL;
            rightSpeed = isRightArrowPressed?-SPEED_LOW:-SPEED_FULL;
        }
        else if(isLeftArrowPressed){
            leftSpeed=-SPEED_FULL;
            rightSpeed=SPEED_FULL;
        }
        else if(isRightArrowPressed){
            leftSpeed = SPEED_FULL;
            rightSpeed = -SPEED_FULL;
        }

        if(lastLeftSpeed != leftSpeed && lastRightSpeed != rightSpeed)
            setSpeed(leftSpeed,rightSpeed);
        else if(lastLeftSpeed!=leftSpeed)
            setSpeed(leftSpeed,null);
        else if(lastRightSpeed!=rightSpeed)
            setSpeed(null,rightSpeed);

        lastLeftSpeed = leftSpeed;
        lastRightSpeed = rightSpeed;

        return true;
    }

    public boolean handleSlideButtonEvent(View v, MotionEvent event) {
        boolean buttonReleased = (event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP;

        Button button = null;
        TextView textView = null;
        boolean isLeft = false;
        boolean isRight = false;

        if (v == leftButton) {
            button = leftButton;
            textView = leftSpeedTextView;
            isLeft = true;
        }
        if (v == rightButton) {
            button = rightButton;
            textView = rightSpeedTextView;
            isRight = true;
        }

        if (button == null || textView == null)
            return false;

        int speed = 0;
        if (!buttonReleased)
            speed = calculateSpeedFromSliderViewTouchEvent(v, event);

        // update only if last speed has changed.
        boolean needsUpdate = (isLeft && lastLeftSpeed != speed) || (isRight && lastRightSpeed != speed);

        if (needsUpdate) {
            setSpeed(isLeft ? speed : null, isRight ? speed : null);

            if (isLeft) lastLeftSpeed = speed;
            if (isRight) lastRightSpeed = speed;
        }
        return true;
    }

    private int calculateSpeedFromSliderViewTouchEvent(View v, MotionEvent event) {

        float height = v.getHeight();
        float halfHeight = height/2;

        // Calculate y as a number between -SPEED_FULL (at bottom of the button) to SPEED_FULL (at the top of the button)
        // middle=0, top = height, bottom = -height
        float signedValue = height / 2f - event.getY();
        float unsignedValue = Math.abs(signedValue);
        int sign = signedValue < 0 ? -1 : 1;

        SpeedValueInterpolator svi = new SpeedValueInterpolator()
                .setValueRange( halfHeight * 0.1f, halfHeight * 0.8f)
                .setSpeedRange(SPEED_LOW,SPEED_FULL)
                .setSpeedStep(10);
        int speed = (int) svi.getSpeedForValue(unsignedValue);
        return sign * speed;
    }

    private Retrofit retrofit(String baseUrl) {
        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        return retrofit;
    }

    private RobocarService getApiService() {
        RobocarService apiService;
        try {
            String baseUrl = webserviceUrlTextView.getText().toString();

            if (baseUrl == null || baseUrl.isEmpty()) {
                Toast.makeText(this, "Please enter robocar service url", Toast.LENGTH_SHORT).show();
                return null;
            }
            baseUrl = "http://" + baseUrl;
            apiService = retrofit(baseUrl).create(RobocarService.class);
        } catch (Exception e) {
            Toast.makeText(this, "Error connecting to robocar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
        return apiService;
    }

    private void setSpeed(Integer left, Integer right) {
        if(left!=null)
            this.leftSpeedTextView.setText(""+left);
        if(right!=null)
            this.rightSpeedTextView.setText(""+right);

        RobocarSpeed speed = new RobocarSpeed();
        speed.setLeft(left);
        speed.setRight(right);

        RobocarService apiService = getApiService();
        if (apiService == null)
            return;

        Call<RobocarSpeed> call = apiService.changeSpeed(speed);
        call.enqueue(new Callback<RobocarSpeed>() {
            @Override
            public void onResponse(Call<RobocarSpeed> call, Response<RobocarSpeed> response) {

            }

            @Override
            public void onFailure(Call<RobocarSpeed> call, Throwable t) {
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences sp = getSharedPreferences("ui-resources", 0);
        SharedPreferences.Editor spe = sp.edit();
        spe.putString("webserviceUrl", webserviceUrlTextView.getText().toString());
        spe.commit();
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences sp = getSharedPreferences("ui-resources", 0);
        SharedPreferences.Editor spe = sp.edit();
        String baseUrl = sp.getString("webserviceUrl", "");
        if (baseUrl != null && !baseUrl.isEmpty())
            this.webserviceUrlTextView.setText(baseUrl);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.controller_activity) {
            // No action
        } else if (id == R.id.debug_activity) {
            // No action
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void initNavigationDrawer() {
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }
}