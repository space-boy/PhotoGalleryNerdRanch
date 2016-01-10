package com.sunrise.ex.photogallery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class StartupReceiver extends BroadcastReceiver {

    private static final String TAG = "BroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Recieived intent: " + intent.getAction());

        PollService.setServiceAlarm(context,QueryPreferences.isAlarmOn(context));
    }


}

