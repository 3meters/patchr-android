package com.aircandi.ui.base;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.aircandi.Aircandi;
import com.aircandi.Aircandi.ThemeTone;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.ServiceConstants;
import com.aircandi.components.AndroidManager;
import com.aircandi.components.EntityManager;
import com.aircandi.components.Logger;
import com.aircandi.components.ModelResult;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.StringManager;
import com.aircandi.controllers.IEntityController;
import com.aircandi.monitors.EntityMonitor;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Link;
import com.aircandi.objects.Links;
import com.aircandi.objects.Photo;
import com.aircandi.objects.Photo.PhotoSource;
import com.aircandi.objects.Place;
import com.aircandi.objects.Route;
import com.aircandi.objects.Shortcut;
import com.aircandi.objects.Shortcut.InstallStatus;
import com.aircandi.objects.ShortcutMeta;
import com.aircandi.objects.ShortcutSettings;
import com.aircandi.objects.TransitionType;
import com.aircandi.ui.base.IBusy.BusyAction;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.ui.widgets.ComboButton;
import com.aircandi.ui.widgets.FlowLayout;
import com.aircandi.ui.widgets.SectionLayout;
import com.aircandi.utilities.Booleans;
import com.aircandi.utilities.Colors;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.Json;
import com.aircandi.utilities.Type;
import com.aircandi.utilities.UI;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class BaseEntityForm extends BaseActivity {

	public    ScrollView mScrollView;
	protected Integer    mLinkProfile;

	/* Inputs */
	@SuppressWarnings("ucd")
	public    String mParentId;
	protected String mListLinkType;

	@Override
	public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mParentId = extras.getString(Constants.EXTRA_ENTITY_PARENT_ID);
			mEntityId = extras.getString(Constants.EXTRA_ENTITY_ID);
			mListLinkType = extras.getString(Constants.EXTRA_LIST_LINK_TYPE);
		}
	}

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
		mScrollView = (ScrollView) findViewById(R.id.scroll_view);
		if (mScrollView != null) {
			mScrollView.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
		}
		mEntityMonitor = new EntityMonitor(mEntityId);
	}

	public void beforeDatabind(final BindingMode mode) {
	    /*
	     * If cache entity is fresher than the one currently bound to or there is
		 * a cache entity available, go ahead and draw before we check against the service.
		 */
		mEntity = EntityManager.getCacheEntity(mEntityId);
		if (mEntity != null) {
			if (mEntity instanceof Place) {
				Aircandi.getInstance().setCurrentPlace(mEntity);
				Logger.v(this, "Setting current place to: " + mEntity.id);
			}
			if (mFirstDraw) {
				draw();
			}
		}
	}

	public void afterDatabind(final BindingMode mode, ModelResult result) {
		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			mLoaded = true;
		}
	}

	@Override
	public void bind(final BindingMode mode) {
		Logger.d(this, "Binding: mode = " + mode.name().toLowerCase(Locale.US));

		final AtomicBoolean refreshNeeded = new AtomicBoolean(false);
		final AtomicBoolean redrawNeeded = new AtomicBoolean(mInvalidated);

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				beforeDatabind(mode);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncGetEntity");
				ModelResult result = new ModelResult();

				if (mEntityMonitor.isChanged()) {
					refreshNeeded.set(true);
				}

				if (refreshNeeded.get() || mode == BindingMode.MANUAL) {
					mBusy.showBusy(mLoaded ? BusyAction.Refreshing : BusyAction.Loading);
				}

				if (refreshNeeded.get()) {
					Links options = Aircandi.getInstance().getEntityManager().getLinks().build(mLinkProfile);
					result = Aircandi.getInstance().getEntityManager().getEntity(mEntityId, true, options);
				}

				return result;
			}

			@Override
			protected void onPostExecute(Object modelResult) {
				if (isFinishing()) return;

				final ModelResult result = (ModelResult) modelResult;
				mBusy.hideBusy(false);
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					if (refreshNeeded.get()) {
						if (result.data != null) {
							mEntity = (Entity) result.data;
							if (mParentId != null) {
								mEntity.toId = mParentId;
							}
							if (mEntity instanceof Place) {
								Aircandi.getInstance().setCurrentPlace(mEntity);
								Logger.v(this, "Setting current place to: " + mEntity.id);
							}
							draw();
						}
						else {
							mBusy.hideBusy(true);
							UI.showToastNotification("This item has been deleted", Toast.LENGTH_SHORT);
							finish();
						}
					}
					else if (redrawNeeded.get()) {
						draw();
					}
				}
				else {
					Errors.handleError(BaseEntityForm.this, result.serviceResponse);
					return;
				}

				afterDatabind(mode, result);
				if (mEntityMonitor instanceof EntityMonitor) {
					/*
					 * Causes cache stamp checks to look clean so when we
					 * rebind the entity list, it thinks there is nothing to do because the
					 * cache stamp looks clean.
					 */
					((EntityMonitor) mEntityMonitor).updateCacheStamp(mEntity);
				}
			}

		}.execute();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onRefresh() {
		bind(BindingMode.MANUAL); // Called from Routing
	}

	@SuppressWarnings("ucd")
	public void onWatchButtonClick(View view) {

		if (Aircandi.getInstance().getCurrentUser().isAnonymous()) {
			String message = StringManager.getString(R.string.alert_signin_message_watch, mEntity.schema);
			Dialogs.signinRequired(this, message);
			return;
		}

		if (mEntity != null) {
			watch(false);
		}
	}

	@SuppressWarnings("ucd")
	public void onShortcutClick(View view) {
		final Shortcut shortcut = (Shortcut) view.getTag();
		Aircandi.dispatch.route(this, Route.SHORTCUT, mEntity, shortcut, null);
	}

	@SuppressWarnings("ucd")
	public void onEntityClick(View view) {
		Entity entity = (Entity) view.getTag();
		Bundle extras = new Bundle();
		if (Type.isTrue(entity.autowatchable)) {
			if (Aircandi.settings.getBoolean(StringManager.getString(R.string.pref_auto_watch)
					, Booleans.getBoolean(R.bool.pref_auto_watch_default))) {
				extras.putBoolean(Constants.EXTRA_AUTO_WATCH, true);
			}
		}
		Aircandi.dispatch.route(this, Route.BROWSE, entity, null, extras);
	}

	@Override
	@SuppressWarnings("ucd")
	public void onPhotoClick(View view) {
		Photo photo = (Photo) view.getTag();

		if (photo != null) {
			final String jsonPhoto = Json.objectToJson(photo);
			Bundle extras = new Bundle();
			extras.putString(Constants.EXTRA_PHOTO, jsonPhoto);
			Aircandi.dispatch.route(this, Route.PHOTO, null, null, extras);
		}
		else if (mEntity.photo != null) {
			Bundle extras = new Bundle();
			extras.putString(Constants.EXTRA_ENTITY_PARENT_ID, mParentId);
			extras.putString(Constants.EXTRA_LIST_LINK_TYPE, (mListLinkType == null) ? Constants.TYPE_LINK_CONTENT : mListLinkType);
			extras.putString(Constants.EXTRA_LIST_LINK_SCHEMA, mEntity.schema);
			Aircandi.dispatch.route(this, Route.PHOTOS, mEntity, null, extras);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		/*
		 * Cases that use activity result
		 * 
		 * - Candi picker returns entity id for a move
		 * - Template picker returns type of candi to add as a child
		 */
		if (resultCode != Activity.RESULT_CANCELED || Aircandi.resultCode != Activity.RESULT_CANCELED) {
			if (requestCode == Constants.ACTIVITY_ENTITY_EDIT) {
				/*
				 * Guarantees that any cache stamp retrieved for parent entity from the service will not equal
				 * any cache stamp including itself. Cleared if parent entity is refreshed from the service.
				 */
				Aircandi.getInstance().getEntityManager().getCacheStampOverrides().put(mParentId, mParentId);
				if (resultCode == Constants.RESULT_ENTITY_DELETED || resultCode == Constants.RESULT_ENTITY_REMOVED) {
					finish();
					Aircandi.getInstance().getAnimationManager().doOverridePendingTransition(this, TransitionType.PAGE_TO_RADAR_AFTER_DELETE);
				}
			}
			else if (requestCode == Constants.ACTIVITY_APPLICATION_PICK) {

				if (intent != null && intent.getExtras() != null) {

					final Bundle extras = intent.getExtras();
					final String entitySchema = extras.getString(Constants.EXTRA_ENTITY_SCHEMA);

					if (entitySchema != null && !entitySchema.equals("")) {
						extras.putString(Constants.EXTRA_ENTITY_PARENT_ID, mEntityId);
						extras.putString(Constants.EXTRA_ENTITY_SCHEMA, entitySchema);
						super.onAdd(extras);
					}
				}
			}
		}
		super.onActivityResult(requestCode, resultCode, intent);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Logger.d(this, "Activity saving state");

		if (mScrollView != null) {
			outState.putIntArray("ARTICLE_SCROLL_POSITION", new int[]{mScrollView.getScrollX(), mScrollView.getScrollY()});
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		/*
		 * Will only be called if the activity is destroyed and restored. Restore
		 * state could be handled in onCreate or here later in the lifecycle after
		 * everything has been initialized.
		 */
		super.onRestoreInstanceState(savedInstanceState);
		Logger.d(this, "Activity restoring state");
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

	/*--------------------------------------------------------------------------------------------
	 * UI
	 *--------------------------------------------------------------------------------------------*/

	protected void drawStats() {
	}

	protected void drawButtons() {

		if (mEntity.id.equals(Aircandi.getInstance().getCurrentUser().id)) {
			UI.setVisibility(findViewById(R.id.button_holder), View.GONE);
		}
		else {
			UI.setVisibility(findViewById(R.id.button_holder), View.VISIBLE);

			ComboButton watched = (ComboButton) findViewById(R.id.button_watch);
			if (watched != null) {
				UI.setVisibility(watched, View.VISIBLE);
				Link link = mEntity.linkByAppUser(Constants.TYPE_LINK_WATCH);
				if (link != null && link.enabled) {
					final int color = Colors.getColor(R.color.brand_primary);
					watched.getImageIcon().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
				}
				else {
					watched.getImageIcon().setColorFilter(null);
				}
			}
		}
	}

	protected void prepareShortcuts(List<Shortcut> shortcuts
			, ShortcutSettings settings
			, Integer titleResId
			, Integer moreResId
			, Integer flowLimit
			, Integer holderId
			, Integer flowItemResId) {

		View holder = LayoutInflater.from(this).inflate(R.layout.section_shortcuts, null);
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

			IEntityController controller = Aircandi.getInstance().getControllerForClass(settings.appClass);
			Intent intent = controller.viewFor(this
					, null
					, mEntityId
					, settings.linkType
					, settings.direction
					, (titleResId != null) ? StringManager.getString(titleResId) : null
					, false
					, false);

			/*
			 * Make 'more' button shortcut
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
		flowShortcuts(flow, shortcuts, flowItemResId);
		((ViewGroup) findViewById(holderId)).addView(holder);

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
		final Integer candiWidthPixels = (layoutWidthPixels - (spacingHorizontalPixels * (candiCount - 1))) / candiCount;

		final Integer candiHeightPixels = (candiWidthPixels * 1);

		layout.setSpacingHorizontal(spacingHorizontalPixels);
		layout.setSpacingVertical(spacingVerticalPixels);

		for (Shortcut shortcut : shortcuts) {

			if (!shortcut.isActive(mEntity)) {
				continue;
			}

			View view = LayoutInflater.from(this).inflate(viewResId, null);
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
				badgeLower.setVisibility(View.VISIBLE);
			}
			else if (shortcut.count != null && shortcut.count > 0) {
				badgeUpper.setTag(shortcut);
				badgeUpper.setText(String.valueOf(shortcut.count));
				badgeUpper.setVisibility(View.VISIBLE);
			}

			if ((shortcut.photo != null && !shortcut.photo.getSource().equals(PhotoSource.assets_applinks))
					|| (shortcut.group != null && shortcut.group.size() > 1)) {
				if (shortcut.app.equals(Constants.TYPE_APP_FACEBOOK)) {
					badgeLower.setBackgroundResource(R.drawable.ic_action_facebook_dark);
					if (Aircandi.themeTone.equals(ThemeTone.LIGHT)) {
						badgeLower.setBackgroundResource(R.drawable.ic_action_facebook_light);
					}
					badgeLower.setVisibility(View.VISIBLE);
				}
				else if (shortcut.app.equals(Constants.TYPE_APP_TWITTER)) {
					badgeLower.setBackgroundResource(R.drawable.ic_action_twitter_dark);
					if (Aircandi.themeTone.equals(ThemeTone.LIGHT)) {
						badgeLower.setBackgroundResource(R.drawable.ic_action_twitter_light);
					}
					badgeLower.setVisibility(View.VISIBLE);
				}
				else if (shortcut.app.equals(Constants.TYPE_APP_WEBSITE)) {
					badgeLower.setBackgroundResource(R.drawable.ic_action_website_dark);
					if (Aircandi.themeTone.equals(ThemeTone.LIGHT)) {
						badgeLower.setBackgroundResource(R.drawable.ic_action_website_light);
					}
					badgeLower.setVisibility(View.VISIBLE);
				}
				else if (shortcut.app.equals(Constants.TYPE_APP_FOURSQUARE)) {
					badgeLower.setBackgroundResource(R.drawable.ic_action_foursquare_dark);
					if (Aircandi.themeTone.equals(ThemeTone.LIGHT)) {
						badgeLower.setBackgroundResource(R.drawable.ic_action_foursquare_light);
					}
					badgeLower.setVisibility(View.VISIBLE);
				}
				else if (shortcut.app.equals(Constants.TYPE_APP_YELP)) {
					badgeLower.setBackgroundResource(R.drawable.ic_action_yelp_dark);
					if (Aircandi.themeTone.equals(ThemeTone.LIGHT)) {
						badgeLower.setBackgroundResource(R.drawable.ic_action_yelp_light);
					}
					badgeLower.setVisibility(View.VISIBLE);
				}
				else if (shortcut.app.equals(Constants.TYPE_APP_CITYGRID)) {
					badgeLower.setBackgroundResource(R.drawable.ic_action_citygrid_dark);
					if (Aircandi.themeTone.equals(ThemeTone.LIGHT)) {
						badgeLower.setBackgroundResource(R.drawable.ic_action_citygrid_light);
					}
					badgeLower.setVisibility(View.VISIBLE);
				}
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

			if (!TextUtils.isEmpty(shortcut.name)) {
				name.setText(shortcut.name);
				name.setVisibility(View.VISIBLE);
			}
			else if (!TextUtils.isEmpty(shortcut.description)) {
				name.setText(shortcut.description);
				name.setVisibility(View.VISIBLE);
			}

			photoView.setTag(shortcut.getPhoto());
			photoView.setSizeHint(candiWidthPixels);
			UI.drawPhoto(photoView, shortcut.getPhoto());

			FlowLayout.LayoutParams params = new FlowLayout.LayoutParams(candiWidthPixels, LayoutParams.WRAP_CONTENT);
			params.setCenterHorizontal(false);
			view.setLayoutParams(params);

			RelativeLayout.LayoutParams paramsImage = new RelativeLayout.LayoutParams(candiWidthPixels, candiHeightPixels);
			photoView.setLayoutParams(paramsImage);

			layout.addView(view);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void watch(final boolean autoWatch) {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				((ComboButton) findViewById(R.id.button_watch)).getViewAnimator().setDisplayedChild(1);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncWatchEntity");
				ModelResult result;
				Aircandi.getInstance().getCurrentUser().activityDate = DateTime.nowDate().getTime();
				Boolean enabled = mEntity.visibleToCurrentUser();
				if (!mEntity.byAppUser(Constants.TYPE_LINK_WATCH)) {

					Shortcut fromShortcut = Aircandi.getInstance().getCurrentUser().getShortcut();
					Shortcut toShortcut = mEntity.getShortcut();

					result = Aircandi.getInstance().getEntityManager().insertLink(Aircandi.getInstance().getCurrentUser().id
							, mEntity.id
							, Constants.TYPE_LINK_WATCH
							, enabled
							, fromShortcut
							, toShortcut
							, "watch_entity_" + mEntity.schema);
				}
				else {
					result = Aircandi.getInstance().getEntityManager().deleteLink(Aircandi.getInstance().getCurrentUser().id
							, mEntity.id
							, Constants.TYPE_LINK_WATCH
							, enabled
							, mEntity.schema
							, "unwatch_entity_" + mEntity.schema);
				}
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				if (isFinishing()) return;
				ModelResult result = (ModelResult) response;
				if (!afterWatch(result)) {
					((ComboButton) findViewById(R.id.button_watch)).getViewAnimator().setDisplayedChild(0);
					if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
						drawButtons();
						drawStats();
						if (autoWatch) {
							UI.showToastNotification(StringManager.getString(R.string.alert_auto_watch), Toast.LENGTH_SHORT, Gravity.CENTER);
						}
					}
					else {
						if (result.serviceResponse.statusCodeService != null
								&& result.serviceResponse.statusCodeService != ServiceConstants.SERVICE_STATUS_CODE_FORBIDDEN_DUPLICATE) {
							Errors.handleError(BaseEntityForm.this, result.serviceResponse);
						}
					}
				}
			}
		}.execute();
	}

	protected Boolean afterWatch(ModelResult result) {
		return false;
	}

	@Override
	public Boolean related(String entityId) {
		return (mEntityId != null && entityId.equals(mEntityId));

	}

	@Override
	public Boolean related(Entity entity) {
		return (mEntity != null && entity.id.equals(mEntity.id));

	}

	/*--------------------------------------------------------------------------------------------
	 * Menus
	 *--------------------------------------------------------------------------------------------*/ 	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/
	@Override
	protected void onResume() {
		super.onResume();
		/*
		 * We have to be pretty aggressive about refreshing the UI because
		 * there are lots of actions that could have happened while this activity
		 * was stopped that change what the user would expect to see.
		 * 
		 * - Entity deleted or modified
		 * - Entity children modified
		 * - New comments
		 * - Change in user which effects which candi and UI should be visible.
		 * - User profile could have been updated and we don't catch that.
		 */
		if (!isFinishing()) {
			if (mEntity instanceof Place) {
				Aircandi.getInstance().setCurrentPlace(mEntity);
				Logger.v(this, "Setting current place to: " + mEntity.id);
			}

			Aircandi.getInstance().getAnimationManager().doOverridePendingTransition(this, TransitionType.PAGE_BACK);

			bind(BindingMode.AUTO);    // check to see if the cache stamp is stale
		}
	}

}