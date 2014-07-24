package com.aircandi.ui.components;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.json.JSONException;

import android.content.Context;
import android.os.AsyncTask;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.aircandi.Aircandi;
import com.aircandi.R;
import com.aircandi.components.LocationManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.StringManager;
import com.aircandi.objects.AirLocation;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Photo;
import com.aircandi.objects.Place;
import com.aircandi.objects.Place.ReasonType;
import com.aircandi.service.RequestListener;
import com.aircandi.ui.base.BaseActivity.SimpleTextWatcher;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.utilities.Json;
import com.aircandi.utilities.UI;

public class PlaceSuggestController implements OnClickListener {

	private static Integer		LIMIT				= 10;

	private ArrayAdapter		mAdapter;
	private Context				mContext;
	private AbsListView			mListView;
	private EditText			mInput;
	private List<Entity>		mEntitiesInjected	= new ArrayList<Entity>();
	private List<Entity>		mEntities			= new ArrayList<Entity>();
	private ProgressBar			mProgress;
	private RequestListener		mListener;
	private Integer				mWatchResId;
	private Integer				mLocationResId;
	private SimpleTextWatcher	mTextWatcher;
	private String				mSuggestInput;
	private Boolean				mSuggestInProgress	= false;

	public PlaceSuggestController(Context context, RequestListener listener) {
		mContext = context;
		mListener = listener;

		mAdapter = new ListAdapter(mEntities);

		final TypedValue resourceName = new TypedValue();
		if (context.getTheme().resolveAttribute(R.attr.iconWatch, resourceName, true)) {
			mWatchResId = resourceName.resourceId;
		}
		if (context.getTheme().resolveAttribute(R.attr.iconLocation, resourceName, true)) {
			mLocationResId = resourceName.resourceId;
		}
	}

	public void init() {

		/* Load any injected suggestions */
		for (Entity entity : mEntitiesInjected) {
			mEntities.add(entity);
		}

		/* Bind to adapter */
		((ListView) mListView).setAdapter(mAdapter);

		/* Bind input */
		mTextWatcher = new TextWatcher();
		mInput.addTextChangedListener(mTextWatcher);

	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@Override
	public void onClick(View v) {

		final Entity entity = (Entity) ((ViewHolder) v.getTag()).data;

		/* Add place to auto complete array */
		try {
			org.json.JSONObject jsonSearchMap = new org.json.JSONObject(Aircandi.settings.getString(
					StringManager.getString(R.string.setting_place_searches), "{}"));
			final String jsonEntity = Json.objectToJson(entity);

			jsonSearchMap.put(entity.id, jsonEntity);
			Aircandi.settingsEditor.putString(StringManager.getString(R.string.setting_place_searches), jsonSearchMap.toString());
			Aircandi.settingsEditor.commit();
		}
		catch (JSONException exception) {
			exception.printStackTrace();
		}

		if (mListener != null) {
			mListener.onComplete(entity);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	public void suggest(final String input) {

		mSuggestInProgress = true;

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				if (mProgress != null) {
					mProgress.setVisibility(View.VISIBLE);
				}
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncSuggestPlaces");
				final AirLocation location = LocationManager.getInstance().getAirLocationLocked();
				ModelResult result = Aircandi.getInstance().getEntityManager()
						.suggestPlaces(input.toString().trim(), Aircandi.getInstance().getCurrentUser().id, location, LIMIT);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {

					if (mProgress != null) {
						mProgress.setVisibility(View.GONE);
					}

					final List<Entity> places = (List<Entity>) result.data;
					mAdapter.setNotifyOnChange(false);
					mAdapter.clear();

					for (Entity place : places) {
						mAdapter.add(place);
					}

					if (mEntitiesInjected != null && mEntitiesInjected.size() > 0) {
						mAdapter.add(mEntitiesInjected.get(0));
					}

					mAdapter.sort(new SortByScoreAndDistance());
					mAdapter.notifyDataSetChanged();
				}

				mSuggestInProgress = false;
				if (!mSuggestInput.equals(input)) {
					suggest(mSuggestInput);
				}
			}
		}.execute();
	}

	// --------------------------------------------------------------------------------------------
	// Properties
	// --------------------------------------------------------------------------------------------

	public void setInput(EditText input) {
		mInput = input;
	}

	public void setAdapter(ArrayAdapter adapter) {
		mAdapter = adapter;
	}

	public void setListView(AbsListView listView) {
		mListView = listView;
	}

	public List<Entity> getEntitiesInjected() {
		return mEntitiesInjected;
	}

	public void setProgress(ProgressBar progress) {
		mProgress = progress;
	}

	// --------------------------------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------------------------------

	private class ListAdapter extends ArrayAdapter<Entity> {

		private ListAdapter(List<Entity> list) {
			super(mContext, 0, list);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			View view = convertView;
			final ViewHolder holder;
			final Entity entity = (Entity) mAdapter.getItem(position);

			if (view == null) {
				view = LayoutInflater.from(mContext).inflate(R.layout.temp_place_search_item, null);
				holder = new ViewHolder();
				holder.photoView = (AirImageView) view.findViewById(R.id.entity_photo);
				holder.indicator = (ImageView) view.findViewById(R.id.indicator);
				holder.name = (TextView) view.findViewById(R.id.name);
				holder.subtitle = (TextView) view.findViewById(R.id.subtitle);
				view.setTag(holder);
			}
			else {
				holder = (ViewHolder) view.getTag();
				if (holder.photoView.getTag().equals(entity.getPhoto().getUri())) return view;
			}

			if (entity != null) {
				holder.data = entity;

				UI.setVisibility(holder.name, View.GONE);
				if (holder.name != null && entity.name != null && entity.name.length() > 0) {
					holder.name.setText(entity.name);
					UI.setVisibility(holder.name, View.VISIBLE);
				}

				UI.setVisibility(holder.subtitle, View.GONE);
				Place place = (Place) entity;
				if (holder.subtitle != null) {
					String address = place.getAddressString(false);

					if (!TextUtils.isEmpty(address)) {
						holder.subtitle.setText(address);
						UI.setVisibility(holder.subtitle, View.VISIBLE);
					}
					else {
						if (place.category != null && !TextUtils.isEmpty(place.category.name)) {
							holder.subtitle.setText(Html.fromHtml(place.category.name));
							UI.setVisibility(holder.subtitle, View.VISIBLE);
						}
					}
				}

				/* Photo */

				if (holder.photoView != null) {
					Photo photo = entity.getPhoto();
					if (holder.photoView.getPhoto() == null || !photo.getUri().equals(holder.photoView.getPhoto().getUri())) {
						UI.drawPhoto(holder.photoView, photo);
						holder.photoView.setTag(photo);
					}
				}

				/* Indicator */
				UI.setVisibility(holder.indicator, View.INVISIBLE);
				if (holder.indicator != null) {
					if (place.reason.equals(ReasonType.WATCH)) {
						holder.indicator.setImageResource(mWatchResId);
						UI.setVisibility(holder.indicator, View.VISIBLE);
					}
					else if (place.reason.equals(ReasonType.LOCATION)) {
						holder.indicator.setImageResource(mLocationResId);
						UI.setVisibility(holder.indicator, View.VISIBLE);
					}
				}

				view.setClickable(true);
				view.setOnClickListener(PlaceSuggestController.this);

			}
			return view;
		}
	}

	public static class SortByScoreAndDistance implements Comparator<Place> {

		@Override
		public int compare(Place object1, Place object2) {

			if (object1.score.floatValue() > object2.score.floatValue())
				return -1;
			else if (object2.score.floatValue() < object1.score.floatValue())
				return 1;
			else {
				if (object1.distance == null || object2.distance == null)
					return 0;
				else if (object1.distance < object2.distance.intValue())
					return -1;
				else if (object1.distance.intValue() > object2.distance.intValue())
					return 1;
				else
					return 0;
			}
		}
	}

	public static class ViewHolder {

		public TextView		name;
		public AirImageView	photoView;
		public ImageView	indicator;
		public TextView		subtitle;
		public String		photoUri;	// Used for verification after fetching image // NO_UCD (unused code)
		public Object		data;		// object binding to
	}

	public class TextWatcher extends SimpleTextWatcher {

		@Override
		public void afterTextChanged(Editable s) {
			String input = s.toString();
			if (!TextUtils.isEmpty(input) && input.length() >= 2) {
				mSuggestInput = input;
				if (!mSuggestInProgress) {
					suggest(input);
				}
			}
			else {
				mAdapter.clear();
				if (mEntitiesInjected != null && mEntitiesInjected.size() > 0) {
					mAdapter.add(mEntitiesInjected.get(0));
				}
				mAdapter.notifyDataSetChanged();
			}
		}
	};

}
