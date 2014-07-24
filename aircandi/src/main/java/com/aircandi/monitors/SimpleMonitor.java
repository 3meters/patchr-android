package com.aircandi.monitors;

import com.aircandi.objects.CacheStamp;

public abstract class SimpleMonitor implements IMonitor {

	protected CacheStamp	mCacheStamp;
	public Boolean			activity	= false;
	public Boolean			modified	= false;
	public Boolean			changed		= false;

	@Override
	public Boolean isChanged() {
		return false;
	}
}
