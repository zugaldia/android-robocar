package com.zugaldia.robocar.mobile.controller;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.zugaldia.robocar.mobile.R;
import com.zugaldia.robocar.mobile.debug.DebugActivity;

public class IntentRouter {

    private Activity mActivity;
    private int mFromId;

    public IntentRouter(Activity activity){
        mActivity= activity;
    }

    public IntentRouter navigateFrom(int id){
        mFromId = id;
        return this;
    }

    public void to(int toId) {

        if (mFromId == toId)
            return;

        Intent newIntent = null;

        if (toId == R.id.debug_controller_activity) {
            newIntent = new Intent(mActivity, DebugActivity.class);
        }
        else if(toId == R.id.game_controller_activity){
            newIntent = new Intent(mActivity, GameControllerActivity.class);
        }
        else if(toId == R.id.tilt_controller_activity){
            newIntent = new Intent(mActivity,TiltControllerActivity.class);
        }
        else if(toId == R.id.settings_activity){
            newIntent = new Intent(mActivity,SettingsActivity.class);
        }

        if(toId != R.id.settings_activity) {
            newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        }

        mActivity.startActivity(newIntent);

        if(toId != R.id.settings_activity) {
            mActivity.finish();
        }


    }


}
