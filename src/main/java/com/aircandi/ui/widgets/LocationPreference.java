package com.aircandi.ui.widgets;

import android.annotation.SuppressLint;
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
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.aircandi.Aircandi;
import com.aircandi.R;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ModelResult;
import com.aircandi.objects.Document;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Json;
import com.aircandi.utilities.UI;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class LocationPreference extends AirListPreference {

	private List<Document> mTestLocations = new ArrayList<Document>();
	private       ListAdapter mAdapter;
	private       Integer     mCurrentIndex;
	public        Document    mLocationDefault;
	public static String      LOCATION_DEFAULT;

	@SuppressWarnings("ucd")
	public LocationPreference(Context context) {
		this(context, null);
	}

	public LocationPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setDefaultLocation();
		setDefaultValue(LOCATION_DEFAULT);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/
	@Override
	protected void onPrepareDialogBuilder(@NonNull Builder builder) {
		setEntriesFromLocations(); // Initialize entry arrays but empty
		builder.setPositiveButton(null, null);
		super.onPrepareDialogBuilder(builder);
		databind();
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);

		if (positiveResult && mCurrentIndex >= 0 && mTestLocations != null) {
			Document location = mTestLocations.get(mCurrentIndex);
			String jsonLocation = Json.objectToJson(location);

			if (callChangeListener(jsonLocation)) {
				setValue(jsonLocation);
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/
	private void databind() {

		mTestLocations.clear();

		new AsyncTask() {

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncLoadTestingLocations");
				ModelResult result = new ModelResult();
				try {
					String filter = "query=" + URLEncoder.encode("{\"type\":\"location\"}", "utf-8") + "&sort[name]=1";
					result = Aircandi.getInstance().getEntityManager().loadDocuments(filter);
				}
				catch (UnsupportedEncodingException exception) {
					exception.printStackTrace();
					result.serviceResponse.responseCode = ResponseCode.FAILED;
				}
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {

					List<Document> locations = (List<Document>) result.data;
					mTestLocations.add(mLocationDefault);

					for (Document location : locations) {
						mTestLocations.add(location);
					}

					setEntriesFromLocations();
					mCurrentIndex = findIndexOfValue(getSharedPreferences().getString(getKey(), LOCATION_DEFAULT));
					AlertDialog dialog = (AlertDialog) getDialog();
					mAdapter = new ListAdapter(getContext(), mTestLocations);
					dialog.getListView().setAdapter(mAdapter);
				}
			}

		}.execute();

	}

	private void deleteLocation(final Document location) {
		new AsyncTask() {

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncDeleteTestingLocation");
				ModelResult result = Aircandi.getInstance().getEntityManager().deleteDocument(location);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					UI.showToastNotification("Location deleted: " + location.name, Toast.LENGTH_SHORT);
					mTestLocations.remove(location);
					mAdapter.notifyDataSetChanged();
				}
			}

		}.execute();
	}

	public void setEntriesFromLocations() {
		CharSequence[] entries = new CharSequence[mTestLocations.size()];
		CharSequence[] entryValues = new CharSequence[mTestLocations.size()];
		int i = 0;
		for (Document location : mTestLocations) {
			entries[i] = location.name;
			String jsonLocation = Json.objectToJson(location);
			entryValues[i] = jsonLocation;
			i++;
		}
		setEntries(entries);
		setEntryValues(entryValues);
	}

	private void setDefaultLocation() {
		Document location = new Document();
		location.name = "Natural";
		String jsonLocation = Json.objectToJson(location);
		mLocationDefault = location;
		LOCATION_DEFAULT = jsonLocation;
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/    private class ListAdapter extends ArrayAdapter<Document> {

		@Override
		public int getCount() {
			return mTestLocations.size();
		}

		private ListAdapter(Context context, List<Document> items) {
			super(context, 0, items);
		}

		@SuppressLint("WrongViewCast")
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			final Document itemData = (Document) mTestLocations.get(position);

			if (view == null) {
				view = LayoutInflater.from(getContext()).inflate(R.layout.temp_listitem_location_preference, null);
			}

			if (itemData != null) {
				((TextView) view.findViewById(R.id.name)).setText(itemData.name);

				UI.setVisibility(view.findViewById(R.id.subtitle), View.GONE);
				if (itemData.modifiedDate != null) {
					String date = DateTime.dateString(itemData.modifiedDate.longValue(), DateTime.DATE_FORMAT_DEFAULT);
					((TextView) view.findViewById(R.id.subtitle)).setText(date);
					UI.setVisibility(view.findViewById(R.id.subtitle), View.VISIBLE);
				}
				((RadioButton) view.findViewById(R.id.checked)).setChecked(position == mCurrentIndex);
				((RadioButton) view.findViewById(R.id.checked)).setClickable(false);

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
							Document location = (Document) v.getTag();
							deleteLocation(location);
						}
					});
					UI.setVisibility(imageDelete, View.VISIBLE);
				}

				view.setTag(itemData);
				view.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						Document location = (Document) v.getTag();
						mCurrentIndex = mTestLocations.indexOf(location);
						LocationPreference.this.onClick(getDialog(), DialogInterface.BUTTON_POSITIVE);
						getDialog().dismiss();
					}
				});
			}
			return view;
		}
	}
}
