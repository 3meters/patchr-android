package com.aircandi.catalina.ui;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ShareCompat;
import android.text.Html;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.aircandi.Aircandi;
import com.aircandi.ServiceConstants;
import com.aircandi.catalina.Constants;
import com.aircandi.catalina.R;
import com.aircandi.catalina.objects.Message.MessageType;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.StringManager;
import com.aircandi.controllers.IEntityController;
import com.aircandi.events.MessageEvent;
import com.aircandi.monitors.EntityMonitor;
import com.aircandi.objects.Count;
import com.aircandi.objects.Link;
import com.aircandi.objects.Link.Direction;
import com.aircandi.objects.Place;
import com.aircandi.objects.Route;
import com.aircandi.objects.User;
import com.aircandi.queries.EntitiesQuery;
import com.aircandi.ui.EntityListFragment;
import com.aircandi.ui.EntityListFragment.ViewType;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.ui.widgets.CandiView;
import com.aircandi.ui.widgets.CandiView.IndicatorOptions;
import com.aircandi.ui.widgets.UserView;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Integers;
import com.aircandi.utilities.UI;
import com.squareup.otto.Subscribe;

public class PlaceForm extends com.aircandi.ui.PlaceForm {

	private EntityListFragment mListFragment;

	@Override
	public void unpackIntent() {
		super.unpackIntent();

		Intent intent = getIntent();
		if (intent != null && intent.getAction() != null && intent.getAction().equals(Intent.ACTION_VIEW)) {
			Uri uri = intent.getData();
			if (uri != null) {
				if (uri.getPath().contains("/place/")) {
					mEntityId = uri.getPath().replace("/place/", "");
					mAutoWatch = true;
				}
				else if (uri.getPath().contains("/patch/")) {
					mEntityId = uri.getPath().replace("/patch/", "");
					mAutoWatch = true;
				}
			}
		}
	}

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mListFragment = new MessageListFragment();

		EntityMonitor monitor = new EntityMonitor(mEntityId);
		EntitiesQuery query = new EntitiesQuery();

		query.setEntityId(mEntityId)
		     .setLinkDirection(Direction.in.name())
		     .setLinkType(Constants.TYPE_LINK_CONTENT)
		     .setPageSize(Integers.getInteger(R.integer.page_size_messages))
		     .setSchema(Constants.SCHEMA_ENTITY_MESSAGE);

		mListFragment.setQuery(query)
		             .setMonitor(monitor)
		             .setListViewType(ViewType.LIST)
		             .setListLayoutResId(R.layout.message_list_place_fragment)
		             .setListLoadingResId(R.layout.temp_list_item_loading)
		             .setListItemResId(R.layout.temp_listitem_message)
		             .setListEmptyMessageResId(R.string.button_list_share)
		             .setListButtonMessageResId(R.string.button_list_share)
		             .setHeaderViewResId(R.layout.widget_list_header_place)
		             .setFooterViewResId(R.layout.widget_list_footer_message)
		             .setSelfBindingEnabled(false)
		             .setButtonSpecialClickable(true);

		getSupportFragmentManager().beginTransaction().add(R.id.fragment_holder, mListFragment).commit();

	}

	@Override
	public void afterDatabind(final BindingMode mode, ModelResult result) {
		super.afterDatabind(mode, result);
		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
		    /*
			 * In case upsizing has changed the id we original bound to.
			 */
			((EntityMonitor) mListFragment.getMonitor()).setEntityId(mEntityId);
			((EntitiesQuery) mListFragment.getQuery()).setEntityId(mEntityId);
			if (mEntityMonitor.changed) {
				mListFragment.bind(BindingMode.MANUAL);
			}
			else {
				mListFragment.bind(mode);
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/
	@Override
	@Subscribe
	@SuppressWarnings("ucd")
	public void onMessage(final MessageEvent event) {
		/*
		 * Refresh the form because something new has been added to it
		 * like a comment or post. Or something has moved like a candigram.
		 */
		if (event.message.action.entity != null
				&& event.message.action.entity.schema.equals(Constants.SCHEMA_ENTITY_MESSAGE)
				&& event.message.action.entity.placeId != null
				&& event.message.action.entity.placeId.equals(mEntityId)) {

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					mListFragment.bind(BindingMode.AUTO);
				}
			});
		}
	}

	//	@Override
	//	@SuppressWarnings("ucd")
	//	public void onWatchButtonClick(View view) {
	//
	//		if (Aircandi.getInstance().getCurrentUser().isAnonymous()) {
	//			String message = StringManager.getString(R.string.alert_signin_message_watch, mEntity.schema);
	//			Dialogs.signinRequired(this, message);
	//			return;
	//		}
	//
	//		if (mEntity.visibility != null) {
	//
	//			/* Public place */
	//			if (mEntity.visibility.equals(Constants.VISIBILITY_PUBLIC)) {
	//				watch();
	//			}
	//
	//			/* Private place owned by current user */
	//			else if (mEntity.isOwnedByCurrentUser()) {
	//				/*
	//				 * Do nothing for now, owners always stay as watchers
	//				 */
	//			}
	//
	//			/* Private place not owned by current user */
	//			else if (!mEntity.visibleToCurrentUser()) {
	//
	//				Link link = mEntity.linkByAppUser(Constants.TYPE_LINK_WATCH);
	//				if (link == null || link.enabled == null) {
	//
	//					/* User doesn't have a pending request */
	//					UI.showToastNotification(StringManager.getString(R.string.button_list_watch_request), Toast.LENGTH_SHORT);
	//				}
	//				else if (!link.enabled) {
	//
	//					/* User has a pending request */
	//					UI.showToastNotification(StringManager.getString(R.string.button_list_watch_request_cancel), Toast.LENGTH_SHORT);
	//					watch();
	//				}
	//				else if (link.enabled) {
	//
	//					/* User has an approved link */
	//					UI.showToastNotification(StringManager.getString(R.string.button_list_watch_request_cancel), Toast.LENGTH_SHORT);
	//					watch();
	//				}
	//			}
	//		}
	//	}

	@SuppressWarnings("ucd")
	public void onWatchersButtonClick(View view) {

		/* The owner of a private place is a permanent member */
		if (mEntity != null) {
			if (mEntity.visibility.equals(Constants.VISIBILITY_PUBLIC)
					|| (mEntity.visibility.equals(Constants.VISIBILITY_PRIVATE) && mEntity.visibleToCurrentUser())) {
				Aircandi.dispatch.route(this, Route.WATCHERS, mEntity, null, null);
			}
		}
	}

	@SuppressWarnings("ucd")
	public void onMoreButtonClick(View view) {
		mListFragment.onMoreButtonClick(view);
	}

	@SuppressWarnings("ucd")
	public void onMapButtonClick(View view) {
		if (mEntity != null) {
			IEntityController controller = Aircandi.getInstance().getControllerForSchema(Constants.TYPE_APP_MAP);
			if (controller != null) {
				controller.view(this, mEntity, mEntity.id, null, null, null, true);
			}
		}
	}

	@SuppressWarnings("ucd")
	public void onAddMessageButtonClick(View view) {
		if (!mClickEnabled) return;
		if (mEntity != null) {
			mClickEnabled = false;
			onAdd(new Bundle());
		}
	}

	@SuppressWarnings("ucd")
	public void onShareButtonClick(View view) {
		if (mEntity != null) {
			Aircandi.dispatch.route(this, Route.SHARE, mEntity, null, null);
		}
	}

	@SuppressWarnings("ucd")
	public void onEditButtonClick(View view) {
		if (mEntity != null) {
			Aircandi.dispatch.route(this, Route.EDIT, mEntity, null, new Bundle());
		}
	}

	@SuppressWarnings("ucd")
	public void onHeaderClick(View view) {
		((MessageListFragment) mListFragment).onHeaderClick(view);
	}

	@Override
	public void onAdd(Bundle extras) {

		if (Aircandi.getInstance().getMenuManager().canUserAdd(mEntity)) {
			String message = StringManager.getString(R.string.label_message_new_message);
			if (!TextUtils.isEmpty(mEntity.name)) {
				message = String.format(StringManager.getString(R.string.label_message_new_to_message), mEntity.name);
			}
			extras.putString(Constants.EXTRA_MESSAGE, message);
			extras.putString(Constants.EXTRA_ENTITY_PARENT_ID, mEntityId);
			extras.putString(Constants.EXTRA_MESSAGE_TYPE, MessageType.ROOT);
			extras.putString(Constants.EXTRA_ENTITY_SCHEMA, Constants.SCHEMA_ENTITY_MESSAGE);
			Aircandi.dispatch.route(this, Route.NEW, null, null, extras);
			return;
		}
		if (mEntity.locked) {
			Dialogs.locked(this, mEntity);
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		View header = mListFragment.getHeaderView();
		if (header != null) {

			/* Reset the image aspect ratio */
			AirImageView image = (AirImageView) header.findViewById(R.id.entity_photo);
			TypedValue typedValue = new TypedValue();
			getResources().getValue(R.dimen.aspect_ratio_place_image, typedValue, true);
			image.setAspectRatio(typedValue.getFloat());

			/* Pass the projected header height */
			final DisplayMetrics metrics = getResources().getDisplayMetrics();
			int screenWidth = (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) ? metrics.widthPixels : metrics.heightPixels;
			positionButton((int) (screenWidth * typedValue.getFloat()));
		}
		else {
			positionButton(null);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/
	@Override
	public void share() {

		ShareCompat.IntentBuilder builder = ShareCompat.IntentBuilder.from(this);

		builder.setSubject(String.format(StringManager.getString(R.string.label_place_share_subject)
				, (mEntity.name != null) ? mEntity.name : "A"));

		builder.setType("text/plain");
		builder.setText(String.format(StringManager.getString(R.string.label_place_share_body), mEntityId));
		builder.setChooserTitle(String.format(StringManager.getString(R.string.label_place_share_title)
				, (mEntity.name != null) ? mEntity.name : StringManager.getString(R.string.container_singular_lowercase)));

		builder.getIntent().putExtra(Constants.EXTRA_SHARE_SOURCE, getPackageName());
		builder.getIntent().putExtra(Constants.EXTRA_SHARE_ID, mEntityId);
		builder.getIntent().putExtra(Constants.EXTRA_SHARE_SCHEMA, Constants.SCHEMA_ENTITY_PLACE);

		builder.startChooser();
	}

	@Override
	public void draw() {
		super.draw();

		final CandiView candiViewInfo = (CandiView) findViewById(R.id.candi_view_info);
		final TextView address = (TextView) findViewById(R.id.candi_form_address);
		final UserView userView = (UserView) findViewById(R.id.user);

		if (candiViewInfo != null) {
			/*
			 * This is a place entity with a fancy image widget
			 */
			IndicatorOptions options = new IndicatorOptions();
			options.showIfZero = true;
			options.imageSizePixels = 15;
			options.iconsEnabled = false;
			candiViewInfo.databind(mEntity, options);
		}

		drawStats();

		/* Place specific info */

		final Place place = (Place) mEntity;

		UI.setVisibility(address, View.GONE);
		if (address != null) {
			String addressBlock = place.getAddressBlock();

			if (place.phone != null) {
				addressBlock += "<br/>" + place.getFormattedPhone();
			}

			if (!"".equals(addressBlock)) {
				address.setText(Html.fromHtml(addressBlock));
				UI.setVisibility(address, View.VISIBLE);
			}
		}

		/* Creator (on info side) */

		UI.setVisibility(userView, View.GONE);
		if (userView != null) {
			if (mEntity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
				if (mEntity.isOwnedBySystem()) {
					User admin = new User();
					admin.name = StringManager.getString(R.string.name_app);
					admin.id = ServiceConstants.ANONYMOUS_USER_ID;
					userView.setLabel(R.string.label_owned_by);
					userView.databind(admin);
				}
				else {
					userView.setTag(mEntity.creator);
					userView.setLabel(R.string.label_owned_by);
					userView.databind(mEntity.creator, mEntity.createdDate != null ? mEntity.createdDate.longValue() : null);
				}
				UI.setVisibility(userView, View.VISIBLE);
			}
		}

		/* Get the special button positioned initially */

		positionButton(null);

		//		final Button messageButton = (Button) findViewById(R.id.footer_holder);
		//		if (messageButton != null && mEntity != null && !TextUtils.isEmpty(mEntity.name)) {
		//			//messageButton.setLabel(String.format(StringManager.getString(R.string.button_send_message_this_place), mEntity.name));
		//		}
	}

	@Override
	protected void drawBody() {
		/* Blocking */
	}

	@Override
	protected void drawShortcuts() {
		/* Blocking */
	}

	@Override
	protected void drawStats() {

		TextView watchersCount = (TextView) findViewById(R.id.watchers_count);
		if (watchersCount != null) {
			Count count = mEntity.getCount(Constants.TYPE_LINK_WATCH, null, true, Direction.in);
			if (count == null) {
				count = new Count(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_PLACE, null, 0);
			}
			watchersCount.setText(String.valueOf(count.count.intValue()));
		}
	}

	@Override
	public void drawButtons() {
		super.drawButtons();

		if (mEntity.visibility != null
				&& mEntity.visibility.equals(Constants.VISIBILITY_PRIVATE)
				&& !mEntity.visibleToCurrentUser()) {

			UI.setVisibility(findViewById(R.id.button_watch), View.INVISIBLE);
			UI.setVisibility(findViewById(R.id.footer_holder), View.INVISIBLE);

			Link link = mEntity.linkByAppUser(Constants.TYPE_LINK_WATCH);
			if (link == null) {
				mListFragment.getButtonSpecial().setText(R.string.button_list_watch_request);
			}
			else if (!link.enabled) {
				mListFragment.getButtonSpecial().setText(R.string.button_list_watch_request_cancel);
			}
		}
		else {
			UI.setVisibility(findViewById(R.id.button_watch), View.VISIBLE);
			UI.setVisibility(findViewById(R.id.footer_holder), View.VISIBLE);
		}

		Place place = (Place) mEntity;
		UI.setVisibility(findViewById(R.id.button_map), View.GONE);
		UI.setVisibility(findViewById(R.id.button_edit), View.GONE);
		/*
		 * We can map it if we have an address or a decent location fix.
		 */
		if (!place.fuzzy || !TextUtils.isEmpty(place.address)) {
			UI.setVisibility(findViewById(R.id.button_map), View.VISIBLE);
		}
	}

	protected void positionButton(final Integer headerHeightProjected) {

		final View header = mListFragment.getHeaderView();
		final Button buttonSpecial = mListFragment.getButtonSpecial();

		if (buttonSpecial != null && header != null) {

			ViewTreeObserver vto = header.getViewTreeObserver();
			vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

				@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
				@SuppressWarnings("deprecation")
				@Override
				public void onGlobalLayout() {

					/*
					 * We don't get this right because this can happen before the image pops in so
					 * the header size changes.
					 */
					if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
						RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(buttonSpecial.getLayoutParams());
						params.addRule(RelativeLayout.CENTER_HORIZONTAL);
						int headerHeight = (headerHeightProjected != null)
						                   ? headerHeightProjected
						                   : (header != null)
						                     ? header.getHeight()
						                     : UI.getRawPixelsForDisplayPixels(150f);

						params.topMargin = headerHeight + UI.getRawPixelsForDisplayPixels(100f);
						buttonSpecial.setLayoutParams(params);
					}
					else {
						RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(buttonSpecial.getLayoutParams());
						params.addRule(RelativeLayout.CENTER_IN_PARENT);
						Logger.i(this, "header " + header.getHeight());
						buttonSpecial.setLayoutParams(params);
					}

					if (Constants.SUPPORTS_JELLY_BEAN) {
						header.getViewTreeObserver().removeOnGlobalLayoutListener(this);
					}
					else {
						header.getViewTreeObserver().removeGlobalOnLayoutListener(this);
					}
				}
			});
		}
	}

	@Override
	protected void upsize() {
		mWaitForContent = false;
		super.upsize();
	}

	@Override
	protected Boolean afterWatch(ModelResult result) {

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			if (mEntity.visibility.equals(Constants.VISIBILITY_PRIVATE) && !mEntity.isOwnedByCurrentUser()) {
				Link link = mEntity.linkByAppUser(Constants.TYPE_LINK_WATCH);
				if (link == null) {
					mListFragment.getButtonSpecial().setText(R.string.button_list_watch_request);
					UI.showToastNotification(StringManager.getString(R.string.alert_watch_request_canceled), Toast.LENGTH_SHORT);
				}
				else if (!link.enabled) {
					mListFragment.getButtonSpecial().setText(R.string.button_list_watch_request_cancel);
					UI.showToastNotification(StringManager.getString(R.string.alert_watch_request_sent), Toast.LENGTH_SHORT);
				}
				return true;
			}
		}
		return false;
	}

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Menus
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem menuItem = menu.findItem(com.aircandi.R.id.share);
		if (menuItem != null) {
			menuItem.setVisible(Aircandi.getInstance().getMenuManager().showAction(Route.SHARE, mEntity, mForId));
		}
		return super.onPrepareOptionsMenu(menu);
	}

	/*--------------------------------------------------------------------------------------------
	 * Misc
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/
}