package com.aircandi.ui;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.aircandi.Aircandi;
import com.aircandi.R;
import com.aircandi.components.Extras;
import com.aircandi.components.Logger;
import com.aircandi.components.MessagingManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.monitors.SimpleMonitor;
import com.aircandi.objects.Route;
import com.aircandi.objects.ServiceActivity;
import com.aircandi.queries.IQuery;
import com.aircandi.ui.base.BaseFragment;
import com.aircandi.ui.base.IBusy.BusyAction;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.DateTime.IntervalContext;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.UI;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ActivityFragment extends BaseFragment implements OnClickListener {

	protected ListView mListView;
	protected View     mLoading;
	protected Integer  mLastViewedPosition; // NO_UCD (unused code)
	protected Integer  mTopOffset; // NO_UCD (unused code)

	protected List<ServiceActivity> mActivities = new ArrayList<ServiceActivity>();
	private   IQuery        mQuery;
	private   SimpleMonitor mMonitor;
	protected ListAdapter   mAdapter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		/*
		 * The listview scroll position seems to be preserved between destroy view
		 * and create view. Probably falls into the buck of view properties that are
		 * auto restored by android.
		 */
		if (view == null) return null;

		mListView = (ListView) view.findViewById(R.id.list);
		mLoading = LayoutInflater.from(getActivity()).inflate(R.layout.temp_list_item_loading, null);

		/*
		 * Triggers data fetch because endless wrapper calls cacheInBackground()
		 * when first created.
		 */
		mAdapter = new ListAdapter(mActivities);
		/*
		 * Bind adapter to UI triggers view generation but we might not
		 * have any data yet. When a new chuck of data is added to mEntities,
		 * notifyDataSetChanged is called on the adapter when then lets the
		 * UI know to repaint.
		 */
		if (mListView != null) {
			mListView.setAdapter(mAdapter);
		}

		return view;
	}

	@Override
	protected void preBind() {
	}

	@Override
	public void bind(final BindingMode mode) {
		Logger.d(this, "Binding called: mode = " + mode.name().toLowerCase(Locale.US));

		if (Aircandi.getInstance().getCurrentUser().isAnonymous()) return;

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				preBind();
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncBindActivities");
				ModelResult result = new ModelResult();
				if (mode == BindingMode.MANUAL || (mMonitor.isChanged() && mMonitor.activity)) {
					mBusy.showBusy(mLoaded ? BusyAction.Refreshing : BusyAction.Loading);
					result = mQuery.execute(0, null);
				}
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				ModelResult result = (ModelResult) response;

				mBusy.hideBusy(false);
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					if (result.data != null) {
						mActivities.clear();
						mAdapter.setNotifyOnChange(false);
						mAdapter.clear();

						for (ServiceActivity activity : (List<ServiceActivity>) result.data) {
							mAdapter.add(activity);
						}

						mAdapter.sort(new ServiceActivity.SortBySortDate());
						draw();
					}
					postBind();
					mLoaded = true;
				}
				else {
					Errors.handleError(getActivity(), result.serviceResponse);
				}
				onActivityComplete();
			}
		}.execute();
	}

	@Override
	protected void postBind() {
		/*
		 * Clear notifications and activity indicator if visible and acting as an activity stream
		 */
		if (mActivityStream) {
			MessagingManager.getInstance().setNewActivity(false);
			((AircandiForm) getActivity()).updateActivityAlert();
			MessagingManager.getInstance().cancelNotifications();
		}
	}

	@Override
	public void draw() {
		mAdapter.notifyDataSetChanged();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/
	@Override
	public void onClick(View v) {

		final ServiceActivity activity = (ServiceActivity) ((ViewHolder) v.getTag()).data;

		if (activity.action.entity != null) {
			Extras extras = new Extras().setForceRefresh(true);
			Aircandi.dispatch.route(getActivity(), Route.BROWSE, activity.action.entity, null, extras.getExtras());
		}
		else {
			Aircandi.dispatch.intent(getActivity(), activity.intent);
		}
	}

	@Override
	public void onRefresh() {
		saveListPosition();
		super.onRefresh();
	}

	public void onMoreButtonClick(View view) {
		lazyLoad();
	}

	@Override
	public void onScollToTop() {
		scrollToTop(mListView);
	}

	@Override
	public void setMenuVisibility(final boolean visible) {
		super.setMenuVisibility(visible);
		doMenuVisibility(visible);
	}

	public void doMenuVisibility(boolean visible) {
		mIsVisible = visible;
		if (mSelfBindingEnabled && mIsVisible) {
			bind(BindingMode.AUTO);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/
	public void onActivityComplete() {
		if (getActivity() != null) {
			showButtonSpecial(mAdapter.getCount() == 0, null, null);
		}
	}

	protected void lazyLoad() {

		final ViewSwitcher switcher = (ViewSwitcher) mLoading.findViewById(R.id.animator_more);

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				switcher.setDisplayedChild(1);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncLazyLoadList");
				ModelResult result = mQuery.execute(mActivities.size(), null);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				ModelResult result = (ModelResult) response;

				if (result.serviceResponse.responseCode != ResponseCode.SUCCESS) {
					Errors.handleError(getActivity(), result.serviceResponse);
				}
				else {
					if (result.data != null) {
						for (ServiceActivity activity : (List<ServiceActivity>) result.data) {
							mAdapter.add(activity);
						}
						mAdapter.sort(new ServiceActivity.SortBySortDate());
						draw();
					}
				}
				switcher.setDisplayedChild(0);
			}
		}.execute();
	}

	protected void saveListPosition() {
		mLastViewedPosition = mListView.getFirstVisiblePosition();
		View view = mListView.getChildAt(0);
		mTopOffset = (view == null) ? 0 : view.getTop();
	}

	@Override
	protected int getLayoutId() {
		return R.layout.activity_list_fragment;
	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/
	public ActivityFragment setMonitor(SimpleMonitor monitor) {
		mMonitor = monitor;
		return this;
	}

	public ActivityFragment setQuery(IQuery query) {
		mQuery = query;
		return this;
	}

	/*--------------------------------------------------------------------------------------------
	 * Menus
	 *--------------------------------------------------------------------------------------------*/ 	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/
	@Override
	public void onPause() {
		super.onPause();
		saveListPosition();
	}

	@Override
	public void onResume() {
		/*
		 * Called when fragment is attached and active but might
		 * not be visible to the user yet. ViewPager has logic to
		 * pre-create fragments even if they aren't visible yet.
		 */
		super.onResume();
		doResume();
	}

	protected void doResume() {
		if (mSelfBindingEnabled) {
			bind(BindingMode.AUTO);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Misc
	 *--------------------------------------------------------------------------------------------*/ 	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/    public class ListAdapter extends ArrayAdapter<ServiceActivity> {

		private ListAdapter(List<ServiceActivity> items) {
			super(getActivity(), 0, items);
		}

		@Override
		public int getCount() {
			if (mActivities == null || mQuery == null) return 0;
			return mActivities.size() + (mQuery.isMore() ? 1 : 0);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			if (position == mActivities.size()) return mLoading;

			View view = convertView;
			final ViewHolder holder;
			final ServiceActivity activity = mActivities.get(position);

			if (view == null || view.findViewById(R.id.animator_more) != null) {
				view = LayoutInflater.from(getActivity()).inflate(R.layout.temp_listitem_activity, null);
				holder = new ViewHolder();

				holder.name = (TextView) view.findViewById(R.id.name);
				holder.subtitle = (TextView) view.findViewById(R.id.subtitle);
				holder.type = (TextView) view.findViewById(R.id.type);
				holder.description = (TextView) view.findViewById(R.id.description);
				holder.date = (TextView) view.findViewById(R.id.timesince);
				holder.byPhotoView = (AirImageView) view.findViewById(R.id.user_photo);

				holder.shortcutOne = view.findViewById(R.id.shortcut_one);
				if (holder.shortcutOne != null) {
					holder.photoViewOne = (AirImageView) holder.shortcutOne.findViewById(R.id.photo_one);
					holder.nameOne = (TextView) holder.shortcutOne.findViewById(R.id.name_one);
				}

				view.setTag(holder);
			}
			else {
				holder = (ViewHolder) view.getTag();
			}

			if (activity != null) {

				drawListItem(activity, view, holder);

				holder.data = activity;
				view.setClickable(true);
				view.setOnClickListener(ActivityFragment.this);
			}
			return view;
		}

		@Override
		public ServiceActivity getItem(int position) {
			return mActivities.get(position);
		}

		@Override
		public boolean areAllItemsEnabled() {
			return false;
		}

		@Override
		public boolean isEnabled(int position) {
			return true;
		}

	}

	protected void drawListItem(ServiceActivity activity, View view, ViewHolder holder) {

		UI.setVisibility(holder.byPhotoView, View.INVISIBLE);
		if (holder.byPhotoView != null) {
			if (activity.photoBy != null) {
				if (holder.byPhotoView.getPhoto() == null || !activity.photoBy.getUri().equals(holder.byPhotoView.getPhoto().getUri())) {
					if (!activity.action.user.isAnonymous() && !activity.action.user.isAdmin()) {
						holder.byPhotoView.setTag(activity.action.user);
						holder.byPhotoView.setClickable(true);
					}
					else {
						holder.byPhotoView.setClickable(false);
					}
					UI.drawPhoto(holder.byPhotoView, activity.photoBy);
				}
				UI.setVisibility(holder.byPhotoView, View.VISIBLE);
			}
		}

		UI.setVisibility(holder.name, View.GONE);
		if (holder.name != null && !TextUtils.isEmpty(activity.title)) {
			holder.name.setText(activity.title);
			UI.setVisibility(holder.name, View.VISIBLE);
		}

		UI.setVisibility(holder.subtitle, View.GONE);
		if (holder.subtitle != null && !TextUtils.isEmpty(activity.subtitle)) {
			holder.subtitle.setText(activity.subtitle);
			UI.setVisibility(holder.subtitle, View.VISIBLE);
		}

		/* Shortcuts */

		UI.setVisibility(holder.shortcutOne, View.GONE);
		if (holder.shortcutOne != null) {
			if (activity.photoOne != null) {
				if (holder.photoViewOne.getPhoto() == null || !activity.photoOne.getUri().equals(holder.photoViewOne.getPhoto().getUri())) {
					UI.drawPhoto(holder.photoViewOne, activity.photoOne);
				}
				holder.nameOne.setText(activity.photoOne.name);
				holder.shortcutOne.setTag(activity.photoOne.shortcut);
				UI.setVisibility(holder.shortcutOne, View.VISIBLE);
			}
		}

		UI.setVisibility(holder.description, View.GONE);
		if (holder.description != null && !TextUtils.isEmpty(activity.description)) {
			holder.description.setMaxLines(5);
			holder.description.setText(activity.description);
			UI.setVisibility(holder.description, View.VISIBLE);
		}

		UI.setVisibility(holder.date, View.GONE);
		if (holder.date != null && activity.activityDate != null) {
			holder.date.setText(DateTime.interval(activity.activityDate.longValue(), DateTime.nowDate().getTime(), IntervalContext.PAST));
			UI.setVisibility(holder.date, View.VISIBLE);
		}

	}

	public static class ViewHolder {

		public TextView     name;
		public AirImageView byPhotoView;

		public TextView subtitle;
		public TextView description;
		public TextView type; // NO_UCD (unused code)
		public TextView date;

		@SuppressWarnings("ucd")
		public String photoUri;        // Used for verification after fetching image
		public Object data;            // object binding to

		public View         shortcutOne;
		public AirImageView photoViewOne;
		public TextView     nameOne;

	}

}