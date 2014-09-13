package com.aircandi.ui;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Picture;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.aircandi.Aircandi;
import com.aircandi.Aircandi.ThemeTone;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.AndroidManager;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ModelResult;
import com.aircandi.components.StringManager;
import com.aircandi.controllers.IEntityController;
import com.aircandi.monitors.IMonitor;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Link.Direction;
import com.aircandi.objects.Place;
import com.aircandi.objects.Route;
import com.aircandi.objects.ServiceBase;
import com.aircandi.objects.Shortcut;
import com.aircandi.objects.Shortcut.InstallStatus;
import com.aircandi.objects.ShortcutMeta;
import com.aircandi.objects.ShortcutSettings;
import com.aircandi.objects.User;
import com.aircandi.queries.IQuery;
import com.aircandi.ui.base.BaseFragment;
import com.aircandi.ui.base.IBusy.BusyAction;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.ui.widgets.FlowLayout;
import com.aircandi.ui.widgets.SectionLayout;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.UI;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("ucd")
public class ShortcutFragment extends BaseFragment {

	protected static int mScrollX = 0;
	protected static int mScrollY = -1;

	protected ScrollView mScrollView;
	protected ViewGroup  mHolderShortcuts;

	private Integer mEmptyMessageResId;

	protected String  mShortcutType;
	protected Integer mShortcutWidthPixels;
	protected Integer mShortcutHeightPixels;
	protected Integer mShortcutCount = 0;

	protected List<View>       mCurrentScrap = new ArrayList<View>();
	protected List<FlowLayout> mFlowLayouts  = new ArrayList<FlowLayout>();

	protected Entity   mEntity;
	private   IQuery   mQuery;
	@SuppressWarnings("unused")
	private   IMonitor mMonitor;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (savedInstanceState != null) {
			Logger.d(this, "Fragment restoring state");
			final int[] position = savedInstanceState.getIntArray("ARTICLE_SCROLL_POSITION");
			if (position != null && mScrollView != null) {
				mScrollView.post(new Runnable() {
					@Override
					public void run() {
						mScrollView.scrollTo(position[0], position[1]);
					}
				});
			}
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		if (view == null) return view;

		mScrollView = (ScrollView) view.findViewById(R.id.scroll_view);

		return view;
	}

	@Override
	public void onDestroyView() {
		recycleViews();
		super.onDestroyView();
	}

	@Override
	public void preBind() {
		mEntity = Aircandi.getInstance().getCurrentUser();
	}

	@Override
	public void bind(final BindingMode mode) {
		Logger.d(this, "Binding called: mode = " + mode.name().toLowerCase(Locale.US));

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				preBind();
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncGetEntityForShortcuts");
				ModelResult result = new ModelResult();
				if (mode == BindingMode.MANUAL || !mLoaded) {
					mBusy.showBusy(mLoaded ? BusyAction.Refreshing : BusyAction.Loading);
					result = mQuery.execute(null, null);
				}
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				ModelResult result = (ModelResult) response;

				mBusy.hideBusy(false);
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					if (result.data != null) {
						mEntity = (Entity) Aircandi.getInstance().getCurrentUser();
					}
					draw();
					mLoaded = true;
				}
				else {
					Errors.handleError(getActivity(), result.serviceResponse);
				}
				onActivityComplete();
			}

		}.execute();
	}

	/*--------------------------------------------------------------------------------------------
	 * UI routines
	 *--------------------------------------------------------------------------------------------*/
	@Override
	public void draw() {

		Logger.d(this, "Fragment drawing");

		if (getView() == null) return;

		if (mEntity == null) return;

		/* Clear shortcut holder */

		mHolderShortcuts = (ViewGroup) getView().findViewById(R.id.holder_shortcuts);
		mHolderShortcuts.removeAllViews();
		mShortcutCount = 0;
		mFlowLayouts.clear();

		if (mShortcutType.equals(Constants.TYPE_LINK_CREATE)) {

			/* Shortcuts for place entities created by user */
			ShortcutSettings settings = new ShortcutSettings(Constants.TYPE_LINK_CREATE, Constants.SCHEMA_ENTITY_PLACE, Direction.out, false, false);
			settings.appClass = Place.class;
			List<Shortcut> shortcuts = mEntity.getShortcuts(settings, new ServiceBase.SortByPositionSortDate(), null);
			if (shortcuts.size() > 0) {
				mShortcutCount += shortcuts.size();
				prepareShortcuts(shortcuts
						, settings
						, R.string.label_section_places_created
						, R.string.label_link_places_more
						, mResources.getInteger(R.integer.limit_shortcuts_flow_create)
						, R.id.holder_shortcuts
						, R.layout.widget_shortcut);
			}

			/* Shortcuts for picture entities created by user */
			settings = new ShortcutSettings(Constants.TYPE_LINK_CREATE, Constants.SCHEMA_ENTITY_PICTURE, Direction.out, false, false);
			settings.appClass = Picture.class;
			shortcuts = mEntity.getShortcuts(settings, new ServiceBase.SortByPositionSortDate(), null);
			if (shortcuts.size() > 0) {
				mShortcutCount += shortcuts.size();
				prepareShortcuts(shortcuts
						, settings
						, R.string.label_section_pictures_created
						, R.string.label_link_pictures_more
						, mResources.getInteger(R.integer.limit_shortcuts_flow_create)
						, R.id.holder_shortcuts
						, R.layout.widget_shortcut);
			}
		}

		else if (mShortcutType.equals(Constants.TYPE_LINK_WATCH)) {

			/* Watching places */
			ShortcutSettings settings = new ShortcutSettings(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_PLACE, Direction.out, false, false);
			settings.appClass = Place.class;
			List<Shortcut> shortcuts = mEntity.getShortcuts(settings, new ServiceBase.SortByPositionSortDate(), null);
			if (shortcuts.size() > 0) {
				mShortcutCount += shortcuts.size();
				prepareShortcuts(shortcuts
						, settings
						, R.string.label_section_places_watching
						, R.string.label_link_places_more
						, mResources.getInteger(R.integer.limit_shortcuts_flow_watch)
						, R.id.holder_shortcuts
						, R.layout.widget_shortcut);
			}

			/* Watching pictures */
			settings = new ShortcutSettings(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_PICTURE, Direction.out, false, false);
			settings.appClass = Picture.class;
			shortcuts = mEntity.getShortcuts(settings, new ServiceBase.SortByPositionSortDate(), null);
			if (shortcuts.size() > 0) {
				mShortcutCount += shortcuts.size();
				prepareShortcuts(shortcuts
						, settings
						, R.string.label_section_pictures_watching
						, R.string.label_link_pictures_more
						, mResources.getInteger(R.integer.limit_shortcuts_flow_watch)
						, R.id.holder_shortcuts
						, R.layout.widget_shortcut);
			}

			/* Watching users */
			settings = new ShortcutSettings(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_USER, Direction.out, false, false);
			settings.appClass = User.class;
			shortcuts = mEntity.getShortcuts(settings, new ServiceBase.SortByPositionSortDate(), null);
			if (shortcuts.size() > 0) {
				mShortcutCount += shortcuts.size();
				prepareShortcuts(shortcuts
						, settings
						, R.string.label_section_users_watching
						, R.string.label_link_users_more
						, mResources.getInteger(R.integer.limit_shortcuts_flow_watch)
						, R.id.holder_shortcuts
						, R.layout.widget_shortcut);
			}
		}

		if (mScrollView != null) {
			mScrollView.setVisibility(View.VISIBLE);
			mScrollView.post(new Runnable() {
				@Override
				public void run() {
					mScrollView.scrollTo(mScrollX, mScrollY);
				}
			});
		}
	}

	protected void prepareShortcuts(List<Shortcut> shortcuts
			, ShortcutSettings settings
			, Integer titleResId
			, Integer moreResId
			, Integer flowLimit
			, Integer holderId
			, Integer flowItemResId) {

		View holder = LayoutInflater.from(getActivity()).inflate(R.layout.section_shortcuts, null);
		SectionLayout section = (SectionLayout) holder.findViewById(R.id.section_layout_shortcuts);
		if (titleResId != null) {
			section.setHeaderTitle(StringManager.getString(titleResId));
		}
		else {
			if (section.getHeader() != null) {
				section.getHeader().setVisibility(View.GONE);
			}
		}

		if (shortcuts.size() > flowLimit) {

			IEntityController controller = Aircandi.getInstance().getControllerForSchema(settings.linkTargetSchema);
			Intent intent = controller.viewFor(getActivity()
					, null
					, mEntity.id
					, settings.linkType
					, settings.direction
					, (titleResId != null) ? StringManager.getString(titleResId) : null
					, false
					, false);
			/*
			 * Make button shortcut
			 */
			Shortcut shortcut = Shortcut.builder(mEntity
					, Constants.SCHEMA_INTENT
					, Constants.TYPE_APP_INTENT
					, null
					, StringManager.getString(moreResId)
					, null
					, "img_more"
					, 10
					, false
					, true);
			shortcut.intent = intent;
			shortcuts = shortcuts.subList(0, flowLimit - 1);
			shortcuts.add(shortcut);
		}

		final FlowLayout flow = (FlowLayout) section.findViewById(R.id.flow_shortcuts);
		flowShortcuts(flow, (shortcuts.size() > flowLimit)
		                    ? shortcuts.subList(0, flowLimit)
		                    : shortcuts, flowItemResId);

		if (getView() != null) {
			mFlowLayouts.add(flow);
			((ViewGroup) getView().findViewById(holderId)).addView(holder);
		}
	}

	private void flowShortcuts(FlowLayout layout, List<Shortcut> shortcuts, Integer viewResId) {

		layout.removeAllViews();

		final DisplayMetrics metrics = getResources().getDisplayMetrics();
		final View parentView = (View) layout.getParent();
		Integer layoutWidthPixels = metrics.widthPixels
				- (parentView.getPaddingLeft() + parentView.getPaddingRight() + layout.getPaddingLeft() + layout.getPaddingRight());
		final Integer bonusPadding = UI.getRawPixelsForDisplayPixels(20f);
		layoutWidthPixels -= bonusPadding;

		final Integer spacing = 3;
		final Integer spacingHorizontalPixels = UI.getRawPixelsForDisplayPixels((float) spacing);
		final Integer spacingVerticalPixels = UI.getRawPixelsForDisplayPixels((float) spacing);

		Integer desiredWidthPixels = (int) (metrics.density * 75);
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			desiredWidthPixels = (int) (metrics.density * 75);
		}

		final Integer candiCount = (int) Math.ceil(layoutWidthPixels / desiredWidthPixels);
		mShortcutWidthPixels = (layoutWidthPixels - (spacingHorizontalPixels * (candiCount - 1))) / candiCount;
		mShortcutHeightPixels = (mShortcutWidthPixels * 1);

		layout.setSpacingHorizontal(spacingHorizontalPixels);
		layout.setSpacingVertical(spacingVerticalPixels);

		for (Shortcut shortcut : shortcuts) {

			if (!shortcut.isActive(mEntity)) {
				continue;
			}

			View view = getScrapView();
			if (view == null) {
				view = LayoutInflater.from(getActivity()).inflate(viewResId, null);
			}

			AirImageView photoView = (AirImageView) view.findViewById(R.id.shortcut_photo);

			TextView name = (TextView) view.findViewById(R.id.shortcut_name);
			TextView badgeUpper = (TextView) view.findViewById(R.id.badge_upper);
			TextView badgeLower = (TextView) view.findViewById(R.id.badge_lower);
			ImageView indicator = (ImageView) view.findViewById(R.id.indicator);
			if (indicator != null) {
				indicator.setVisibility(View.GONE);
			}
			if (badgeUpper != null) {
				badgeUpper.setVisibility(View.GONE);
			}
			if (badgeLower != null) {
				badgeLower.setVisibility(View.GONE);
			}
			name.setVisibility(View.GONE);

			view.setTag(shortcut);

			if (shortcut.group != null && shortcut.group.size() > 1) {
				badgeUpper.setText(String.valueOf(shortcut.group.size()));
				badgeUpper.setVisibility(View.VISIBLE);

				if (shortcut.app.equals(Constants.TYPE_APP_FACEBOOK)) {
					badgeLower.setBackgroundResource(Aircandi.themeTone.equals(ThemeTone.LIGHT) ? R.drawable.ic_action_facebook_light
					                                                                            : R.drawable.ic_action_facebook_dark);
				}
				else if (shortcut.app.equals(Constants.TYPE_APP_TWITTER)) {
					badgeLower.setBackgroundResource(Aircandi.themeTone.equals(ThemeTone.LIGHT) ? R.drawable.ic_action_twitter_light
					                                                                            : R.drawable.ic_action_twitter_dark);
				}
				else if (shortcut.app.equals(Constants.TYPE_APP_WEBSITE)) {
					badgeLower.setBackgroundResource(Aircandi.themeTone.equals(ThemeTone.LIGHT) ? R.drawable.ic_action_website_light
					                                                                            : R.drawable.ic_action_website_dark);
				}
				else if (shortcut.app.equals(Constants.TYPE_APP_FOURSQUARE)) {
					badgeLower.setBackgroundResource(Aircandi.themeTone.equals(ThemeTone.LIGHT) ? R.drawable.ic_action_foursquare_light
					                                                                            : R.drawable.ic_action_foursquare_dark);
				}
				badgeLower.setVisibility(View.VISIBLE);
			}
			else if (shortcut.count != null && shortcut.count > 0) {
				badgeUpper.setTag(shortcut);
				badgeUpper.setText(String.valueOf(shortcut.count));
				badgeUpper.setVisibility(View.VISIBLE);
			}

			/* Show hint if source has app that hasn't been installed */
			final ShortcutMeta meta = Shortcut.shortcutMeta.get(shortcut.app);
			if (meta == null || meta.installStatus == InstallStatus.NONE || meta.installStatus == InstallStatus.LATER) {
				if (AndroidManager.hasIntentSupport(shortcut.app)
						&& AndroidManager.appExists(shortcut.app)
						&& !AndroidManager.isAppInstalled(shortcut.app)) {

					/* Show install indicator */
					if (indicator != null) {
						indicator.setVisibility(View.VISIBLE);
					}
				}
			}

			if (shortcut.name != null && !shortcut.name.equals("")) {
				name.setText(shortcut.name);
				name.setVisibility(View.VISIBLE);
			}

			photoView.setTag(shortcut.getPhoto());
			photoView.setSizeHint(mShortcutWidthPixels);
			view.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View view) {
					final Shortcut shortcut = (Shortcut) view.getTag();
					Aircandi.dispatch.route(getActivity(), Route.SHORTCUT, null, shortcut, null);
				}
			});

			UI.drawPhoto(photoView, shortcut.getPhoto());

			FlowLayout.LayoutParams params = new FlowLayout.LayoutParams(mShortcutWidthPixels, LayoutParams.WRAP_CONTENT);
			params.setCenterHorizontal(false);
			view.setLayoutParams(params);

			RelativeLayout.LayoutParams paramsImage = new RelativeLayout.LayoutParams(mShortcutWidthPixels, mShortcutHeightPixels);
			photoView.setLayoutParams(paramsImage);

			layout.addView(view);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/
	@Override
	public void onSaveInstanceState(Bundle outState) {
		/*
		 * I think this only gets called if the fragment is detached.
		 */
		super.onSaveInstanceState(outState);
		Logger.d(this, "Fragment saving state");
		if (mScrollView != null) {
			outState.putIntArray("ARTICLE_SCROLL_POSITION", new int[]{mScrollView.getScrollX(), mScrollView.getScrollY()});
		}
	}

	@Override
	public void onScollToTop() {
		scrollToTop(mScrollView);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/
	public void onActivityComplete() {
		showButtonSpecial((mLoaded && mShortcutCount == 0), mEmptyMessageResId, null);
	}

	protected View getScrapView() {
		List<View> scrapViews = mCurrentScrap;
		int size = scrapViews.size();
		if (size > 0) {
			View view = scrapViews.remove(size - 1);
			return view;
		}
		else {
			return null;
		}
	}

	protected void addScrapView(View scrap) {
		mCurrentScrap.add(scrap);
	}

	protected void recycleViews() {
		mCurrentScrap.clear();
		for (FlowLayout flow : mFlowLayouts) {
			int childCount = flow.getChildCount();
			for (int i = childCount - 1; i >= 0; i--) {
				View view = flow.getChildAt(i);
				AirImageView photoView = (AirImageView) view.findViewById(R.id.shortcut_photo);
				photoView.getImageView().setImageBitmap(null);
				addScrapView(view);
				flow.removeView(view);
			}
		}
	}

	@Override
	public void setMenuVisibility(final boolean visible) {
		/*
		 * Called when fragment is going to be visible to the user and that's when
		 * we want to start the data binding work. CreateView will have already been called.
		 */
		super.setMenuVisibility(visible);
		doMenuVisibility(visible);
	}

	public void doMenuVisibility(boolean visible) {
		mIsVisible = visible;
		if (mSelfBindingEnabled && mIsVisible) {
			bind(BindingMode.AUTO);
		}
	}

	public ShortcutFragment setShortcutType(String shortcutType) {
		mShortcutType = shortcutType;
		return this;
	}

	public ShortcutFragment setMonitor(IMonitor monitor) {
		mMonitor = monitor;
		return this;
	}

	public ShortcutFragment setQuery(IQuery query) {
		mQuery = query;
		return this;
	}

	@Override
	protected int getLayoutId() {
		return R.layout.shortcut_fragment;
	}

	/*--------------------------------------------------------------------------------------------
	 * Menus
	 *--------------------------------------------------------------------------------------------*/
	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/
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

	@Override
	public void onPause() {
		super.onPause();
		mScrollX = mScrollView.getScrollX();
		mScrollY = mScrollView.getScrollY();
	}

	public Integer getEmptyMessageResId() {
		return mEmptyMessageResId;
	}

	public ShortcutFragment setEmptyMessageResId(Integer emptyMessageResId) {
		mEmptyMessageResId = emptyMessageResId;
		return this;
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/
}