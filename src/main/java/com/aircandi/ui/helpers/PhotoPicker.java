package com.aircandi.ui.helpers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;

import com.aircandi.Patchr;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.ServiceConstants;
import com.aircandi.components.EntityManager;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ModelResult;
import com.aircandi.components.StringManager;
import com.aircandi.objects.Entity;
import com.aircandi.objects.ImageResult;
import com.aircandi.objects.ImageResult.Thumbnail;
import com.aircandi.objects.Photo;
import com.aircandi.objects.Photo.PhotoSource;
import com.aircandi.objects.Place;
import com.aircandi.objects.Provider;
import com.aircandi.objects.ServiceData;
import com.aircandi.service.RequestType;
import com.aircandi.service.ResponseFormat;
import com.aircandi.service.ServiceRequest;
import com.aircandi.service.ServiceRequest.AuthType;
import com.aircandi.service.ServiceResponse;
import com.aircandi.ui.base.BaseActivity;
import com.aircandi.interfaces.IBusy.BusyAction;
import com.aircandi.ui.widgets.AirAutoCompleteTextView;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.ui.widgets.AirTextView;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.Json;
import com.aircandi.utilities.Reporting;
import com.aircandi.utilities.UI;
import com.commonsware.cwac.endless.EndlessAdapter;

import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/*
 * We often will get duplicates because the ordering of images isn't
 * guaranteed while paging.
 */
public class PhotoPicker extends BaseActivity {

	private GridView                mGridView;
	private AirAutoCompleteTextView mSearch;
	private final List<ImageResult> mImages = new ArrayList<ImageResult>();
	private Entity mEntity;
	private String mEntityId;

	private long mOffset = 0;
	private String mQuery;
	private String mDefaultSearch;
	private List<String> mPreviousSearches = new ArrayList<String>();
	private ArrayAdapter<String> mSearchAdapter;
	private String               mTitleOptional;
	private Boolean mPlacePhotoMode = false;
	private Provider mProvider;
	private Integer  mPhotoWidthPixels;

	private static final long   PAGE_SIZE     = 30L;
	private static final long   LIST_MAX      = 300L;
	private static final String QUERY_PREFIX  = "";
	private static final String QUERY_DEFAULT = "wallpaper unusual places";

	@Override
	public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mEntityId = extras.getString(Constants.EXTRA_ENTITY_ID);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!isFinishing() && !mPlacePhotoMode) {
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
		}
	}

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		if (mEntityId != null) {
			mPlacePhotoMode = true;
		}

		mSearch = (AirAutoCompleteTextView) findViewById(R.id.search_text);

		if (mPlacePhotoMode) {
			mEntity = EntityManager.getCacheEntity(mEntityId);
			mProvider = ((Place) mEntity).getProvider();
			mSearch.setVisibility(View.GONE);
			mBusy.showBusy(BusyAction.ActionWithMessage, R.string.progress_searching);
		}
		else {

			int inputType = mSearch.getInputType();
			inputType &= ~InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE;
			mSearch.setRawInputType(inputType);

			final Bundle extras = getIntent().getExtras();
			if (extras != null) {
				mDefaultSearch = extras.getString(Constants.EXTRA_SEARCH_PHRASE);
			}

			if (!TextUtils.isEmpty(mDefaultSearch)) {
				mSearch.setText(mDefaultSearch);
			}
			else {
				String lastSearch = Patchr.settings.getString(StringManager.getString(R.string.setting_picture_search_last), null);
				if (!TextUtils.isEmpty(lastSearch)) {
					mSearch.setText(lastSearch);
				}
			}

			mSearch.setOnKeyListener(new OnKeyListener() {
				@Override
				public boolean onKey(View view, int keyCode, KeyEvent event) {
					if (keyCode == KeyEvent.KEYCODE_ENTER) {
						startSearch(view);
						return true;
					}
					else
						return false;
				}
			});

			mSearch.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					startSearch(view);
				}
			});

			mSearch.requestFocus();
		}

		mGridView = (GridView) findViewById(R.id.grid);

		/* Set spacing */
		Integer requestedHorizontalSpacing = mResources.getDimensionPixelSize(R.dimen.grid_spacing_horizontal);
		Integer requestedVerticalSpacing = mResources.getDimensionPixelSize(R.dimen.grid_spacing_vertical);
		mGridView.setHorizontalSpacing(requestedHorizontalSpacing);
		mGridView.setVerticalSpacing(requestedVerticalSpacing);

		/* Stash some sizing info */
		final DisplayMetrics metrics = mResources.getDisplayMetrics();
		final Integer availableSpace = metrics.widthPixels - mGridView.getPaddingLeft() - mGridView.getPaddingRight();

		Integer requestedColumnWidth = mResources.getDimensionPixelSize(R.dimen.grid_column_width_requested_medium);

		Integer mNumColumns = (availableSpace + requestedHorizontalSpacing) / (requestedColumnWidth + requestedHorizontalSpacing);
		if (mNumColumns <= 0) {
			mNumColumns = 1;
		}

		int spaceLeftOver = availableSpace - (mNumColumns * requestedColumnWidth) - ((mNumColumns - 1) * requestedHorizontalSpacing);

		mPhotoWidthPixels = requestedColumnWidth + spaceLeftOver / mNumColumns;

		mGridView.setColumnWidth(mPhotoWidthPixels);
		mGridView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (((EndlessImageAdapter) mGridView.getAdapter()).getItemViewType(position) != Adapter.IGNORE_ITEM_VIEW_TYPE) {

					ImageResult imageResult = mImages.get(position);
					Photo photo = imageResult.getPhoto();
					/*
					 * Photo gets set for images that are already being used like pictures linked to places so
					 * an empty photo means the image is coming from external service like bing.
					 */
					if (photo == null) {
						photo = new Photo(imageResult.getMediaUrl(), null, imageResult.getWidth(), imageResult.getHeight(), PhotoSource.generic);
					}
					photo.name = mTitleOptional;

					final Intent intent = new Intent();
					final String jsonPhoto = Json.objectToJson(photo);
					intent.putExtra(Constants.EXTRA_PHOTO, jsonPhoto);
					setResultCode(Activity.RESULT_OK, intent);
					finish();
				}
			}
		});

		if (mPlacePhotoMode) {
			bind(BindingMode.AUTO);
			draw(null);
		}
		else {
			/* Autocomplete */
			initAutoComplete();
			bindAutoCompleteAdapter();
			draw(null);
		}
	}

	@Override
	public void bind(BindingMode mode) {
		if (mPlacePhotoMode || !TextUtils.isEmpty(mQuery)) {
			mGridView.setAdapter(new EndlessImageAdapter(mImages));
		}
	}

	public void draw(View view){
		if (mPlacePhotoMode) {
			setActivityTitle(mEntity.name);
		}
		else {
			setActivityTitle(StringManager.getString(R.string.dialog_photo_picker_search_title));
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@SuppressWarnings("ucd")
	public void onSearchClick(View view) {
		startSearch(view);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	private void startSearch(View view) {

		mSearch.dismissDropDown();

		mQuery = mSearch.getText().toString().trim();

		/* Prep the UI */
		mImages.clear();
		mBusy.showBusy(BusyAction.Loading);

		/* Hide soft keyboard */
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(mSearch.getWindowToken(), 0);

		/* Stash query so we can restore it in the future */
		Patchr.settingsEditor.putString(StringManager.getString(R.string.setting_picture_search_last), mQuery);
		Patchr.settingsEditor.commit();

		/* Add query to auto complete array */
		try {
			org.json.JSONObject jsonSearchMap = new org.json.JSONObject(Patchr.settings.getString(StringManager.getString(R.string.setting_picture_searches),
					"{}"));
			jsonSearchMap.put(mQuery, mQuery);
			Patchr.settingsEditor.putString(StringManager.getString(R.string.setting_picture_searches), jsonSearchMap.toString());
			Patchr.settingsEditor.commit();
		}
		catch (JSONException e) {
			Reporting.logException(e);
		}

		/* Make sure the latest search appears in auto complete */
		initAutoComplete();
		bindAutoCompleteAdapter();

		mOffset = 0;
		mTitleOptional = mQuery;

		/* Trigger the adapter */
		mGridView.setAdapter(new EndlessImageAdapter(mImages));
	}

	private void initAutoComplete() {
		try {
			org.json.JSONObject jsonSearchMap = new org.json.JSONObject(Patchr.settings.getString(StringManager.getString(R.string.setting_picture_searches),
					"{}"));
			mPreviousSearches.clear();
			if (mDefaultSearch != null) {
				jsonSearchMap.put(mDefaultSearch, mDefaultSearch);
			}
			org.json.JSONArray jsonSearches = jsonSearchMap.names();
			if (jsonSearches != null) {
				for (int i = 0; i < jsonSearches.length(); i++) {
					String name = jsonSearches.getString(i);
					mPreviousSearches.add(jsonSearchMap.getString(name));
				}
			}
		}
		catch (JSONException e) {
			Reporting.logException(e);
		}
	}

	private void bindAutoCompleteAdapter() {
		mSearchAdapter = new ArrayAdapter<String>(this
				, android.R.layout.simple_dropdown_item_1line
				, mPreviousSearches);
		mSearch.setAdapter(mSearchAdapter);
	}

	/*--------------------------------------------------------------------------------------------
	 * Services
	 *--------------------------------------------------------------------------------------------*/

	private ModelResult loadSearchImages(String query, long count, long offset, Integer maxSize, Integer maxDimen) {

		ModelResult result = new ModelResult();
		try {
			query = "%27" + URLEncoder.encode(query, "UTF-8") + "%27";
		}
		catch (UnsupportedEncodingException e) {
			Reporting.logException(e);
		}

		final String bingUrl = ServiceConstants.URL_PROXIBASE_SEARCH_IMAGES
				+ "?Query=" + query
				+ "&Market=%27en-US%27&Adult=%27Strict%27&ImageFilters=%27size%3alarge%27"
				+ "&$top=" + String.valueOf(count + 1)
				+ "&$skip=" + String.valueOf(offset)
				+ "&$format=Json";

		final ServiceRequest serviceRequest = new ServiceRequest(bingUrl, RequestType.GET, ResponseFormat.JSON);
		serviceRequest.setAuthType(AuthType.BASIC)
		              .setUserName(null)
		              .setPassword(Patchr.getInstance().getContainer().getString(Patchr.BING_ACCESS_KEY));

		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {

			final ServiceData serviceData = (ServiceData) Json.jsonToObjects((String) result.serviceResponse.data
					, Json.ObjectType.IMAGE_RESULT
					, Json.ServiceDataWrapper.TRUE);

			final List<ImageResult> images = (ArrayList<ImageResult>) serviceData.data;

			/* There are more so pop off the last */
			if (images.size() > count) {
				serviceData.more = true;
				images.remove(images.size() - 1);
			}

			/* Stash the original count for paging */
			serviceData.count = images.size();

			result.serviceResponse.data = serviceData;

			List<ImageResult> imagesFiltered = new ArrayList<ImageResult>();
			for (ImageResult imageResult : images) {

				Boolean usable = false;
				if (imageResult.getFileSize() <= maxSize
						&& imageResult.getHeight() <= maxDimen
						&& imageResult.getWidth() <= maxDimen) {
					usable = true;
				}

				if (usable) {
					usable = (imageResult.getThumbnail() != null && imageResult.getThumbnail().getUrl() != null);
				}

				if (usable) {
					imagesFiltered.add(imageResult);
				}
			}

			result.data = imagesFiltered;
		}

		return result;
	}

	private ServiceResponse loadPlaceImages(long count, long offset) {
		final ModelResult result = Patchr.getInstance().getEntityManager().getPlacePhotos(mProvider, count, offset);
		return result.serviceResponse;
	}

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected void onDestroy() {
		super.onDestroy();
		System.gc();
	}

	/*--------------------------------------------------------------------------------------------
	 * Misc
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected int getLayoutId() {
		return R.layout.photo_picker;
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Adapter
	 *--------------------------------------------------------------------------------------------*/

	public class EndlessImageAdapter extends EndlessAdapter {

		private List<ImageResult> mMoreImages = new ArrayList<ImageResult>();

		private EndlessImageAdapter(List<ImageResult> list) {
			super(new ListAdapter(list));
		}

		@Override
		protected boolean cacheInBackground() {
			/*
			 * Triggered first time the adapter runs and when this function reported
			 * more available and the special pending view is being rendered by getView.
			 * Returning true means we think there are more items available to QUERY for.
			 * 
			 * This is called on background thread from an AsyncTask started by EndlessAdapter.
			 * We load some data plus report whether there is more data available. If more data is
			 * available, the pending view is appended.
			 */
			mMoreImages.clear();
			if (mPlacePhotoMode) {

				Place place = (Place) mEntity;
				/*
				 * Place provider is foursquare
				 */
				if (place.getProvider().type != null && place.getProvider().type.equals("foursquare")) {

					ServiceResponse serviceResponse = loadPlaceImages(PAGE_SIZE, mOffset);
					mBusy.hideBusy(false);
					if (serviceResponse.responseCode == ResponseCode.SUCCESS) {
						final List<Photo> photos = (ArrayList<Photo>) serviceResponse.data;
						if (photos.size() == 0) {
							if (mOffset == 0) {
								runOnUiThread(new Runnable() {

									@Override
									public void run() {
										mBubbleButton.setText(StringManager.getString(R.string.label_photo_picker_empty) + " " + mEntity.name);
										mBubbleButton.fadeIn();
									}
								});
							}
							return false;
						}
						else {
							mMoreImages = new ArrayList<ImageResult>();
							for (Photo photo : photos) {
								ImageResult imageResult = photo.getAsImageResult();
								imageResult.setPhoto(photo);
								mMoreImages.add(imageResult);
							}
							mOffset += PAGE_SIZE;
							return mMoreImages.size() >= PAGE_SIZE;
						}
					}
					else {
						Errors.handleError(PhotoPicker.this, serviceResponse);
						return false;
					}
				}
				else {
					return false;
				}
			}
			else {
				String queryDecorated = mQuery;
				if (TextUtils.isEmpty(queryDecorated)) {
					queryDecorated = QUERY_DEFAULT;
				}
				else {
					queryDecorated = (QUERY_PREFIX + " " + queryDecorated).trim();
				}

				ModelResult result = loadSearchImages(queryDecorated, PAGE_SIZE, mOffset, Constants.BING_IMAGE_BYTES_MAX, Constants.BING_IMAGE_DIMENSION_MAX);
				ServiceData serviceData = (ServiceData) result.serviceResponse.data;

				mBusy.hideBusy(false);
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {

					mMoreImages = (ArrayList<ImageResult>) result.data;

					if (mMoreImages.size() == 0) {
						if (mOffset == 0) {
							runOnUiThread(new Runnable() {

								@Override
								public void run() {
									mBubbleButton.setText(StringManager.getString(R.string.label_photo_picker_empty) + " " + mQuery);
									mBubbleButton.fadeIn();
								}
							});
						}
						return false;
					}
					else {
						Logger.d(this, "Query Bing for more images: start = " + String.valueOf(mOffset)
								+ " new total = "
								+ String.valueOf(getWrappedAdapter().getCount() + mMoreImages.size()));

						if (!serviceData.more) {
							return false;
						}
						else {
							mOffset += serviceData.count.intValue();
							return (getWrappedAdapter().getCount() + mMoreImages.size()) < LIST_MAX;
						}
					}
				}
				else {
					Errors.handleError(PhotoPicker.this, result.serviceResponse);
					return false;
				}
			}
		}

		@Override
		protected View getPendingView(ViewGroup parent) {
			/*
			 * Gets called when adapter is being asked for a view for the last position
			 * and the previous call to cacheInBackground reported that more = true. Also starts
			 * another call to cacheInBackground().
			 */
			/* If nothing to show, return something empty. */
			if (mImages.size() == 0) return new View(PhotoPicker.this);
			View view = LayoutInflater.from(PhotoPicker.this).inflate(R.layout.temp_picture_search_item_placeholder, null);
			Integer nudge = mResources.getDimensionPixelSize(R.dimen.grid_item_height_kick);
			final FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(mPhotoWidthPixels, mPhotoWidthPixels - nudge);
			AirTextView placeholder = (AirTextView) view.findViewById(R.id.item_image);
			placeholder.setLayoutParams(params);
			return view;
		}

		@Override
		protected void appendCachedData() {
			/*
			 * Is called immediately after cacheInBackground regardless
			 * of whether it returned true/false.
			 */
			final ArrayAdapter<ImageResult> list = (ArrayAdapter<ImageResult>) getWrappedAdapter();
			for (ImageResult imageResult : mMoreImages) {
				list.add(imageResult);
			}
			notifyDataSetChanged();
		}
	}

	private class ListAdapter extends ArrayAdapter<ImageResult> {

		private ListAdapter(List<ImageResult> list) {
			super(PhotoPicker.this, 0, list);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			View view = convertView;
			final ViewHolder holder;
			final ImageResult itemData = mImages.get(position);

			if (view == null) {
				view = LayoutInflater.from(PhotoPicker.this).inflate(R.layout.temp_picture_search_item, null);
				holder = new ViewHolder();
				holder.photoView = (AirImageView) view.findViewById(R.id.photo);
				Integer nudge = mResources.getDimensionPixelSize(R.dimen.grid_item_height_kick);
				final FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(mPhotoWidthPixels, mPhotoWidthPixels - nudge);
				holder.photoView.setLayoutParams(params);
				view.setTag(holder);
			}
			else {
				holder = (ViewHolder) view.getTag();
				if (holder.photoView.getTag().equals(itemData.getThumbnail().getUrl())) return view;
			}

			if (itemData != null) {

				holder.data = itemData;
				holder.photoView.setTag(itemData.getThumbnail().getUrl());
				Thumbnail thumbnail = itemData.getThumbnail();
				Photo photo = new Photo().setPrefix(thumbnail.getUrl()).setSource(PhotoSource.bing);
				UI.drawPhoto(holder.photoView, photo);
			}
			return view;
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	public static class ViewHolder {

		public AirImageView photoView;
		public ImageResult  data; // NO_UCD (unused code)
	}

}