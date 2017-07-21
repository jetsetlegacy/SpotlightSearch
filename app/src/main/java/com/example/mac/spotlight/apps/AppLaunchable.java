package com.example.mac.spotlight.apps;

import android.content.Intent;
import android.graphics.drawable.Drawable;

import com.example.mac.spotlight.Launchable;
import com.example.mac.spotlight.Launcher;

public class AppLaunchable extends Launchable {
	private final Intent mIntent;
	private final Drawable mThumbnail;
	
	public AppLaunchable(Launcher launcher, int id, String name, Intent intent, Drawable thumbnail) {
		super(launcher, id, name);
		mIntent = intent;
		mThumbnail = thumbnail;
	}
	
	@Override
	public Drawable getThumbnail() {
		return mThumbnail;
	}
	
	public Intent getIntent() {
		return mIntent;
	}
	
	public int hashCode() {
		return super.hashCode();
	}
	
	public boolean equals(Object o) {
		return super.equals(o);
	}
}