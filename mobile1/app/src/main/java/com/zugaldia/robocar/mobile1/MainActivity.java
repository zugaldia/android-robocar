package com.zugaldia.robocar.mobile1;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zugaldia.robocar.software.webserver.models.RobocarSpeed;

import java.text.DecimalFormat;

import butterknife.OnTouch;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.leftButton)
    Button leftButton;

    @BindView(R.id.rightButton)
    Button rightButton;

    @BindView(R.id.leftSpeedTextView)
    TextView leftSpeedTextView;

    @BindView(R.id.rightSpeedTextView)
    TextView rightSpeedTextView;

    @BindView(R.id.leftPercentageTextView)
    TextView leftPercentageTextView;

    @BindView(R.id.rightPercentageTextView)
    TextView rightPercentageTextView;

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

    int lastLeftSpeed = 0;
    int lastRightSpeed = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        // Hide percentage text view. Used only for debugging.
        leftPercentageTextView.setVisibility(View.GONE);
        rightPercentageTextView.setVisibility(View.GONE);
    }

    @OnTouch({
            R.id.upArrowButton,
            R.id.downArrowButton,
            R.id.leftArrowButton,
            R.id.rightArrowButton,
            R.id.leftButton,
            R.id.rightButton
    })
    public boolean onTouch(View v, MotionEvent event) {
        if (v == upArrowButton || v == downArrowButton || v == leftArrowButton || v == rightArrowButton)
            return handleArrowButtonEvent(v, event);
        if (v == leftButton || v == rightButton)
            return handleSlideButtonEvent(v, event);
        return false;
    }

    public boolean handleArrowButtonEvent(View v, MotionEvent event) {
        boolean buttonReleased = (event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP;
        leftSpeedTextView.setText("");
        rightSpeedTextView.setText("");

        if (buttonReleased) {
            setSpeed(0, 0);
            return true;
        }

        if (v == upArrowButton) {
            setSpeed(255, 255);
            return true;
        }
        if (v == downArrowButton) {
            setSpeed(-255, -255);
            return true;
        }
        if (v == leftArrowButton) {
            setSpeed(-255, 255);
            return true;
        }
        if (v == rightArrowButton) {
            setSpeed(255, -255);
            return true;
        }
        return false;
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
            speed = calculateSpeedFromViewTouchEvent(v, event);

        // update only if last speed has changed.
        boolean needsUpdate = (isLeft && lastLeftSpeed != speed) || (isRight && lastRightSpeed != speed);

        if (needsUpdate) {
            setSpeed(isLeft ? speed : null, isRight ? speed : null);
            String message = "Speed:" + speed;
            textView.setText(message);
            if (isLeft) lastLeftSpeed = speed;
            if (isRight) lastRightSpeed = speed;
        }
        return true;
    }

    private int calculateSpeedFromViewTouchEvent(View v, MotionEvent event) {

        float height = v.getHeight();

        // Calculate y as a number between -255 (at bottom of the button) to 255 (at the top of the button)

        // calculate the signed percentage of the y offset from the middle.
        // middle=0, top=100, bottom=-100
        float signedPercentage = ((height - event.getY()) - (height / 2)) * 200 / height;
        DecimalFormat f = new DecimalFormat("##");
        if (v == leftButton) {
            leftPercentageTextView.setText("%" + f.format(signedPercentage));
        }
        if (v == rightButton) {
            rightPercentageTextView.setText("%" + f.format(signedPercentage));
        }
        return getSpeedFromSignedPercentage((int) signedPercentage);
    }

    int getSpeedFromSignedPercentage(int signedPercentage) {
        int sign = signedPercentage < 0 ? -1 : 1;
        int absPercentage = Math.abs(signedPercentage);
        int speed = SpeedPercentageMap.getSpeedForPercentage(absPercentage);
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
}