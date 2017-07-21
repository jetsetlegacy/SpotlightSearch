package com.example.mac.spotlight.favoriteitems;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MergeCursor;

import java.util.ArrayList;
import java.util.HashMap;

import com.example.mac.spotlight.Launchable;
import com.example.mac.spotlight.Launcher;
import com.example.mac.spotlight.MainActivity;
import com.example.mac.spotlight.WidgetDataProvider;

public class FavoriteItemsLauncher extends Launcher {
	private static final String NAME = "FavoriteItemsLauncher";
	private static final String[] FAVORITE_ITEMS_PROJECTION = new String[] {"_ID", "SearchText", "LauncherID", "LaunchableID", "Counter"};

	private static final int LAUNCHER_ID_COLUMN_INDEX = 2;
	private static final int LAUNCHABLE_ID_COLUMN_INDEX = 3;

		
	private ContentResolver mContentResolver;
	private  MainActivity mMainActivity;
	private WidgetDataProvider mwidgetDataProvider;
	private ArrayList<Launcher> mLaunchers;
	private int mNumLaunchers;
	private HashMap<Integer, Integer> mLauncherIndexes;
	
	public FavoriteItemsLauncher(MainActivity mainActivity) {
		mMainActivity = mainActivity;
		mContentResolver = mainActivity.getContentResolver();
	}

	public FavoriteItemsLauncher(WidgetDataProvider widgetDataProvider) {
		mwidgetDataProvider = widgetDataProvider;
//		mContentResolver = widgetDataProvider.getContentResolver();
	}

	public void init2() {
		mLaunchers = mwidgetDataProvider.getLaunchers();
		mNumLaunchers = mLaunchers.size();
		mLauncherIndexes = new HashMap<Integer, Integer>(mNumLaunchers);
		for (int i = 0; i < mNumLaunchers; i++) {
			mLauncherIndexes.put(mLaunchers.get(i).getId(), i);
		}
	}
	
	public void init() {
		mLaunchers = mMainActivity.getLaunchers();
		mNumLaunchers = mLaunchers.size();
		mLauncherIndexes = new HashMap<Integer, Integer>(mNumLaunchers);
		for (int i = 0; i < mNumLaunchers; i++) {
			mLauncherIndexes.put(mLaunchers.get(i).getId(), i);			
		}
	}
	
	@Override
	public String getName() {
		return NAME;
	}
	
	@Override
	public ArrayList<Launchable> getSuggestions(String searchText, int offset, int limit) {
		Cursor cursor = null;


			String searchPattern = "";
			for(char c : searchText.toCharArray()) {
				searchPattern +=  c+"%";
			}
			searchPattern += "%";

        Cursor cursor00 =mContentResolver.query(FavoriteItemsProvider.FAVORITE_ITEMS_URI, FAVORITE_ITEMS_PROJECTION,
                "LOWER(SearchText) LIKE ? ",
                new String[] { searchText+"%"  },
                "SearchText ASC, Counter DESC");

        Cursor cursor0 = mContentResolver.query(FavoriteItemsProvider.FAVORITE_ITEMS_URI, FAVORITE_ITEMS_PROJECTION,
                "LOWER(SearchText) LIKE ? ",
                new String[] { searchPattern  },
                "SearchText ASC, Counter DESC");

        Cursor cursor1 =mContentResolver.query(FavoriteItemsProvider.FAVORITE_ITEMS_URI, FAVORITE_ITEMS_PROJECTION,
                "LOWER(SearchText) LIKE ? ",
                new String[] { "%"+searchText+"%"  },
                "SearchText ASC, Counter DESC");

        cursor = new MergeCursor(new Cursor[]{cursor00,cursor0,cursor1});
		ArrayList<Launchable> suggestions = new ArrayList<Launchable>();
		if (cursor != null) {
			if (cursor.getCount() > offset) {
				cursor.moveToFirst();
				cursor.move(offset);
				int i = 0;
				while (!cursor.isAfterLast() && i++ < limit) {
					Integer launcherID = cursor.getInt(LAUNCHER_ID_COLUMN_INDEX);
					Integer launcherIndex = mLauncherIndexes.get(launcherID);
					if (launcherIndex != null) {
						Launchable launchable = mLaunchers.get(launcherIndex).getLaunchable(cursor.getInt(LAUNCHABLE_ID_COLUMN_INDEX));
						if ((launchable != null) && !suggestions.contains(launchable)) {
							suggestions.add(launchable);
						}
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
		return null;
	}
	
	@Override
	public boolean activate(Launchable launchable) {		
		return false;
	}
	
	public boolean registerContentObserver(ContentObserver observer) {
		return true;
	}
	
	public boolean unregisterContentObserver(ContentObserver observer) {
		return true;
	}
	
	@Override
    public Intent getIntent(Launchable launchable) {
	    return null;
	}
}