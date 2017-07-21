package com.example.mac.spotlight.apps;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MergeCursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Binder;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import com.example.mac.spotlight.Launchable;
import com.example.mac.spotlight.Launcher;

import static com.example.mac.spotlight.apps.AppProvider.APPS_URI;

public class AppLauncher extends Launcher {
	private static final String NAME = "AppLauncher";
	private static final String[] APPS_PROJECTION = new String[] {
	       "_ID", // 0
	        "Label", // 1
	        "Package", // 2
	        "Class" // 3
	    };
	private static final int ID_COLUMN_INDEX = 0;
	private static final int LABEL_COLUMN_INDEX = 1;
	private static final int PACKAGE_COLUMN_INDEX = 2;
	private static final int CLASS_COLUMN_INDEX = 3;
	
	private Context mContext;
	private ContentResolver mContentResolver;
	private PackageManager mPackageManager;
	
	public AppLauncher(Context context) {
		mContext = context;
		mContext.grantUriPermission("com.example.mac.spotlight.WidgetProvider", Uri.parse("content://spotlight/apps"), Intent.FLAG_GRANT_READ_URI_PERMISSION );

		mContentResolver = mContext.getContentResolver();
		mPackageManager = mContext.getPackageManager();
	}
	
	@Override
	public String getName() {
		return NAME;
	}
	
	@Override
	public ArrayList<Launchable> getSuggestions(String searchText, int offset, int limit) {
		Cursor cursor0 = null;

			String searchPattern = "";
			for(char c : searchText.toCharArray()) {
				searchPattern +=  c+"%";
			}
			searchPattern += "%";
        Cursor cursor00 = mContentResolver.query(APPS_URI, APPS_PROJECTION,
                "LOWER(Label) LIKE ? ",
                new String[] { searchText+"%"  },
                "Label");
			cursor0 = mContentResolver.query(APPS_URI, APPS_PROJECTION,
				"LOWER(Label) LIKE ? ",
				new String[] { searchPattern  },
				"Label");

            Cursor cursor1 = mContentResolver.query(APPS_URI, APPS_PROJECTION,
                    "LOWER(Label) LIKE ? ",
                    new String[] { "%"+searchText+"%"  },
                    "Label");

        Cursor cursor = new MergeCursor(new Cursor[]{cursor00,cursor0,cursor1});

		ArrayList<Launchable> suggestions = new ArrayList<Launchable>();
		if (cursor != null) {
			if (cursor.getCount() > offset) {
				cursor.moveToFirst();
				cursor.move(offset);
				int i = 0;
				while (!cursor.isAfterLast() && i++ < limit) {
					Intent intent = new Intent(Intent.ACTION_MAIN);
					intent.addCategory(Intent.CATEGORY_LAUNCHER);
					intent.setClassName(cursor.getString(PACKAGE_COLUMN_INDEX), cursor.getString(CLASS_COLUMN_INDEX));
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
					Drawable thumbnail = getThumbnail(cursor.getInt(ID_COLUMN_INDEX), intent);
					Launchable G =new AppLaunchable(this, cursor.getInt(ID_COLUMN_INDEX), cursor.getString(LABEL_COLUMN_INDEX), intent, thumbnail);
                    if ((G != null) && !suggestions.contains(G)) {
                        suggestions.add(G);
                    }
					cursor.moveToNext();
				}
			}
			cursor.close(); 			
		}
		return suggestions;
	}
	
	@Override
	public Launchable getLaunchable(int id) {
		Launchable launchable = null;
		Cursor cursor = null;
		final long token = Binder.clearCallingIdentity();
		try {
			mContext.grantUriPermission("com.example.mac.spotlight", APPS_URI, Intent.FLAG_GRANT_READ_URI_PERMISSION);
			mContentResolver=mContext.getContentResolver();
			cursor = mContentResolver.query(APPS_URI, APPS_PROJECTION,
					"_ID = ?",
					new String[] { String.valueOf(id) },
					null);
		} finally {
			Binder.restoreCallingIdentity(token);
		}

		if(cursor != null) {
			if(cursor.getCount() > 0) {
				cursor.moveToFirst();
				Intent intent = new Intent(Intent.ACTION_MAIN);
				intent.addCategory(Intent.CATEGORY_LAUNCHER);
				intent.setClassName(cursor.getString(PACKAGE_COLUMN_INDEX), cursor.getString(CLASS_COLUMN_INDEX));
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
				Drawable thumbnail = getThumbnail(cursor.getInt(ID_COLUMN_INDEX), intent);
				launchable = new AppLaunchable(this, cursor.getInt(ID_COLUMN_INDEX), cursor.getString(LABEL_COLUMN_INDEX), intent, thumbnail); 				
			}
			cursor.close();
		}
		return launchable;
	}
	
	@Override
	public boolean activate(Launchable launchable) {
		if(launchable instanceof AppLaunchable) {
			AppLaunchable appLaunchable = (AppLaunchable) launchable;
			List<ResolveInfo> list = mContext.getPackageManager().queryIntentActivities(appLaunchable.getIntent(),
				PackageManager.MATCH_DEFAULT_ONLY);
			if(list.size() > 0) {
				try {
	            	mContext.startActivity(appLaunchable.getIntent());
	            } catch (Exception e) {
	            	Toast.makeText(mContext, "Sorry: Cannot launch \"" + launchable.getLabel() + "\"", Toast.LENGTH_SHORT).show();
	            	//Log.e(mContext.getResources().getString(R.string.appName), e.getMessage());
	            	return false;
	            }
				return true;
			}
		}
		return false;
	}
	
	public boolean registerContentObserver(ContentObserver observer) {
		mContentResolver.registerContentObserver(APPS_URI, false, observer);
		return true;
	}
	
	public boolean unregisterContentObserver(ContentObserver observer) {
		mContentResolver.unregisterContentObserver(observer);
		return true;
	}
	
	private Drawable getThumbnail(int id, Intent intent) {
		Drawable thumbnail = null;
		try {
			// PackageManager already caches app icons
			thumbnail = mPackageManager.getActivityIcon(intent);
		} catch (NameNotFoundException e) {
		}
		return thumbnail;
	}
	
	@Override
    public Intent getIntent(Launchable launchable) {
        AppLaunchable appLaunchable = (AppLaunchable) launchable;
        return appLaunchable.getIntent(); 
    }
}