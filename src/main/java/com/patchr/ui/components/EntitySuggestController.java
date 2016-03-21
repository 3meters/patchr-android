package com.patchr.ui.components;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.SearchView;
import android.text.Editable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.DataController;
import com.patchr.components.LocationManager;
import com.patchr.components.ModelResult;
import com.patchr.components.NetworkManager;
import com.patchr.components.NetworkManager.ResponseCode;
import com.patchr.components.StringManager;
import com.patchr.components.UserManager;
import com.patchr.objects.AirLocation;
import com.patchr.objects.Entity;
import com.patchr.ui.views.ImageLayout;
import com.patchr.ui.views.SearchItemView;
import com.patchr.ui.widgets.AirTokenCompleteTextView;
import com.patchr.ui.widgets.TokenCompleteTextView;
import com.patchr.utilities.Json;
import com.patchr.utilities.Reporting;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class EntitySuggestController implements TokenCompleteTextView.TokenListener {
	/*
	 * Used by SearchForm and MessageEdit.
	 */
	private static Integer LIMIT = 10;

	private List<Entity>  seedEntities;
	private ArrayAdapter  adapter;
	private Context       context;
	private AbsListView   listView;
	private BusyPresenter busyPresenter;

	private EditText   searchInput;
	private SearchView searchView;
	private View       searchProgress;
	private View       searchImage;
	private boolean    suggestInProgress;

	private Integer                             watchResId;
	private Integer                             locationResId;
	private Integer                             userResId;
	private SimpleTextWatcher                   textWatcher;
	private String                              suggestInput;
	private String                              prefix;
	private TokenCompleteTextView.TokenListener tokenListener;

	private DataController.SuggestScope suggestScope;

	public EntitySuggestController(Context context) {

		this.context = context;
		this.seedEntities = new ArrayList<>();
		this.adapter = new SuggestArrayAdapter(this.seedEntities);
		this.suggestScope = DataController.SuggestScope.PATCHES;

		final TypedValue resourceName = new TypedValue();
		if (context.getTheme().resolveAttribute(R.attr.iconWatch, resourceName, true)) {
			watchResId = resourceName.resourceId;
		}
		if (context.getTheme().resolveAttribute(R.attr.iconLocation, resourceName, true)) {
			locationResId = resourceName.resourceId;
		}
		if (context.getTheme().resolveAttribute(R.attr.iconUser, resourceName, true)) {
			userResId = resourceName.resourceId;
		}
	}

	public void init() {

		/* Bind to adapter */
		if (searchInput instanceof AirTokenCompleteTextView) {
			AirTokenCompleteTextView input = (AirTokenCompleteTextView) searchInput;
			input.allowDuplicates(false);
			input.setDeletionStyle(TokenCompleteTextView.TokenDeleteStyle.Clear);
			input.setTokenClickStyle(TokenCompleteTextView.TokenClickStyle.Select);
			input.setTokenListener(tokenListener != null ? tokenListener : this);
		}

		if (searchInput instanceof AutoCompleteTextView) {
			((AutoCompleteTextView) searchInput).setAdapter(adapter);
		}
		else if (searchInput instanceof EditText && listView != null) {
			((ListView) listView).setAdapter(adapter);
			searchInput.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(@NonNull Editable s) {
					textChanged(s.toString());
				}
			});
		}
		else if (searchView != null && listView != null) {
			((ListView) listView).setAdapter(adapter);
			searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
				@Override
				public boolean onQueryTextSubmit(@NonNull String s) {
					textChanged(s);
					return true;
				}

				@Override
				public boolean onQueryTextChange(@NonNull String s) {
					textChanged(s);
					return true;
				}
			});
		}
	}

	public void textChanged(@NonNull String input) {
		if (!TextUtils.isEmpty(input) && input.length() >= 3) {
			adapter.getFilter().filter(input);
		}
		else {
			adapter.clear();
			if (seedEntities.size() > 0) {
				adapter.add(seedEntities.get(0));
			}
			adapter.notifyDataSetChanged();
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public void onTokenAdded(Object o) {
		Entity entity = (Entity) o;

		/* Add patch to auto complete array */
		try {
			org.json.JSONObject jsonSearchMap = new org.json.JSONObject(Patchr.settings.getString(
					StringManager.getString(R.string.setting_patch_searches), "{}"));
			final String jsonEntity = Json.objectToJson(entity);
			jsonSearchMap.put(entity.id, jsonEntity);
			Patchr.settingsEditor.putString(StringManager.getString(R.string.setting_patch_searches), jsonSearchMap.toString());
			Patchr.settingsEditor.commit();
		}
		catch (JSONException e) {
			Reporting.logException(e);
		}
	}

	@Override public void onTokenRemoved(Object o) {}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	@NonNull public EntitySuggestController setSearchInput(EditText input) {
		searchInput = input;
		return this;
	}

	@NonNull public EntitySuggestController setAdapter(ArrayAdapter adapter) {
		this.adapter = adapter;
		return this;
	}

	@NonNull public EntitySuggestController setTokenListener(TokenCompleteTextView.TokenListener tokenListener) {
		this.tokenListener = tokenListener;
		return this;
	}

	@NonNull public EntitySuggestController setSearchProgress(View progress) {
		searchProgress = progress;
		return this;
	}

	@NonNull public EntitySuggestController setSearchImage(View image) {
		searchImage = image;
		return this;
	}

	@NonNull public EntitySuggestController setPrefix(String mPrefix) {
		this.prefix = mPrefix;
		return this;
	}

	@NonNull public EntitySuggestController setSuggestScope(DataController.SuggestScope suggestScope) {
		this.suggestScope = suggestScope;
		return this;
	}

	@NonNull public EntitySuggestController setListView(AbsListView listView) {
		this.listView = listView;
		return this;
	}

	@NonNull public EntitySuggestController setSearchView(SearchView searchView) {
		this.searchView = searchView;
		return this;
	}

	@NonNull public EntitySuggestController setBusyPresenter(BusyPresenter busyPresenter) {
		this.busyPresenter = busyPresenter;
		return this;
	}

	public SearchView getSearchView() {
		return searchView;
	}

	public AbsListView getListView() {
		return listView;
	}

	@NonNull public List<Entity> getSeedEntities() {
		return seedEntities;
	}

	public ArrayAdapter getAdapter() {
		return adapter;
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	private class SuggestArrayAdapter extends ArrayAdapter<Entity> {

		private Filter       mFilter;
		private List<Entity> mSeedEntities;

		private SuggestArrayAdapter(List<Entity> seedEntities) {
			this(context, 0, 0, seedEntities);
		}

		public SuggestArrayAdapter(@NonNull Context context, int resource, int textViewResourceId, List<Entity> seedEntities) {
			super(context, resource, textViewResourceId, new ArrayList<>(seedEntities));
			mSeedEntities = seedEntities;
		}

		@SuppressWarnings("ConstantConditions")
		@Override public View getView(int position, View convertView, ViewGroup parent) {

			View view = convertView;
			final ViewHolder holder;
			final Entity entity = (Entity) adapter.getItem(position);

			if (view == null) {
				view = LayoutInflater.from(context).inflate(R.layout.temp_listitem_search, null);
			}

			if (entity != null) {
				SearchItemView searchItemView = (SearchItemView) view.findViewById(R.id.item_view);
				searchItemView.setTag(entity);
				searchItemView.databind(entity);
			}

			return view;
		}

		@Override public Filter getFilter() {
			if (mFilter == null) {
				mFilter = new SuggestFilter();
			}
			return mFilter;
		}

		private class SuggestFilter extends Filter {

			@NonNull
			@Override
			protected FilterResults performFiltering(CharSequence chars) {
			    /*
			     * Called on background thread.
                 */
				FilterResults result = new FilterResults();
				if (!suggestInProgress) {

					suggestInProgress = true;
					if (chars != null && chars.length() > 0) {

						if (busyPresenter != null) {
							Patchr.mainThreadHandler.post(new Runnable() {
								@Override
								public void run() {
									busyPresenter.startProgressBar();
								}
							});
						}

						if (searchProgress != null) {
							searchInput.post(new Runnable() {
								@Override
								public void run() {
									searchImage.setVisibility(View.INVISIBLE);
									searchProgress.setVisibility(View.VISIBLE);
								}
							});
						}

						Thread.currentThread().setName("AsyncSuggestEntities");
						final AirLocation location = LocationManager.getInstance().getAirLocationLocked();
						ModelResult modelResult = DataController.getInstance().suggest(chars.toString().trim()
								, suggestScope
								, UserManager.shared().authenticated() ? UserManager.currentUser.id : null
								, location
								, LIMIT, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);

						if (modelResult.serviceResponse.responseCode == ResponseCode.SUCCESS) {

							final List<Entity> entities = (List<Entity>) modelResult.data;

							if (mSeedEntities != null && mSeedEntities.size() > 0) {
								entities.add(mSeedEntities.get(0));
							}

							result.count = entities.size();
							result.values = entities;
						}
					}
					else {
					    /* Add all seed entities */
						result.values = mSeedEntities;
						result.count = mSeedEntities.size();
					}

					suggestInProgress = false;
				}
				return result;
			}

			@SuppressWarnings("unchecked")
			@Override
			protected void publishResults(CharSequence constraint, @NonNull FilterResults results) {
			    /*
			     * Called on UI thread.
                 */
				if (busyPresenter != null) {
					Patchr.mainThreadHandler.post(new Runnable() {
						@Override
						public void run() {
							busyPresenter.stopProgressBar();
						}
					});
				}

				if (searchProgress != null) {
					searchInput.post(new Runnable() {
						@Override
						public void run() {
							searchImage.setVisibility(View.VISIBLE);
							searchProgress.setVisibility(View.INVISIBLE);
						}
					});
				}

				clear();
				if (results.count > 0) {
					addAll((Collection) results.values);
					sort(new SortByScoreAndDistance());
					notifyDataSetChanged();
				}
				else {
					notifyDataSetInvalidated();
				}
			}
		}
	}

	public static class SortByScoreAndDistance implements Comparator<Entity> {

		@Override
		public int compare(@NonNull Entity object1, @NonNull Entity object2) {

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

		public TextView    name;
		public TextView    subhead;
		public TextView    categoryName;
		public TextView    type;
		public ImageLayout photoView;
		public ImageView   indicator;
		public String      photoUri;    // Used for verification after fetching image // NO_UCD (unused code)
		public Object      data;        // object binding to
	}
}
