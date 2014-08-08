package com.aircandi.components;

import java.util.ArrayList;
import java.util.List;

import com.aircandi.Aircandi;
import com.aircandi.objects.Log.LogCategory;
import com.aircandi.utilities.Debug;

public class Stopwatch {

	private long   mTotalTime;
	private long   mLastThreshold;
	private String mName;
	private List<String> mLog = new ArrayList<String>();

	public long getTotalTime() {
		return mTotalTime;
	}

	public long getTotalTimeMills() {
		return mTotalTime / 1000000;
	}

	/**
	 * Returns last lap time, process statistic.
	 */
	public long segmentTime(String message) {
		return processSegmentTime(message);
	}

	private long processSegmentTime(String message) {
		if (mLastThreshold == 0) return 0;
		final long now = System.nanoTime();
		final long lapTime = now - mLastThreshold;
		mTotalTime += lapTime;
		mLastThreshold = System.nanoTime();
		String stats = "segment time: " + String.valueOf(lapTime / 1000000) + "ms, total time: " + String.valueOf(mTotalTime / 1000000) + "ms";
		if (message != null) {
			stats = mName + ": " + message + ": " + stats;
		}
		mLog.add(stats);
		return lapTime;
	}

	/**
	 * Starts time watching.
	 */
	public void start(String name, String message) {
		mName = name;
		mTotalTime = 0;
		mLastThreshold = System.nanoTime();
		mLog.add(mName + ": *** Started ***: " + message);
	}

	/**
	 * Suspends time watching, returns last lap time.
	 */
	public long stop(String message) {
		final long lapTime = processSegmentTime("*** Stopped ***: " + message);
		mLastThreshold = 0;
		if (Aircandi.DEBUG) {
			Debug.insertLog(LogCategory.TIMING, mName, NetworkManager.getInstance().getNetworkType(), getTotalTimeMills(), mLog);
		}
		Logger.v(this, "*** Timer log ***");
		for (String line : mLog) {
			Logger.v(this, line);
		}

		mLog.clear();
		return lapTime;
	}

	public boolean isStarted() {
		return mLastThreshold > 0;
	}

	public String getName() {
		return mName;
	}

	@SuppressWarnings("ucd")
	public Stopwatch setName(String name) {
		this.mName = name;
		return this;
	}
}