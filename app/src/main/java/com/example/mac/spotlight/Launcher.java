package com.example.mac.spotlight;

import android.content.Intent;
import android.database.ContentObserver;
import android.view.View;

import java.util.ArrayList;

public abstract class Launcher {
	private int mId;
	private int mMaxSuggestions;

	
	public Launcher() {
		mId = getName().hashCode();
		mMaxSuggestions = 10;

	}
	
	public abstract String getName();

	public abstract ArrayList<Launchable> getSuggestions(String intentFilter, int offset, int limit);
	
	public abstract Launchable getLaunchable(int id);
	
	public boolean activate(Launchable launchable) {
		return false;
	}
	
	public boolean activateBadge(Launchable launchable, View badgeParent) {
		return activate(launchable);
	}
	
	public void deactivate(Launchable launchable) {
	}
	
	public void deactivateBadge(Launchable launchable, View badgeParent) {
		deactivate(launchable);
	}
	
	public int getId() {
		return mId;
	}
	
	public boolean registerContentObserver(ContentObserver observer) {
		return false;
	}
	
	public boolean unregisterContentObserver(ContentObserver observer) {
		return false;
	}
	
	public void setMaxSuggestions(int numSuggestions) {
		mMaxSuggestions = numSuggestions;
	}
	
	public int getMaxSuggestions() {
		return mMaxSuggestions;
	}
	

	
	public abstract Intent getIntent(Launchable launchable);
}