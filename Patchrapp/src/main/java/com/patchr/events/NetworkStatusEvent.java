package com.patchr.events;

import com.patchr.objects.enums.NetworkStatus;

@SuppressWarnings("ucd")
public class NetworkStatusEvent {
	public final NetworkStatus status;

	public NetworkStatusEvent(NetworkStatus status) {
		this.status = status;
	}
}
