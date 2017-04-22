package com.zugaldia.robocar.app;

import android.app.Application;

import timber.log.Timber;

/**
 * Created by antonio on 4/21/17.
 */

public class RobocarApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
    }
}
