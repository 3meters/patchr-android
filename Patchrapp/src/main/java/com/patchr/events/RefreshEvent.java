package com.patchr.events;

import com.patchr.objects.enums.FetchMode;

@SuppressWarnings("ucd")
public class RefreshEvent {
	public final FetchMode  mode;

	public RefreshEvent(FetchMode mode) {
		this.mode = mode;
	}
}
