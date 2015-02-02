package com.tobykurien.batteryfu;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.os.Build;
import com.tobykurien.batteryfu.compat.Api7;
import com.tobykurien.batteryfu.compat.Api20;

public class ScreenService extends Service {
	private GeneralReceiver generalReceiver;
	//private boolean screenOn = true;

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d("BatteryFu", "Screen service started");
		
        // register receiver that handles screen on and screen off logic
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        generalReceiver = new GeneralReceiver();
        setScreenOn(getApplicationContext(), true);
        registerReceiver(generalReceiver, filter);
	}
	
	@Override
	public void onDestroy() {
		Log.d("BatteryFu", "Screen service stopped");
		
		try {
			unregisterReceiver(generalReceiver);
		} catch (Exception e) {			
		}
		super.onDestroy();
	}

	public static boolean isScreenOn(Context context) {
        boolean ret;
        if(Build.VERSION.SDK_INT >= 20)
        {
            ret = Api20.isScreenOn(context);
        }
        else if(Build.VERSION.SDK_INT >= 7)
        {
            ret = Api7.isScreenOn(context);
        }
        else {
            Settings settings = Settings.getSettings(context);
            ret = settings.getScreenOnOff();
        }
        return ret;
	}

	public static void setScreenOn(Context context, boolean screenOn) {
        Settings settings = Settings.getSettings(context);
        settings.setScreenOnOff(screenOn);
	}
}
