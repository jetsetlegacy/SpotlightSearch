package com.example.mac.spotlight.contacts;


import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MergeCursor;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.QuickContact;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.InputStream;
import java.util.ArrayList;

import com.example.mac.spotlight.Launchable;
import com.example.mac.spotlight.Launcher;
import com.example.mac.spotlight.R;
import com.example.mac.spotlight.ThumbnailFactory;

import static android.R.attr.bitmap;

@TargetApi(5)
public class ContactLauncher extends Launcher {
	public static final String NAME = "ContactLauncher";
	private static final String NAME_COLUMN = ContactsContract.Contacts.DISPLAY_NAME;
	private static final String PRESENCE_STATUS = ContactsContract.Contacts.CONTACT_PRESENCE;
	private static final String LOOKUP_KEY = ContactsContract.Contacts.LOOKUP_KEY;
	private static final String VISIBILITY = ContactsContract.Contacts.IN_VISIBLE_GROUP;
	private static final String[] CONTACTS_PROJECTION = new String[] {
		ContactsContract.Contacts._ID, // 0
        NAME_COLUMN, // 1
        PRESENCE_STATUS, // 2
        LOOKUP_KEY, // 3
        VISIBILITY // 4
    };
	private static final int ID_COLUMN_INDEX = 0;
	private static final int NAME_COLUMN_INDEX = 1;
	private static final int PRESENCE_STATUS_COLUMN_INDEX = 2;
	private static final int LOOKUP_KEY_COLUMN_INDEX = 3;
	private static final int VISIBILITY_COLUMN_INDEX = 4;
	

	private static final Uri MY_CONTACTS = ContactsContract.Contacts.CONTENT_URI;
	
    private Context mContext;
    private ContentResolver mContentResolver;

	private Drawable mContactDefaultThumbnail;

	
	public ContactLauncher(Context context) {
		mContext = context;
		mContentResolver = context.getContentResolver();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		mContactDefaultThumbnail = ThumbnailFactory.createThumbnail(context, context.getResources().getDrawable(R.drawable.contact_launcher2));
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
        Cursor cursor00 =mContentResolver.query(MY_CONTACTS,
                CONTACTS_PROJECTION,
                "LOWER(" + ContactsContract.Contacts.DISPLAY_NAME + ") LIKE ?" ,
                new String[] { searchText+"%" },
                ContactsContract.Contacts.DISPLAY_NAME + " ASC");

		Cursor cursor0 = mContentResolver.query(MY_CONTACTS,
				CONTACTS_PROJECTION,
				"LOWER(" + ContactsContract.Contacts.DISPLAY_NAME + ") LIKE ?" ,
				new String[] { searchPattern },
				ContactsContract.Contacts.DISPLAY_NAME + " ASC");
		Cursor cursor1 =mContentResolver.query(MY_CONTACTS,
				CONTACTS_PROJECTION,
				"LOWER(" + ContactsContract.Contacts.DISPLAY_NAME + ") LIKE ?" ,
				new String[] { "%"+searchText+"%" },
				ContactsContract.Contacts.DISPLAY_NAME + " ASC");
		cursor = new MergeCursor(new Cursor[]{cursor00,cursor0,cursor1});



		ArrayList<Launchable> suggestions = new ArrayList<Launchable>();
 		if (cursor != null) {
 			if (cursor.getCount() > offset) {
 				cursor.moveToFirst();
 				cursor.move(offset);
 				int i = 0;
 				while (!cursor.isAfterLast() && i < limit) {
 					if ( cursor.getInt(VISIBILITY_COLUMN_INDEX) != 0) {
	 					ContactLaunchable contactLaunchable = new ContactLaunchable(this,
	 						cursor.getInt(ID_COLUMN_INDEX),
	 						cursor.getString(NAME_COLUMN_INDEX),
	 						cursor.getInt(PRESENCE_STATUS_COLUMN_INDEX),
	 						ContactsContract.Contacts.getLookupUri(cursor.getInt(ID_COLUMN_INDEX),
	 							cursor.getString(LOOKUP_KEY_COLUMN_INDEX)));
                        if ((contactLaunchable != null) && !suggestions.contains(contactLaunchable)) {
                            suggestions.add(contactLaunchable);
                        }

	 					i++;
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
		Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id);
		Cursor cursor = mContentResolver.query(uri, CONTACTS_PROJECTION, null, null, null);
		Launchable launchable = null;
		if(cursor != null) {
 			if(cursor.getCount() > 0) {
 				cursor.moveToFirst();
 				launchable = new ContactLaunchable(this,
					cursor.getInt(ID_COLUMN_INDEX),
					cursor.getString(NAME_COLUMN_INDEX),
					cursor.getInt(PRESENCE_STATUS_COLUMN_INDEX),
					ContactsContract.Contacts.getLookupUri(cursor.getInt(ID_COLUMN_INDEX),
 						cursor.getString(LOOKUP_KEY_COLUMN_INDEX)));
 			}
 			cursor.close();
		}
		return launchable;
	}
    
    @Override
	public boolean activate(Launchable launchable) {
    	if(launchable instanceof ContactLaunchable) {
    		Intent intent = new Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, launchable.getId()));
    		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
    		try {
            	mContext.startActivity(intent);
            } catch (Exception e) {
            	Toast.makeText(mContext, "Sorry: Cannot launch \"" + launchable.getLabel() + "\"", Toast.LENGTH_SHORT).show();
            	//Log.e(mContext.getResources().getString(R.string.appName), e.getMessage());
            	return false;
            }
			return true;
    	}
    	return false;
	}
    
    @Override
	public boolean activateBadge(Launchable launchable, View badgeParent) {
    	if((launchable instanceof ContactLaunchable) && (badgeParent != null)) {
    		ContactsContract.QuickContact.showQuickContact(mContext,
    			badgeParent,
    			((ContactLaunchable)launchable).getLookupUri(),
				QuickContact.MODE_MEDIUM,
				null);
			return true;
    	}
    	return false;
	}
    
   /* public Drawable getThumbnail(ContactLaunchable launchable) {

		    Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, launchable.getId());
		    InputStream contactPhoto = ContactsContract.Contacts.openContactPhotoInputStream(mContentResolver, uri);
		    if (contactPhoto == null) {
		         return mContactDefaultThumbnail;
		    }
		    return ThumbnailFactory.createThumbnail(mContext, BitmapFactory.decodeStream(contactPhoto));

	}*/

	public Drawable getThumbnail(ContactLaunchable launchable) {
			Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, launchable.getId());
			InputStream contactPhoto = ContactsContract.Contacts.openContactPhotoInputStream(mContentResolver, uri);
			if (contactPhoto == null) {
				return mContactDefaultThumbnail;
			}
			Drawable d = new BitmapDrawable(mContext.getResources(),contactPhoto);
			return ThumbnailFactory.createThumbnail(mContext, d);

/*		Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, launchable.getId());
		InputStream contactPhoto = ContactsContract.Contacts.openContactPhotoInputStream(mContentResolver, uri);
		if (contactPhoto == null) {
			return mContactDefaultThumbnail;
		}
		return ThumbnailFactory.createThumbnail(mContext, BitmapFactory.decodeStream(contactPhoto));*/
	}
    
    @Override
    public Intent getIntent(Launchable launchable) {
        Intent intent = new Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, launchable.getId()));
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        return intent;
    }
}