package com.tobykurien.batteryfu.data_switcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.widget.Toast;

import com.tobykurien.batteryfu.Settings;

public abstract class MobileDataSwitcher {
	public static MobileDataSwitcher apn = new APNSwitcher();
	public static MobileDataSwitcher apndroid = new APNDroidSwitcher();
	public static MobileDataSwitcher gb = new GingerbreadSwitcher();
	public static MobileDataSwitcher ics = new ICSSwitcher();
	public static MobileDataSwitcher lollipop = new LollipopSwitcher();

	/**
	 * Get the right mobile data switcher
	 * 
	 * @param pref
	 * @return
	 */
	public static MobileDataSwitcher getSwitcher(Context context,
			Settings settings) {
		if (Build.VERSION.SDK_INT > 19) {
			return lollipop;
		}
		
		if ((Build.VERSION.SDK_INT >= 14) && (Build.VERSION.SDK_INT <= 19)) {
			return ics;
		}

		if ((Build.VERSION.SDK_INT >= 9) && (Build.VERSION.SDK_INT < 14)) {
			return gb;
		}

		if (settings.isUseApndroid()) {
			return apndroid;
		} else {
			return apn;
		}
	}

	/**
	 * Used to restore all APN settings on startup to avoid conflicts between
	 * them
	 * 
	 * @param context
	 */
	public static void enableAll(Context context) {
		if (apn == null)
			apn = new APNSwitcher();
		apn.enableMobileData(context);

		if (apndroid == null)
			apndroid = new APNDroidSwitcher();
		apndroid.enableMobileData(context);

		if (gb == null)
			gb = new GingerbreadSwitcher();
		gb.enableMobileData(context);
	}

	public static void disableMobileData(Context context, Settings settings) {
		getSwitcher(context, settings).disableMobileData(context);
	}

	public static void enableMobileData(Context context, Settings settings) {
		getSwitcher(context, settings).enableMobileData(context);
	}

	/**
	 * Method to be implemented that will switch on mobile data
	 * 
	 * @param context
	 */
	public abstract void enableMobileData(Context context);

	/**
	 * Method to be implemented that will switch off mobile data
	 * 
	 * @param context
	 */
	public abstract void disableMobileData(Context context);

	/**
	 * Test to see if APN toggling will work. Returns the ID of the string
	 * message to display in the dialog box, or 0 if all is well. The id of the
	 * dialog title will be assumed to be return value - 1
	 * 
	 * @param context
	 * @return
	 */
	public abstract int isToggleWorking(Context context);
}
