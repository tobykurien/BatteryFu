package com.tobykurien.batteryfu;

import java.util.HashMap;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class Settings {
	public static boolean DEBUG_NIGHT_MODE = false;
	public static final String DEFAULT_NIGHT_MODE_END = "06:00";
	public static final String DEFAULT_NIGHT_MODE_START = "22:00";
	public final static int MIN_AWAKE_TIME = 2;
	public static final String DEFAULT_AWAKE = "3";
	public static final String DEFAULT_SLEEP = "60";
	public static int AWAKE_PERIOD = 0;
	public static int SLEEP_PERIOD = 0;
	public static int FIRST_RUN_VERSION = 2;

	private SharedPreferences pref;
	private static HashMap settingsCache = new HashMap();
	
	private Settings(SharedPreferences preferences) {
		pref = preferences;
	}
	
	/**
	 * Factory method to cache instances of settings class, since it's called a lot.
	 * @param preferences
	 * @return
	 */
	public static Settings getSettings(Context context) {
	   /*
		if (settingsCache.get(context) == null) {
			SharedPreferences preferences = 	PreferenceManager.getDefaultSharedPreferences(context);
			settingsCache.put(context, new Settings(preferences));
		} 
			
		return (Settings) settingsCache.get(context); 
		*/

      SharedPreferences preferences =  PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
      return new Settings(preferences);
	}
	
	public boolean isDnsFix() {
		return pref.getBoolean("dns_fix", false);		
	}

   public void setIsDnsFix(boolean dnsFix) {
      pref.edit().putBoolean("dns_fix", dnsFix).commit();
   }
	
	public boolean isEnabled() {
		return pref.getBoolean("enabled", false);
	}

   public boolean isForceSync() {
      return pref.getBoolean("force_sync", true);
   }
	
   public boolean isNightmodeOnly() {
      if (!isNightmodeEnabled()) return false;
      return pref.getBoolean("night_mode_only", false);
   }

   public String getNightmodeStart() {
		return getNightmodeStart(Settings.DEFAULT_NIGHT_MODE_START);
	}

	public String getNightmodeStart(String defaultVal) {
		return pref.getString("night_mode_start", defaultVal);
	}	
	
	public String getNightmodeEnd() {
		return pref.getString("night_mode_end", Settings.DEFAULT_NIGHT_MODE_END);
	}
	
	public boolean isDataWhileScreenOn() {
		return pref.getBoolean("screen_on_data", false);
	}

	public boolean isScreenOnKeepData() {
      return pref.getBoolean("screen_keep_data", true);
   }

	public boolean isMobileDataEnabled() {
		return pref.getBoolean("mobile", false);
	}

	public boolean isDataWhileCharging() {
		return pref.getBoolean("charger_on_data", false);
	}
	
	public boolean isNightmodeEnabled() {
		return pref.getBoolean("night_mode_enabled", false);
	}

	public boolean isWifiEnabled() {
		return pref.getBoolean("wifi", true);
	}

	public String getScreenOffDelayTime() {
		return pref.getString("screen_off_sleep_time", "30");
	}

	public boolean isWaitForScreenUnlock() {
		return pref.getBoolean("screen_on_unlock", true);
	}

	public boolean isStartOnBoot() {
		return pref.getBoolean("start_on_boot", true);
	}

	public String getSleepTime() {
		return pref.getString("sleep_time", Settings.DEFAULT_SLEEP);
	}

	public String getAwakeTime() {
		return pref.getString("awake_time", Settings.DEFAULT_AWAKE);
	}

	public boolean isShowNotification() {
		return pref.getBoolean("show_notification", true);
	}

	public boolean isUseApndroid() {
		return pref.getBoolean("apndroid", false);
	}

   public boolean isFirstRun() {
      return pref.getInt("first_run", 0) != FIRST_RUN_VERSION;
   }

   public void setFirstRun() {
      pref.edit().putInt("first_run", FIRST_RUN_VERSION).commit();     
   }
   
	public void setNightmodeStart(String start) {
		pref.edit().putString("night_mode_start", start).commit();		
	}

	public void setNightmodeEnd(String end) {
		pref.edit().putString("night_mode_end", end).commit();		
	}

	public void setEnabled(boolean enabled) {
		pref.edit().putBoolean("enabled", enabled).commit();
	}

	public boolean isDataOn() {
		return pref.getBoolean("data_state_on", true);
	}

	public void setDataStateOn(boolean on) {
		pref.edit().putBoolean("data_state_on", on).commit();
	}

	public void setSyncOnData(boolean sync) {
		pref.edit().putBoolean("sync_on_data", sync).commit();
	}

	public boolean isSyncOnData() {
		return pref.getBoolean("sync_on_data", false);
	}

	public void setIsCharging(boolean isCharging) {
		pref.edit().putBoolean("state_charging", isCharging).commit();
	}
	
	public boolean isCharging() {
		return pref.getBoolean("state_charging", false);
	}
	
	public void setIsNightmode(boolean isNightmode) {
		pref.edit().putBoolean("state_nightmode", isNightmode).commit();
	}
	
	public boolean isNightmode() {
		return pref.getBoolean("state_nightmode", false);
	}
	
	public void setLastWakeTime(long time) {
		pref.edit().putLong("last_wake_time", time).commit();
	}
	
	public long getLastWakeTime() {
		return pref.getLong("last_wake_time", 0);
	}
	
   public void setIsTravelMode(boolean isTravelMode) {
      pref.edit().putBoolean("state_travelmode", isTravelMode).commit();
   }
	
   public boolean isTravelMode() {
      return pref.getBoolean("state_travelmode", false);
   }

   public void setUseApnDroid(boolean use) {
      pref.edit().putBoolean("apndroid", use).commit();
   }
   
   public boolean isDisconnectOnScreenOff() {
      return pref.getBoolean("state_disconnect_on_screen_off", false);
   }
   
   public void setDisconnectOnScreenOff(boolean disable) {
      pref.edit().putBoolean("state_disconnect_on_screen_off", disable).commit();
   }
}
