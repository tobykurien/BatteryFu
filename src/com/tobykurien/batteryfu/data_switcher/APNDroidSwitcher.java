package com.tobykurien.batteryfu.data_switcher;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;

import com.tobykurien.android.Utils;
import com.tobykurien.batteryfu.R;

public class APNDroidSwitcher extends MobileDataSwitcher {
	/* APNDroid Constants */
	public static final int STATE_OFF = 0;
	public static final int STATE_ON = 1;
	public static final String TARGET_MMS_STATE = "com.google.code.apndroid.intent.extra.TARGET_MMS_STATE";
	public static final String TARGET_APN_STATE = "com.google.code.apndroid.intent.extra.TARGET_STATE";
	public static final String SHOW_NOTIFICATION = "com.google.code.apndroid.intent.extra.SHOW_NOTIFICATION";
	public static final String CHANGE_STATUS_REQUEST = "com.google.code.apndroid.intent.action.CHANGE_REQUEST";

	private void setAPNDroid(Context context, int state) {
		try {
			Bundle extras = new Bundle();
			extras.putInt(TARGET_MMS_STATE, STATE_ON);
			extras.putInt(TARGET_APN_STATE, state);
			extras.putBoolean(SHOW_NOTIFICATION, false);

			// service not available
			Intent intent = new Intent(CHANGE_STATUS_REQUEST);
			intent.putExtras(extras);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			if (isCallable(context, intent)) {
				context.startActivity(intent);
			} else {
				throw new Exception("APNdroid not installed.");
			}
		} catch (Exception e) {
			Utils.handleException("BatteryFu", context, e);
		}
	}

	private static boolean isCallable(Context context, Intent intent) {
		List<ResolveInfo> list = context.getPackageManager().queryIntentActivities(
				intent, PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}

	@Override
	public void enableMobileData(Context context) {
		setAPNDroid(context, STATE_ON);
	}

	@Override
	public void disableMobileData(Context context) {
		setAPNDroid(context, STATE_OFF);
	}

	@Override
	public int isToggleWorking(Context context) {
		int retVal = 0;
		
		try {
			Bundle extras = new Bundle();
			extras.putInt(TARGET_MMS_STATE, STATE_ON);
			extras.putInt(TARGET_APN_STATE, STATE_OFF);
			extras.putBoolean(SHOW_NOTIFICATION, false);

			// service not available
			Intent intent = new Intent(CHANGE_STATUS_REQUEST);
			intent.putExtras(extras);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			if (!isCallable(context, intent)) {
				retVal = R.string.apndroid_error_text;
			}
		} catch (Exception e) {
			Utils.handleException("BatteryFu", context, e);
		}
		
		return retVal;
	}

	public static boolean isApnDroidInstalled(Context context) {
	   boolean retVal = false;

      Bundle extras = new Bundle();
      extras.putInt(TARGET_MMS_STATE, STATE_ON);
      extras.putInt(TARGET_APN_STATE, STATE_OFF);
      extras.putBoolean(SHOW_NOTIFICATION, false);

      Intent intent = new Intent(CHANGE_STATUS_REQUEST);
      intent.putExtras(extras);
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      if (isCallable(context, intent)) {
         retVal = true;
      }
	   
	   return retVal;
	}
	
}
