package com.aircandi.events;

import com.aircandi.components.ProximityManager.WifiScanResult;

import java.util.List;

@SuppressWarnings("ucd")
public class MonitoringWifiScanReceivedEvent {
	public final List<WifiScanResult> wifiList;

	public MonitoringWifiScanReceivedEvent(List<WifiScanResult> wifiList) {
		this.wifiList = wifiList;
	}
}
