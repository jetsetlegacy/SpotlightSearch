package com.example.mac.spotlight.contacts;


import android.graphics.drawable.Drawable;
import android.net.Uri;

import com.example.mac.spotlight.Launchable;
import com.example.mac.spotlight.Launcher;

public class ContactLaunchable extends Launchable {
	private final ContactLauncher mContactLauncher;
	private int mPresenceStatus;
	private Uri mLookupUri;
	
	public ContactLaunchable(Launcher launcher, int id, String label, int presenceStatus, Uri lookupUri) {
		super(launcher, id, label);
		mPresenceStatus = presenceStatus;
		mLookupUri = lookupUri;
		mContactLauncher = (ContactLauncher) launcher;
	}

	@Override
	public Drawable getThumbnail() {
		return mContactLauncher.getThumbnail(this);
	}
	
	public int getPresenceStatus() {
		return mPresenceStatus;
	}
	
	public Uri getLookupUri() {
		return mLookupUri;
	}
	
	public int hashCode() {
		return super.hashCode();
	}
	
	public boolean equals(Object o) {
		return super.equals(o);
	}
}
