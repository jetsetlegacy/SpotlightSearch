package com.example.mac.spotlight;


import android.graphics.drawable.Drawable;
import android.view.View;

public class Launchable {
	private Launcher mLauncher;
	private int mId;
	private String mLabel;
	private String mInfoText;
	private View mBadgeParent;

	public Launchable(Launcher launcher, int id, String label) {
		mLauncher = launcher;
		mId = id;
		mLabel = label;
	}
	
	public Launchable(Launcher launcher, int id, String label, String infoText) {
		mLauncher = launcher;
		mId = id;
		mLabel = label;
		mInfoText = infoText;
	}
	
	public Launcher getLauncher() {
		return mLauncher;
	}
	
	public int getId() {
		return mId;
	}
	
	public String getLabel() {
		return mLabel;
	}
	
	public String getInfoText() {
		return mInfoText;
	}
	
	public Drawable getThumbnail() {
		return null;
	}
	
	public boolean activate() {
		return mLauncher.activate(this);
	}
	


	public void setBadgeParent(View badgeParent) {
		mBadgeParent = badgeParent;
	}
	
	public int hashCode() {
		int hashCode = 17;
		hashCode = hashCode * 31 + mId;
		hashCode = hashCode * 31 + (mLauncher != null ? mLauncher.getId() : 0);
		return hashCode;
	}
	
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Launchable launchable = (Launchable) o;
		if (mLauncher != null ? mLauncher.getId() != launchable.getLauncher().getId() : launchable.getLauncher() != null) return false;
		if (mId != launchable.mId) return false;
		return true;
	}
}