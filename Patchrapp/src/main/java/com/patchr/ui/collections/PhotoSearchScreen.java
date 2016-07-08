package com.patchr.ui.collections;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
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
import android.widget.TextView;

import com.commonsware.cwac.endless.EndlessAdapter;
import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.AnimationManager;
import com.patchr.components.ContainerManager;
import com.patchr.components.Logger;
import com.patchr.components.ModelResult;
import com.patchr.components.NetworkManager;
import com.patchr.components.NetworkManager.ResponseCode;
import com.patchr.components.StringManager;
import com.patchr.model.Photo;
import com.patchr.objects.ImageResult;
import com.patchr.objects.ImageResult.Thumbnail;
import com.patchr.objects.ServiceData;
import com.patchr.objects.TransitionType;
import com.patchr.service.RequestType;
import com.patchr.service.ResponseFormat;
import com.patchr.service.ServiceRequest;
import com.patchr.service.ServiceRequest.AuthType;
import com.patchr.ui.BaseScreen;
import com.patchr.ui.components.BusyController;
import com.patchr.ui.components.EmptyController;
import com.patchr.ui.widgets.AirAutoCompleteTextView;
import com.patchr.ui.widgets.ImageWidget;
import com.patchr.utilities.Errors;
import com.patchr.utilities.Json;
import com.patchr.utilities.Reporting;

import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/*
 * We often will get duplicates because the ordering of images isn't
 * guaranteed while paging.
 */
public class PhotoSearchScreen extends BaseScreen {

	private GridView                gridView;
	private AirAutoCompleteTextView search;
	private List<ImageResult>       images;

	private long                 offset;
	private String               query;
	private String               defaultSearch;
	private List<String>         previousSearches;
	private ArrayAdapter<String> searchAdapter;
	private String               titleOptional;
	private Integer              photoWidthPixels;
	private EmptyController      emptyController;

	private static final long   PAGE_SIZE     = 49L;
	private static final long   LIST_MAX      = 300L;
	private static final String QUERY_PREFIX  = "";
	private static final String QUERY_DEFAULT = "wallpaper unusual places";

	@Override public void onCreate(Bundle savedInstanceState) {

		this.images = new ArrayList<ImageResult>();
		this.previousSearches = new ArrayList<>();

		super.onCreate(savedInstanceState);

		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
		this.emptyController = new EmptyController(findViewById(R.id.form_message));
		bind();
	}

	@Override protected void onDestroy() {
		super.onDestroy();
		System.gc();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_search_start, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override public boolean onOptionsItemSelected(MenuItem item) {

		if (item.getItemId() == R.id.submit) {
			submitAction();
		}
		else {
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		actionBarTitle.setText(R.string.screen_title_photo_picker);

		search = (AirAutoCompleteTextView) findViewById(R.id.search_text);

		if (search != null) {

			int inputType = search.getInputType();
			inputType &= ~InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE;
			search.setRawInputType(inputType);

			final Bundle extras = getIntent().getExtras();
			if (extras != null) {
				defaultSearch = extras.getString(Constants.EXTRA_SEARCH_PHRASE);
			}

			if (!TextUtils.isEmpty(defaultSearch)) {
				search.setText(defaultSearch);
			}
			else {
				String lastSearch = Patchr.settings.getString(StringManager.getString(R.string.setting_picture_search_last), null);
				if (!TextUtils.isEmpty(lastSearch)) {
					search.setText(lastSearch);
				}
			}

			search.setOnKeyListener(new OnKeyListener() {
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

			search.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					startSearch(view);
				}
			});

			search.requestFocus();
		}

		gridView = (GridView) findViewById(R.id.grid);

		/* Set spacing */
		Integer requestedHorizontalSpacing = getResources().getDimensionPixelSize(R.dimen.grid_spacing_horizontal);
		Integer requestedVerticalSpacing = getResources().getDimensionPixelSize(R.dimen.grid_spacing_vertical);
		gridView.setHorizontalSpacing(requestedHorizontalSpacing);
		gridView.setVerticalSpacing(requestedVerticalSpacing);

		/* Stash some sizing info */
		final DisplayMetrics metrics = getResources().getDisplayMetrics();
		final Integer availableSpace = metrics.widthPixels - gridView.getPaddingLeft() - gridView.getPaddingRight();

		Integer requestedColumnWidth = getResources().getDimensionPixelSize(R.dimen.grid_column_width_requested_medium);

		Integer mNumColumns = (availableSpace + requestedHorizontalSpacing) / (requestedColumnWidth + requestedHorizontalSpacing);
		if (mNumColumns <= 0) {
			mNumColumns = 1;
		}

		int spaceLeftOver = availableSpace - (mNumColumns * requestedColumnWidth) - ((mNumColumns - 1) * requestedHorizontalSpacing);

		photoWidthPixels = requestedColumnWidth + spaceLeftOver / mNumColumns;

		gridView.setColumnWidth(photoWidthPixels);
		gridView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (((EndlessImageAdapter) gridView.getAdapter()).getItemViewType(position) != Adapter.IGNORE_ITEM_VIEW_TYPE) {

					ImageResult imageResult = images.get(position);
					Photo photo = imageResult.photo;
					/*
					 * Photo gets set for images that are already being used like pictures linked to places so
					 * an empty photo means the image is coming from external service like bing.
					 */
					if (photo == null) {
						photo = new Photo(imageResult.getMediaUrl(), imageResult.getWidth().intValue(), imageResult.getHeight().intValue(), Photo.PhotoSource.generic);
					}

					final Intent intent = new Intent();
					final String jsonPhoto = Json.objectToJson(photo);
					intent.putExtra(Constants.EXTRA_PHOTO, jsonPhoto);
					setResult(Activity.RESULT_OK, intent);
					finish();
					AnimationManager.doOverridePendingTransition(PhotoSearchScreen.this, TransitionType.DIALOG_BACK);
				}
			}
		});

		/* Autocomplete */
		initAutoComplete();
		bindAutoCompleteAdapter();
	}

	@Override protected int getLayoutId() {
		return R.layout.screen_photo_picker;
	}

	@Override protected int getTransitionBack(int transitionType) {
		return TransitionType.DIALOG_BACK;
	}

	@Override public void submitAction() {
		startSearch(null);
	}

	public void bind() {
		if (!TextUtils.isEmpty(query)) {
			gridView.setAdapter(new EndlessImageAdapter(images));
		}
	}

	private void startSearch(View view) {

		search.dismissDropDown();

		query = search.getText().toString().trim();

		/* Prep the UI */
		images.clear();
		busyController.show(BusyController.BusyAction.Refreshing_Empty);

		/* Hide soft keyboard */
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(search.getWindowToken(), 0);

		/* Stash query so we can restore it in the future */
		SharedPreferences.Editor editor = Patchr.settings.edit();
		editor.putString(StringManager.getString(R.string.setting_picture_search_last), query);
		editor.apply();

		/* Add query to auto complete array */
		try {
			org.json.JSONObject jsonSearchMap = new org.json.JSONObject(Patchr.settings.getString(StringManager.getString(R.string.setting_picture_searches),
				"{}"));
			jsonSearchMap.put(query, query);
			editor.putString(StringManager.getString(R.string.setting_picture_searches), jsonSearchMap.toString());
			editor.apply();
		}
		catch (JSONException e) {
			Reporting.logException(e);
		}

		/* Make sure the latest search appears in auto complete */
		initAutoComplete();
		bindAutoCompleteAdapter();

		offset = 0;
		titleOptional = query;

		/* Trigger the adapter */
		gridView.setAdapter(new EndlessImageAdapter(images));
	}

	private void initAutoComplete() {
		try {
			org.json.JSONObject jsonSearchMap = new org.json.JSONObject(Patchr.settings.getString(StringManager.getString(R.string.setting_picture_searches),
				"{}"));
			previousSearches.clear();
			if (defaultSearch != null) {
				jsonSearchMap.put(defaultSearch, defaultSearch);
			}
			org.json.JSONArray jsonSearches = jsonSearchMap.names();
			if (jsonSearches != null) {
				for (int i = 0; i < jsonSearches.length(); i++) {
					String name = jsonSearches.getString(i);
					previousSearches.add(jsonSearchMap.getString(name));
				}
			}
		}
		catch (JSONException e) {
			Reporting.logException(e);
		}
	}

	private void bindAutoCompleteAdapter() {
		searchAdapter = new ArrayAdapter<String>(this
			, android.R.layout.simple_dropdown_item_1line
			, previousSearches);
		search.setAdapter(searchAdapter);
	}

	private ModelResult loadSearchImages(String query, long count, long offset, Integer maxSize, Integer maxDimen) {

		ModelResult result = new ModelResult();
		try {
			query = "%27" + URLEncoder.encode(query, "UTF-8") + "%27";
		}
		catch (UnsupportedEncodingException e) {
			Reporting.logException(e);
		}

		final String bingUrl = Constants.URI_PROXIBASE_SEARCH_IMAGES
			+ "?Query=" + query
			+ "&Market=%27en-US%27&Adult=%27Strict%27&ImageFilters=%27size%3alarge%27"
			+ "&$top=" + String.valueOf(count + 1)
			+ "&$skip=" + String.valueOf(offset)
			+ "&$format=Json";

		final ServiceRequest serviceRequest = new ServiceRequest(bingUrl, RequestType.GET, ResponseFormat.JSON);
		serviceRequest.setAuthType(AuthType.BASIC)
			.setUserName(null)
			.setPassword(ContainerManager.getContainerHolder().getContainer().getString(Patchr.BING_ACCESS_KEY));

		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {

			final ServiceData serviceData = (ServiceData) Json.jsonToObjects((String) result.serviceResponse.data
				, Json.ObjectType.IMAGE_RESULT
				, Json.ServiceDataWrapper.TRUE);

			List<ImageResult> images = new ArrayList<ImageResult>();

			if (serviceData.data != null) {
				images = (ArrayList<ImageResult>) serviceData.data;
			}

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
					for (ImageResult image : this.images) {
						if (image.getThumbnail().getUrl().equals(imageResult.getThumbnail().getUrl())) {
							usable = false;
							break;
						}
					}
				}

				if (usable) {
					imagesFiltered.add(imageResult);
				}
			}

			result.data = imagesFiltered;
		}

		return result;
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	public class EndlessImageAdapter extends EndlessAdapter {

		private List<ImageResult> mMoreImages = new ArrayList<ImageResult>();

		private EndlessImageAdapter(List<ImageResult> list) {
			super(new ListAdapter(list));
		}

		@Override protected boolean cacheInBackground() {
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
			String queryDecorated = query;
			if (TextUtils.isEmpty(queryDecorated)) {
				queryDecorated = QUERY_DEFAULT;
			}
			else {
				queryDecorated = (QUERY_PREFIX + " " + queryDecorated).trim();
			}

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					emptyController.hide(true);
				}
			});
			ModelResult result = loadSearchImages(queryDecorated, PAGE_SIZE, offset, Constants.BING_IMAGE_BYTES_MAX, Constants.BING_IMAGE_DIMENSION_MAX);
			ServiceData serviceData = (ServiceData) result.serviceResponse.data;

			busyController.hide(false);
			if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {

				mMoreImages = (ArrayList<ImageResult>) result.data;

				if (mMoreImages.size() == 0) {
					if (offset == 0) {
						runOnUiThread(new Runnable() {

							@Override
							public void run() {
								emptyController.setText(StringManager.getString(R.string.empty_photo_search) + " " + query);
								emptyController.show(true);
							}
						});
					}
					return false;
				}
				else {
					Logger.d(this, "Query Bing for more images: start = " + String.valueOf(offset)
						+ " new total = "
						+ String.valueOf(getWrappedAdapter().getCount() + mMoreImages.size()));

					if (!serviceData.more) {
						return false;
					}
					else {
						offset += serviceData.count.intValue();
						return (getWrappedAdapter().getCount() + mMoreImages.size()) < LIST_MAX;
					}
				}
			}
			else {
				Errors.handleError(PhotoSearchScreen.this, result.serviceResponse);
				return false;
			}
		}

		@Override protected View getPendingView(ViewGroup parent) {
			/*
			 * Gets called when adapter is being asked for a view for the last position
			 * and the previous call to cacheInBackground reported that more = true. Also starts
			 * another call to cacheInBackground().
			 */
			/* If nothing to show, return something empty. */
			if (images.size() == 0) return new View(PhotoSearchScreen.this);
			View view = LayoutInflater.from(PhotoSearchScreen.this).inflate(R.layout.view_photo_search_placeholder, null);
			Integer nudge = getResources().getDimensionPixelSize(R.dimen.grid_item_height_kick);
			final FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(photoWidthPixels, photoWidthPixels - nudge);
			TextView placeholder = (TextView) view.findViewById(R.id.item_image);
			placeholder.setLayoutParams(params);
			return view;
		}

		@Override protected void appendCachedData() {
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
			super(PhotoSearchScreen.this, 0, list);
		}

		@Override public View getView(int position, View convertView, ViewGroup parent) {

			View view = convertView;
			final ViewHolder holder;
			final ImageResult itemData = images.get(position);

			if (view == null) {
				view = LayoutInflater.from(PhotoSearchScreen.this).inflate(R.layout.listitem_photo_search, null);
				holder = new ViewHolder();
				holder.photoView = (ImageWidget) view.findViewById(R.id.photo);
				Integer nudge = getResources().getDimensionPixelSize(R.dimen.grid_item_height_kick);
				final FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(photoWidthPixels, photoWidthPixels - nudge);
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
				Photo photo = new Photo(thumbnail.getUrl(), Photo.PhotoSource.bing);
				holder.photoView.setImageWithPhoto(photo, null);
			}
			return view;
		}
	}

	public static class ViewHolder {
		public ImageWidget photoView;
		public ImageResult data; // NO_UCD (unused code)
	}
}