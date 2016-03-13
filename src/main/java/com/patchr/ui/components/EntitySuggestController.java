package com.patchr.ui.components;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.SearchView;
import android.text.Editable;
import android.text.Html;
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
import com.patchr.objects.Patch;
import com.patchr.objects.Patch.ReasonType;
import com.patchr.objects.User;
import com.patchr.ui.views.ImageLayout;
import com.patchr.ui.widgets.AirTokenCompleteTextView;
import com.patchr.ui.widgets.TokenCompleteTextView;
import com.patchr.utilities.Json;
import com.patchr.utilities.Reporting;
import com.patchr.utilities.UI;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class EntitySuggestController implements TokenCompleteTextView.TokenListener {
	/*
	 * Used by SearchForm and MessageEdit.
	 */
	@NonNull
	private static Integer LIMIT = 10;

	@NonNull
	private List<Entity> mSeedEntities      = new ArrayList<>();
	@NonNull
	private Boolean      mSuggestInProgress = false;

	private ArrayAdapter mAdapter;
	private Context      mContext;
	private AbsListView  mListView;
	private UiController mUiController;

	private EditText   mSearchInput;
	private SearchView mSearchView;
	private View       mSearchProgress;
	private View       mSearchImage;

	private Integer                             mWatchResId;
	private Integer                             mLocationResId;
	private Integer                             mUserResId;
	private SimpleTextWatcher                   mTextWatcher;
	private String                              mSuggestInput;
	private String                              mPrefix;
	private TokenCompleteTextView.TokenListener mTokenListener;

	private DataController.SuggestScope mSuggestScope = DataController.SuggestScope.PATCHES;

	public EntitySuggestController(@NonNull Context context) {

		mContext = context;
		mAdapter = new SuggestArrayAdapter(mSeedEntities);

		final TypedValue resourceName = new TypedValue();
		if (context.getTheme().resolveAttribute(R.attr.iconWatch, resourceName, true)) {
			mWatchResId = resourceName.resourceId;
		}
		if (context.getTheme().resolveAttribute(R.attr.iconLocation, resourceName, true)) {
			mLocationResId = resourceName.resourceId;
		}
		if (context.getTheme().resolveAttribute(R.attr.iconUser, resourceName, true)) {
			mUserResId = resourceName.resourceId;
		}
	}

	public void init() {

		/* Bind to adapter */
		if (mSearchInput instanceof AirTokenCompleteTextView) {
			AirTokenCompleteTextView input = (AirTokenCompleteTextView) mSearchInput;
			input.allowDuplicates(false);
			input.setDeletionStyle(TokenCompleteTextView.TokenDeleteStyle.Clear);
			input.setTokenClickStyle(TokenCompleteTextView.TokenClickStyle.Select);
			input.setTokenListener(mTokenListener != null ? mTokenListener : this);
		}

		if (mSearchInput instanceof AutoCompleteTextView) {
			((AutoCompleteTextView) mSearchInput).setAdapter(mAdapter);
		}
		else if (mSearchInput instanceof EditText && mListView != null) {
			((ListView) mListView).setAdapter(mAdapter);
			mSearchInput.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(@NonNull Editable s) {
					textChanged(s.toString());
				}
			});
		}
		else if (mSearchView != null && mListView != null) {
			((ListView) mListView).setAdapter(mAdapter);
			mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
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
			mAdapter.getFilter().filter(input);
		}
		else {
			mAdapter.clear();
			if (mSeedEntities.size() > 0) {
				mAdapter.add(mSeedEntities.get(0));
			}
			mAdapter.notifyDataSetChanged();
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
		mSearchInput = input;
		return this;
	}

	@NonNull public EntitySuggestController setAdapter(ArrayAdapter adapter) {
		mAdapter = adapter;
		return this;
	}

	@NonNull public EntitySuggestController setTokenListener(TokenCompleteTextView.TokenListener tokenListener) {
		mTokenListener = tokenListener;
		return this;
	}

	@NonNull public EntitySuggestController setSearchProgress(View progress) {
		mSearchProgress = progress;
		return this;
	}

	@NonNull public EntitySuggestController setSearchImage(View image) {
		mSearchImage = image;
		return this;
	}

	@NonNull public EntitySuggestController setPrefix(String mPrefix) {
		this.mPrefix = mPrefix;
		return this;
	}

	@NonNull public EntitySuggestController setSuggestScope(DataController.SuggestScope suggestScope) {
		mSuggestScope = suggestScope;
		return this;
	}

	@NonNull public EntitySuggestController setListView(AbsListView listView) {
		mListView = listView;
		return this;
	}

	@NonNull public EntitySuggestController setSearchView(SearchView searchView) {
		mSearchView = searchView;
		return this;
	}

	@NonNull public EntitySuggestController setUiController(UiController uiController) {
		mUiController = uiController;
		return this;
	}

	public SearchView getSearchView() {
		return mSearchView;
	}

	public AbsListView getListView() {
		return mListView;
	}

	@NonNull public List<Entity> getSeedEntities() {
		return mSeedEntities;
	}

	public ArrayAdapter getAdapter() {
		return mAdapter;
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	private class SuggestArrayAdapter extends ArrayAdapter<Entity> {

		private Filter       mFilter;
		private List<Entity> mSeedEntities;

		private SuggestArrayAdapter(List<Entity> seedEntities) {
			this(mContext, 0, 0, seedEntities);
		}

		public SuggestArrayAdapter(@NonNull Context context, int resource, int textViewResourceId, List<Entity> seedEntities) {
			super(context, resource, textViewResourceId, new ArrayList<>(seedEntities));
			mSeedEntities = seedEntities;
		}

		@SuppressWarnings("ConstantConditions")
		@Override public View getView(int position, View convertView, ViewGroup parent) {

			View view = convertView;
			final ViewHolder holder;
			final Entity entity = (Entity) mAdapter.getItem(position);

			if (view == null) {
				view = LayoutInflater.from(mContext).inflate(R.layout.temp_user_search_item, null);
				holder = new ViewHolder();
				holder.photoView = (ImageLayout) view.findViewById(R.id.image_layout);
				holder.name = (TextView) view.findViewById(R.id.name);
				holder.subhead = (TextView) view.findViewById(R.id.subhead);
				holder.categoryName = (TextView) view.findViewById(R.id.category_name);
				holder.type = (TextView) view.findViewById(R.id.type);
				view.setTag(holder);
			}
			else {
				holder = (ViewHolder) view.getTag();
				if (entity.photo != null && holder.photoView.getTag() != null) {
					if (holder.photoView.getTag().equals(entity.photo.uriDirect()))
						return view;
				}
			}

			if (entity != null) {
				holder.data = entity;

				UI.setVisibility(holder.name, View.GONE);
				if (holder.name != null && entity.name != null && entity.name.length() > 0) {
					holder.name.setText(entity.name);
					UI.setVisibility(holder.name, View.VISIBLE);
				}

				UI.setVisibility(holder.type, View.GONE);
				if (entity instanceof Patch) {
					if (entity.type != null && !TextUtils.isEmpty(entity.type)) {
						holder.type.setText(Html.fromHtml(entity.type));
						UI.setVisibility(holder.type, View.VISIBLE);
					}
				}

				UI.setVisibility(holder.subhead, View.GONE);
				if (holder.subhead != null) {
					if (entity instanceof User) {
						User user = (User) entity;
						if (!TextUtils.isEmpty(user.area)) {
							holder.subhead.setText(user.area);
							UI.setVisibility(holder.subhead, View.VISIBLE);
						}
					}
				}

                /* Photo */

				if (holder.photoView != null) {
					holder.photoView.setImageWithEntity(entity);
				}

		        /* Indicator */

				UI.setVisibility(holder.indicator, View.INVISIBLE);
				if (holder.indicator != null) {
					if (entity instanceof Patch) {
						if (entity.reason != null) {
							if (entity.reason.equals(ReasonType.WATCH)) {
								holder.indicator.setImageResource(mWatchResId);
								UI.setVisibility(holder.indicator, View.VISIBLE);
							}
							else if (entity.reason.equals(ReasonType.LOCATION)) {
								holder.indicator.setImageResource(mLocationResId);
								UI.setVisibility(holder.indicator, View.VISIBLE);
							}
						}
					}
					else if (entity instanceof User) {
						holder.indicator.setImageResource(mUserResId);
						UI.setVisibility(holder.indicator, View.VISIBLE);
					}
				}
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
				if (!mSuggestInProgress) {

					mSuggestInProgress = true;
					if (chars != null && chars.length() > 0) {

						if (mUiController != null) {
							Patchr.mainThreadHandler.post(new Runnable() {
								@Override
								public void run() {
									mUiController.getBusyController().startProgressBar();
								}
							});
						}

						if (mSearchProgress != null) {
							mSearchInput.post(new Runnable() {
								@Override
								public void run() {
									mSearchImage.setVisibility(View.INVISIBLE);
									mSearchProgress.setVisibility(View.VISIBLE);
								}
							});
						}

						Thread.currentThread().setName("AsyncSuggestEntities");
						final AirLocation location = LocationManager.getInstance().getAirLocationLocked();
						ModelResult modelResult = DataController.getInstance().suggest(chars.toString().trim()
								, mSuggestScope
								, UserManager.getInstance().authenticated() ? UserManager.getInstance().getCurrentUser().id : null
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

					mSuggestInProgress = false;
				}
				return result;
			}

			@SuppressWarnings("unchecked")
			@Override
			protected void publishResults(CharSequence constraint, @NonNull FilterResults results) {
			    /*
			     * Called on UI thread.
                 */
				if (mUiController != null) {
					Patchr.mainThreadHandler.post(new Runnable() {
						@Override
						public void run() {
							mUiController.getBusyController().stopProgressBar();
						}
					});
				}

				if (mSearchProgress != null) {
					mSearchInput.post(new Runnable() {
						@Override
						public void run() {
							mSearchImage.setVisibility(View.VISIBLE);
							mSearchProgress.setVisibility(View.INVISIBLE);
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
