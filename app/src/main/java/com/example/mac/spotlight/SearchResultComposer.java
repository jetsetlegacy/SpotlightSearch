package com.example.mac.spotlight;

import android.database.ContentObserver;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;

public class SearchResultComposer extends BaseAdapter {
	private final MainActivity mMainActivity;
	private final LayoutInflater mLayoutInflater;
	private final ArrayList<Searcher> mSearchers;
	private final ArrayList<Launcher> mLaunchers;
	private final int mNumLaunchers;
	private final HashMap<Integer, Integer> mLauncherIndexes;
	private final LauncherObserver mLauncherObserver;
	private LinkedList<Launchable> mSuggestions;
	private ArrayList<Launchable> mViewableSuggestions;
	private int[] mSearchResultInsertPositions;
	private String mSearchText = null;
	private boolean mClearSuggestions = false;
	private int mNumDoneSearchers = 0;
	private Launcher mFavoriteItemsLauncher;
	
	public SearchResultComposer(MainActivity mainActivity) {
		mMainActivity = mainActivity;
		mLayoutInflater = LayoutInflater.from(mMainActivity);
		mLaunchers = mMainActivity.getLaunchers();
		mNumLaunchers = mLaunchers.size();
		mLauncherIndexes = new HashMap<Integer, Integer>(mNumLaunchers);
		mLauncherObserver = new LauncherObserver(new Handler());
		mSearchers = new ArrayList<Searcher>();
		for (int i = 0; i < mNumLaunchers; i++) {
			mLauncherIndexes.put(mLaunchers.get(i).getId(), i);
			mLaunchers.get(i).registerContentObserver(mLauncherObserver);
			mSearchers.add(new Searcher(mLaunchers.get(i), this));
		}
		mFavoriteItemsLauncher = mMainActivity.getFavoriteItemsLauncher();
		mSuggestions = new LinkedList<Launchable>();
		mViewableSuggestions = new ArrayList<Launchable>();
		mSearchResultInsertPositions = new int[mNumLaunchers * 4];
	}
	
	public void onDestroy() {
		for (int i = 0; i < mNumLaunchers; i++) {
			mLaunchers.get(i).unregisterContentObserver(mLauncherObserver);
			mSearchers.get(i).cancel();
			mSearchers.get(i).destroy();
		}
	}
	
	public int getCount() {
		return mViewableSuggestions.size();
	}
	
	public Object getItem(int position) {
		if (position < mViewableSuggestions.size()) {
			return mViewableSuggestions.get(position);
		} else {
			return null;
		}
	}
	
	public long getItemId(int position) {
		if (position < mViewableSuggestions.size()) {
			return mViewableSuggestions.get(position).getId();
		} else {
			return -1;
		}
	}
	
	private class ViewHolder {
		ImageView mThumbnail;
		TextView mLabel;
		TextView mInfoText;
    }
	
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
        Launchable launchable = mViewableSuggestions.get(position);
        viewHolder.mThumbnail.setImageDrawable(mMainActivity.getResources().getDrawable(R.drawable.app_thumbnail));
        if(launchable.getThumbnail() != null) {
        	viewHolder.mThumbnail.setImageDrawable(launchable.getThumbnail().getCurrent());
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

	public final void search(final String searchText) {
		resetSearchResultPositions();
		mSearchText = searchText;
		if (mSearchText != null && mSearchText.trim().length() > 0) {
			mSearchText = mSearchText.trim();
			mMainActivity.setProgressBarIndeterminateVisibility(true);
			mClearSuggestions = true;
			mNumDoneSearchers = 0;
			for (int i = 0; i < mNumLaunchers; i++) {
				mSearchers.get(i).search(mSearchText.toLowerCase());
			}			
		} else {
			mSuggestions.clear();
			mViewableSuggestions.clear();
			notifyDataSetChanged();
			mMainActivity.setProgressBarIndeterminateVisibility(false);
		}
	}
	
	public void addSuggestions(Launcher launcher, ArrayList<Launchable> suggestions) {
		if (suggestions != null) {
			if (mClearSuggestions) {
				mClearSuggestions = false;
				mSuggestions.clear();
			}

			Integer launcherIndex = mLauncherIndexes.get(launcher.getId());
			
			if (mFavoriteItemsLauncher != null) {
				if (launcher != mFavoriteItemsLauncher) {
					// Do not add new suggestions if they are already available as favorite items
					for (Launchable launchable : mSuggestions) {
						int i = 0;
						for (i = 0; i < suggestions.size(); i++) {						
							if ((launchable != null) && (launchable.equals(suggestions.get(i)))) {
								break;
							}					
						}
						if (i < suggestions.size()) {
							suggestions.remove(i);
						}
					}
				} else {
					// Do not add duplicate favorite items
				    int nextFavoriteItemPos = 0;

		                nextFavoriteItemPos += mSearchResultInsertPositions[mNumLaunchers - 1];

		            nextFavoriteItemPos += mSearchResultInsertPositions[  mNumLaunchers + launcherIndex];

					for (int i = 0; i < nextFavoriteItemPos; i++) {
						Launchable launchable = mSuggestions.get(i);
						int j = 0;
						for (j = 0; j < suggestions.size(); j++) {
							if ((launchable != null) && (launchable.equals(suggestions.get(j)))) {
								break;
							}
						}
						if (j < suggestions.size()) {
							suggestions.remove(j);
						}
					}

					// Remove suggestions in favor of equipollent highly ranked favorite items
					for (Launchable launchable : suggestions) {
						int i = 0;
						for (i = 0; i < mSuggestions.size(); i++) {
							if ((mSuggestions.get(i) != null) && (launchable.equals(mSuggestions.get(i)))) {
								break;
							}
						}
						if (i < mSuggestions.size()) {
							mSuggestions.set(i, null);
						}
					}
				}
			}

			// The mSearchResultInsertPositions array holds the insert positions for new suggestions -> sort order: pattern matching level, launcher ID
			int insertPosition = 0;

				insertPosition += mSearchResultInsertPositions[ mNumLaunchers - 1];

			insertPosition += mSearchResultInsertPositions[mNumLaunchers + launcherIndex];

			mSuggestions.addAll(insertPosition, suggestions);
			int pos =  mNumLaunchers + launcherIndex;
			for (int i = pos; i < pos + mNumLaunchers - launcherIndex; i++) {
				mSearchResultInsertPositions[i] += suggestions.size();
			}

			mViewableSuggestions.clear();
			ListIterator<Launchable> itr = mSuggestions.listIterator();
			while (itr.hasNext()) {
				Launchable launchable = itr.next();
				if (launchable != null) {
					mViewableSuggestions.add(launchable);
				}
			}
			if (mMainActivity.getListView().getItemAtPosition(0) != null) {
				mMainActivity.getListView().setSelection(0);
			}
			notifyDataSetChanged();
		}			
   	}
	
	public void onDone(Searcher searcher) {
		mNumDoneSearchers++;
		if (mNumDoneSearchers >= mSearchers.size()) {
			if (mClearSuggestions) {
				mClearSuggestions = false;
				mSuggestions.clear();
				mViewableSuggestions.clear();
				notifyDataSetChanged();
			}
			mMainActivity.setProgressBarIndeterminateVisibility(false);
	//		mMainActivity.updateSearchTextColor();
		}
	}
	
	public boolean hasSuggestions() {
		return (!mViewableSuggestions.isEmpty());
	}
	
	private final void resetSearchResultPositions() {
		Arrays.fill(mSearchResultInsertPositions, 0);
	}
	
	private class LauncherObserver extends ContentObserver {
		public LauncherObserver(Handler handler) {
			super(handler);
		}
		
		@Override
		public void onChange(boolean selfChange) {
			if (mSearchText != null && mSearchText.length() > 0) {
				search(mSearchText);				
			}
		}
	}
}