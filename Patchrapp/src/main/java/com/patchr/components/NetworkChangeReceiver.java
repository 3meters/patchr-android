package com.patchr.components;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.patchr.events.NetworkStatusEvent;
import com.patchr.objects.enums.NetworkStatus;

public class NetworkChangeReceiver extends BroadcastReceiver {
	private boolean connected;

	@Override public void onReceive(Context context, Intent intent) {

		if (intent == null || intent.getExtras() == null) return;

		boolean prev = connected;
		connected = NetworkManager.getInstance().isConnected();
		if (prev != connected) {
			Dispatcher.getInstance().post(new NetworkStatusEvent(connected ? NetworkStatus.CONNECTED : NetworkStatus.NOTCONNECTED));
		}
	}
}
