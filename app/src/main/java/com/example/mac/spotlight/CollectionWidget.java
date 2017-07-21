package com.example.mac.spotlight;


import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.RemoteViews;

public class CollectionWidget extends AppWidgetProvider {
    public static String EXTRA_ITEM = new String("-1");
    public static int Launcher_id=-1;
    public static int Launchable_id=-1;
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int i = 0; i < appWidgetIds.length; i++) {


            Log.e("CHECK","KKK");

            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds[i], R.id.widget_list);

            int appWidgetId = appWidgetIds[i];
            updateAppWidget(context, appWidgetManager, appWidgetId);
            Intent intent = new Intent(context, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            Log.e("ttt",CollectionWidget.EXTRA_ITEM);
            Bundle extras = new Bundle();
            extras.putInt("launchable.id",WidgetDataProvider.le_id);
            extras.putInt("launcher.id",WidgetDataProvider.lr_id);
            intent.putExtras(extras);
  //          intent.setAction(CollectionWidget.EXTRA_ITEM);
            intent.setAction("KAAM");

            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
       //     PendingIntent pendingIntent1 = PendingIntent.getBroadcast(context, appWidgetIds[i], intent, PendingIntent.FLAG_UPDATE_CURRENT);

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.collection_widget);
            //views.setOnClickPendingIntent(R.id.search_widget_thumbnail, pendingIntent);
            views.setOnClickPendingIntent(R.id.widget_img_launcher, pendingIntent);
            views.setOnClickPendingIntent(R.id.widget_text, pendingIntent);
            Intent clickIntent = new Intent(context, MainActivity.class);

            //views.setPendingIntentTemplate(R.id.widget_list, pendingIntent);
            views.setPendingIntentTemplate(R.id.widget_list, pendingIntent);


      //      final AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        //    alarm.cancel(pendingIntent);
          //  long interval = 1000*60;
            //alarm.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(),interval, pendingIntent);

            Log.e("in on update", "updating widget");


            clickIntent.setAction(CollectionWidget.EXTRA_ITEM);
            clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
            clickIntent.setData(Uri.parse(clickIntent.toUri(Intent.URI_INTENT_SCHEME)));
            PendingIntent clickPI = PendingIntent.getActivity(context, 0,
                    clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            Log.e("ttt",CollectionWidget.EXTRA_ITEM);

            views.setPendingIntentTemplate(R.id.widget_text, clickPI);
            appWidgetManager.updateAppWidget(appWidgetIds[i], views);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
    @Override
    public void onReceive(Context ctx, Intent intent) {
   //     Log.e("CHECK","KKK");





            final String action = intent.getAction();

if(action!=null) {
    if (action.equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE)) {
        final AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        final ComponentName cn = new ComponentName(ctx, MainActivity.class);
        mgr.notifyAppWidgetViewDataChanged(mgr.getAppWidgetIds(cn), R.id.widget_list);
      //  mgr.notifyAll();

        Log.e("CHECK", "in kaam ---------------------- NOW UPDATE");


    }
}
        super.onReceive(ctx, intent);
         }
    static public void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.collection_widget);

        final AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        final ComponentName cn = new ComponentName(context, MainActivity.class);
        mgr.notifyAppWidgetViewDataChanged(mgr.getAppWidgetIds(cn), R.id.widget_list);
//        mgr.notifyAll();
//        views.setTextViewText(R.id.appwidget_text, widgetText);

        // Set up the collection
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            setRemoteAdapter(context, views);
        } else {
            setRemoteAdapterV11(context, views);
        }
        // Instruct the widget manager to update the widget

        appWidgetManager.updateAppWidget(appWidgetId, views);
        Log.e("in updateappwidget","KKK");

    }



    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    /**
     * Sets the remote adapter used to fill in the list items
     *
     * @param views RemoteViews to set the RemoteAdapter
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private static void setRemoteAdapter(Context context, @NonNull final RemoteViews views) {
        views.setRemoteAdapter(R.id.widget_list,
                new Intent(context, WidgetService.class));
        Log.e("CHECK","KKK");

    }

    /**
     * Sets the remote adapter used to fill in the list items
     *
     * @param views RemoteViews to set the RemoteAdapter
     */
    @SuppressWarnings("deprecation")
    private static void setRemoteAdapterV11(Context context, @NonNull final RemoteViews views) {
        views.setRemoteAdapter(0, R.id.widget_list,
                new Intent(context, WidgetService.class));
        Log.e("CHECK","KKK");

    }
}