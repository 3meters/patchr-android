package com.aircandi.ui.components;

import android.annotation.TargetApi;
import android.content.Context;
import android.text.Html;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import com.aircandi.Aircandi;
import com.aircandi.R;
import com.aircandi.components.EntityManager;
import com.aircandi.components.LocationManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.StringManager;
import com.aircandi.objects.AirLocation;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Photo;
import com.aircandi.objects.Place;
import com.aircandi.objects.Place.ReasonType;
import com.aircandi.objects.User;
import com.aircandi.ui.base.BaseActivity.SimpleTextWatcher;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.ui.widgets.AirTokenCompleteTextView;
import com.aircandi.ui.widgets.TokenCompleteTextView;
import com.aircandi.utilities.Json;
import com.aircandi.utilities.UI;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class EntitySuggestController implements TokenCompleteTextView.TokenListener {

	private static Integer LIMIT = 10;

	private List<Entity> mSeedEntities      = new ArrayList<Entity>();
	private Boolean      mSuggestInProgress = false;
	private ArrayAdapter                        mAdapter;
	private Context                             mContext;
	private AbsListView                         mListView;
	private AutoCompleteTextView                mInput;
	private View                                mSearchProgress;
	private View                                mSearchImage;
	private Integer                             mWatchResId;
	private Integer                             mLocationResId;
	private Integer                             mUserResId;
	private SimpleTextWatcher                   mTextWatcher;
	private String                              mSuggestInput;
	private String                              mPrefix;
	private TokenCompleteTextView.TokenListener mTokenListener;

	private EntityManager.SuggestScope mSuggestScope = EntityManager.SuggestScope.PLACES;

	public EntitySuggestController(Context context) {
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
		if (mInput instanceof AirTokenCompleteTextView) {
			AirTokenCompleteTextView input = (AirTokenCompleteTextView) mInput;
			input.allowDuplicates(false);
			input.setDeletionStyle(TokenCompleteTextView.TokenDeleteStyle.Clear);
			input.setTokenClickStyle(TokenCompleteTextView.TokenClickStyle.Select);
			input.setTokenListener(mTokenListener != null ? mTokenListener : this);
		}
		mInput.setAdapter(mAdapter);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onTokenAdded(Object o) {
		Entity entity = (Entity) o;

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
	}

	@Override
	public void onTokenRemoved(Object o) {
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	public EntitySuggestController setInput(AutoCompleteTextView input) {
		mInput = input;
		return this;
	}

	public EntitySuggestController setAdapter(ArrayAdapter adapter) {
		mAdapter = adapter;
		return this;
	}

	public EntitySuggestController setTokenListener(TokenCompleteTextView.TokenListener tokenListener) {
		mTokenListener = tokenListener;
		return this;
	}

	public EntitySuggestController setSearchProgress(View progress) {
		mSearchProgress = progress;
		return this;
	}

	public EntitySuggestController setSearchImage(View image) {
		mSearchImage = image;
		return this;
	}

	public EntitySuggestController setPrefix(String mPrefix) {
		this.mPrefix = mPrefix;
		return this;
	}

	public EntitySuggestController setSuggestScope(EntityManager.SuggestScope suggestScope) {
		mSuggestScope = suggestScope;
		return this;
	}

	public List<Entity> getSeedEntities() {
		return mSeedEntities;
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

		public SuggestArrayAdapter(Context context, int resource, int textViewResourceId, List<Entity> seedEntities) {
			super(context, resource, textViewResourceId, new ArrayList<Entity>(seedEntities));
			mSeedEntities = seedEntities;
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
				if (holder.subtitle != null) {
					if (entity instanceof Place) {
						Place place = (Place) entity;
						String address = place.getAddressString(false);

						if (!TextUtils.isEmpty(address)) {
							holder.subtitle.setText(address);
							UI.setVisibility(holder.subtitle, View.VISIBLE);
						}
						else if (place.category != null && !TextUtils.isEmpty(place.category.name)) {
							holder.subtitle.setText(Html.fromHtml(place.category.name));
							UI.setVisibility(holder.subtitle, View.VISIBLE);
						}
					}
					else if (entity instanceof User) {
						User user = (User) entity;
						if (!TextUtils.isEmpty(user.area)) {
							holder.subtitle.setText(user.area);
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
					if (entity instanceof Place) {
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

		@Override
		public Filter getFilter() {
			if (mFilter == null) {
				mFilter = new SuggestFilter();
			}
			return mFilter;
		}

		private class SuggestFilter extends Filter {

			@Override
			protected FilterResults performFiltering(CharSequence chars) {
			    /*
			     * Called on background thread.
                 */
				FilterResults result = new FilterResults();
				if (!mSuggestInProgress) {

					mSuggestInProgress = true;
					if (chars != null && chars.length() > 0) {

						if (mSearchProgress != null) {
							mInput.post(new Runnable() {
								@Override
								public void run() {
									mSearchImage.setVisibility(View.INVISIBLE);
									mSearchProgress.setVisibility(View.VISIBLE);
								}
							});
						}

						Thread.currentThread().setName("AsyncSuggestEntities");
						final AirLocation location = LocationManager.getInstance().getAirLocationLocked();
						ModelResult modelResult = Aircandi.getInstance().getEntityManager().suggest(chars.toString().trim()
								, mSuggestScope, Aircandi.getInstance().getCurrentUser().id
								, location
								, LIMIT);

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
			protected void publishResults(CharSequence constraint, FilterResults results) {
                /*
                 * Called on UI thread.
                 */
				if (mSearchProgress != null) {
					mInput.post(new Runnable() {
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
		public int compare(Entity object1, Entity object2) {

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

		public TextView     name;
		public AirImageView photoView;
		public ImageView    indicator;
		public TextView     subtitle;
		public String       photoUri;    // Used for verification after fetching image // NO_UCD (unused code)
		public Object       data;        // object binding to
	}
}
