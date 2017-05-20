package com.zugaldia.robocar.mobile1;

import java.util.TreeMap;

public class SpeedPercentageMap {

    public static int getSpeedForPercentage(int percentage) {

        float minPercent = 10;
        float maxPercent = 80;
        float minSpeed = 100;
        float maxSpeed = 255;
        float percentageRange = maxPercent - minPercent;
        float speedRange = maxSpeed - minSpeed;
        float changeRate = speedRange / percentageRange;
        int speed = (int)( changeRate * ( percentage - minPercent ) + minSpeed );
        speed = speed - (speed % 10);

        if(speed > maxSpeed)
            speed = (int) maxSpeed;

        if(speed < minSpeed)
            speed = 0;

        return speed;
    }
}
