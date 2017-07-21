package com.example.mac.spotlight.apps;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class AppSyncStateManager extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		SharedPreferences settings = context.getSharedPreferences(AppSyncer.APPS_SETTINGS, 0);
    	SharedPreferences.Editor editor = settings.edit();
    	editor.putInt("syncState", AppProvider.OUT_OF_SYNC);
    	editor.commit();
		context.startService(new Intent(context, AppSyncer.class));
	}
}