package com.aircandi.monitors;

import com.aircandi.objects.CacheStamp;

@SuppressWarnings("ucd")
public class TrendMonitor extends SimpleMonitor {
	private Boolean mFirstRun = true;

	@Override
	public Boolean isChanged() {
		if (mFirstRun) {
			mFirstRun = false;
			mCacheStamp = new CacheStamp();
			this.activity = true;
			this.modified = true;
			return true;
		}
		return false;
	}
}
