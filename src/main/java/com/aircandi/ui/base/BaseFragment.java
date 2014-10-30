package com.aircandi.ui.base;

import android.app.Activity;
import android.app.Fragment;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ListView;
import android.widget.ScrollView;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.components.BusProvider;
import com.aircandi.components.BusyManager;
import com.aircandi.components.Logger;
import com.aircandi.interfaces.IBind;
import com.aircandi.interfaces.IBusy;
import com.aircandi.interfaces.IForm;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Route;
import com.aircandi.ui.AircandiForm;
import com.aircandi.ui.components.EntitySuggestController;
import com.aircandi.ui.widgets.AirAutoCompleteTextView;
import com.aircandi.utilities.Json;
import com.aircandi.utilities.UI;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseFragment extends Fragment implements IForm, IBind {
	/*
	 * Fragment lifecycle
	 *
	 * - onAttach (activity may not be fully initialized)
	 * - onCreate
	 * - onCreateView
	 * - onActivityCreated (views created, safe to use findById)
	 * - onViewStateRestored
	 * - onStart (fragment becomes visible)
	 * - onResume
	 *
	 * - onPause
	 * - onStop
	 * - onSaveInstanceState
	 * - onDestroyView
	 * - onDestroy
	 * - onDetach
	 */

	public    Entity      mEntity;
	protected Resources   mResources;
	protected BusyManager mBusy;
	protected Boolean mIsVisible          = false;
	protected Boolean mFeed               = false;
	protected Boolean mSelfBindingEnabled = true;
	protected Boolean mFabEnabled         = true;

	protected Boolean mLoaded      = false; // Used to control busy feedback
	protected Integer mScrollState = OnScrollListener.SCROLL_STATE_IDLE;

	private   AirAutoCompleteTextView           mTo;
	private   View                              mToImage;
	private   View                              mToProgress;
	private   EntitySuggestController           mEntitySuggest;
	protected BaseActivity.FloatingActionButton mFab;

	protected BaseActivity.BubbleButton mBubbleButton;

	/* Resources */
	protected Integer mTitleResId;
	protected List<Integer> mMenuResIds = new ArrayList<Integer>();

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onAttach(Activity activity) {
		/* Called when the fragment has been associated with the activity. */
		super.onAttach(activity);
		Logger.d(this, "Fragment attached");
		if (mBusy == null) {
			mBusy = ((BaseActivity) getActivity()).getBusy();
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Logger.d(this, "Fragment created: contextId: " + this.hashCode());
		/*
		 * Triggers fragment menu construction in some android versions
		 * so mBusyManager must have already been created.
		 */
		setHasOptionsMenu(true);
		mResources = getResources();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Logger.d(this, "Fragment view created");
		if (getActivity() == null || getActivity().isFinishing()) return null;

		final View view = inflater.inflate(getLayoutId(), container, false);

		mBubbleButton = ((BaseActivity) getActivity()).getBubbleButton();
		mFab = ((BaseActivity) getActivity()).getFab();

		return view;
	}

	@Override
	public void onViewCreated(final View view, Bundle savedInstanceState) {

		if (view != null && view.getViewTreeObserver().isAlive()) {
			view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

				public void onGlobalLayout() {
					//noinspection deprecation
					view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
					onViewLayout();
				}
			});
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		Logger.d(this, "Activity for fragment created");
	}

	public void onViewLayout() {
		/*
		 * Called when initial view layout has completed and
		 * views have been measured and sized.
		 */
		Logger.d(this, "Fragment view layout completed");
	}

	@Override
	public void onDetach() {
		super.onDetach();
		Logger.d(this, "Fragment detached");
	}

	@Override
	public void onRefresh() {
		bind(BindingMode.MANUAL); // Called from Routing
	}

	@Override
	public void onAdd(Bundle extras) {}

	@Override
	public void onHelp() {}

	@Override
	public void onError() {}

	public abstract void onScollToTop();

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		Logger.d(this, "Configuration changed");
		super.onConfigurationChanged(newConfig);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void unpackIntent() {}

	@Override
	public void initialize(Bundle savedInstanceState) {}

	protected void preBind() {}

	@Override
	public void bind(BindingMode mode) {}

	protected void postBind() {}

	@Override
	public void draw(View view) {}

	protected void scrollToTop(final Object scroller) {
		if (scroller instanceof ListView) {
			getActivity().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					((ListView) scroller).setSelection(0);
				}
			});
		}
		else if (scroller instanceof ScrollView) {
			getActivity().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					((ScrollView) scroller).smoothScrollTo(0, 0);
				}
			});
		}
	}

	@Override
	public void share() {}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	public BaseFragment setActivityStream(Boolean activityStream) {
		mFeed = activityStream;
		return this;
	}

	public BaseFragment setSelfBindingEnabled(Boolean pagingEnabled) {
		mSelfBindingEnabled = pagingEnabled;
		return this;
	}

	public BaseFragment setTitleResId(Integer titleResId) {
		mTitleResId = titleResId;
		return this;
	}

	public BaseFragment setFabEnabled(Boolean fabEnabled) {
		mFabEnabled = fabEnabled;
		return this;
	}

	public Boolean isFeed() {
		return mFeed;
	}

	public Boolean isPagingEnabled() {
		return mSelfBindingEnabled;
	}

	public List<Integer> getMenuResIds() {
		return mMenuResIds;
	}

	public int getTitleResId() {
		return mTitleResId;
	}

	protected int getLayoutId() {
		return 0;
	}

	public Boolean getFabEnabled() {
		return mFabEnabled;
	}

	public Integer getScrollState() {
		return mScrollState;
	}

	/*--------------------------------------------------------------------------------------------
	 * Menus
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		/*
		 * This is triggered by onCreate in some android versions
		 * so any dependencies must have already been created.
		 */
		Logger.d(this, "Creating fragment options menu");

		for (Integer menuResId : mMenuResIds) {
			inflater.inflate(menuResId, menu);
		}

		/* If fragment has a search action then configure it */

		final MenuItem item = menu.findItem(R.id.search);
		if (item != null) {
			mTo = (AirAutoCompleteTextView) item.getActionView().findViewById(R.id.search_input);
			mToImage = item.getActionView().findViewById(R.id.search_image);
			mToProgress = item.getActionView().findViewById(R.id.search_progress);

			if (mEntitySuggest == null) {
				mEntitySuggest = new EntitySuggestController(getActivity());
			}

			mEntitySuggest.setInput((AutoCompleteTextView) mTo);
			mEntitySuggest.setSearchImage(mToImage);
			mEntitySuggest.setSearchProgress(mToProgress);
			mEntitySuggest.init();

			mTo.setOnItemClickListener(new AdapterView.OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					item.collapseActionView();
					Entity entity = (Entity) mTo.getAdapter().getItem(position);

					Bundle extras = new Bundle();
					if (entity.synthetic) {
						final String jsonEntity = Json.objectToJson(entity);
						extras.putString(Constants.EXTRA_ENTITY, jsonEntity);
						extras.putBoolean(Constants.EXTRA_UPSIZE_SYNTHETIC, true);
					}
					Patchr.dispatch.route(getActivity(), Route.BROWSE, entity, null, extras);
				}
			});

			item.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {

				@Override
				public boolean onMenuItemActionExpand(MenuItem item) {

					mTo.setText(null);

					mTo.post(new Runnable() {
						@Override
						public void run() {
							mTo.requestFocus();
							UI.showSoftInput(mTo);
						}
					});
					return true;
				}

				@Override
				public boolean onMenuItemActionCollapse(MenuItem menuItem) {
					UI.hideSoftInput(mTo);
					return true;
				}
			});
		}

		super.onCreateOptionsMenu(menu, inflater);
	}

	@SuppressWarnings("ucd")
	public boolean onCreatePopupMenu(android.view.Menu menu) {
		Logger.d(this, "Creating fragment options menu");
		return Patchr.getInstance().getMenuManager().onCreatePopupMenu(getActivity(), menu, mEntity);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		Logger.d(this, "Preparing fragment options menu");
		super.onPrepareOptionsMenu(menu);
	}

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onStart() {
		Logger.d(this, "Fragment start");
		Patchr.tracker.fragmentStart(this);
		super.onStart();
	}

	@Override
	public void onResume() {
		Logger.d(this, "Fragment resume");
		BusProvider.getInstance().register(this);
		if (getActivity() != null && getActivity() instanceof AircandiForm) {
			((AircandiForm) getActivity()).updateNotificationIndicator();
		}

		super.onResume();
	}

	@Override
	public void onPause() {
		/*
		 * user might be leaving fragment so do any work needed
		 * because they might not come back.
		 */
		Logger.d(this, "Fragment pause");
		BusProvider.getInstance().unregister(this);
		super.onPause();
	}

	@Override
	public void onStop() {
		Logger.d(this, "Fragment stop");
		super.onStop();
	}

	@Override
	public void onDestroyView() {
		Logger.d(this, "Fragment destroy view");
		super.onDestroy();
	}

	@Override
	public void onDestroy() {
		Logger.d(this, "Fragment destroy");
		super.onDestroy();
	}
}