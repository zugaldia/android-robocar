package com.zugaldia.robocar.mobile.controller;

public class SpeedValueInterpolator {

    private float minValue;
    private float maxValue;
    private float minSpeed;
    private float maxSpeed;
    private int step;

    public SpeedValueInterpolator(){
        this.step = 10;
    }

    public SpeedValueInterpolator setSpeedRange(float minSpeed, float maxSpeed){
        this.minSpeed = minSpeed;
        this.maxSpeed = maxSpeed;
        return this;
    }
    public SpeedValueInterpolator setValueRange(float minValue, float maxValue){
        this.minValue = minValue;
        this.maxValue = maxValue;
        return this;
    }
    public SpeedValueInterpolator setSpeedStep(int step){
        this.step = step;
        return this;
    }

    public float getSpeedForValue(float value) {

        float valueRange = maxValue - minValue;
        float speedRange = maxSpeed - minSpeed;
        float changeRate = speedRange / valueRange;
        float speed = changeRate * ( value - minValue) + minSpeed;
        speed = speed - (speed % step);

        if(speed > maxSpeed)
            speed =  maxSpeed;

        if(speed < minSpeed)
            speed = 0;

        return speed;
    }


}
