package com.tobykurien.batteryfu.data_switcher;

import java.io.File;
import java.io.FileWriter;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.tobykurien.android.Utils;
import com.tobykurien.batteryfu.R;
import com.tobykurien.batteryfu.Settings;

public class APNSwitcher extends MobileDataSwitcher {
	Thread dnsFixThread = null;
    
	@Override
	public void enableMobileData(Context context) {
		Log.i("BatteryFu", "Restoring APN settings");
		
		try {
			// also undo our APN changes
			ContentValues values = new ContentValues();
			values.put("type", "default,supl"); // "default,supl" enables AGPS

			// Adding Values using the Content Resolver
			context.getContentResolver().update(
					Uri.parse("content://telephony/carriers"), values,
					"type='batteryfu_disabled'", new String[] {});
			
			// notify that settings have changed
			context.getContentResolver().notifyChange(Uri.parse("content://telephony/carriers"), null);
		} catch (Exception e) {
			Log.e("BatteryFu", "Error restoring APN settings", e);
			Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
		}  
		
		Settings settings = Settings.getSettings(context);
		if (settings.isDnsFix()) {
			Log.i("BatteryFu", "Waiting for connection");
			
			final Context c = context;
			dnsFixThread = Utils.waitNonBlocking(12, new Runnable() {
				public void run() {
					// we might be connected, but that doesn't mean the dns is correct
						checkReconnect(c);
				}
			});
		}
	}

	@Override
	public void disableMobileData(Context context) {
		Log.i("BatteryFu", "Disabling APN settings");
		
		if (dnsFixThread != null && dnsFixThread.isAlive()) {
			dnsFixThread.interrupt();
		}
		dnsFixThread = null;
		
		try {
			// Change the APN type
			ContentValues values = new ContentValues();
			values.put("type", "batteryfu_disabled");

			// Adding Values using the Content Resolver
			context.getContentResolver().update(
					Uri.parse("content://telephony/carriers"), values,
					"current = 1 and (type='default' or type='default,supl' or type='' or type=null)", 
					new String[] {});
			
			// notify that settings have changed
			//context.getContentResolver().notifyChange(Uri.parse("content://telephony/carriers"), null);
		} catch (Exception e) {
			Log.e("BatteryFu", "Error restoring APN settings", e);
			Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
		}        		
	}

	@Override
	public int isToggleWorking(Context context) {
		Log.i("BatteryFu", "Testing APN settings");
		int retVal = 0;
		
		// check for APN we can toggle
		Cursor results = null;
		try {
			results = context.getContentResolver().query(
					Uri.parse("content://telephony/carriers"), 
					new String[] { "_id", "apn", "type" }, 
					"current = 1 and (type='default' or type='default,supl' or type='' or type=null or type='batteryfu_disabled')", 
					null, 
					null);
			
			if (results.getCount() <= 0) {
				Log.i("BatteryFu", "APN problem: no APN's found to toggle");
				retVal = R.string.apn_problem_text;
			}
		} catch (Exception e) {
			Log.e("BatteryFu", "Error testing APN settings", e);
			Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
		} finally {
			if (results != null) {
				results.close();
			}
		}
		
		return retVal;
	}

	// from APNDroid
    public void checkReconnect(Context context) {
    	Log.d("BatteryFu", "Checking if mobile reconnect was successful");
    	
        String net_dns1 = Utils.exec(new String[]{ "getprop", "net.dns1" });
        String net_dns2 = Utils.exec(new String[]{ "getprop", "net.dns2" });
        String net_rmnet0_dns1 = Utils.exec(new String[]{ "getprop", "net.rmnet0.dns1" });
        String net_rmnet0_dns2 = Utils.exec(new String[]{ "getprop", "net.rmnet0.dns2" });
        
        if (Utils.isNetworkConnected(context) && !Utils.isMobileNetworkConnected(context)) {
        	Log.d("BatteryFu", "Aborting mobile reconnect check as we're connected another way");
        	// we're not connected via mobile network so this doesn't apply
        	return;
        }
        
        Log.d("BatteryFu", "dns1=" + net_dns1 + ",dns2=" + net_dns2 + ", rdns1=" + net_rmnet0_dns1 + ", rdns2=" + net_rmnet0_dns2);
        
        if (!net_dns1.equals(net_rmnet0_dns1) || !net_dns2.equals(net_rmnet0_dns2)) {
			Log.d("BatteryFu", "DNS bug detected, setting DNS manually");

			try {
				// create a script file to execute, otherwise su permission won't be persistent
				String filename = context.getCacheDir().getPath() + "/dnsfix.sh";
				File f = new File(filename);
				if (f.exists()) f.delete();
				FileWriter fw = new FileWriter(f);
				fw.append("setprop net.dns1 " + net_rmnet0_dns1 + "\n");
				fw.append("setprop net.dns2 " + net_rmnet0_dns2 + "\n");
				fw.flush();
				fw.close();
				
				// execute the script file
				Utils.exec(new String[]{ "chmod", "+x", filename });        			
				Utils.exec(new String[]{ "su", "-c", filename });
				
//				if (!Utils.isMobileNetworkConnected(context)) {
//					Log.d("BatteryFu", "Mobile network still not connected, toggling wifi");
//					// now we need to toggle the wifi to make this work, even if wifi toggle is disabled
//					final WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
//					try {
//						wm.setWifiEnabled(true);
//						wm.reconnect();
//						
//						// give it a few moments. We can use a blocking call as this is in a different thread
//						Utils.waitSeconds(5);
//					} finally {
//						// make sure we close the wifi connection
//						wm.disconnect();
//						wm.setWifiEnabled(false);
//					}
//				}
			} catch (Exception e) {
				Log.e("BatteryFu", "Error applying DNS fix", e);
			}
        }
    }	
}
