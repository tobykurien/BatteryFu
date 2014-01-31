package com.tobykurien.android;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.http.client.HttpResponseException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

public class Utils {
   public static final String EXCEPTION_NO_APP = "No app found to handle request";
   public static final String PREF_KEY_SD_CACHE = "sd_cache";


   /**
    * Generate a unique ID for this device. TODO - should this id change when
    * device is reset, SIM card swopped, or google user account changed? Also,
    * can we do this without requiring Telephony as tablets don't have this?
    * 
    * @param context
    * @return
    */
   public static String getDeviceIdOld(Context context) {
      final TelephonyManager tm = (TelephonyManager) context
               .getSystemService(Context.TELEPHONY_SERVICE);

      // String imsi = tm.getSubscriberId();
      // String imei = tm.getDeviceId();

      String tmDevice, tmSerial, tmPhone, androidId;
      tmDevice = "" + tm.getDeviceId();
      tmSerial = "" + tm.getSimSerialNumber();
      androidId = ""
               + android.provider.Settings.Secure.getString(context.getContentResolver(),
                        android.provider.Settings.Secure.ANDROID_ID);
      if ("9774d56d682e549c".equalsIgnoreCase(androidId)) {
         // Android 2.2 bug, use wifi mac address rather
         try {
            WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (wm.getConnectionInfo().getMacAddress() != null) {
               androidId = wm.getConnectionInfo().getMacAddress();
            }
         } catch (Exception e) {
            // wifi off or no permission
         }
      }

      UUID deviceUuid = new UUID(androidId.hashCode(), ((long) tmDevice.hashCode() << 32)
               | tmSerial.hashCode());
      return deviceUuid.toString();
   }

   /**
    * Returns app's heap size
    * 
    * @return
    */
   public static long getMaxMemoryMb() {
      return Runtime.getRuntime().maxMemory() / 1024 / 1024;
   }

   /**
    * Returns amount of free RAM for current app
    * 
    * @return
    */
   public static long getFreeMemoryMb() {
      return Runtime.getRuntime().freeMemory() / 1024 / 1024;
   }

   /**
    * Returns the width of the device screen in pixels
    * @param context
    * @return
    */
   public static int getDisplayWidth(Activity context) {
      DisplayMetrics metrics = new DisplayMetrics();
      context.getWindow().getWindowManager().getDefaultDisplay().getMetrics(metrics);
      int displayWidth = metrics.widthPixels;
      if (metrics.heightPixels < displayWidth) {
         return metrics.heightPixels;
      }
      
      return displayWidth;
   }
   
   public static void enableStrictMode() {
      if (isGingerbreadOrBetter()) {
         // use reflection to call ApiLevel9Funcs.enableStrictMode();
         try {
            Method m = Class.forName("com.tobykurien.android.misc.ApiLevel9Funcs")
                     .getMethod("enableStrictMode", null);
            m.invoke(null, null);
         } catch (Exception e) {
            throw new IllegalStateException(e);
         }
      }
   }
   
   /**
    * Print out debug information about an intent
    * 
    * @param tag
    * @param intent
    */
   public static void debugIntentData(String tag, Intent intent) {
      if (intent == null) {
         Log.i(tag, "Intent is null");
         return;
      }
      Log.i(tag, "Intent data string: " + intent.getDataString());
      Log.i(tag, "Intent action: " + intent.getAction());
      Log.i(tag, "Intent type: " + intent.getType());

      if (intent.getExtras() != null) {
         Set<String> keys = intent.getExtras().keySet();
         for (String key : keys) {
            Log.i(tag, "Intent extras: " + key + " = " + intent.getStringExtra(key));
         }
      } else {
         Log.i(tag, "Intent has no extras");
      }
   }

   /**
    * Check if the intent has an activity receiver
    * 
    * @param context
    * @param i
    * @return
    */
   public static boolean hasIntentActivity(Context context, Intent i) {
      List l = context.getPackageManager().queryIntentActivities(i, 0);
      return !l.isEmpty();
   }

   /**
    * Launch an app
    * 
    * @param packageName
    * @param className
    * @throws Exception
    */
   public static void launchActivity(Activity context, String packageName, String className) {
      Intent conIntent = new Intent();
      conIntent.setAction(Intent.ACTION_MAIN);
      conIntent.addCategory(Intent.CATEGORY_LAUNCHER);
      conIntent.setClassName(packageName, className);
      conIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

      if (hasIntentActivity(context, conIntent)) {
         context.startActivity(conIntent);
      } else {
         context.runOnUiThread(handleExUi(UtilsConstants.LOG_TAG, context, new Exception(
                  EXCEPTION_NO_APP)));
      }
   }

   /**
    * Open specified URL
    * 
    * @param url
    */
   public static void openActionView(Context context, String url) {
      Intent updateIntent = null;
      updateIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
      context.startActivity(updateIntent);
   }

   /**
    * Executes a process in the shell and returns the result
    * 
    * @param command
    * @return
    */
   public static String exec(String[] command) {
      try {
         Process process = Runtime.getRuntime().exec(command);
         BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
         String line = null;
         StringBuffer sb = new StringBuffer();
         while ((line = reader.readLine()) != null) {
            sb.append(line);
         }
         reader.close();
         return sb.toString();
      } catch (Exception err) {
         return "error: " + err.getMessage();
      }
   }

   /**
    * Work out the cache directory based on settings and SD card availability
    * 
    * @param context
    * @return
    */
   public static String getCachePath(Context context) {
      String cachePath = context.getCacheDir().getAbsolutePath() + "/";
      SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
      if (pref.getBoolean(PREF_KEY_SD_CACHE, false)) {
         // check if sd card is available
         boolean mExternalStorageAvailable = false;
         boolean mExternalStorageWriteable = false;
         String state = Environment.getExternalStorageState();

         if (Environment.MEDIA_MOUNTED.equals(state)) {
            // We can read and write the media
            mExternalStorageAvailable = mExternalStorageWriteable = true;
         } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // We can only read the media
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
         } else {
            // Something else is wrong. It may be one of many other states, but
            // all we need
            // to know is we can neither read nor write
            mExternalStorageAvailable = mExternalStorageWriteable = false;
         }

         if (mExternalStorageAvailable && mExternalStorageWriteable) {
            cachePath = Environment.getExternalStorageDirectory() + "Android/data/"
                     + context.getClass().getPackage().getName() + "/cache/";
         }
      }
      return cachePath;
   }

   /**
    * Do HTTP get, cache he result (if specified) and return the text.
    * 
    * @param context
    * @param urlString
    * @param forceDownload
    *           - true to always download from internet
    * @param noCache
    *           - true to never cache the result
    * @param cachePeriod
    *           - if <= 0, will check internet for newer version, else waits
    *           till timeout before reloading from internet
    * @return
    * @throws Exception
    */
   public static String getData(Context context, String urlString, boolean forceDownload,
            boolean noCache, long cachePeriod) throws Exception {
      return getData(context, urlString, forceDownload, noCache, cachePeriod, null);
   }

   /**
    * Do HTTP get, cache he result (if specified) and return the text.
    * 
    * @param context
    * @param urlString
    * @param forceDownload
    *           - true to always download from internet
    * @param noCache
    *           - true to never cache the result
    * @param cachePeriod
    *           - if <= 0, will check internet for newer version, else waits
    *           till timeout before reloading from internet
    * @param cacheName
    *           - normally only the filename in the URL is used for cache name.
    *           Here a prefix can be specified, or left null.
    * @return
    * @throws Exception
    */
   public static String getData(Context context, String urlString, boolean forceDownload,
            boolean noCache, long cachePeriod, String cacheName) throws Exception {
      String aUrlString[] = urlString.split("/");
      String filename = aUrlString[aUrlString.length - 1];
      if (cacheName != null) {
         filename = cacheName;
      }
      filename = filename.toLowerCase().replaceAll("[^A-Za-z0-9 ]", " ").trim()
               .replaceAll(" +", "_");

      String cachePath = getCachePath(context);
      File f = new File(cachePath + filename);
      if (UtilsDebug.NET) Log.d(UtilsConstants.LOG_TAG, "Looking for cached file:" + cachePath
               + filename);
      boolean reCache = forceDownload;
      if (!reCache) {
         // check if cache file exists
         if (!f.exists() || !f.canRead() || f.length() == 0) {
            if (UtilsDebug.NET) Log.d(UtilsConstants.LOG_TAG, "Cache file invalid");
            reCache = true;
         }
      }

      if (!reCache) {
         if (!f.exists() || f.length() == 0) {
            reCache = true;
         } else {
            // check if cache file expired
            Date now = new Date();
            if (cachePeriod <= 0) {
               reCache = true; // cache until server updates
            } else if ((now.getTime() - f.lastModified()) > cachePeriod) {
               if (UtilsDebug.NET) Log.d(UtilsConstants.LOG_TAG,
                        "Cached file expired " + (now.getTime() - f.lastModified()));
               reCache = true;
            }
         }
      }

      // cache the file
      if (reCache) {
         if (UtilsDebug.NET) Log.d(UtilsConstants.LOG_TAG, "Checking for updated file on internet");
         try {
            URL url = new URL(urlString.toString());
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setFollowRedirects(true);
            urlConnection.setIfModifiedSince(f.lastModified());
            urlConnection.connect();

            if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {
               if (UtilsDebug.NET) Log.d(UtilsConstants.LOG_TAG, "Cache file still valid.");
               urlConnection.disconnect();
            } else if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
               if (UtilsDebug.NET) Log.d(UtilsConstants.LOG_TAG,
                        "Cache not found or expired, loading data from the internet");
               
               InputStream is = urlConnection.getInputStream();
               ByteArrayOutputStream os = new ByteArrayOutputStream();

               FileWriter fr = null;
               if (!noCache) {
                  try {
                     try {
                        f.mkdirs();
                        f.delete();
                        f.createNewFile();
                     } catch (Exception e) {
                     }
                     fr = new FileWriter(f);
                  } catch (Exception e) {
                     // can't cache, so ignore
                     Log.e(UtilsConstants.LOG_TAG, "Error creating cache file", e);
                     noCache = true;
                  }
               }

               byte[] buf = new byte[1024];
               int len;
               while ((len = is.read(buf)) > 0) {
                  os.write(buf, 0, len);
               }
               is.close();
               os.close();
               urlConnection.disconnect();

               if (!noCache) {
                  fr.write(os.toString());
                  fr.flush();
                  fr.close();
               }

               return os.toString();
            } else {
               if (urlConnection.getResponseCode() < 0) {
                  throw new MalformedURLException(urlString);
               } else {
                  throw new HttpResponseException(urlConnection.getResponseCode(),
                           urlConnection.getResponseMessage());
               }
            }
         } catch (Exception e) {
            // if something goes wrong, delete the possibly corrupt file
            f.delete();
            throw e;
         }
      }

      if (!f.exists() || f.length() == 0) {
         // problem!
         throw new IOException("Unable to get data, please check internet connection.");
      }

      // read the cache file and return it
      InputStream is = new FileInputStream(f);
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      byte[] buf = new byte[1024];
      int len;
      while ((len = is.read(buf)) > 0) {
         os.write(buf, 0, len);
      }
      is.close();
      os.close();
      
      return os.toString();
   }

   /**
    * Halts a process for the given number of seconds
    * 
    * @param seconds
    * @throws InterruptedException
    */
   public static void waitSeconds(int seconds) {
      try {
         int i = 0;
         while (i++ <= seconds) {
            Thread.sleep(100);
            Thread.yield();
            Thread.sleep(100);
            Thread.yield();
            Thread.sleep(100);
            Thread.yield();
            Thread.sleep(100);
            Thread.yield();
            Thread.sleep(100);
            Thread.yield();
            Thread.sleep(100);
            Thread.yield();
            Thread.sleep(100);
            Thread.yield();
            Thread.sleep(100);
            Thread.yield();
            Thread.sleep(100);
            Thread.yield();
            Thread.sleep(100);
            Thread.yield();
         }
      } catch (InterruptedException e) {
         // ignore interruptions
      }
   }

   /**
    * Show a warning dialog
    * 
    * @param edTitleDiscountWarning
    * @param discountReject
    */
   public static Dialog getWarningDialog(final Activity activity, int refTitleId,
            String warningMessage) {
      return getWarningDialog(activity, refTitleId, warningMessage, null);
   }

   public static Dialog getWarningDialog(final Activity activity, int refTitleId,
            String warningMessage, final Runnable onSuccess) {
      return new AlertDialog.Builder(activity).setTitle(refTitleId).setMessage(warningMessage)
               .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                     dialog.dismiss();
                     if (onSuccess != null) {
                        activity.runOnUiThread(onSuccess);
                     }
                  }
               }).create();
   }

   /**
    * Prompt user whether to proceed or not, if so, execute runnable
    * 
    * @param context
    * @param titleResId
    * @param msgResId
    * @param onConfirm
    */
   public static void confirm(Context context, String logTag, int titleResId, int msgResId,
            int OkResId, int cancelResId, final Runnable onConfirm) {
      AlertDialog dialog = null;
      try {
         Builder b = new Builder(context);
         b.setCancelable(true);
         if (titleResId >= 0) b.setTitle(titleResId);
         if (msgResId >= 0) b.setMessage(msgResId);
         if (cancelResId >= 0) b.setNegativeButton(cancelResId, null);
         if (onConfirm != null && OkResId >= 0) b.setPositiveButton(OkResId, new OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
               onConfirm.run();
            }
         });

         b.create().show();
      } catch (Exception e) {
         if (logTag != null) Utils.handleException(logTag, context, e);
      }
   }

   /**
    * Prompt user whether to proceed or not, if so, execute runnable
    * 
    * @param context
    * @param titleResId
    * @param msgResId
    * @param onConfirm
    */
   public static void confirm(Context context, String logTag, String title, String message,
            String okButton, String cancelButton, final Runnable onConfirm, final Runnable onCancel) {
      AlertDialog dialog = null;
      try {
         Builder b = new Builder(context);
         b.setCancelable(true);
         if (title != null) b.setTitle(title);
         if (message != null) b.setMessage(message);

         if (cancelButton != null) b.setNegativeButton(cancelButton, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
               if (onCancel != null) onCancel.run();
            }
         });

         if (okButton != null) b.setPositiveButton(okButton, new OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
               if (onConfirm != null) onConfirm.run();
            }
         });

         b.create().show();
      } catch (Exception e) {
         if (logTag != null) Utils.handleException(logTag, context, e);
      }
   }

   /**
    * Show a progress dialog while a long-running process takes place
    * 
    * @param context
    * @param progressMessage
    * @param runnable
    */
   public static Thread longRunningProcess(Context context, Runnable runnable) {
      String message = "Loading...";
      return longRunningProcess(context, null, message, runnable);
   }

   public static Thread longRunningProcess(final Context context, String progressTitle,
            String progressMessage, final Runnable runnable) {
      ProgressDialog dialog = null;
      if (progressTitle != null || progressMessage != null) {
         try {
            dialog = ProgressDialog.show(context, progressTitle, progressMessage);
            dialog.setCancelable(true);
            dialog.setIndeterminate(true);
            dialog.show();
         } catch (Exception e) {
            // activity might be hidden at this point, or destroyed by a
            // rotation
         }
      }

      final ProgressDialog d = dialog;
      Thread t = new Thread() {
         @Override
         public void run() {

            try {
               // we need to catch exceptions to avoid "Intent leaked" exception
               // in Android
               runnable.run();
            } catch (Exception e1) {
               try {
                  if (d != null) d.dismiss();
               } catch (Exception e) {
                  // activity might be hidden at this point, or destroyed by a
                  // rotation
               }

               throw new RuntimeException(e1);
            }

            try {
               if (d != null) d.dismiss();
            } catch (Exception e) {
               // activity might be hidden at this point, or destroyed by a
               // rotation
            }
         }
      };

      t.start();

      return t;
   }

   /**
    * Non-blocking wait. Starts a new thread to run the supplied Runnable, and
    * returns the Thread.
    * 
    * @param seconds
    * @param runMe
    * @return
    */
   public static Thread waitNonBlocking(final int seconds, final Runnable runMe) {
      Thread t = new Thread() {
         @Override
         public void run() {
            try {
               Thread.sleep(seconds * 1000);
               runMe.run();
            } catch (InterruptedException e) {
               // abort if interrupted
            }
         }
      };

      t.start();

      return t;
   }

   /**
    * Returns true if there is a network connected
    * 
    * @param context
    * @return
    */
   public static boolean isNetworkConnected(Context context) {
      boolean retVal = false;

      ConnectivityManager cm = (ConnectivityManager) context
               .getSystemService(context.CONNECTIVITY_SERVICE);
      if (cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnectedOrConnecting()) {
         retVal = true;
      } else {
         retVal = false;
      }

      return retVal;
   }

   /**
    * Returns true if the active network is the mobile network (rather than
    * wifi, etc)
    * 
    * @param context
    * @return
    */
   public static boolean isMobileNetworkConnected(Context context) {
      boolean retVal = false;
      if (context == null) return retVal;

      ConnectivityManager cm = (ConnectivityManager) context
               .getSystemService(context.CONNECTIVITY_SERVICE);
      if (cm.getActiveNetworkInfo() != null
               && cm.getActiveNetworkInfo().getType() == cm.TYPE_MOBILE
               && cm.getActiveNetworkInfo().isConnectedOrConnecting()) {
         retVal = true;
      } else {
         retVal = false;
      }

      return retVal;
   }

   /**
    * Handle exceptions by showing the user a Toast and logging it
    * 
    * @param tagName
    * @param context
    * @param e
    */
   public static void handleException(String tagName, Context context, Exception e) {
      Log.e(tagName, "Error", e);
      if (e instanceof NullPointerException) {
         Toast.makeText(context,
                  "Null error occurred. Try clearing the application cache and data.",
                  Toast.LENGTH_LONG).show();
      } else {
         Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
      }
   }

   /**
    * A runnable version of handleException for running on UI thread
    * 
    * @param tagName
    * @param context
    * @param e
    * @return
    */
   public static Runnable handleExUi(final String tagName, final Context context, final Exception e) {
      return new Runnable() {
         @Override
         public void run() {
            Utils.handleException(tagName, context, e);
         }
      };
   }

   /**
    * Check the runtime Android version. Returns true if it is less than Android
    * 1.6
    * 
    * @return
    */
   public static boolean isBelowApi4() {
      return Integer.parseInt(Build.VERSION.SDK) < 4;
   }

   /**
    * Check the runtime Android version. Returns true if Honeycomb
    * 
    * @return
    */
   public static boolean isHoneycomb() {
      return Integer.parseInt(Build.VERSION.SDK) > 11 && Integer.parseInt(Build.VERSION.SDK) < 14;
   }

   public static boolean isHoneycombOrBetter() {
      return Integer.parseInt(Build.VERSION.SDK) > 11;
   }

   public static boolean isGingerbreadOrBetter() {
      return Integer.parseInt(Build.VERSION.SDK) > 9;
   }

   /**
    * Check the runtime Android version. Returns true if ICS
    * 
    * @return
    */
   public static boolean isICS() {
      return Integer.parseInt(Build.VERSION.SDK) >= 14;
   }

   /**
    * Checks if the device is a tablet capable of displaying the
    * tablet/honeycomb interface
    * 
    * @param context
    * @return
    */
   public static boolean isTablet(Activity activity) {
      if (isBelowApi4()) {
         return false;
      } else {
         // use reflection to call ApiLevel4Funcs.isTablet();
         try {
            Method m = Class.forName("com.tobykurien.android.misc.ApiLevel4Funcs")
                     .getMethod("isTablet", Activity.class);
            Boolean ret = (Boolean) m.invoke(null, activity);
            return ret;
         } catch (Exception e) {
            throw new IllegalStateException(e);
         }
         
      }
   }

   /**
    * Returns true if device screen width is as specified
    * 
    * @param activity
    * @param orBigger
    * @return
    */
   public static boolean isScreenWidth(Activity activity, int width, boolean orBigger) {
      boolean retVal = false;

      if (activity == null || activity.getWindowManager() == null) return retVal;

      Display d = activity.getWindowManager().getDefaultDisplay();
      if (orBigger) {
         if (d.getWidth() < d.getHeight()) {
            if (d.getWidth() >= width) retVal = true;
         } else {
            if (d.getHeight() >= width) retVal = true;
         }
      } else {
         if (d.getWidth() < d.getHeight()) {
            if (d.getWidth() == width) retVal = true;
         } else {
            if (d.getHeight() == width) retVal = true;
         }

      }

      return retVal;
   }

   /**
    * Get the root view for an activity
    * 
    * @param activity
    * @return
    */
   public View getRootView(Activity activity) {
      return ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0);
   }

   /**
    * Reliably get orientation. Return values are one of
    * Configuration.ORIENTATION_XXXXX
    * 
    * @return
    */
   public int getScreenOrientation(Activity context) {
      Display getOrient = context.getWindowManager().getDefaultDisplay();
      int orientation = Configuration.ORIENTATION_UNDEFINED;
      if (getOrient.getWidth() == getOrient.getHeight()) {
         orientation = Configuration.ORIENTATION_SQUARE;
      } else {
         if (getOrient.getWidth() < getOrient.getHeight()) {
            orientation = Configuration.ORIENTATION_PORTRAIT;
         } else {
            orientation = Configuration.ORIENTATION_LANDSCAPE;
         }
      }
      return orientation;
   }

}
