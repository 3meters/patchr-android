package com.aircandi.monitors;

import com.aircandi.interfaces.IMonitor;
import com.aircandi.objects.CacheStamp;

public abstract class SimpleMonitor implements IMonitor {

	protected CacheStamp mCacheStamp;
	protected Boolean mFirstRun = true;
	public  Boolean activity  = false;
	public  Boolean modified  = false;
	public  Boolean changed   = false;

	@Override
	public Boolean isChanged() {
		return false;
	}
}
