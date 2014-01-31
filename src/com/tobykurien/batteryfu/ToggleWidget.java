package com.tobykurien.batteryfu;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

public class ToggleWidget extends AppWidgetProvider {
	public static String ACTION_WIDGET_RECEIVER = "com.tobykurien.batteryFu.WidgetReceiver";

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		Log.i("BatteryFu", "Got widget update: " + appWidgetManager.toString());
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		
		RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget);
		initWidget(context, remoteViews);
		appWidgetManager.updateAppWidget(appWidgetIds, remoteViews);
	}

	/**
	 * Initialise the widget views and click handlers
	 * @param context
	 * @param remoteViews
	 */
	private void initWidget(Context context, RemoteViews remoteViews) {
		// put the right icon image on
		Settings settings = Settings.getSettings(context);
		if (settings.isEnabled()) {
			remoteViews.setImageViewResource(R.id.widget_icon, R.drawable.widget_icon_toggle_on);
		} else {
			remoteViews.setImageViewResource(R.id.widget_icon, R.drawable.widget_icon_toggle_off);
		}
		
		// separate intent for clicking the icon
		Intent active = new Intent(context, ToggleWidget.class);
		active.setAction(ACTION_WIDGET_RECEIVER);
		active.setData(Uri.parse("click://icon"));
		PendingIntent actionPendingIntent = PendingIntent.getBroadcast(context, 0, active, 0);		
		remoteViews.setOnClickPendingIntent(R.id.widget_icon, actionPendingIntent);
		
		// vs clicking the text
		active.setData(Uri.parse("click://text"));
		actionPendingIntent = PendingIntent.getBroadcast(context, 0, active, 0);		
		remoteViews.setOnClickPendingIntent(R.id.widget_text, actionPendingIntent);
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		Log.i("BatteryFu", "Got widget receive: " + intent.getAction());

		// gonna keep re-initializing the widget to avoid it becoming unresponsive
		RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget);
      initWidget(context, remoteViews);
		
		// v1.5 fix that doesn't call onDelete Action
		final String action = intent.getAction();
		if (AppWidgetManager.ACTION_APPWIDGET_DELETED.equals(action)) {
			final int appWidgetId = intent.getExtras().getInt(
					AppWidgetManager.EXTRA_APPWIDGET_ID,
					AppWidgetManager.INVALID_APPWIDGET_ID);
			if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
				this.onDeleted(context, new int[] { appWidgetId });
			}
		} else {
			// check, if our Action was called
			if (intent.getAction().equals(ACTION_WIDGET_RECEIVER)) {

				if (intent.getDataString().equals("batteryfu://enabled")) {
					setImage(context, R.drawable.widget_icon_toggle_on);
				}				

				if (intent.getDataString().equals("batteryfu://disabled")) {
					setImage(context, R.drawable.widget_icon_toggle_off);
				}				
				
				if (intent.getDataString().equals("click://icon")) {
					// change image
					setImage(context, R.drawable.widget_icon_toggle_middle);
					
					Intent active = new Intent(context, DataToggler.class);
					active.setData(Uri.parse("batteryfu://toggle"));
					context.sendBroadcast(active);        
				}

				if (intent.getDataString().equals("click://text")) {					
					// show the preferences screen
					Intent active = new Intent(context, ModeSelect.class);
					active.setAction(Intent.ACTION_VIEW);
					active.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					context.startActivity(active);
				}
			} else {
				// maybe the launcher is telling us to update (e.g. orientation change)
				AppWidgetManager.getInstance(context).updateAppWidget(
						new ComponentName(context, ToggleWidget.class)
						, remoteViews);
			}
		}				
	}

	/**
	 * Change the icon image
	 * @param context
	 * @param imageId
	 */
	private void setImage(Context context, int imageId) {
		RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget);
		remoteViews.setImageViewResource(R.id.widget_icon, imageId);
		AppWidgetManager.getInstance(context).updateAppWidget(
				new ComponentName(context, ToggleWidget.class)
				, remoteViews);
	}
}
