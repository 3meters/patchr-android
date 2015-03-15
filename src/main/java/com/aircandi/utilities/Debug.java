package com.aircandi.utilities;

import android.os.AsyncTask;
import android.widget.Toast;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.components.DataController;
import com.aircandi.components.ModelResult;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.objects.Beacon;
import com.aircandi.objects.Document;
import com.aircandi.objects.Patch;

import java.util.HashMap;

public class Debug {

	public static void insertBeacon() {

		final Document document = new Document();
		if (Patchr.getInstance().getCurrentPatch() != null) {

			Patch place = (Patch) Patchr.getInstance().getCurrentPatch();
			Beacon beacon = place.getBeaconFromLink(Constants.TYPE_LINK_PROXIMITY, true);

			if (beacon != null) {

				document.name = place.name;
				document.type = "beacon";
				document.data = new HashMap<String, Object>();
				document.data.put("bssid", beacon.bssid);
				document.data.put("ssid", beacon.ssid);

				new AsyncTask() {

					@Override
					protected Object doInBackground(Object... params) {
						Thread.currentThread().setName("AsyncInsertReport");
						ModelResult result = DataController.getInstance().insertDocument(document, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
						return result;
					}

					@Override
					protected void onPostExecute(Object response) {
						final ModelResult result = (ModelResult) response;

						if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
							UI.showToastNotification("Beacon saved", Toast.LENGTH_SHORT);
						}
					}
				}.executeOnExecutor(Constants.EXECUTOR);
			}
			else {
				UI.showToastNotification("The patch doesn't have any beacons", Toast.LENGTH_SHORT);
			}
		}
	}
}