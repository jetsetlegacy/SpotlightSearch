package com.example.mac.spotlight.audio;


import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.MergeCursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.example.mac.spotlight.Launchable;
import com.example.mac.spotlight.Launcher;
import com.example.mac.spotlight.R;
import com.example.mac.spotlight.ThumbnailFactory;

public class SongLauncher extends Launcher {
	public static final String NAME = "SongLauncher";
	
	private static final String[] SONG_PROJECTION = new String[] {
		MediaStore.Audio.Media._ID, // 0
		MediaStore.Audio.Media.ARTIST, // 1
		MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.TITLE // 3
	};
	private static final int ID_COLUMN_INDEX = 0;
	private static final int ARTIST_COLUMN_INDEX = 1;
	private static final int ALBUM_COLUMN_INDEX = 2;
	private static final int TITLE_COLUMN_INDEX = 3;
	public Uri path;
	private Context mContext;
	private ContentResolver mContentResolver;
	private Drawable mThumbnail;
	
	public SongLauncher(Context context) {
		mContext = context;
		mContentResolver = context.getContentResolver();
		mThumbnail = ThumbnailFactory.createThumbnail(context, context.getResources().getDrawable(R.drawable.music_tracks));
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
        Cursor cursor00 = mContentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                SONG_PROJECTION,
                MediaStore.Audio.Media.IS_MUSIC + " = 1" +
                        " AND LOWER(" + MediaStore.Audio.Media.TITLE + ") LIKE ?" ,
                new String[] { searchText+"%"},
                MediaStore.Audio.Media.TITLE + " ASC");
				Cursor cursor0 = mContentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
					SONG_PROJECTION,
					MediaStore.Audio.Media.IS_MUSIC + " = 1" +
						" AND LOWER(" + MediaStore.Audio.Media.TITLE + ") LIKE ?" ,
					new String[] { searchPattern},
					MediaStore.Audio.Media.TITLE + " ASC");
        Cursor cursor1 = mContentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                SONG_PROJECTION,
                MediaStore.Audio.Media.IS_MUSIC + " = 1" +
                        " AND LOWER(" + MediaStore.Audio.Media.TITLE + ") LIKE ?" ,
                new String[] { "%"+searchText+"%"},
                MediaStore.Audio.Media.TITLE + " ASC");
        cursor = new MergeCursor(new Cursor[]{cursor00,cursor0,cursor1});


		ArrayList<Launchable> suggestions = new ArrayList<Launchable>();
 		if (cursor != null) {
 			if (cursor.getCount() > offset) {
 				cursor.moveToFirst();
 				cursor.move(offset);
 				int i = 0;
 				while (!cursor.isAfterLast() && i++ < limit) {
 					SongLaunchable launchable = new SongLaunchable(this,
						cursor.getInt(ID_COLUMN_INDEX),
						cursor.getString(TITLE_COLUMN_INDEX), cursor.getString(ARTIST_COLUMN_INDEX) 
							+ " - " + cursor.getString(ALBUM_COLUMN_INDEX));
                    if ((launchable != null) && !suggestions.contains(launchable)) {
                        suggestions.add(launchable);
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
		Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
        path = uri;
		Cursor cursor = mContentResolver.query(uri, SONG_PROJECTION,	null, null, null);
		Launchable launchable = null;
		if(cursor != null) {
 			if(cursor.getCount() > 0) {
 				cursor.moveToFirst();
 				launchable = new SongLaunchable(this,
					cursor.getInt(ID_COLUMN_INDEX),
					cursor.getString(TITLE_COLUMN_INDEX), cursor.getString(ARTIST_COLUMN_INDEX) + " - " + cursor.getString(ALBUM_COLUMN_INDEX));
 			}
 			cursor.close();

		}
		return launchable;
	}

    @Override
    public boolean activate(Launchable launchable) {
        if (launchable instanceof SongLaunchable) {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, launchable.getId()));
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            List<ResolveInfo> list = mContext.getPackageManager().queryIntentActivities(intent,
                    PackageManager.MATCH_DEFAULT_ONLY);
            if(list.size() > 0) {
                try {
                    Log.e("bla",launchable.getLabel());
                    Log.e("bla1",launchable.getInfoText());
                    Log.e("bla1",launchable.toString());
                    Log.e("bla1", ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,launchable.getId()).toString());
                    String koshish = getRealPathFromURI(mContext, ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,launchable.getId()));

                    File file = new File(koshish);
                    intent.setDataAndType(Uri.fromFile(file), "audio/*");


                    mContext.startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(mContext, "Sorry: Cannot launch \"" + launchable.getLabel() + "\"", Toast.LENGTH_SHORT).show();
                    //Log.e(mContext.getResources().getString(R.string.appName), e.getMessage());
                    return false;
                }
            }
            return true;
        }
        return false;
    }
    
    public Drawable getThumbnail(Launchable launchable) {
		return mThumbnail;
	}
    
    @Override
    public Intent getIntent(Launchable launchable) {
        Intent intent = new Intent(Intent.ACTION_VIEW,
            ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, launchable.getId()));
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        return intent;
    }
    public String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Audio.Media.DATA };
            cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
            cursor.moveToFirst();
            Log.e("bla1",cursor.getString(column_index));
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
