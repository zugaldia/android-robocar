package com.zugaldia.robocar.app;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

  private static final String LOG_TAG = MainActivity.class.getSimpleName();

  private DCTest test;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // Don't block the UI thread
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          Log.d(LOG_TAG, "Launching demo.");
          test = new DCTest();
          for (int motorIndex = 1; motorIndex <= 4; motorIndex++) {
            Log.d(LOG_TAG, String.format("Running motor %d", motorIndex));
            test.run(motorIndex);
          }
        } catch (InterruptedException e) {
          Log.d(LOG_TAG, "Demo failed:", e);
        }
      }
    }).start();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    // Don't forget to close at exit
    Log.d(LOG_TAG, "Closing demo.");
    if (test != null) {
      test.close();
    }
  }
}
