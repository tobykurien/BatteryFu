package com.tobykurien.batteryfu;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

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
		SharedPreferences pref = PreferenceManager
		.getDefaultSharedPreferences(context);
		return pref.getBoolean("screen_state_on", false);
		
		//return screenOn;
	}

	public static void setScreenOn(Context context, boolean screenOn) {
		SharedPreferences pref = PreferenceManager
		.getDefaultSharedPreferences(context);
		pref.edit().putBoolean("screen_state_on", screenOn).commit();
	}
}
