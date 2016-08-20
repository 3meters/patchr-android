package com.patchr.ui.collections;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.AnimationManager;
import com.patchr.components.Logger;
import com.patchr.components.StringManager;
import com.patchr.model.Photo;
import com.patchr.objects.ImageResult;
import com.patchr.objects.enums.TransitionType;
import com.patchr.service.RestClient;
import com.patchr.ui.BaseScreen;
import com.patchr.ui.components.BusyController;
import com.patchr.ui.components.EmptyController;
import com.patchr.ui.components.EndlessRecyclerViewScrollListener;
import com.patchr.ui.components.OnItemClickListener;
import com.patchr.ui.widgets.AirAutoCompleteTextView;
import com.patchr.ui.widgets.AirAutofitRecyclerView;
import com.patchr.ui.widgets.ImageWidget;
import com.patchr.utilities.Colors;
import com.patchr.utilities.Errors;
import com.patchr.components.ReportingManager;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/*
 * We often will get duplicates because the ordering of images isn't
 * guaranteed while paging.
 */
public class PhotoSearchScreen extends BaseScreen {

	private AirAutofitRecyclerView  recyclerView;
	private ImageArrayAdapter       adapter;
	private AirAutoCompleteTextView searchView;
	private EmptyController         emptyController;

	private List<ImageResult> images;
	private int               offset;
	private int               offsetAddCount;
	private String            searchText;

	private String       defaultSearchText;
	private List<String> previousSearches;

	private static final int    PAGE_SIZE     = 150;
	private static final int    LIST_MAX      = 300;
	private static final String QUERY_PREFIX  = "";
	private static final String QUERY_DEFAULT = "wallpaper unusual places";

	@Override public void onCreate(Bundle savedInstanceState) {

		this.images = new ArrayList<>();
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

	@Override public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		recyclerView.invalidate();
		adapter.notifyDataSetChanged();
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		actionBarTitle.setText(R.string.screen_title_photo_picker);

		recyclerView = (AirAutofitRecyclerView) findViewById(R.id.grid);
		recyclerView.setHasFixedSize(true);

		adapter = new ImageArrayAdapter(view -> {

			ImageWidget imageWidget = (ImageWidget) view;
			ImageResult imageResult = (ImageResult) imageWidget.getTag();
			Photo photo = imageResult.asPhoto();

			final Intent intent = new Intent();
			final String jsonPhoto = Patchr.gson.toJson(photo);
			intent.putExtra(Constants.EXTRA_PHOTO, jsonPhoto);
			setResult(Activity.RESULT_OK, intent);
			finish();
			AnimationManager.doOverridePendingTransition(PhotoSearchScreen.this, TransitionType.DIALOG_BACK);
		});

		searchView = (AirAutoCompleteTextView) findViewById(R.id.search_text);
		if (searchView != null) {

			int inputType = searchView.getInputType();
			inputType &= ~InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE;
			searchView.setRawInputType(inputType);

			if (!TextUtils.isEmpty(defaultSearchText)) {
				searchView.setText(defaultSearchText);
			}
			else {
				String lastSearch = Patchr.settings.getString(StringManager.getString(R.string.setting_picture_search_last), null);
				if (!TextUtils.isEmpty(lastSearch)) {
					searchView.setText(lastSearch);
				}
			}

			searchView.setOnKeyListener((view, keyCode, event) -> {
				if (keyCode == KeyEvent.KEYCODE_ENTER) {
					if (event.getAction() == KeyEvent.ACTION_UP) {
						search(view);
					}
					return true;
				}
				return false;
			});

			searchView.setOnItemClickListener((parent, view, position, id) -> {
				search(view);   // When user taps on autocomplete item
			});

			searchView.requestFocus();
		}

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
		search(null);   // Used by menu item
	}

	public void bind() {
		this.recyclerView.setAdapter(this.adapter);
	}

	private void search(View view) {

		if (!processing) {

			processing = true;
			searchView.dismissDropDown();

			searchText = searchView.getText().toString().trim();

			/* Prep the UI */
			images.clear();
			busyController.show(BusyController.BusyAction.Refreshing_Empty);

			/* Hide soft keyboard */
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);

			/* Stash query so we can restore it in the future */
			SharedPreferences.Editor editor = Patchr.settings.edit();
			editor.putString(StringManager.getString(R.string.setting_picture_search_last), searchText);
			editor.apply();

			/* Add query to auto complete array */
			try {
				org.json.JSONObject jsonSearchMap = new org.json.JSONObject(Patchr.settings.getString(StringManager.getString(R.string.setting_picture_searches),
					"{}"));
				jsonSearchMap.put(searchText, searchText);
				editor.putString(StringManager.getString(R.string.setting_picture_searches), jsonSearchMap.toString());
				editor.apply();
			}
			catch (JSONException e) {
				ReportingManager.logException(e);
			}

			/* Make sure the latest search appears in auto complete */
			initAutoComplete();
			bindAutoCompleteAdapter();

			offset = 0;
			fetchImages(searchText);
		}
	}

	private void initAutoComplete() {
		try {
			org.json.JSONObject jsonSearchMap = new org.json.JSONObject(Patchr.settings.getString(StringManager.getString(R.string.setting_picture_searches),
				"{}"));
			previousSearches.clear();
			if (defaultSearchText != null) {
				jsonSearchMap.put(defaultSearchText, defaultSearchText);
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
			ReportingManager.logException(e);
		}
	}

	private void bindAutoCompleteAdapter() {
		ArrayAdapter<String> autoCompleteAdapter = new ArrayAdapter<>(this
			, android.R.layout.simple_dropdown_item_1line
			, previousSearches);
		searchView.setAdapter(autoCompleteAdapter);
	}

	private void fetchImages(final String queryText) {

		AsyncTask.execute(() -> subscription = RestClient.getInstance().loadSearchImages(queryText, PAGE_SIZE, offset)
			.map(response -> {
				List<ImageResult> imagesFiltered = new ArrayList<>();
				if (response.data != null && response.data.size() > 0) {
					for (ImageResult imageResult : response.data) {

						Boolean usable = false;
						if (imageResult.contentSize <= Constants.BING_IMAGE_BYTES_MAX
							&& imageResult.height <= Constants.BING_IMAGE_DIMENSION_MAX
							&& imageResult.width <= Constants.BING_IMAGE_DIMENSION_MAX) {
							usable = true;
						}

						if (usable) {
							usable = (imageResult.thumbnailUrl != null);
						}

						if (usable) {
							imagesFiltered.add(imageResult);
						}
					}
				}

				response.data = imagesFiltered;
				return response;
			})
			.subscribe(
				response -> {
					processing = false;
					busyController.hide(false);
					List<ImageResult> moreImages = (ArrayList<ImageResult>) response.data;

					if (moreImages.size() > 0) {
						Integer positionStart = images.size();
						images.addAll(moreImages);
						adapter.notifyItemRangeChanged(positionStart, images.size() - 1);
						recyclerView.requestLayout();
						Logger.d(this, String.format("Query Bing for more images: start = %1$s new total = %2$s", String.valueOf(offset), String.valueOf(images.size())));
					}

					if (images.size() == 0) {
						emptyController.setText(StringManager.getString(R.string.empty_photo_search) + " " + this.searchText);
						emptyController.show(true);
					}

					if (response.more && images.size() < LIST_MAX) {
						offset = offset + PAGE_SIZE + response.offsetAddCount;
						recyclerView.addOnScrollListener(new EndlessRecyclerViewScrollListener((GridLayoutManager) recyclerView.getLayoutManager()) {
							@Override public void onLoadMore(int page, int totalItemsCount) {
								recyclerView.removeOnScrollListener(this);
								fetchImages(queryText);
							}
						});
					}
					else {
						Logger.d(this, "No more search images available");
					}
				},
				error -> {
					processing = false;
					busyController.hide(false);
					Errors.handleError(this, error);
				}));
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	private class ImageArrayAdapter extends RecyclerView.Adapter<ImageViewHolder> {

		private LayoutInflater      inflater;
		private OnItemClickListener listener;

		private ImageArrayAdapter(OnItemClickListener listener) {
			this.listener = listener;
			this.inflater = LayoutInflater.from(Patchr.applicationContext);
		}

		@Override public ImageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View view = inflater.inflate(R.layout.listitem_photo_search, parent, false);
			return new ImageViewHolder(view);
		}

		@Override public void onBindViewHolder(ImageViewHolder holder, int position) {
			ImageResult image = images.get(position);
			holder.bind(image, listener);
		}

		@Override public int getItemCount() {
			return images.size();
		}
	}

	static private class ImageViewHolder extends RecyclerView.ViewHolder {
		public ImageWidget imageWidget;

		public ImageViewHolder(final View itemView) {
			super(itemView);
			imageWidget = (ImageWidget) itemView.findViewById(R.id.photo);
			imageWidget.setBackgroundColor(Colors.getColor(R.color.background_placeholder));
		}

		public void bind(ImageResult imageResult, final OnItemClickListener listener) {
			imageWidget.setImageWithPhoto(imageResult.thumbnailAsPhoto(), null, null);
			imageWidget.setTag(imageResult);
			imageWidget.setOnClickListener(listener::onItemClick);
		}
	}
}