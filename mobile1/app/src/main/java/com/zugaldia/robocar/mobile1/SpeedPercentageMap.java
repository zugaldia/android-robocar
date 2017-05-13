package com.zugaldia.robocar.mobile1;

import java.util.TreeMap;

public class SpeedPercentageMap {

    private static final TreeMap<Integer, Integer> speedMap;

    static {
        speedMap = new TreeMap<Integer, Integer>();

        speedMap.put(0, 0); // %0-%10 => 0 speed
        speedMap.put(10, 118); // %10-%20 => 118 speed
        speedMap.put(20, 140); //%20-%30 => 140 speed
        speedMap.put(30, 162);
        speedMap.put(40, 184);
        speedMap.put(50, 206);
        speedMap.put(60, 228);
        speedMap.put(70, 255); // %70 and up => 255 speed
    }

    public static int getSpeedForPercentage(int percentage) {

        int speed = speedMap.floorEntry(percentage).getValue();

        return speed;
    }
}
