package com.zugaldia.robocar.app;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import timber.log.Timber;

/**
 * Created by antonio on 4/1/17.
 */

public class RobocarApp extends Application {
  @Override
  public void onCreate() {
    super.onCreate();
    Timber.plant(new Timber.DebugTree());
    registerActivityLifecycleCallbacksLogging();
  }

  private void registerActivityLifecycleCallbacksLogging() {
    registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
      @Override
      public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        Timber.d("Activity %s was created (bundle present: %b).",
          activity.getClass().getSimpleName(), savedInstanceState != null);
      }

      @Override
      public void onActivityStarted(Activity activity) {
        Timber.d("Activity %s was started.", activity.getClass().getSimpleName());
      }

      @Override
      public void onActivityResumed(Activity activity) {
        Timber.d("Activity %s was resumed.", activity.getClass().getSimpleName());
      }

      @Override
      public void onActivityPaused(Activity activity) {
        Timber.d("Activity %s was paused.", activity.getClass().getSimpleName());
      }

      @Override
      public void onActivityStopped(Activity activity) {
        Timber.d("Activity %s was stopped.", activity.getClass().getSimpleName());
      }

      @Override
      public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        Timber.d("Activity %s instance state was saved (bundle present: %b).",
          activity.getClass().getSimpleName(), outState != null);
      }

      @Override
      public void onActivityDestroyed(Activity activity) {
        Timber.d("Activity %s was destroyed.", activity.getClass().getSimpleName());
      }
    });
  }
}
