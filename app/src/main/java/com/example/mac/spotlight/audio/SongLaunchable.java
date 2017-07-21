package com.example.mac.spotlight.audio;


import android.graphics.drawable.Drawable;

import com.example.mac.spotlight.Launchable;
import com.example.mac.spotlight.Launcher;

public class SongLaunchable extends Launchable {
	private final SongLauncher mSongLauncher;
	
	public SongLaunchable(Launcher launcher, int id, String label, String infoText) {
		super(launcher, id, label, infoText);
		mSongLauncher = (SongLauncher) launcher;
	}
	
	@Override
	public Drawable getThumbnail() {
		return mSongLauncher.getThumbnail(this);
	}
	
	public int hashCode() {
		return super.hashCode();
	}
	
	public boolean equals(Object o) {
		return super.equals(o);
	}
}
