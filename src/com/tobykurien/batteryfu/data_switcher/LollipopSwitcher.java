package com.tobykurien.batteryfu.data_switcher;

import java.lang.Runtime;
import java.io.DataOutputStream;
import java.io.IOException;

import android.content.Context;
import android.net.ConnectivityManager;
import android.util.Log;
import android.widget.Toast;

import com.tobykurien.batteryfu.BatteryFu;
import com.tobykurien.batteryfu.R;

/**
 * A Lollipop-compatible data switcher
 * 
 * @author andy
 */
public class LollipopSwitcher extends MobileDataSwitcher {
	private ConnectivityManager connMan;
	private Context context;

	@Override
	public void enableMobileData(Context context) {

		try {
			// Preform su to get root priviledges
			Process p = Runtime.getRuntime().exec("su");

			// Attempt to write a file to a root-only
			DataOutputStream os = new DataOutputStream(p.getOutputStream());
			os.writeBytes("svc data enable\n");
			os.writeBytes("exit\n");
			os.flush();
			try {
				p.waitFor();
				if (p.exitValue() != 255) {
					Log.d("BatteryFu", "LollipopSwitcher: Enabled Mobile Data");
				} else {
					Log.d("BatteryFu",
							"LollipopSwitcher: Error enabling mobile data");
				}
			} catch (InterruptedException e) {
				Log.d("BatteryFu",
						"LollipopSwitcher: Error enabling mobile data");
			}
		} catch (IOException e) {
			Log.d("BatteryFu", "LollipopSwitcher: Error enabling mobile data");
		}

	}

	@Override
	public void disableMobileData(Context context) {
		try {
			// Preform su to get root privledges
			Process p = Runtime.getRuntime().exec("su");

			// Attempt to write a file to a root-only
			DataOutputStream os = new DataOutputStream(p.getOutputStream());
			os.writeBytes("svc data disable\n");
			os.writeBytes("exit\n");
			os.flush();
			try {
				p.waitFor();
				if (p.exitValue() != 255) {
					Log.d("BatteryFu", "LollipopSwitcher: Disabled Mobile Data");
				} else {
					Log.d("BatteryFu",
							"LollipopSwitcher: Error disabling mobile data");
				}
			} catch (InterruptedException e) {
				Log.d("BatteryFu",
						"LollipopSwitcher: Error disabling mobile data");
			}
		} catch (IOException e) {
			Log.d("BatteryFu", "LollipopSwitcher: Error disabling mobile data");
		}
	}

	@Override
	public int isToggleWorking(Context context) {
		if (init(context))
			return 0;
		else
			return R.string.need_root_text;
	}

	public boolean init(Context context) {
		try {
			// Perform su to get root priviledges
			Process p = Runtime.getRuntime().exec("su");

			DataOutputStream os = new DataOutputStream(p.getOutputStream());
			os.writeBytes("exit\n");
			os.flush();

			try {
				p.waitFor();
				if (p.exitValue() == 0) {
					Log.d("BatteryFu",
							"LollipopSwitcher: Successfully got root rights in init!");
					return true;
				} else {
					Log.d("BatteryFu",
							"LollipopSwitcher: Error gaining root access");
				}
			} catch (InterruptedException e) {
				Log.d("BatteryFu",
						"LollipopSwitcher: Error gaining root access");
			}
		} catch (IOException e) {
			Log.d("BatteryFu", "LollipopSwitcher: Error gaining root access");
		}
		return false;
	}

}