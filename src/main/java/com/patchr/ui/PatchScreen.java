package com.patchr.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ShareCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.share.model.AppInviteContent;
import com.facebook.share.widget.AppInviteDialog;
import com.flipboard.bottomsheet.BottomSheetLayout;
import com.flipboard.bottomsheet.commons.MenuSheetView;
import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.AnimationManager;
import com.patchr.components.DataController;
import com.patchr.components.Dispatcher;
import com.patchr.components.IntentBuilder;
import com.patchr.components.Logger;
import com.patchr.components.MenuManager;
import com.patchr.components.StringManager;
import com.patchr.components.UserManager;
import com.patchr.events.EntitiesQueryEvent;
import com.patchr.events.EntityQueryEvent;
import com.patchr.events.EntityQueryResultEvent;
import com.patchr.events.LinkDeleteEvent;
import com.patchr.events.LinkInsertEvent;
import com.patchr.events.ProcessingCompleteEvent;
import com.patchr.events.WatchStatusChangedEvent;
import com.patchr.objects.ActionType;
import com.patchr.objects.Entity;
import com.patchr.objects.FetchMode;
import com.patchr.objects.Link;
import com.patchr.objects.LinkSpecType;
import com.patchr.objects.Message;
import com.patchr.objects.Notification;
import com.patchr.objects.Patch;
import com.patchr.objects.Photo;
import com.patchr.objects.PhotoCategory;
import com.patchr.objects.Route;
import com.patchr.objects.Shortcut;
import com.patchr.objects.TransitionType;
import com.patchr.objects.WatchStatus;
import com.patchr.ui.components.BusyController;
import com.patchr.ui.components.CircleTransform;
import com.patchr.ui.components.EmptyController;
import com.patchr.ui.components.InsetViewTransformer;
import com.patchr.ui.components.ListPresenter;
import com.patchr.ui.edit.MessageEdit;
import com.patchr.ui.fragments.EntityListFragment;
import com.patchr.ui.views.PatchDetailView;
import com.patchr.utilities.Colors;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Integers;
import com.patchr.utilities.Json;
import com.patchr.utilities.Maps;
import com.patchr.utilities.UI;
import com.patchr.utilities.Utils;
import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Locale;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.util.LinkProperties;

@SuppressLint("Registered")
public class PatchScreen extends BaseActivity implements View.OnClickListener
		, SwipeRefreshLayout.OnRefreshListener
		, NfcAdapter.CreateNdefMessageCallback
		, NfcAdapter.OnNdefPushCompleteCallback {

	private final Handler handler = new Handler();

	private   PatchDetailView   header;
	private   ListPresenter     listPresenter;
	protected BottomSheetLayout bottomSheetLayout;

	protected String  listLinkType;
	protected String  notificationId;
	protected Integer watchStatus;    // Set in draw
	private   boolean bound;

	protected CallbackManager callbackManager;      // For facebook
	protected String          branchLink;           // Uri
	protected boolean         showReferrerWelcome;
	protected String          referrerName;
	protected String          referrerPhotoUrl;
	protected boolean         authenticatedForInvite;
	protected boolean         autoJoin;
	protected boolean         justApproved;               // Set in onMessage via notification

	public PatchScreen() {
		watchStatus = WatchStatus.NONE;
	}

	@Override protected void onStart() {
		super.onStart();
		Dispatcher.getInstance().register(this);
	}

	@Override protected void onResume() {
		super.onResume();
		if (!isFinishing()) {
			Patchr.getInstance().setCurrentPatch(entity);
		}

		if (entity != null && entity instanceof Patch) {
			Patch patch = (Patch) entity;
			if (referrerName != null) {     // Active invitation
				if (bottomSheetLayout.isSheetShowing()) {
					if (UserManager.shared().authenticated() && patch.watchStatus() != WatchStatus.NONE) {
						bottomSheetLayout.dismissSheet();
					}
					else if (UserManager.shared().authenticated() != authenticatedForInvite) {
						showInviteWelcome(1000);
					}
				}
				else {
					showInviteWelcome(1500);
				}
			}
		}

		if (autoJoin) {
			onJoinButtonClick(null);
			UI.showToastNotification("You are now a member of this patch!", Toast.LENGTH_SHORT);
			autoJoin = false;
		}
	}

	@Override protected void onStop() {
		Dispatcher.getInstance().unregister(this);
		super.onStop();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public void onClick(View view) {
		/*
		 * Base activity broadcasts view clicks that target onViewClick. There are actions
		 * that should be handled at the activity level like add a new entity.
		 */
		if (processing) return;

		processing = true;
		Integer id = view.getId();

		/* Dynamic button we need to redirect */
		if (id == R.id.action_button) {
			id = (Integer) view.getTag();
		}

		if (id == R.id.add || id == R.id.list_fab) {
			onAdd(new Bundle());
		}
		processing = false;

		if (view.getTag() != null) {
			if (view.getTag() instanceof Photo) {
				Photo photo = (Photo) view.getTag();
				if (photo != null) {
					final String jsonPhoto = Json.objectToJson(photo);
					Bundle extras = new Bundle();
					extras.putString(Constants.EXTRA_PHOTO, jsonPhoto);
					Patchr.router.route(this, Route.PHOTO, null, extras);
				}
			}
			else if (view.getTag() instanceof Entity) {
				final Entity entity = (Entity) view.getTag();
				final Bundle extras = new Bundle();
				extras.putInt(Constants.EXTRA_TRANSITION_TYPE, TransitionType.DRILL_TO);

				if (entity instanceof Notification) {
					Notification notification = (Notification) entity;
					extras.putString(Constants.EXTRA_ENTITY_SCHEMA, Entity.getSchemaForId(notification.targetId));
					extras.putString(Constants.EXTRA_ENTITY_ID, notification.targetId);
					extras.putString(Constants.EXTRA_ENTITY_PARENT_ID, notification.parentId);
				}

				Patchr.router.route(this, Route.BROWSE, entity, extras);
			}
			else {

			}
		}
	}

	@Override public boolean onCreateOptionsMenu(Menu menu) {

		this.optionMenu = menu;
		getMenuInflater().inflate(R.menu.menu_log_in, menu);
		getMenuInflater().inflate(R.menu.menu_invite, menu);
		getMenuInflater().inflate(R.menu.menu_map, menu);
		getMenuInflater().inflate(R.menu.menu_refresh, menu);
		getMenuInflater().inflate(R.menu.menu_edit_patch, menu);
		getMenuInflater().inflate(R.menu.menu_leave_patch, menu);
		getMenuInflater().inflate(R.menu.menu_delete, menu);
		getMenuInflater().inflate(R.menu.menu_qrcode, menu);
		getMenuInflater().inflate(R.menu.menu_report_patch, menu);

		configureStandardMenuItems(menu);   // Tweaks based on permissions
		return true;
	}

	@Override public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.leave_patch) {
			onJoinButtonClick(null);
		}
		else {
			Patchr.router.route(this, Patchr.router.routeForMenuId(item.getItemId()), entity, null);
		}
		return true;
	}

	@Override public boolean onPrepareOptionsMenu(Menu menu) {
		configureStandardMenuItems(menu);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override public void onRefresh() {
		fetch(FetchMode.MANUAL);
		if (this.listPresenter != null) {
			if (!isFinishing()) {
				this.listPresenter.refresh();
			}
			else {
				this.listPresenter.busyController.hide(false);
			}
		}
	}

	@Override public void onAdd(Bundle extras) {

		if (!UserManager.shared().authenticated()) {
			UserManager.shared().showGuestGuard(this, "Sign up for a free account to post messages and more.");
			return;
		}

		if (entity == null) return;

		if (MenuManager.canUserAdd(entity)) {

			String message = StringManager.getString(R.string.label_message_new_message);
			if (!TextUtils.isEmpty(entity.name)) {
				message = String.format(StringManager.getString(R.string.label_message_new_to_message), entity.name);
			}

			extras.putString(Constants.EXTRA_MESSAGE, message);
			extras.putString(Constants.EXTRA_ENTITY_PARENT_ID, entityId);
			extras.putString(Constants.EXTRA_MESSAGE_TYPE, Message.MessageType.ROOT);
			extras.putString(Constants.EXTRA_ENTITY_SCHEMA, Constants.SCHEMA_ENTITY_MESSAGE);

			Patchr.router.route(this, Route.NEW, null, extras);
		}
	}

	@Override public NdefMessage createNdefMessage(NfcEvent event) {
		String uri = "https://patchr.com";
		if (branchLink != null) {
			uri = branchLink;
		}
		/* Create an NDEF message containing the branch link for this patch */
		NdefRecord rec = NdefRecord.createUri(uri);
		NdefRecord[] records = new NdefRecord[]{rec};
		return new NdefMessage(records);
	}

	@Override public void onNdefPushComplete(NfcEvent event) {
		UI.showToastNotification("Patch beamed!", Toast.LENGTH_SHORT);
	}

	@Override public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (bottomSheetLayout.isSheetShowing()) {
			bottomSheetLayout.peekSheet();
		}
	}

	@Override public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (resultCode != Activity.RESULT_CANCELED) {
			if (requestCode == Constants.ACTIVITY_ENTITY_EDIT) {
				if (resultCode == Constants.RESULT_ENTITY_DELETED || resultCode == Constants.RESULT_ENTITY_REMOVED) {
					finish();
					AnimationManager.doOverridePendingTransition(this, TransitionType.PAGE_TO_RADAR_AFTER_DELETE);
				}
			}
			else if (resultCode == Constants.RESULT_USER_SIGNED_IN && UserManager.shared().authenticated()) {
				onRefresh();
			}
			else if (requestCode == Constants.ACTIVITY_ENTITY_INSERT) {
				if (intent != null && intent.getExtras() != null) {
					String schema = intent.getExtras().getString(Constants.EXTRA_ENTITY_SCHEMA);
					if (schema != null && schema.equals(Constants.SCHEMA_ENTITY_MESSAGE)) {
						Boolean hasMessaged = (entity.linkByAppUser(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_MESSAGE) != null);
						Patch patch = (Patch) entity;
						if (patch.watchStatus() == WatchStatus.NONE && !patch.isRestricted() && !hasMessaged) {
							autoJoin = true;
						}
					}
				}
			}
			callbackManager.onActivityResult(requestCode, resultCode, intent);
		}
		super.onActivityResult(requestCode, resultCode, intent);
	}

	private void onShareButtonClick(View view) {

		if (!UserManager.shared().authenticated()) {
			UserManager.shared().showGuestGuard(this, "Sign up for a free account to send patch invites and more.");
			return;
		}

		if (entity != null) {
			share();
		}
	}

	protected void onJoinButtonClick(View view) {

		if (entity == null) return;

		if (!UserManager.shared().authenticated()) {
			UserManager.shared().showGuestGuard(this, "Sign up for a free account to watch patches and more.");
			return;
		}

		/* Cancel request */
		if (watchStatus == WatchStatus.WATCHING) {
			if (((Patch) entity).isRestrictedForCurrentUser()) {
				confirmLeave();
			}
			else {
				watch(false /* delete */);
			}
		}
		else if (watchStatus == WatchStatus.REQUESTED) {
			watch(false /* delete */);
		}
		else if (watchStatus == WatchStatus.NONE) {
			watch(true /* insert */);
		}
	}

	private void onMembersListButtonClick(View view) {
		if (entity != null) {
			Bundle extras = new Bundle();
			extras.putString(Constants.EXTRA_LIST_LINK_TYPE, Constants.TYPE_LINK_WATCH);
			extras.putInt(Constants.EXTRA_LIST_TITLE_RESID, R.string.form_title_watching_list);
			extras.putInt(Constants.EXTRA_LIST_ITEM_RESID, R.layout.temp_listitem_user);
			extras.putInt(Constants.EXTRA_TRANSITION_TYPE, TransitionType.DRILL_TO);
			Patchr.router.route(this, Route.USER_LIST, entity, extras);
		}
	}

	private void onMuteButtonClick(View view) {
		Link link = entity.linkFromAppUser(Constants.TYPE_LINK_WATCH);
		mute(link.mute == null || !link.mute);
	}

	private void onTuneButtonClick(View view) {
		Patchr.router.route(this, Route.TUNE, entity, null);
	}

	/*--------------------------------------------------------------------------------------------
	 * Notifications
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe(threadMode = ThreadMode.MAIN) public void onEntityResult(final EntityQueryResultEvent event) {

		if (event.actionType == ActionType.ACTION_GET_ENTITY) {

			if (event.entity != null && event.entity.id != null && event.entity.id.equals(entityId)) {

				Boolean firstBind = (entity == null);
				this.entity = event.entity;
				this.listPresenter.scopingEntity = event.entity;
				this.listPresenter.scopingEntityId = event.entity.id;
				if (firstBind && referrerName != null) {     // Active invitation
					showInviteWelcome(1500);
				}
				Patchr.getInstance().setCurrentPatch(entity);
				if (firstBind) {
					makeBranchLink();           // Create or refresh so it's ready and correct
				}
				bind();
				supportInvalidateOptionsMenu();    // In case user authenticated
				this.listPresenter.fetch(FetchMode.AUTO);
			}
		}
	}

	@Subscribe public void onProcessingComplete(ProcessingCompleteEvent event) {
		/*
		 * Gets called direct at the activity level and receives
		 * events from fragments.
		 */
		processing = false;
		uiController.getBusyController().hide(false);

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				/*
				 * Non-members can't add messages to private patches.
				 */
				if (entity != null && entity instanceof Patch) {
					Patch patch = (Patch) entity;
					ListPresenter controller = ((EntityListFragment) currentFragment).listPresenter;
				}
			}
		});
	}

	@Subscribe public void onWatchStatusChangedEvent(WatchStatusChangedEvent event) {
		onRefresh();
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			entityId = extras.getString(Constants.EXTRA_ENTITY_ID);
			listLinkType = extras.getString(Constants.EXTRA_LIST_LINK_TYPE);
			transitionType = extras.getInt(Constants.EXTRA_TRANSITION_TYPE, TransitionType.FORM_TO);
			notificationId = extras.getString(Constants.EXTRA_NOTIFICATION_ID);
			referrerName = extras.getString(Constants.EXTRA_INVITER_NAME);
			referrerPhotoUrl = extras.getString(Constants.EXTRA_INVITER_PHOTO_URL);
		}
	}

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		assert this.rootView != null;

		this.header = new PatchDetailView(this);

		this.listPresenter = new ListPresenter(this);
		this.listPresenter.listView = (AbsListView) ((ViewGroup) this.rootView.findViewById(R.id.swipe)).getChildAt(1);
		this.listPresenter.listItemResId = R.layout.temp_listitem_message;
		this.listPresenter.busyController = new BusyController();
		this.listPresenter.busyController.setProgressBar(this.rootView.findViewById(R.id.list_progress));
		this.listPresenter.emptyController = new EmptyController(this.rootView.findViewById(R.id.list_message));
		this.listPresenter.emptyController.setLabel(StringManager.getString(R.string.button_list_share));
		this.listPresenter.headerView = this.header;

		this.listPresenter.query = EntitiesQueryEvent.build(ActionType.ACTION_GET_ENTITIES
				, Integers.getInteger(R.integer.page_size_messages)
				, Maps.asMap("enabled", true)
				, Link.Direction.in.name()
				, Constants.TYPE_LINK_CONTENT
				, Constants.SCHEMA_ENTITY_MESSAGE
				, this.entityId);

		/* Inject swipe refresh component - listController performs operations that impact swipe behavior */
		SwipeRefreshLayout swipeRefresh = (SwipeRefreshLayout) this.rootView.findViewById(R.id.swipe);
		if (swipeRefresh != null) {
			swipeRefresh.setColorSchemeColors(Colors.getColor(UI.getResIdForAttribute(this, R.attr.refreshColor)));
			swipeRefresh.setProgressBackgroundColorSchemeResource(UI.getResIdForAttribute(this, R.attr.refreshColorBackground));
			swipeRefresh.setOnRefreshListener(this);
			swipeRefresh.setRefreshing(false);
			swipeRefresh.setEnabled(true);
			this.listPresenter.busyController.setSwipeRefresh(swipeRefresh);
		}

		View footer = LayoutInflater.from(this).inflate(R.layout.widget_list_footer_message, null);
		View header = LayoutInflater.from(this).inflate(R.layout.entity_form, null);
		listPresenter.footerView = footer;
		listPresenter.headerView = header;

		this.listPresenter.initialize(this, this.rootView);        // We init after everything is setup

		bottomSheetLayout = (BottomSheetLayout) findViewById(R.id.bottomsheet);
		if (bottomSheetLayout != null) bottomSheetLayout.setPeekOnDismiss(true);
		callbackManager = CallbackManager.Factory.create();

		NfcAdapter nfc = NfcAdapter.getDefaultAdapter(Patchr.applicationContext);

		if (nfc != null) {
			nfc.setNdefPushMessageCallback(this, this);
			nfc.setOnNdefPushCompleteCallback(this, this);
		}
	}

	@Override protected int getLayoutId() {
		return R.layout.patch_form;
	}

	public void fetch(final FetchMode mode) {
		/*
		 * Called on main thread.
		 */
		Logger.v(this, "Fetching: " + mode.name().toString());

		Boolean currentUser = UserManager.shared().authenticated() && UserManager.currentUser.id.equals(this.entityId);
		Integer linkProfile = currentUser ? LinkSpecType.LINKS_FOR_USER_CURRENT : LinkSpecType.LINKS_FOR_USER;

		EntityQueryEvent request = new EntityQueryEvent();
		request.setLinkProfile(linkProfile)
				.setActionType(ActionType.ACTION_GET_ENTITY)
				.setFetchMode(mode)
				.setEntityId(this.entityId)
				.setTag(System.identityHashCode(this));

		if (this.bound && this.entity != null && mode != FetchMode.MANUAL) {
			request.setCacheStamp(this.entity.getCacheStamp());
		}

		Dispatcher.getInstance().post(request);
	}

	public void bind() {
		assert this.entity != null;
		header.databind(this.entity);
	}

	public void share() {

		final String patchName = (entity.name != null) ? entity.name : StringManager.getString(R.string.container_singular_lowercase);
		final String title = String.format(StringManager.getString(R.string.label_patch_share_title), patchName);
		final Activity activity = this;

		MenuSheetView menuSheetView = new MenuSheetView(this, MenuSheetView.MenuType.GRID, "Invite friends using...", new MenuSheetView.OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				if (item.getItemId() == R.id.invite_using_patchr) {
					/*
					 * Go to patchr share directly but looks just like an external share
					 */
					final IntentBuilder intentBuilder = new IntentBuilder(activity, MessageEdit.class);
					final Intent intent = intentBuilder.create();
					intent.putExtra(Constants.EXTRA_SHARE_SOURCE, getPackageName());
					intent.putExtra(Constants.EXTRA_SHARE_ID, entityId);
					intent.putExtra(Constants.EXTRA_SHARE_SCHEMA, Constants.SCHEMA_ENTITY_PATCH);
					intent.setAction(Intent.ACTION_SEND);
					activity.startActivity(intent);
					AnimationManager.doOverridePendingTransition(activity, TransitionType.FORM_TO);
				}
				else if (item.getItemId() == R.id.invite_using_facebook) {
					FacebookProvider provider = new FacebookProvider();
					provider.invite(title);
				}
				else if (item.getItemId() == R.id.invite_using_other) {
					BranchProvider provider = new BranchProvider();
					provider.invite(title);
				}

				bottomSheetLayout.dismissSheet();
				return true;
			}
		});

		menuSheetView.inflateMenu(R.menu.menu_invite_sheet);
		bottomSheetLayout.setPeekOnDismiss(true);
		bottomSheetLayout.showWithSheetView(menuSheetView, new InsetViewTransformer());
	}

	protected void confirmJoin() {

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				final AlertDialog dialog = Dialogs.alertDialog(null
						, null
						, StringManager.getString(R.string.alert_autowatch_message)
						, null
						, Patchr.applicationContext
						, R.string.alert_autowatch_positive
						, R.string.alert_autowatch_negative
						, null
						, new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								if (which == DialogInterface.BUTTON_POSITIVE) {
									watch(true /* activate */);
								}
							}
						}, null);
				dialog.setCanceledOnTouchOutside(false);
			}
		});
	}

	public void watch(final boolean activate) {

		final boolean enabled = !(((Patch) entity).isRestrictedForCurrentUser());

		if (activate) {

			/* Used as part of link management */
			Shortcut fromShortcut = UserManager.currentUser.getAsShortcut();
			Shortcut toShortcut = entity.getAsShortcut();

			LinkInsertEvent update = new LinkInsertEvent()
					.setFromId(UserManager.currentUser.id)
					.setToId(entity.id)
					.setType(Constants.TYPE_LINK_WATCH)
					.setEnabled(enabled)
					.setFromShortcut(fromShortcut)
					.setToShortcut(toShortcut)
					.setActionEvent(((Patch) entity).isVisibleToCurrentUser() ? "watch_entity_patch" : "request_watch_entity")
					.setSkipCache(false);

			update.setActionType(ActionType.ACTION_LINK_INSERT_WATCH)
					.setTag(System.identityHashCode(this));

			Dispatcher.getInstance().post(update);
		}
		else {

			LinkDeleteEvent update = new LinkDeleteEvent()
					.setFromId(UserManager.currentUser.id)
					.setToId(entity.id)
					.setType(Constants.TYPE_LINK_WATCH)
					.setEnabled(enabled)
					.setSchema(entity.schema)
					.setActionEvent("unwatch_entity_" + entity.schema.toLowerCase(Locale.US));

			update.setActionType(ActionType.ACTION_LINK_DELETE_WATCH)
					.setTag(System.identityHashCode(this));

			Dispatcher.getInstance().post(update);
		}
	}

	public void mute(final Boolean mute) {

		final Link link = entity.linkFromAppUser(Constants.TYPE_LINK_WATCH);
		final String actionEvent = mute ? "mute_watch_entity" : "unmute_watch_entity";

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				if (header != null) {
					ViewAnimator animator = (ViewAnimator) header.bannerView.muteButton;
					if (animator != null) {
						animator.setDisplayedChild(1);  // Turned off in drawButtons
					}
				}
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncMuteLink");
				return DataController.getInstance().muteLink(link.id, mute, actionEvent);
			}

			@Override
			protected void onPostExecute(Object response) {
				//				bind(BindingMode.AUTO);
				//				onProcessingComplete(); // Updates ui like floating button
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	protected void confirmLeave() {
		/* User (non-owner) wants to unwatch a private patch */
		final AlertDialog dialog = Dialogs.alertDialog(null
				, null
				, StringManager.getString(R.string.alert_unwatch_message)
				, null
				, Patchr.applicationContext
				, R.string.alert_unwatch_positive
				, android.R.string.cancel
				, null
				, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (which == DialogInterface.BUTTON_POSITIVE) {
							justApproved = false;
							watch(false /* delete */);
						}
					}
				}, null);
		dialog.setCanceledOnTouchOutside(false);
	}

	private void makeBranchLink() {

		BranchProvider provider = new BranchProvider();
		BranchUniversalObject applink = provider.buildApplink();
		LinkProperties linkProperties = new LinkProperties()
				.setChannel("patchr-android")
				.setFeature(Branch.FEATURE_TAG_INVITE);

		applink.generateShortUrl(PatchScreen.this, linkProperties, new Branch.BranchLinkCreateListener() {
			@Override public void onLinkCreate(String uri, BranchError error) {
				if (error == null) {
					branchLink = uri;
				}
			}
		});
	}

	private void showInviteWelcome(int delay) {

		final Patch patch = (Patch) entity;

		/* Don't show invite if already a member */
		if (UserManager.shared().authenticated() && patch.watchStatus() != WatchStatus.NONE) {
			return;
		}

		handler.postDelayed(new Runnable() {

			@Override public void run() {

				View view = LayoutInflater.from(PatchScreen.this).inflate(R.layout.onboarding_view, null, false);

				String heading = (referrerName != null)
				                 ? String.format("%1$s invites you to join this patch.", referrerName)
				                 : "A friend invites you to join this patch.";
				((TextView) view.findViewById(R.id.message)).setText(heading);

				ImageView imageView = (ImageView) view.findViewById(R.id.user_photo);

				if (referrerPhotoUrl != null) {
					Picasso.with(Patchr.applicationContext)
							.load(Uri.parse(referrerPhotoUrl))
							.config(Bitmap.Config.RGB_565)
							.transform(new CircleTransform())
							.into(imageView);
				}
				else {
					imageView.setVisibility(View.GONE);
				}

				if (UserManager.shared().authenticated()) {

					authenticatedForInvite = true;
					((TextView) view.findViewById(R.id.action2_button)).setVisibility(View.GONE);
					((TextView) view.findViewById(R.id.action1_button)).setText("JOIN");
					((Button) view.findViewById(R.id.action1_button)).setOnClickListener(new View.OnClickListener() {
						@Override public void onClick(View view) {
							bottomSheetLayout.dismissSheet();
							onJoinButtonClick(view);
						}
					});
				}
				else {
					authenticatedForInvite = false;
					((TextView) view.findViewById(R.id.action1_button)).setText("LOG IN");
					((Button) view.findViewById(R.id.action1_button)).setOnClickListener(new View.OnClickListener() {
						@Override public void onClick(View v) {
							Patchr.router.route(PatchScreen.this, Route.LOGIN, null, null);
						}
					});
					((TextView) view.findViewById(R.id.action2_button)).setText("SIGN UP");
					((Button) view.findViewById(R.id.action2_button)).setOnClickListener(new View.OnClickListener() {
						@Override public void onClick(View v) {
							Patchr.router.route(PatchScreen.this, Route.SIGNUP, null, null);
						}
					});
				}

				bottomSheetLayout.showWithSheetView(view, new InsetViewTransformer(0.2f, 0.95f));
				handler.removeCallbacks(this);
			}
		}, delay);
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	public class BranchProvider {

		public BranchUniversalObject buildApplink() {

			final String patchName = (entity.name != null) ? entity.name : StringManager.getString(R.string.container_singular_lowercase);
			final String referrerName = UserManager.currentUser.name;
			final String referrerId = UserManager.currentUser.id;
			final String ownerName = entity.owner.name;
			final String path = "patch/" + entityId;

			BranchUniversalObject applink = new BranchUniversalObject()
					.setCanonicalIdentifier(path)
					.setTitle(String.format("Invite by %1$s to the %2$s patch", referrerName, patchName)) // $og_title
					.addContentMetadata("entityId", entityId)
					.addContentMetadata("entitySchema", Constants.SCHEMA_ENTITY_PATCH)
					.addContentMetadata("referrerName", referrerName)
					.addContentMetadata("referrerId", referrerId)
					.addContentMetadata("ownerName", ownerName)
					.addContentMetadata("patchName", patchName);

			if (UserManager.currentUser.photo != null) {
				Photo photo = UserManager.currentUser.photo;
				applink.addContentMetadata("referrerPhotoUrl", photo.uri(PhotoCategory.PROFILE));
			}

			if (entity.photo != null) {
				Photo photo = entity.photo;
				String settings = "h=500&crop&fit=crop&q=50";
				String photoUrl = String.format("https://3meters-images.imgix.net/%1$s?%2$s", photo.prefix, settings);
				applink.setContentImageUrl(photoUrl);  // $og_image_url
			}

			if (entity.description != null) {
				applink.setContentDescription(entity.description); // $og_description
			}

			return applink;
		}

		public void invite(final String title) {

			final String patchName = (entity.name != null) ? entity.name : StringManager.getString(R.string.container_singular_lowercase);
			final String referrerName = UserManager.currentUser.name;

			BranchUniversalObject applink = buildApplink();

			LinkProperties linkProperties = new LinkProperties()
					.setChannel("patchr-android")
					.setFeature(Branch.FEATURE_TAG_INVITE);

			applink.generateShortUrl(PatchScreen.this, linkProperties, new Branch.BranchLinkCreateListener() {

				@Override
				public void onLinkCreate(String url, BranchError error) {

					if (error == null) {
						ShareCompat.IntentBuilder builder = ShareCompat.IntentBuilder.from(PatchScreen.this);
						builder.setChooserTitle(title);
						builder.setType("text/plain");
					/*
					 * subject: Invitation to the \'%1$s\' patch
					 * body: %1$s has invited you to the %2$s patch! %3$s
					 */
						builder.setSubject(String.format(StringManager.getString(R.string.label_patch_share_subject), patchName));
						builder.setText(String.format(StringManager.getString(R.string.label_patch_share_body), referrerName, patchName, url));

						builder.getIntent().putExtra(Constants.EXTRA_SHARE_SOURCE, getPackageName());
						builder.getIntent().putExtra(Constants.EXTRA_SHARE_ID, entityId);
						builder.getIntent().putExtra(Constants.EXTRA_SHARE_SCHEMA, Constants.SCHEMA_ENTITY_PATCH);

						builder.startChooser();
					}
				}
			});
		}
	}

	public class FacebookProvider {

		public void invite(final String title) {

			AppInviteDialog inviteDialog = new AppInviteDialog(PatchScreen.this);

			if (AppInviteDialog.canShow()) {

				String patchName = (entity.name != null) ? entity.name : StringManager.getString(R.string.container_singular_lowercase);
				String patchPhotoUrl = null;
				String referrerNameEncoded = Utils.encode(UserManager.currentUser.name);
				String referrerPhotoUrl = "";

				if (UserManager.currentUser.photo != null) {
					Photo photo = UserManager.currentUser.photo;
					String photoUrlEncoded = Utils.encode(photo.uri(PhotoCategory.PROFILE));
					referrerPhotoUrl = String.format("&referrerPhotoUrl=%1$s", photoUrlEncoded);
				}

				String queryString = String.format("entityId=%1$s&entitySchema=%2$s&referrerName=%3$s%4$s", entity.id, entity.schema, referrerNameEncoded, referrerPhotoUrl);
				Uri applink = Uri.parse(String.format("https://fb.me/934234473291708?%1$s", queryString));

				if (entity.photo != null) {
					Photo photo = entity.photo;
					String patchNameEncoded = Utils.encode(patchName);
					String settings = "w=1200&h=628&crop&fit=crop&q=25&txtsize=96&txtalign=left,bottom&txtcolor=fff&txtshad=5&txtpad=60&txtfont=Helvetica%20Neue%20Light";
					patchPhotoUrl = String.format("https://3meters-images.imgix.net/%1$s?%2$s&txt=%3$s", photo.prefix, settings, patchNameEncoded);
				}

				AppInviteContent.Builder builder = new AppInviteContent.Builder();
				builder.setApplinkUrl(applink.toString());
				if (patchPhotoUrl != null) {
					builder.setPreviewImageUrl(patchPhotoUrl);
				}

				inviteDialog.registerCallback(callbackManager, new FacebookCallback<AppInviteDialog.Result>() {

					@Override
					public void onSuccess(AppInviteDialog.Result result) {
						UI.showToastNotification("Facebook invites sent", Toast.LENGTH_SHORT);
					}

					@Override
					public void onCancel() {
						UI.showToastNotification("Facebook invite cancelled", Toast.LENGTH_SHORT);
					}

					@Override
					public void onError(FacebookException error) {
						Logger.w(this, String.format("Facebook invite error: %1$s", error.toString()));
					}
				});
				AppInviteDialog.show(PatchScreen.this, builder.build());
			}
		}
	}
}