package com.example.mac.spotlight;

import android.os.Handler;
import android.os.Message;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Searcher extends Handler {
	public static final int MIN_CORE_POOL_SIZE = 1;
    private static final int MAX_POOL_SIZE = 6;
    private static final int MAX_QUEUE_SIZE = 14;
    private static final int KEEP_ALIVE = 10;

    private static final AtomicInteger sNumSearchers = new AtomicInteger(0);
    
    private static final BlockingQueue<Runnable> sWorkQueue =
    	new ArrayBlockingQueue<Runnable>(MAX_QUEUE_SIZE);

    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable runnable) {
        	Thread thread = new Thread(runnable, "Searcher #" + mCount.getAndIncrement());
        	return thread;
        }
    };

    private static final ThreadPoolExecutor sExecutor = new ThreadPoolExecutor(MIN_CORE_POOL_SIZE,
    	MAX_POOL_SIZE, KEEP_ALIVE, TimeUnit.SECONDS, sWorkQueue, sThreadFactory, new ThreadPoolExecutor.DiscardOldestPolicy());

	private static final int MAX_SUGGESTIONS_PER_QUERY = 8;
    private static final int MSG_PUBLISH_SUGGESTIONS = 1;

	private final Launcher mLauncher;
	private final SearchResultComposer mSearchResultComposer;
	private AsyncSearcher mAsyncSearcher;
	private String mSearchText;
	private int mNumSuggestions;
	private int mOffset;
	
	private class AsyncSearcher implements Runnable {
		private SearchResult mSearchResult;
		private String mSearchText;

		private int mOffset;
		private int mLimit;

		public AsyncSearcher(String searchText, int offset, int limit) {
			mSearchResult = new SearchResult();
			mSearchText = searchText;

			mOffset = offset;
			mLimit = limit;
			mSearchResult.searchText = mSearchText;

		}
		
		@Override
		public void run() {
			mSearchResult.suggestions = mLauncher.getSuggestions(mSearchText, mOffset, mLimit);
			Message msg = obtainMessage();
			msg.what = MSG_PUBLISH_SUGGESTIONS;
			msg.obj = mSearchResult;
			msg.sendToTarget();
		}
		
		public void cancel() {
			mSearchResult.searchText = null;
		}
	}
	
	private class SearchResult {
		public String searchText;
        public ArrayList<Launchable> suggestions;
    }
		
	public Searcher(Launcher launcher, SearchResultComposer searchResultComposer) {
		int numSearchers = sNumSearchers.incrementAndGet();
		setThreadPoolSize(numSearchers + 1);
		mLauncher = launcher;
		mSearchResultComposer = searchResultComposer;
	}
	
	public void destroy() {
		int numSearchers = sNumSearchers.decrementAndGet();
		setThreadPoolSize(numSearchers + 1);
		// post an empty runnable to the thread pool in order to shrink its size immediately
		sExecutor.execute(new Runnable() {
			@Override
			public void run() {
			}
		});
	}
	
	public void search(final String searchText) {
		if (mAsyncSearcher != null) {
			sExecutor.remove(mAsyncSearcher);
			mAsyncSearcher.cancel();
			mAsyncSearcher = null;
		}
		mSearchText = searchText;
		mNumSuggestions = 0;
		mOffset = 0;
		doSearch();
	}
	
	public void cancel() {
		mSearchText = null;
		if (mAsyncSearcher != null) {
			sExecutor.remove(mAsyncSearcher);
			mAsyncSearcher.cancel();
			mAsyncSearcher = null;
		}
	}
	
	protected void doSearch() {
		mAsyncSearcher = new AsyncSearcher(mSearchText, mOffset, mLauncher.getMaxSuggestions() - mNumSuggestions);
        sExecutor.execute(mAsyncSearcher);
	}

	@Override
    public void handleMessage(Message msg) {
		SearchResult searchResult = (SearchResult) msg.obj;
        int event = msg.what;
		if (event == MSG_PUBLISH_SUGGESTIONS) {
        	if (searchResult.searchText != null) {
        		mSearchResultComposer.addSuggestions(mLauncher, searchResult.suggestions);
        		int numSuggestions = (searchResult.suggestions != null) ? searchResult.suggestions.size() : 0;
        		mNumSuggestions += numSuggestions;

        			mOffset += numSuggestions;


        			mSearchResultComposer.onDone(this);

        	}
        }
	}
	
	private static void setThreadPoolSize(int size) {
		int corePoolSize = (size < MIN_CORE_POOL_SIZE) ? MIN_CORE_POOL_SIZE : size;
		corePoolSize = (corePoolSize > MAX_POOL_SIZE) ? MAX_POOL_SIZE : corePoolSize;
		sExecutor.setCorePoolSize(corePoolSize);
	}
}