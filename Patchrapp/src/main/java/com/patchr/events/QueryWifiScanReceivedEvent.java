package com.patchr.events;

import com.patchr.components.ProximityController.WifiScanResult;

import java.util.List;

@SuppressWarnings("ucd")
public class QueryWifiScanReceivedEvent {
	public final List<WifiScanResult> wifiList;

	public QueryWifiScanReceivedEvent(List<WifiScanResult> wifiList) {
		this.wifiList = wifiList;
	}
}
