package com.example.mac.spotlight;

import android.Manifest;
import android.app.LauncherActivity;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Icon;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import android.widget.TextView;
import android.content.Context;
import android.widget.Toast;

import com.example.mac.spotlight.apps.AppLauncher;
import com.example.mac.spotlight.apps.AppSyncer;
import com.example.mac.spotlight.audio.SongLauncher;
import com.example.mac.spotlight.contacts.ContactLauncher;
import com.example.mac.spotlight.favoriteitems.FavoriteItemsLauncher;
import com.example.mac.spotlight.favoriteitems.FavoriteItemsProvider;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.snapshot.DetectedActivityResult;
import com.google.android.gms.awareness.snapshot.HeadphoneStateResult;
import com.google.android.gms.awareness.snapshot.LocationResult;
import com.google.android.gms.awareness.state.HeadphoneState;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static android.R.attr.drawable;
import static android.R.attr.id;
import static android.R.attr.layout;
import static android.R.attr.parentActivityName;
import static android.content.ContentValues.TAG;


public class WidgetDataProvider implements RemoteViewsService.RemoteViewsFactory {

    private static final String TAG = "WidgetDataProvider";

    public static List<Launchable> mCollection = new ArrayList<Launchable>();
    Context mContext = null;
    public static int useractivity;
    public static int headphonesON;
    public static Date localtime;
    public static int Timeslot;
    public static int le_id=-1;
    public static int lr_id=-1;


    public static Location locationlonglat;

    private ArrayList<Launcher> mLaunchers;
    private FavoriteItemsLauncher mFavoriteItemsLauncher;
    private ContentResolver mContentResolver;
    private LayoutInflater layoutInflater;

    private final WidgetDataProvider.SearchHistoryDatabase mSearchHistoryDatabase;

    private  int mNumLaunchers;
    private  HashMap<Integer, Integer> mLauncherIndexes;

    private GoogleApiClient mGoogleApiClient;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 10001;
    private static final int ASK_MULTIPLE_PERMISSION_REQUEST_CODE = 1004;

    private static final String SEARCH_HISTORY_DB = "SearchHistory";

    KnnUsingKDTree K;


    private static class SearchHistoryDatabase extends SQLiteOpenHelper {
        private static final String DB_NAME = "SearchHistory.db";
        private static final int DB_VERSION = 2;

        public SearchHistoryDatabase(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
            Log.e("in constructor","!!");

        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            updateDatabase(db, 0, DB_VERSION);
            Log.e("createee","database");


        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            updateDatabase(db, oldVersion, newVersion);
        }


        private void updateDatabase(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion < 2) {
                db.execSQL("DROP TABLE IF EXISTS " + SEARCH_HISTORY_DB);
            }
            db.execSQL("CREATE TABLE IF NOT EXISTS " + SEARCH_HISTORY_DB + " ( "
                    + "_ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "LauncherID INTEGER, "
                    + "LaunchableID INTEGER, "
                    + "Counter REAL, "
                    + "TimeSlot INTEGER, "
                    + "HeadphonesOn INTEGER, "
                    + "DetectedActivity INTEGER, "
                    + "Latitude REAL, "
                    + "Longitude REAL"
                    + ")");
            Log.e("create table","done");

        }
    }




    public WidgetDataProvider(Context context, Intent intent) {
        mContext = context;
        mSearchHistoryDatabase=new SearchHistoryDatabase(mContext);
        //getApplicationContext().getContentResolver()

        mContentResolver = mContext.getContentResolver();

        mContext.grantUriPermission("com.example.mac.spotlight.apps.AppProvider", Uri.parse("content://spotlight/apps"), Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        if(context!=null)
            Log.e("IN CONSTRUCTOR", "WIDGETDATAPROVIDER");

    }
    public void onItemClick(AdapterView<?> parent, View view,
                            int position, long id)
    {


    }


    @Override
    public void onCreate() {
        initData();
        RemoteViews views = new RemoteViews(mContext.getPackageName(), R.layout.collection_widget);


        //views.setOnClickPendingIntent(mCollection, );
        //// TODO: 23/04/17 set on click listener on m collections

    }

    @Override
    public void onDataSetChanged() {
        Log.e("triggered","ondatasetchanged");
        initData();
    }

    @Override
    public void onDestroy() {

    }

    @Override
    public int getCount() {
        return mCollection.size();
    }

    private class ViewHolder {
        ImageView mThumbnail;
        TextView mLabel;
        TextView mInfoText;
    }

    @Override
    public RemoteViews getViewAt(int position) {



        final RemoteViews remoteView = new RemoteViews(
                mContext.getPackageName(), R.layout.launchable);
        Launchable K = null;
        if(mCollection.size()>position) {// android.R.layout.two_line_list_item); //
             K = mCollection.get(position);
        }
        if(K!=null) {


            //LauncherActivity.ListItem listItem = mCollection.get(position);
            //      remoteView.setTextViewText(android.R.id.text1, K.getLabel());
            //    remoteView.setTextColor(android.R.id.text1, Color.BLACK);
            //  remoteView.setTextViewText(android.R.id.text2,"shubhi");

            remoteView.setTextViewText(R.id.label, K.getLabel());
            remoteView.setTextColor(R.id.label, Color.BLACK);
            remoteView.setTextViewText(R.id.infoText, K.getInfoText());
            remoteView.setTextColor(R.id.infoText, Color.DKGRAY);

            if (K.getThumbnail() != null) {
                Bitmap anImage = ((BitmapDrawable) K.getThumbnail()).getBitmap();
                remoteView.setImageViewBitmap(R.id.thumbnail, anImage);
            }


            //remoteView.setTextViewText(R.id.content, listItem.content);

            Bundle extras = new Bundle();
            extras.putInt("position", position);
            // Log.e("position",Integer.toString(position));
            extras.putInt(Integer.toString(position) + "1", (K.getId()));
            extras.putInt(Integer.toString(position) + "2", (K.getLauncher().getId()));
            extras.putString(Integer.toString(position) + "3", K.getLabel());

            extras.putInt("launcher.id", this.lr_id = K.getLauncher().getId());
            extras.putInt("launchable.id", this.le_id = K.getId());
            extras.putInt("launcher.id", this.lr_id = K.getLauncher().getId());
            this.lr_id = K.getLauncher().getId();
            this.le_id = K.getId();

            Log.e("ttt", CollectionWidget.EXTRA_ITEM);
            Intent fillInIntent = new Intent();
            fillInIntent.putExtras(extras);


            // Make it possible to distinguish the individual on-click
            // action of a given item

            //    remoteView.setOnClickFillInIntent(android.R.id.text1, fillInIntent);
            //  remoteView.setOnClickFillInIntent(android.R.id.text2, fillInIntent);


            remoteView.setOnClickFillInIntent(R.id.label, fillInIntent);
            remoteView.setOnClickFillInIntent(R.id.infoText, fillInIntent);
            remoteView.setOnClickFillInIntent(R.id.thumbnail, fillInIntent);


            Log.e("ttt", CollectionWidget.EXTRA_ITEM);
        }



        return remoteView;
    }


    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {


        return position;
    }



    @Override
    public boolean hasStableIds() {
        return true;
    }


    private void initData() {
        mCollection.clear();


        getSnapshots();
        mContentResolver = mContext.getContentResolver();





        mLaunchers = new ArrayList<Launcher>();
        createLaunchers();
        if (mFavoriteItemsLauncher != null) {
            mFavoriteItemsLauncher.init2();
        }

        mNumLaunchers = mLaunchers.size();
        mLauncherIndexes = new HashMap<Integer, Integer>(mNumLaunchers);
  //      mLauncherObserver = new SearchHistoryComposer.LauncherObserver(new Handler());

        for (int i = 0; i < mNumLaunchers; i++) {
            Log.e("mLauncherIndex","initialize");
            mLauncherIndexes.put(mLaunchers.get(i).getId(), i);
    //        mLaunchers.get(i).registerContentObserver(mLauncherObserver);
        }




        initSearchHistory();


    }


    private void initSearchHistory() {

        SQLiteDatabase db;
        try {
            db = mSearchHistoryDatabase.getWritableDatabase();
        } catch (SQLiteException e) {
            db = null;
        }
        if (db != null) {


            Cursor cursor = db.rawQuery("SELECT " +
                            "_ID, " +
                            "LauncherID, " +
                            "LaunchableID, " +
                            "TimeSlot, " +
                            "DetectedActivity, " +
                            "HeadphonesOn, " +
                            "Counter, " +
                            "Latitude, " +
                            "Longitude " +
                            "FROM " + SEARCH_HISTORY_DB
                    //		+ " ORDER BY _ID DESC LIMIT " + mMaxSearchHistorySize
                    , null);

            if (cursor != null) {
                if (cursor.getCount() > 0) {


                    int numpoints = cursor.getCount();
                    K = new KnnUsingKDTree(numpoints);

                    double x[] = new double[7];

                    double s[] = {MainActivity.Timeslot, MainActivity.useractivity, MainActivity.headphonesON, 7, 0, 0};
                    if (MainActivity.locationlonglat != null) {
                        s[4] = MainActivity.locationlonglat.getLatitude();
                        s[5] = MainActivity.locationlonglat.getLongitude();
                    }
                    Log.e("Query","here");
                    System.out.println("QUERY: (" + s[0] + " , " + s[1] + " , " + s[2] + " , " + s[3] + " , " + s[4] + " , " + s[5] + ")");

                    cursor.moveToFirst();
                    while (!cursor.isAfterLast()) {

                        x[0] = cursor.getInt(3);
                        x[1] = cursor.getInt(4);
                        x[2] = cursor.getInt(5);
                        x[3] = cursor.getInt(6);
                        x[4] = cursor.getDouble(7);
                        x[5] = cursor.getDouble(8);
                        x[6] = cursor.getInt(0);
                        K.kdt.add(x);


                        cursor.moveToNext();
                    }
                    double s1[] = {MainActivity.Timeslot, MainActivity.useractivity, MainActivity.headphonesON, 7, 0, 0};
                    if (MainActivity.locationlonglat != null) {
                        s1[4] = MainActivity.locationlonglat.getLatitude();
                        s1[5] = MainActivity.locationlonglat.getLongitude();
                    }
                    int n=0;
                    double id=0;
                    while(n<10 && id!=-1) {
                        System.out.println("---------------------------------");
                        id = K.knnnearestneighbour(K.kdt, s1);
                        Log.e("jj", Double.toString(id));
                        Cursor cursor1 = db.rawQuery("SELECT LauncherID, LaunchableID FROM " + SEARCH_HISTORY_DB + " WHERE _ID = ?", new String[]{Double.toString(id)});
                        cursor1.moveToFirst();
                        if (cursor1 != null && cursor1.getCount() > 0) {
                            int launcher_id = cursor1.getInt(0);
                            int launchable_id = cursor1.getInt(1);
                            Log.e("Cursor data1 IN WIDGET", Integer.toString(launcher_id));
                            Log.e("Cursor data2 IN WIDGET", Integer.toString(launchable_id));

                            Integer launcherIndex = mLauncherIndexes.get(launcher_id);
                            Log.e("IN widget",Integer.toString(mLauncherIndexes.size()));
                            if (launcherIndex != null) {
                                Launchable launchable = mLaunchers.get(launcherIndex).getLaunchable(launchable_id);
                               if(!mCollection.contains(launchable)){
                                   n++;
                                   mCollection.add(launchable);


                                }

                            }
                            //Provide_Results();

                            //K.knnnearestneighbour(K.kdt,s);

                        }
                    }

                    cursor.close();
                }
                db.close();
            }
      //      AppSyncer.start(mContext);


        }
    }







    private void createLaunchers() {
        int launcherIndex = 0;


      //  if (mSettings.getBoolean("searchFavoriteItems", true)) {
            mFavoriteItemsLauncher = new FavoriteItemsLauncher(this);
            String strNumSuggestions = Integer.toString(4);
            try {
                int numSuggestions = Integer.parseInt(strNumSuggestions);
                mFavoriteItemsLauncher.setMaxSuggestions(numSuggestions);
            } catch (NumberFormatException e) {
            }
            mLaunchers.add(launcherIndex++, mFavoriteItemsLauncher);
    //    }

      //  if (mSettings.getBoolean("searchApps", true)) {
            AppLauncher appLauncher = new AppLauncher(mContext);

            try {
                int numSuggestions = 4;
                appLauncher.setMaxSuggestions(numSuggestions);
            } catch (NumberFormatException e) {
            }

            mLaunchers.add(launcherIndex++, appLauncher);
        //}

     //   if (mSettings.getBoolean("searchContacts", true)) {
            Launcher contactLauncher;

            contactLauncher = new ContactLauncher(mContext);


            try {
                int numSuggestions = 4;
                contactLauncher.setMaxSuggestions(numSuggestions);
            } catch (NumberFormatException e) {
            }


            mLaunchers.add(launcherIndex++, contactLauncher);
       // }



  //      if (mSettings.getBoolean("searchSongs", true)) {
            SongLauncher songLauncher = new SongLauncher(mContext);

            try {
                int numSuggestions = 4;
                songLauncher.setMaxSuggestions(numSuggestions);
            } catch (NumberFormatException e) {
            }

            mLaunchers.add(launcherIndex++, songLauncher);
    //    }*/
    }

    public ArrayList<Launcher> getLaunchers() {
        return mLaunchers;
    }






    public void getSnapshots() {

        // User Activity
        mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                .addApi(Awareness.API)
                .build();
        mGoogleApiClient.connect();
        Log.e("in get snapshots","----");


        Awareness.SnapshotApi.getDetectedActivity(mGoogleApiClient)
                .setResultCallback(new ResultCallback<DetectedActivityResult>() {
                    @Override
                    public void onResult(@NonNull DetectedActivityResult detectedActivityResult) {
                        if (!detectedActivityResult.getStatus().isSuccess()) {
                            Log.e("in getsnapshots ", "Could not detect user activity");
                            //            mUserActivityTextView.setText("--Could not detect user activity--");
                            useractivity=50;


                            return;
                        }
                        ActivityRecognitionResult arResult = detectedActivityResult.getActivityRecognitionResult();
                        DetectedActivity probableActivity = arResult.getMostProbableActivity();
                        Log.e("activity?",Integer.toString(probableActivity.getType()));
                        Log.e("in getsnapshots ", probableActivity.toString());
                        useractivity=probableActivity.getType();

                        if((useractivity==3))
                        {
                            useractivity=0;
                        }
                        else if((useractivity!=3))
                        {
                            useractivity=100;
                        }

                    }
                });



        // Headphones
        Awareness.SnapshotApi.getHeadphoneState(mGoogleApiClient)
                .setResultCallback(new ResultCallback<HeadphoneStateResult>() {
                    @Override
                    public void onResult(@NonNull HeadphoneStateResult headphoneStateResult) {
                        if (!headphoneStateResult.getStatus().isSuccess()) {
                            Log.e(TAG, "Could not detect headphone state");
                            //mHeadphonesTextView.setText("Could not detect headphone state");
                            Log.e("HEADPHONE ","Could not detect headphone state");

                            headphonesON=50;

                            //mHeadphonesTextView.setTextColor(Color.RED);
                            return;
                        }
                        HeadphoneState headphoneState = headphoneStateResult.getHeadphoneState();
                        if (headphoneState.getState() == HeadphoneState.PLUGGED_IN) {
                            //mHeadphonesTextView.setText("Headphones plugged in");
                            //mHeadphonesTextView.setTextColor(Color.GREEN);
                            Log.e("HEADPHONE ","headphones connected");

                            headphonesON=100;

                        } else {
                            //mHeadphonesTextView.setText("Headphones NOT plugged in");
                            //mHeadphonesTextView.setTextColor(Color.BLACK);
                            Log.e("HEADPHONE ","Headphones NOT plugged in");
                            headphonesON=0;
                        }

                    }
                });

        // Time (Simply get device time)
        Calendar calendar = Calendar.getInstance();
        //mTimeTextView.setText(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(calendar.getTime()));
        //mTimeTextView.setTextColor(Color.GREEN);
        Log.e("DATE ",new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(calendar.getTime()));

        localtime=calendar.getTime();
        //       DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date());

        Date d=new Date();
        //     SimpleDateFormat sdf=new SimpleDateFormat("HH:mm");
        //    String currentDateTimeString = sdf.format(d);
        int g=d.getHours();
        Log.e("date format g",Integer.toString(g));
        Timeslot=g/4;
        if(Timeslot==0)
            Timeslot++;

        Log.e("date format slot",Integer.toString(Timeslot));



        getFineLocationSnapshots();


    }

    private void getFineLocationSnapshots() {
        // Check for permission first
        if (ContextCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            Log.e(TAG, "Fine Location Permission not yet granted");



            locationlonglat=null;


        } else {
            Log.i(TAG, "Fine Location permission already granted");
            // Location
            Awareness.SnapshotApi.getLocation(mGoogleApiClient)
                    .setResultCallback(new ResultCallback<LocationResult>() {
                        @Override
                        public void onResult(@NonNull LocationResult locationResult) {
                            if (!locationResult.getStatus().isSuccess()) {
                                Log.e(TAG, "Could not detect user location");
                                //mLocationTextView.setText("Could not detect user location");
                                Log.e("LOCATION ","Could not detect user location");


                                locationlonglat=null;
                                // mLocationTextView.setTextColor(Color.RED);
                                return;
                            }
                            Location location = locationResult.getLocation();
                            //mLocationTextView.setText(location.toString());
                            Log.e("LOCATION ",location.toString());

                            locationlonglat=location;




                        }
                    });

        }
        //mSearchHistoryComposer.Provide
    }

}