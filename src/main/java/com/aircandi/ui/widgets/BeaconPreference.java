package com.aircandi.ui.widgets;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.aircandi.Patch;
import com.aircandi.R;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ModelResult;
import com.aircandi.objects.Document;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Json;
import com.aircandi.utilities.Reporting;
import com.aircandi.utilities.UI;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class BeaconPreference extends AirListPreference {

	private List<Document> mTestBeacons = new ArrayList<Document>();
	private       ListAdapter mAdapter;
	private       Integer     mCurrentIndex;
	public        Document    mBeaconDefault;
	public static String      BEACON_DEFAULT;

	@SuppressWarnings("ucd")
	public BeaconPreference(Context context) {
		this(context, null);
	}

	public BeaconPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setDefaultBeacon();
		setDefaultValue(BEACON_DEFAULT);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected void onPrepareDialogBuilder(@NonNull Builder builder) {
		setEntriesFromBeacons(); // Initialize entry arrays but empty
		builder.setPositiveButton(null, null);
		super.onPrepareDialogBuilder(builder);
		databind();
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);

		if (positiveResult && mCurrentIndex >= 0 && mTestBeacons != null) {
			Document location = mTestBeacons.get(mCurrentIndex);
			String jsonBeacon = Json.objectToJson(location);

			if (callChangeListener(jsonBeacon)) {
				setValue(jsonBeacon);
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	private void databind() {

		mTestBeacons.clear();

		new AsyncTask() {

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncLoadTestingBeacons");
				ModelResult result = new ModelResult();
				try {
					String filter = "query=" + URLEncoder.encode("{\"type\":\"beacon\"}", "utf-8") + "&sort[name]=1";
					result = Patch.getInstance().getEntityManager().loadDocuments(filter);
				}
				catch (UnsupportedEncodingException e) {
					Reporting.logException(e);
					result.serviceResponse.responseCode = ResponseCode.FAILED;
				}
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {

					List<Document> beacons = (List<Document>) result.data;
					mTestBeacons.add(mBeaconDefault);

					for (Document beacon : beacons) {
						mTestBeacons.add(beacon);
					}

					setEntriesFromBeacons();
					mCurrentIndex = findIndexOfValue(getSharedPreferences().getString(getKey(), BEACON_DEFAULT));
					AlertDialog dialog = (AlertDialog) getDialog();
					mAdapter = new ListAdapter(getContext(), mTestBeacons);
					dialog.getListView().setAdapter(mAdapter);
				}
			}

		}.execute();

	}

	private void deleteBeacon(final Document beacon) {
		new AsyncTask() {

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncDeleteTestingBeacon");
				ModelResult result = Patch.getInstance().getEntityManager().deleteDocument(beacon);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					UI.showToastNotification("Beacon deleted: " + beacon.name, Toast.LENGTH_SHORT);
					mTestBeacons.remove(beacon);
					mAdapter.notifyDataSetChanged();
				}
			}

		}.execute();
	}

	public void setEntriesFromBeacons() {

		CharSequence[] entries = new CharSequence[mTestBeacons.size()];
		CharSequence[] entryValues = new CharSequence[mTestBeacons.size()];
		int i = 0;
		for (Document beacon : mTestBeacons) {
			entries[i] = beacon.name;
			String jsonBeacon = Json.objectToJson(beacon);
			entryValues[i] = jsonBeacon;
			i++;
		}
		setEntries(entries);
		setEntryValues(entryValues);
	}

	private void setDefaultBeacon() {
		Document beacon = new Document();
		beacon.name = "Natural";
		String jsonBeacon = Json.objectToJson(beacon);
		mBeaconDefault = beacon;
		BEACON_DEFAULT = jsonBeacon;
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/    private class ListAdapter extends ArrayAdapter<Document> {

		@Override
		public int getCount() {
			return mTestBeacons.size();
		}

		private ListAdapter(Context context, List<Document> items) {
			super(context, 0, items);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			final Document itemData = (Document) mTestBeacons.get(position);

			if (view == null) {
				view = LayoutInflater.from(getContext()).inflate(R.layout.temp_listitem_beacon_preference, null);
			}

			if (itemData != null) {
				((TextView) view.findViewById(R.id.name)).setText(itemData.name);

				UI.setVisibility(view.findViewById(R.id.subtitle), View.GONE);
				if (itemData.modifiedDate != null) {
					String date = DateTime.dateString(itemData.modifiedDate.longValue(), DateTime.DATE_FORMAT_DEFAULT);
					((TextView) view.findViewById(R.id.subtitle)).setText(date);
					UI.setVisibility(view.findViewById(R.id.subtitle), View.VISIBLE);
				}
				((CheckBox) view.findViewById(R.id.checked)).setChecked(position == mCurrentIndex);
				((CheckBox) view.findViewById(R.id.checked)).setClickable(false);

				View imageDelete = view.findViewById(R.id.image_delete);
				if (position == 0) {
					imageDelete.setTag(null);
					UI.setVisibility(imageDelete, View.INVISIBLE);
				}
				else {
					imageDelete.setTag(itemData);
					imageDelete.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View v) {
							Document beacon = (Document) v.getTag();
							deleteBeacon(beacon);
						}
					});
					UI.setVisibility(imageDelete, View.VISIBLE);
				}

				view.setTag(itemData);
				view.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						Document beacon = (Document) v.getTag();
						mCurrentIndex = mTestBeacons.indexOf(beacon);
						BeaconPreference.this.onClick(getDialog(), DialogInterface.BUTTON_POSITIVE);
						getDialog().dismiss();
					}
				});
			}
			return view;
		}
	}
}