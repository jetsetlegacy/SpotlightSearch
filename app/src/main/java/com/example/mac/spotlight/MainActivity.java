package com.example.mac.spotlight;

import android.Manifest;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import com.example.mac.spotlight.apps.AppLauncher;
import com.example.mac.spotlight.apps.AppProvider;
import com.example.mac.spotlight.apps.AppSyncer;
import com.example.mac.spotlight.contacts.ContactLauncher;
import com.example.mac.spotlight.favoriteitems.FavoriteItemsLauncher;
import com.example.mac.spotlight.favoriteitems.FavoriteItemsProvider;
import com.example.mac.spotlight.audio.SongLauncher;

import android.app.*;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.TextView.OnEditorActionListener;

import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.SnapshotApi;
import com.google.android.gms.awareness.snapshot.DetectedActivityResult;
import com.google.android.gms.awareness.snapshot.HeadphoneStateResult;
import com.google.android.gms.awareness.snapshot.LocationResult;
import com.google.android.gms.awareness.snapshot.PlacesResult;
import com.google.android.gms.awareness.snapshot.internal.Snapshot;
import com.google.android.gms.awareness.state.HeadphoneState;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.places.PlaceLikelihood;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static android.content.ContentValues.TAG;

public class MainActivity extends ListActivity {

    private GoogleApiClient mGoogleApiClient;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 10001;
    private static final int ASK_MULTIPLE_PERMISSION_REQUEST_CODE = 1004;


    private static final String CLEAR_SEARCH_TEXT_APPROVAL = "clearSearchTextApproval";


    public static int useractivity;
    public static int confidene;
    public static int headphonesON;
    public static Date localtime;
    public static int Timeslot;
    public static Location locationlonglat;
    private ArrayList<Launcher> mLaunchers;
    private SearchResultComposer mSearchResultComposer;
    public SearchHistoryComposer mSearchHistoryComposer;
    private BaseAdapter mListAdapter;
    private EditText mSearchText;
    private ImageButton mClearSearchText;
    private SharedPreferences mSettings;
    private Launchable mActiveLaunchable;
    private ProgressDialog progress;

    private boolean mClearSearchTextApproval;
    private ContentResolver mContentResolver;
    private FavoriteItemsLauncher mFavoriteItemsLauncher;


    public void updateWidgetshubhi()
    {

        Intent intent = new Intent(this,CollectionWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
// Use an array and EXTRA_APPWIDGET_IDS instead of AppWidgetManager.EXTRA_APPWIDGET_ID,
// since it seems the onUpdate() is only fired on that:
        int[] ids = {R.xml.collection_widget_info};
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,ids);
        sendBroadcast(intent);

        Log.e("in update widget shubhi", "sent broadcast");

    }


    public static void updateWidgets(Context context) {
        Intent intent = new Intent(context.getApplicationContext(), CollectionWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        // Use an array and EXTRA_APPWIDGET_IDS instead of AppWidgetManager.EXTRA_APPWIDGET_ID,
        // since it seems the onUpdate() is only fired on that:
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
        int[] ids = widgetManager.getAppWidgetIds(new ComponentName(context, CollectionWidget.class));

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            widgetManager.notifyAppWidgetViewDataChanged(ids, android.R.id.list);

        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        context.sendBroadcast(intent);
        Log.e("in update widget shubhi", "sent broadcast");

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


  //      updateWidgets(getApplicationContext());
/*
        Intent intent1=new Intent(getApplicationContext(),CollectionWidget.class);
        intent1.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent1, PendingIntent.FLAG_UPDATE_CURRENT);

    //    final AlarmManager alarm = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
      //      alarm.cancel(pendingIntent);
        //  long interval = 1000*20;
     //   alarm.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(),interval, pendingIntent);
        int[] ids = {R.xml.collection_widget_info};
        //int[] ids1 = AppWidgetManager.getInstance(getApplication()).getAppWidgetI‌​ds(new ComponentName(getApplication(), CollectionWidget.class));
        intent1.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,ids);
sendBroadcast(intent1);
        Log.e("sent intent from main","send broadcast");
*/
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            Log.e(TAG, "permissions not granted");

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS,Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.ACCESS_FINE_LOCATION},
                    ASK_MULTIPLE_PERMISSION_REQUEST_CODE);



        }


        this.getSnapshots();

        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        View rootView = getLayoutInflater().inflate(R.layout.main_activity, null);
        setContentView(rootView);
        AppSyncer.start(MainActivity.this);
        // ListView listView = (ListView)findViewById(R.id.text_list_view) ;


        mContentResolver = getContentResolver();

        mSettings = PreferenceManager.getDefaultSharedPreferences(this);

        mLaunchers = new ArrayList<Launcher>();
        createLaunchers();
        if (mFavoriteItemsLauncher != null) {
            mFavoriteItemsLauncher.init();
        }

        mSearchText = (EditText) findViewById(R.id.searchText);
        mSearchText.setHint("type to search");

        mSearchResultComposer = new SearchResultComposer(this);

        mSearchHistoryComposer = new SearchHistoryComposer(this);
        setListAdapter(mSearchHistoryComposer);


        //mSearchText.s(this);
        mSearchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable searchText) {
            }

            @Override
            public void beforeTextChanged(CharSequence searchText, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence searchText, int start, int before, int count) {
                //deactivateLaunchable();
                if (searchText.length() > 0) {

                    setListAdapter(mSearchResultComposer);
                    mSearchResultComposer.search(mSearchText.getText().toString());
                    mClearSearchText.setVisibility(View.VISIBLE);
                } else {
                    setListAdapter(mSearchHistoryComposer);
                    mSearchResultComposer.search(null);
                    mClearSearchText.setVisibility(View.GONE);
                }
            }
        });
        mSearchText.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO ||
                        (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {

                    Launchable launchable = (Launchable) mListAdapter.getItem(0);
                    if(launchable != null) {
                        InputMethodManager imm = MainActivity.this.getInputMethodManager();
                        if (imm != null) {
                            imm.hideSoftInputFromWindow(mSearchText.getWindowToken(), 0);
                        }
                        activateLaunchable(launchable);
                    }
                    return true;
                }
                return false;
            }
        });
        mSearchText.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER ||
                        keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                    Launchable launchable = (Launchable) mListAdapter.getItem(0);
                    if(launchable != null) {
                        InputMethodManager imm = MainActivity.this.getInputMethodManager();
                        if (imm != null) {
                            imm.hideSoftInputFromWindow(mSearchText.getWindowToken(), 0);
                        }
                        activateLaunchable(launchable);
                        finish();
                        // mLaunchers = new ArrayList<Launcher>();
                    }
                    return true;
                }
                return false;
            }
        });



        getListView().setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Launchable launchable = (Launchable) mListAdapter.getItem(position);
                if(launchable != null) {
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    activateLaunchable(launchable);
                    finish();
                    //mLaunchers = new ArrayList<Launcher>();
                }
            }
        });
        getListView().setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
                final Launchable launchable = (Launchable) mListAdapter.getItem(position);
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(launchable.getLabel());
                ArrayList<CharSequence> items = new ArrayList<CharSequence>();
                items.add("CREATE SHORTCUT");
                if (mListAdapter == mSearchHistoryComposer) {
                    items.add("REMOVE FROM LIST");
                }
                builder.setItems(items.toArray(new CharSequence[items.size()]), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int pos) {
                        switch (pos) {
                            case 0:
                                Intent shortcutIntent = launchable.getLauncher().getIntent(launchable);
                                if (shortcutIntent != null) {
                                    Intent intent = new Intent();
                                    intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
                                    intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, launchable.getLabel());
                                    intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, ThumbnailFactory.createShortcutIcon(launchable.getThumbnail()));
                                    intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
                                    MainActivity.this.sendBroadcast(intent);
                                    Toast.makeText(getApplicationContext(), "SHORTCUT CREATED", Toast.LENGTH_SHORT).show();
                                }
                                break;
                            case 1:
                                mSearchHistoryComposer.removeLaunchable(launchable);
                                break;
                        }
                    }
                });
                builder.setNegativeButton("Cancel", null);
                AlertDialog dialog = builder.create();
                dialog.show();
                return true;
            }
        });


        getListView().setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                InputMethodManager imm = getInputMethodManager();
                if (imm != null) {
                    imm.hideSoftInputFromWindow(mSearchText.getWindowToken(), 0);
                }
            }
        });



        mClearSearchText = (ImageButton) findViewById(R.id.clearSearchText);

        mClearSearchText.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mSearchText.setText("");
            }
        });






        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(CLEAR_SEARCH_TEXT_APPROVAL)) {
                mClearSearchTextApproval = savedInstanceState.getBoolean(CLEAR_SEARCH_TEXT_APPROVAL);
            } else {
                mClearSearchTextApproval = true;
            }
        } else {
            mClearSearchTextApproval = false;
        }
        Intent intent = getIntent();


        if(intent.getAction()!=null) {

            if (intent.getAction().equals("KAAM")) {
                Log.e("hhhh",CollectionWidget.EXTRA_ITEM);
                Log.e("VALUE",Integer.toString(intent.getIntExtra("launchable.id",0)));
                Log.e("VALUE",Integer.toString(intent.getIntExtra("launcher.id",0)));


               Log.e("position in main 1",Integer.toString(intent.getIntExtra("position",0)));
                int pos=intent.getIntExtra("position",0);



                Log.e("position in main 2",Integer.toString(intent.getIntExtra(Integer.toString(pos)+"1",0)));
                Log.e("position in main 3",Integer.toString(intent.getIntExtra(Integer.toString(pos)+"2",0)));
                int l_id=intent.getIntExtra(Integer.toString(pos)+"1",0);
                int le_id=intent.getIntExtra(Integer.toString(pos)+"2",0);
                Launcher l=null;
                for(int i=0;i<4;i++)
                {
                    Log.e("ID MATCHING",Integer.toString(le_id)+" DEFAULT "+ Integer.toString(mLaunchers.get(i).getId()));
                    if(mLaunchers.get(i).getId()==le_id) {
                        l = mLaunchers.get(i);
                    break;
                    }

                }
                if(l!=null){
                    Log.e("ID MATCHING",Integer.toString(le_id)+" DEFAULTXX "+ Integer.toString(l.getId()));


                    if( l.getLaunchable(l_id)!=null){



                            Launchable aa=l.getLaunchable(l_id);
                            activateLaunchable(aa);

                        
                    }







                }
                else
                    Log.e("position","panga");
       //         Launchable l = new Launchable(le_id,l_id,"bbb"); //mLaunchers.get(le_id).getLaunchable(l_id);
         //       activateLaunchable(l);


                //Toast.makeText(getApplicationContext(), "Touched view " + WidgetDataProvider.lr_id, Toast.LENGTH_SHORT).show();
            } else {
                //Toast.makeText(getApplicationContext(), "Touched view none ", Toast.LENGTH_SHORT).show();
            }
        }


    }

    @Override
    public void onSaveInstanceState(Bundle instanceState) {
        super.onSaveInstanceState(instanceState);
        if (getChangingConfigurations() != 0) {
            mClearSearchTextApproval = false;
            instanceState.putBoolean(CLEAR_SEARCH_TEXT_APPROVAL, mClearSearchTextApproval);
        }
    }

    @Override
    public void onDestroy() {
        mSearchHistoryComposer.onDestroy();
        mSearchResultComposer.onDestroy();
        super.onDestroy();
    }

    private void createLaunchers() {
        int launcherIndex = 0;


        if (mSettings.getBoolean("searchFavoriteItems", true)) {
            mFavoriteItemsLauncher = new FavoriteItemsLauncher(this);
            String strNumSuggestions = Integer.toString(4);
            try {
                int numSuggestions = Integer.parseInt(strNumSuggestions);
                mFavoriteItemsLauncher.setMaxSuggestions(numSuggestions);
            } catch (NumberFormatException e) {
            }
            mLaunchers.add(launcherIndex++, mFavoriteItemsLauncher);
        }

        if (mSettings.getBoolean("searchApps", true)) {
            AppLauncher appLauncher = new AppLauncher(this);

            try {
                int numSuggestions = 4;
                appLauncher.setMaxSuggestions(numSuggestions);
            } catch (NumberFormatException e) {
            }

            mLaunchers.add(launcherIndex++, appLauncher);
        }

        if (mSettings.getBoolean("searchContacts", true)) {
            Launcher contactLauncher;

            contactLauncher = new ContactLauncher(this);


            try {
                int numSuggestions = 4;
                contactLauncher.setMaxSuggestions(numSuggestions);
            } catch (NumberFormatException e) {
            }


            mLaunchers.add(launcherIndex++, contactLauncher);
        }



        if (mSettings.getBoolean("searchSongs", true)) {
            SongLauncher songLauncher = new SongLauncher(this);

            try {
                int numSuggestions = 4;
                songLauncher.setMaxSuggestions(numSuggestions);
            } catch (NumberFormatException e) {
            }

            mLaunchers.add(launcherIndex++, songLauncher);
        }
    }

    public ArrayList<Launcher> getLaunchers() {
        return mLaunchers;
    }


    public void activateLaunchable(Launchable launchable) {
        mActiveLaunchable = launchable;
        if (mActiveLaunchable.activate()) {
            mSearchHistoryComposer.addLaunchable(launchable, true, true, true);

            if ((mFavoriteItemsLauncher != null) && (mSearchText.getText().length() > 0)) {
                // TODO: AsyncTask
                ContentValues values = new ContentValues();
                values.put("SearchText", mSearchText.getText().toString());
                values.put("LauncherID", launchable.getLauncher().getId());
                values.put("LaunchableID", launchable.getId());
                //            values.put("TimeSlot",);
                mContentResolver.insert(FavoriteItemsProvider.FAVORITE_ITEMS_URI, values);
            }


            finish();

        }
    }



    private void setListAdapter(BaseAdapter listAdapter) {
        if (getListAdapter() != listAdapter) {
            mListAdapter = listAdapter;
            super.setListAdapter(mListAdapter);
            //mSearchText.requestFocus();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {


    }

    public FavoriteItemsLauncher getFavoriteItemsLauncher() {
        return mFavoriteItemsLauncher;
    }

    private InputMethodManager getInputMethodManager() {
        return (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    }




    public void getSnapshots() {

        progress = new ProgressDialog(this);
        progress.setMessage("Learning about You");
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.show();



        // User Activity
        mGoogleApiClient = new GoogleApiClient.Builder(MainActivity.this)
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

                                mSearchHistoryComposer = new SearchHistoryComposer(MainActivity.this);
                                setListAdapter(mSearchHistoryComposer);




                            return;
                        }
                        ActivityRecognitionResult arResult = detectedActivityResult.getActivityRecognitionResult();
                        DetectedActivity probableActivity = arResult.getMostProbableActivity();
                        Log.e("activity?",Integer.toString(probableActivity.getType()));
                        Log.e("in getsnapshots ", probableActivity.toString());
                        useractivity=probableActivity.getType();
                        confidene=probableActivity.getConfidence();

                        if((useractivity==3))
                        {
                            useractivity=0;
                        }
                        else if((useractivity!=3))
                        {
                            useractivity=100;
                        }

                            mSearchHistoryComposer = new SearchHistoryComposer(MainActivity.this);
                            setListAdapter(mSearchHistoryComposer);

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


                                mSearchHistoryComposer = new SearchHistoryComposer(MainActivity.this);
                                setListAdapter(mSearchHistoryComposer);


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

                            mSearchHistoryComposer = new SearchHistoryComposer(MainActivity.this);
                            setListAdapter(mSearchHistoryComposer);


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


        progress.dismiss();
    }

    private void getFineLocationSnapshots() {
        // Check for permission first
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            Log.e(TAG, "Fine Location Permission not yet granted");

                mSearchHistoryComposer = new SearchHistoryComposer(MainActivity.this);
                setListAdapter(mSearchHistoryComposer);



            progress.dismiss();

            locationlonglat=null;
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);

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


                                    mSearchHistoryComposer = new SearchHistoryComposer(MainActivity.this);
                                    setListAdapter(mSearchHistoryComposer);


                                progress.dismiss();
                                locationlonglat=null;
                                // mLocationTextView.setTextColor(Color.RED);
                                return;
                            }
                            Location location = locationResult.getLocation();
                            //mLocationTextView.setText(location.toString());
                            Log.e("LOCATION ",location.toString());

                            locationlonglat=location;



                                mSearchHistoryComposer = new SearchHistoryComposer(MainActivity.this);
                                setListAdapter(mSearchHistoryComposer);


                            progress.dismiss();

                        }
                    });


        }
        //mSearchHistoryComposer.Provide
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getFineLocationSnapshots();
                } else {
                    // Do nothing
                }
                return;
            }
        }
    }


}

