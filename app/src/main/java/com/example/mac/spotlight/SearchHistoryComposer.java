package com.example.mac.spotlight;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.mac.spotlight.apps.AppSyncer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

public class SearchHistoryComposer extends BaseAdapter {
	private static final String SEARCH_HISTORY_DB = "SearchHistory";

	private static final String[] SEARCH_HISTORY_PROJECTION = new String[] {
			"_ID", // 0
			"LauncherID", // 1
			"LaunchableID" // 2
	};
	private static final int ID_COLUMN_INDEX = 0;
	private static final int LAUNCHER_ID_COLUMN_INDEX = 1;
	private static final int LAUNCHABLE_ID_COLUMN_INDEX = 2;

	private static final int MSG_INIT_SEARCH_HISTORY = 1;
	private static final int MSG_ADD_LAUNCHABLE_TO_SEARCH_HISTORY = 2;
	private static final int MSG_REMOVE_LAUNCHABLE_FROM_SEARCH_HISTORY = 3;
	private static final int MSG_CLEAR_SEARCH_HISTORY = 4;
	private static HandlerThread sHandlerThread = null;
	public static KnnUsingKDTree K;

	private MainActivity mMainActivity;
	private WidgetDataProvider widgetDataProvider;
	private final Context mContext;
	private LayoutInflater mLayoutInflater;
	private final SearchHistoryWorker mSearchHistoryWorker;
	private  ArrayList<Launcher> mLaunchers;
	private  int mNumLaunchers;
	private  HashMap<Integer, Integer> mLauncherIndexes;
	private final LauncherObserver mLauncherObserver;
	public Vector<Launchable> mSuggestions = new Vector<Launchable>();
	private boolean mSearchHistoryEnabled = true;
	private int mMaxSearchHistorySize = 10;
	private boolean mCancelInitSearchHistory = false;
	private boolean mPendingListUpdate = false;

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

	public SearchHistoryComposer(MainActivity mainActivity) {
		mMainActivity = mainActivity;
		mContext = mainActivity;
		mLayoutInflater = LayoutInflater.from(mMainActivity);
		mSearchHistoryWorker = new SearchHistoryWorker(mContext);
		mLaunchers = mMainActivity.getLaunchers();
		if (mLaunchers!=null)
			mNumLaunchers = mLaunchers.size();
		else
			mNumLaunchers=0;
		mLauncherIndexes = new HashMap<Integer, Integer>(mNumLaunchers);
		mLauncherObserver = new LauncherObserver(new Handler());

		for (int i = 0; i < mNumLaunchers; i++) {
			mLauncherIndexes.put(mLaunchers.get(i).getId(), i);
			mLaunchers.get(i).registerContentObserver(mLauncherObserver);
		}

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mainActivity);
		mSearchHistoryEnabled = settings.getBoolean("searchHistory", true);

		try {
			mMaxSearchHistorySize = 100;
		} catch (NumberFormatException e) {
		}

		mSearchHistoryWorker.initSearchHistory(true);
	}

	public SearchHistoryComposer(WidgetDataProvider widgetDataProvider, int d) {
//		widgetDataProvider = null;
		mContext = widgetDataProvider.mContext;
	//	mLayoutInflater = LayoutInflater.from(mMainActivity);
		mSearchHistoryWorker = new SearchHistoryWorker(mContext);
		mLaunchers = widgetDataProvider.getLaunchers();
		mNumLaunchers = mLaunchers.size();
		mLauncherIndexes = new HashMap<Integer, Integer>(mNumLaunchers);
		mLauncherObserver = new LauncherObserver(new Handler());

		for (int i = 0; i < mNumLaunchers; i++) {
			mLauncherIndexes.put(mLaunchers.get(i).getId(), i);
			mLaunchers.get(i).registerContentObserver(mLauncherObserver);
		}
		mSearchHistoryEnabled = true;

		try {
			mMaxSearchHistorySize = 100;
		} catch (NumberFormatException e) {
		}

		mSearchHistoryWorker.initSearchHistory(true);
	}

	public void onDestroy() {
		mCancelInitSearchHistory = true;
		for (int i = 0; i < mNumLaunchers; i++) {
			mLaunchers.get(i).unregisterContentObserver(mLauncherObserver);
		}
	}



    @Override
	public void notifyDataSetChanged() {
		mPendingListUpdate = false;
		super.notifyDataSetChanged();
	}

	public boolean isListUpdatePending() {
		return mPendingListUpdate;
	}

	public void addLaunchable(Launchable launchable, boolean topOfList, boolean updateList, boolean updateDatabase) {
		int present = 0;
        if (mSearchHistoryEnabled) {
			for (Launchable l : mSuggestions) {
				if (launchable.getId() == l.getId() &&
						launchable.getLauncher().getId() == l.getLauncher().getId()) {
					//mSuggestions.remove(l);
                    present=1;
					break;
				}
			}
			if(present!=1) {
                if (topOfList) {
                    mSuggestions.add(0, launchable);
                } else {
                    mSuggestions.add(launchable);
                }
            }
			if (mSuggestions.size() > mMaxSearchHistorySize) {
				mSuggestions.setSize(mMaxSearchHistorySize);
			}
			if (updateList) {
				notifyDataSetChanged();
			} else {
				mPendingListUpdate = true;
			}
			if (updateDatabase) {
				mSearchHistoryWorker.addLaunchable(launchable);
			}
		}






    //    mMainActivity.updateWidgetshubhi();
        mMainActivity.updateWidgets(mContext);







	}

	public void removeLaunchable(Launchable launchable) {
		if (mSearchHistoryEnabled) {
			for (Launchable l : mSuggestions) {
				if (launchable.getId() == l.getId() &&
						launchable.getLauncher().getId() == l.getLauncher().getId()) {
					mSuggestions.remove(l);
					break;
				}
			}
			notifyDataSetChanged();
			mSearchHistoryWorker.removeLaunchable(launchable);
		}
	}

	public void clearSearchHistory(boolean clearSearchHistoryDatabase) {
		mSuggestions.clear();
		notifyDataSetChanged();
		if (clearSearchHistoryDatabase) {
			mSearchHistoryWorker.clearSearchHistory();
		}
	}

	@Override
	public int getCount() {
		return mSuggestions.size();
	}

	@Override
	public Object getItem(int position) {
		if (position < mSuggestions.size()) {
			return mSuggestions.get(position);
		} else {
			return null;
		}
	}

	@Override
	public long getItemId(int position) {
		return mSuggestions.get(position).getId();
	}

	private class ViewHolder {
		ImageView mThumbnail;
		TextView mLabel;
		TextView mInfoText;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder viewHolder;
		if (convertView == null) {
			convertView = mLayoutInflater.inflate(R.layout.launchable, null);
			viewHolder = new ViewHolder();
			LinearLayout textContainer = (LinearLayout) convertView.findViewById(R.id.textContainer);
			viewHolder.mThumbnail =  (ImageView) convertView.findViewById(R.id.thumbnail);
			viewHolder.mLabel = (TextView) textContainer.findViewById(R.id.label);
			viewHolder.mInfoText = (TextView) textContainer.findViewById(R.id.infoText);
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		Launchable launchable = mSuggestions.get(position);
		if(launchable.getThumbnail() != null) {
			viewHolder.mThumbnail.setImageDrawable(launchable.getThumbnail());
			launchable.setBadgeParent(viewHolder.mThumbnail);
			viewHolder.mThumbnail.setTag(position);
			viewHolder.mThumbnail.setVisibility(View.VISIBLE);
		} else {
			launchable.setBadgeParent(null);
			viewHolder.mThumbnail.setTag(null);
			viewHolder.mThumbnail.setOnClickListener(null);
			viewHolder.mThumbnail.setVisibility(View.GONE);
		}
		viewHolder.mLabel.setText(launchable.getLabel());
		if(launchable.getInfoText() != null) {
			viewHolder.mInfoText.setText(launchable.getInfoText());
			viewHolder.mInfoText.setVisibility(View.VISIBLE);
		} else {
			viewHolder.mInfoText.setVisibility(View.GONE);
		}
		return convertView;
	}

	public class SearchHistoryWorker extends Handler {
		private final AsyncSearchHistoryWorker mAsyncSearchHistoryWorker;
		private final SearchHistoryDatabase mSearchHistoryDatabase;

		public SearchHistoryWorker(Context context) {
			synchronized (SearchHistoryWorker.class) {
				if (sHandlerThread == null) {
					sHandlerThread = new HandlerThread("SearchHistoryWorker");
					sHandlerThread.start();
				}
			}
			mAsyncSearchHistoryWorker = new AsyncSearchHistoryWorker(sHandlerThread.getLooper());
			mSearchHistoryDatabase = new SearchHistoryDatabase(context);
		}

		public void initSearchHistory(boolean clearSearchHistory) {
			Message msg = mAsyncSearchHistoryWorker.obtainMessage();
			msg.what = MSG_INIT_SEARCH_HISTORY;
			msg.arg1 = clearSearchHistory ? 1 : 0;
			msg.obj = this;
			mAsyncSearchHistoryWorker.sendMessage(msg);
		}

		public void addLaunchable(Launchable launchable) {
			Message msg = mAsyncSearchHistoryWorker.obtainMessage();
			msg.what = MSG_ADD_LAUNCHABLE_TO_SEARCH_HISTORY;
			msg.obj = launchable;
			mAsyncSearchHistoryWorker.sendMessage(msg);
		}

		public void removeLaunchable(Launchable launchable) {
			Message msg = mAsyncSearchHistoryWorker.obtainMessage();
			msg.what = MSG_REMOVE_LAUNCHABLE_FROM_SEARCH_HISTORY;
			msg.obj = launchable;
			mAsyncSearchHistoryWorker.sendMessage(msg);
		}

		public void clearSearchHistory() {
			Message msg = mAsyncSearchHistoryWorker.obtainMessage();
			msg.what = MSG_CLEAR_SEARCH_HISTORY;
			mAsyncSearchHistoryWorker.sendMessage(msg);
		}

		@Override
		public void handleMessage(Message msg) {
			int event = msg.what;
			switch (event) {
				case MSG_INIT_SEARCH_HISTORY: {
					Launchable launchable = (Launchable) msg.obj;
					SearchHistoryComposer.this.addLaunchable(launchable, false, true, false);
				} break;
				case MSG_CLEAR_SEARCH_HISTORY: {
					SearchHistoryComposer.this.clearSearchHistory(false);
				} break;
			}
		}

		private class AsyncSearchHistoryWorker extends Handler {
            KnnUsingKDTree K;
			public AsyncSearchHistoryWorker(Looper looper) {
				super(looper);
			}

			@Override
			public void handleMessage(Message msg) {
				int event = msg.what;
				switch (event) {
					case MSG_INIT_SEARCH_HISTORY: {
						Handler handler = (Handler) msg.obj;
						boolean clearSearchHistory = (msg.arg1 != 0) ? true : false;
						initSearchHistory(handler, clearSearchHistory);
                        //Provide_Results();
					} break;
					case MSG_ADD_LAUNCHABLE_TO_SEARCH_HISTORY: {
						Launchable launchable = (Launchable) msg.obj;
						addLaunchable(launchable);
                        //Provide_Results();
					} break;
					case MSG_REMOVE_LAUNCHABLE_FROM_SEARCH_HISTORY: {
						Launchable launchable = (Launchable) msg.obj;
						removeLaunchable(launchable);
					} break;
					case MSG_CLEAR_SEARCH_HISTORY: {
						clearSearchHistory();
					} break;
				}
			}

//TODO
            public void Provide_Results() {
                double s[] = {MainActivity.Timeslot, MainActivity.useractivity, MainActivity.headphonesON, 7, 0, 0};
                if (MainActivity.locationlonglat != null) {
                    s[4] = MainActivity.locationlonglat.getLatitude();
                    s[5] = MainActivity.locationlonglat.getLongitude();
                }
                SQLiteDatabase db;
                try {
                    db = mSearchHistoryDatabase.getWritableDatabase();
                } catch (SQLiteException e) {
                    db = null;
                }
                if (db != null) {
                    mSuggestions.clear();
                    int index=0;
                    int flag=0;
                    int num = 10;
                    double is[] = new double[num];
                    int i = 0;
                    Log.e("jj",Double.toString(is[i]));

                    while (is[i] != -1 && i < num-1) {
                        is[i] = K.knnnearestneighbour(K.kdt, s);
                        Log.e("jj",Double.toString(is[i]));
                        Cursor cursor1 = db.rawQuery("SELECT LauncherID, LaunchableID FROM " + SEARCH_HISTORY_DB + " WHERE _ID = ?" ,new String[] {Double.toString(is[i])});
                        cursor1.moveToFirst();
                        if(cursor1!=null && cursor1.getCount()>0) {
                            int launcher_id = cursor1.getInt(0);
                            int launchable_id = cursor1.getInt(1);
                            Log.e("Cursor data1",Integer.toString(launcher_id));
                            Log.e("Cursor data2",Integer.toString(launchable_id));


                            Integer launcherIndex = mLauncherIndexes.get(launcher_id);
                            if (launcherIndex != null) {
                                Launchable launchable = mLaunchers.get(launcherIndex).getLaunchable(launchable_id);
                                Log.e("Cursor data3",launchable.getLabel());

                                if (launchable != null) {
                                    for (Launchable l : mSuggestions) {
                                        if (launchable.getId() == l.getId() &&
                                                launchable.getLauncher().getId() == l.getLauncher().getId()) {
                                            //mSuggestions.remove(l);
                                            Log.e("ALREADY PRESENT",launchable.getLabel());
                                            flag=1;
                                            break;
                                        }
                                    }
                                    if(flag==0) {
                                        //SearchHistoryComposer.this.addLaunchable(launchable, false, false, false);
                                        mSuggestions.add(index, launchable);
                                        Log.e("ADD HUA",launchable.getLabel());
                                        Log.e("ADDED AT INDEX",Integer.toString(index));
                                        index++;
                                        SearchHistoryComposer.this.addLaunchable(launchable,false,false,false);
                                        for (Launchable l : mSuggestions) {
                                            Log.e("PRESENT CURRENTLY",l.getLabel());
                                        }
                                        //notifyDataSetChanged();



                                    }
                                    flag=0;


                                }
                            }
                            cursor1.close();
                        }
                        i++;
                    }


                }
                //TODO load msuggestion in list
            }


            private void initSearchHistory(Handler handler, boolean clearSearchHistory) {
                if (clearSearchHistory) {
                    Message reply = handler.obtainMessage();
                    reply.what = MSG_CLEAR_SEARCH_HISTORY;
                    reply.sendToTarget();
                }
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
                            while (!cursor.isAfterLast() && !mCancelInitSearchHistory) {

                                x[0] = cursor.getInt(3);
                                x[1] = cursor.getInt(4);
                                x[2] = cursor.getInt(5);
                                x[3] = cursor.getDouble(6);
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

                            double id=0;
                            int n=0;
                            while(n<15 && id!=-1) {
                                Log.e("S_SIZE",Integer.toString(mSuggestions.size()));
								System.out.println("---------------------------------");
                                id = K.knnnearestneighbour(K.kdt, s1);
                                Log.e("jj", Double.toString(id));
                                Cursor cursor1 = db.rawQuery("SELECT LauncherID, LaunchableID FROM " + SEARCH_HISTORY_DB + " WHERE _ID = ?", new String[]{Double.toString(id)});
                                cursor1.moveToFirst();
                                if (cursor1 != null && cursor1.getCount() > 0) {
                                    int launcher_id = cursor1.getInt(0);
                                    int launchable_id = cursor1.getInt(1);
                                    Log.e("Cursor data1", Integer.toString(launcher_id));
                                    Log.e("Cursor data2", Integer.toString(launchable_id));
//todo update n_suggestions

                                    Integer launcherIndex = mLauncherIndexes.get(launcher_id);
                                    if (launcherIndex != null) {
                                        Launchable launchable = mLaunchers.get(launcherIndex).getLaunchable(launchable_id);

                                        if (launchable != null) {
                                            if(!mSuggestions.contains(launchable)){
                                                n=n+1;
                                            }
                                            Log.e("Cursor data3", launchable.getLabel());
                                            Message reply = handler.obtainMessage();
                                            reply.what = MSG_INIT_SEARCH_HISTORY;
                                            reply.obj = launchable;
                                            reply.sendToTarget();
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
                    AppSyncer.start(mContext);


                }
            }
			private double deg2rad(double deg) {
				return (deg * Math.PI / 180.0);
			}

			private double rad2deg(double rad) {
				return (rad * 180 / Math.PI);
			}
			public double distance(double lat1, double lon1, double lat2, double lon2, String unit) {
				double theta = lon1 - lon2;
				double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
				dist = Math.acos(dist);
				dist = rad2deg(dist);
				dist = dist * 60 * 1.1515;
				if (unit == "K") {
					dist = dist * 1.609344;
				} else if (unit == "N") {
					dist = dist * 0.8684;
				}

				return (dist);
			}

			private void addLaunchable(Launchable launchable) {
				SQLiteDatabase db;
				try {
					db = mSearchHistoryDatabase.getWritableDatabase();
				} catch (SQLiteException e) {
					db = null;
				}
				if (db != null) {

					Cursor cursor1 = db.rawQuery("SELECT _ID, Counter, Latitude, Longitude, DetectedActivity, HeadphonesOn FROM " + SEARCH_HISTORY_DB + " WHERE LauncherID = ? AND LaunchableID = ? AND TimeSlot = ?",new String[] { String.valueOf(launchable.getLauncher().getId()),  String.valueOf(launchable.getId()), String.valueOf(MainActivity.Timeslot)});
					boolean flag=false;
					int id1 = -1;
					double counter1 = -1;
					int headphonesON=-1;
					Location longlat=new Location("shubhi");
					int useractivity=4;
                    int h = cursor1.getCount();
                    Log.i("CURSOR COUNT",Integer.toString(h));

                    if (cursor1 != null) {
						cursor1.moveToFirst();
                        int temp=0;
						while (!cursor1.isAfterLast() && temp!=1 ) {
                            id1 = cursor1.getInt(0);
//todo
                            counter1 = cursor1.getDouble(1);
							useractivity=cursor1.getInt(4);
							Log.e("id",Integer.toString(id1));
							headphonesON=cursor1.getInt(5);
//todo
							if((cursor1.getInt(2)!=0)&&MainActivity.locationlonglat!=null) {
								longlat.setLatitude(cursor1.getDouble(2));
								longlat.setLongitude(cursor1.getDouble(3));
								Double lat1 = cursor1.getDouble(2);
								Double lon1 = cursor1.getDouble(3);
								Double lat2 = (MainActivity.locationlonglat.getLatitude());
								Double lon2 = MainActivity.locationlonglat.getLongitude();
                                double distance=distance(lat1,lon1,lat2,lon2,"M");
                                if (distance >= 0 && distance <= 1) {
									flag = true;
                                    temp=1;
									cursor1.close();
                                    break;
								}
							}
							else if((cursor1.getInt(2)==0)&&MainActivity.locationlonglat==null) {
								flag = true;
                                temp=1;
								cursor1.close();
                                break;
							}
                                cursor1.moveToNext();
						}

					}
					cursor1.close();

					if (id1 >= 0) {

						if(flag) {
                            double lamda =-0.2;
                            double normalisation_factor = Math.pow(2,lamda);
                            Log.i("CHECK FUNCTION",Double.toString(normalisation_factor));

                            db.execSQL("UPDATE " + SEARCH_HISTORY_DB + " SET Counter = " + ((counter1 * normalisation_factor)+1) + " WHERE _ID = " + id1);
							db.execSQL("UPDATE " + SEARCH_HISTORY_DB + " SET Counter = max(Counter*"+  normalisation_factor + ",1) WHERE _ID != " + id1 +" AND TimeSlot = "+ String.valueOf(MainActivity.Timeslot));
                            Log.i("CHECK FUNCTION",Double.toString((counter1 * normalisation_factor)+1));
                            db.execSQL("UPDATE " + SEARCH_HISTORY_DB + " SET DetectedActivity = " +(((useractivity * counter1)+ MainActivity.useractivity)/((counter1 * normalisation_factor)+1)) + " WHERE _ID = " + id1);
                            db.execSQL("UPDATE " + SEARCH_HISTORY_DB + " SET HeadphonesOn = " +(((headphonesON * counter1)+ MainActivity.headphonesON)/((counter1 * normalisation_factor)+1)) + " WHERE _ID = " + id1);

                            //db.execSQL("UPDATE " + SEARCH_HISTORY_DB + " SET DetectedActivity = " +(((useractivity*((Math.log(counter1))/Math.log(2))+1)+MainActivity.useractivity)/(((Math.log(counter1))/Math.log(2))+2)) + " WHERE _ID = " + id1);
							//db.execSQL("UPDATE " + SEARCH_HISTORY_DB + " SET HeadphonesOn = " +(((headphonesON*((Math.log(counter1))/Math.log(2))+1)+MainActivity.headphonesON)/(((Math.log(counter1))/Math.log(2))+2)) + " WHERE _ID = " + id1);


						}


					}
					//bdb.execSQL("UPDATE " + FAVORITE_ITEMS_TABLE_NAME + " SET Counter = min(Counter - 1, 64) WHERE SearchText = '" + searchText + "'");
					//db.execSQL("DELETE FROM " + FAVORITE_ITEMS_TABLE_NAME + " WHERE Counter <= 0");
					if ((id1 < 0)||(!flag)) {

						if (MainActivity.locationlonglat != null) {
							db.execSQL("INSERT INTO " + SEARCH_HISTORY_DB + "(LauncherID, "
									+ "LaunchableID, "
									+ "Counter, "
									+ "TimeSlot, "
									+ "HeadphonesOn, "
									+ "DetectedActivity, "
									+ "Latitude, "
									+ "Longitude "
									+ ") Values" + "('" + launchable.getLauncher().getId() + "', '"
									+ launchable.getId() + "','1','" + MainActivity.Timeslot + "','" + MainActivity.headphonesON + "','" + MainActivity.useractivity + "','" + MainActivity.locationlonglat.getLatitude() + "','" + MainActivity.locationlonglat.getLongitude() + "');");
						}
						else {
							db.execSQL("INSERT INTO " + SEARCH_HISTORY_DB + "(LauncherID, "
									+ "LaunchableID, "
									+ "Counter, "
									+ "TimeSlot, "
									+ "HeadphonesOn, "
									+ "DetectedActivity, "
									+ "Latitude, "
									+ "Longitude "
									+ ") Values" + "('" + launchable.getLauncher().getId() + "', '"
									+ launchable.getId() + "','1','" + MainActivity.Timeslot + "','" + MainActivity.headphonesON + "','" + MainActivity.useractivity + "','" + 0 + "','" + 0 + "');");

						}
					}
					db.close();
				}
			}

			private void removeLaunchable(Launchable launchable) {
				SQLiteDatabase db;
				try {
					db = mSearchHistoryDatabase.getWritableDatabase();
				} catch (SQLiteException e) {
					db = null;
				}
				if (db != null) {
					db.execSQL("DELETE FROM " + SEARCH_HISTORY_DB
							+ " WHERE LauncherID = " + launchable.getLauncher().getId() + " AND LaunchableID = " + launchable.getId());
					db.close();
				}
			}

			private void clearSearchHistory() {
				SQLiteDatabase db;
				try {
					db = mSearchHistoryDatabase.getWritableDatabase();
				} catch (SQLiteException e) {
					db = null;
				}
				if (db != null) {
					db.delete(SEARCH_HISTORY_DB, null, null);
					db.close();
				}
			}
		}
	}
	private static Double toRad(Double value) {
		return value * Math.PI / 180;
	}
	private class LauncherObserver extends ContentObserver {
		public LauncherObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			mSearchHistoryWorker.initSearchHistory(true);
		}
	}
}