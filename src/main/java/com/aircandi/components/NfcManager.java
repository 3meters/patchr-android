package com.aircandi.components;

import android.app.Activity;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;

import com.aircandi.Patchr;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("ucd")
public class NfcManager {

	public static void pushUri(Uri uri, Activity activity) {
		NfcAdapter nfc = NfcAdapter.getDefaultAdapter(Patchr.applicationContext);

		if (nfc != null) {

			/* Create an NDEF message containing the Uri */
			NdefRecord rec = NdefRecord.createUri(uri);
			NdefRecord[] records = new NdefRecord[]{rec};
			NdefMessage ndef = new NdefMessage(records);

			/* Make it available via Android Beam */
			nfc.setNdefPushMessage(ndef, activity);
		}
	}
}
