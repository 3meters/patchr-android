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
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
import com.patchr.events.DataQueryResultEvent;
import com.patchr.events.EntitiesQueryEvent;
import com.patchr.events.EntityQueryEvent;
import com.patchr.events.EntityQueryResultEvent;
import com.patchr.events.LinkDeleteEvent;
import com.patchr.events.LinkInsertEvent;
import com.patchr.events.NotificationReceivedEvent;
import com.patchr.objects.ActionType;
import com.patchr.objects.Command;
import com.patchr.objects.Count;
import com.patchr.objects.Entity;
import com.patchr.objects.FetchMode;
import com.patchr.objects.Link;
import com.patchr.objects.LinkSpecType;
import com.patchr.objects.Patch;
import com.patchr.objects.Photo;
import com.patchr.objects.PhotoCategory;
import com.patchr.objects.Shortcut;
import com.patchr.objects.TransitionType;
import com.patchr.objects.WatchStatus;
import com.patchr.ui.components.BusyPresenter;
import com.patchr.ui.components.CircleTransform;
import com.patchr.ui.components.EmptyPresenter;
import com.patchr.ui.components.InsetViewTransformer;
import com.patchr.ui.components.RecyclePresenter;
import com.patchr.ui.edit.MessageEdit;
import com.patchr.ui.views.PatchDetailView;
import com.patchr.utilities.Colors;
import com.patchr.utilities.Dialogs;
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
public class PatchScreen extends BaseScreen implements SwipeRefreshLayout.OnRefreshListener
		, NfcAdapter.CreateNdefMessageCallback
		, NfcAdapter.OnNdefPushCompleteCallback {

	private final Handler handler = new Handler();

	private   PatchDetailView   header;
	private   RecyclePresenter  listPresenter;
	protected BottomSheetLayout bottomSheetLayout;

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

		Patchr.getInstance().setCurrentPatch(entity);

		bind();                             // Shows any data we already have
		fetch(FetchMode.AUTO);              // Checks for data changes and binds again if needed
		if (this.listPresenter != null) {
			this.listPresenter.onResume();  // Update ui
		}

		/* Check for invitation */
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
			joinAction();
			UI.showToastNotification("You are now a member of this patch!", Toast.LENGTH_SHORT);
			autoJoin = false;
		}
	}

	@Override protected void onPause() {
		super.onPause();
		if (this.listPresenter != null) {
			this.listPresenter.onPause();
		}
	}

	@Override protected void onStop() {
		Dispatcher.getInstance().unregister(this);
		super.onStop();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	public void onClick(View view) {
		/*
		 * Base activity broadcasts view clicks that target onViewClick. There are actions
		 * that should be handled at the activity level like add a new entity.
		 */
		if (processing) return;

		processing = true;
		Integer id = view.getId();

		/* Action button redirects based on tag */
		if (id == R.id.action_button) {
			id = (Integer) view.getTag();
		}

		if (id == R.id.fab) {
			addAction();
		}
		else if (id == R.id.invite) {
			inviteAction();
		}
		else if (id == R.id.join_button) {
			joinAction();
		}
		else if (id == R.id.members_button) {
			memberListAction();
		}
		else if (id == R.id.tune_button) {
			tuneAction();
		}
		else if (id == R.id.mute_button) {
			muteAction();
		}
		else if (view.getTag() != null) {
			if (view.getTag() instanceof Photo) {
				Photo photo = (Photo) view.getTag();
				navigateToPhoto(photo);
			}
			else if (view.getTag() instanceof Entity) {
				final Entity entity = (Entity) view.getTag();
				navigateToEntity(entity);
			}
		}

		processing = false;
	}

	@Override public boolean onCreateOptionsMenu(Menu menu) {

		this.optionMenu = menu;

		/* Shown for owner */
		getMenuInflater().inflate(R.menu.menu_edit, menu);
		getMenuInflater().inflate(R.menu.menu_delete, menu);

		/* Shown for everyone */
		getMenuInflater().inflate(R.menu.menu_invite, menu);
		getMenuInflater().inflate(R.menu.menu_refresh, menu);
		getMenuInflater().inflate(R.menu.menu_leave_patch, menu);

		getMenuInflater().inflate(R.menu.menu_login, menu);         // base
		getMenuInflater().inflate(R.menu.menu_map, menu);           // base
		getMenuInflater().inflate(R.menu.menu_report, menu);        // base

		configureStandardMenuItems(menu);   // Tweaks based on permissions
		return true;
	}

	@Override public boolean onOptionsItemSelected(MenuItem item) {

		if (item.getItemId() == R.id.delete) {
			deleteAction();
		}
		else if (item.getItemId() == R.id.edit) {
			editAction();
		}
		else if (item.getItemId() == R.id.invite) {
			inviteAction();
		}
		else if (item.getItemId() == R.id.refresh) {
			onRefresh();
		}
		else if (item.getItemId() == R.id.leave_patch) {
			joinAction();
		}
		else {
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	@Override public void onRefresh() {
		fetch(FetchMode.MANUAL);
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

	public void onFetchComplete() {
		super.onFetchComplete();            // Handles busy ui
		supportInvalidateOptionsMenu();     // In case user authenticated
	}

	/*--------------------------------------------------------------------------------------------
	 * Notifications
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe(threadMode = ThreadMode.MAIN) public void onEntityResult(final EntityQueryResultEvent event) {

		if (event.actionType == ActionType.ACTION_GET_ENTITY) {
			if (event.entity != null && event.entity.id != null && event.entity.id.equals(entityId)) {
				if (event.error != null) {
					onFetchComplete();
					return;
				}

				this.bound = true;

				if (!event.noop) {

					Logger.v(this, "Data result accepted: " + event.actionType.name());
					Boolean firstBind = (this.entity == null);

					this.entity = event.entity;
					this.listPresenter.scopingEntity = event.entity;
					this.listPresenter.scopingEntityId = event.entity.id;
					Patchr.getInstance().setCurrentPatch(entity);   // Fresh!

					if (firstBind && referrerName != null) {     // Active invitation
						showInviteWelcome(1500);
					}

					if (firstBind) {
						makeBranchLink();           // Create or refresh so it's ready and correct
					}

					this.listPresenter.fetch(event.fetchMode); // Next in the chain
				}

				onFetchComplete();
				bind();
			}
		}
	}

	@Subscribe(threadMode = ThreadMode.MAIN) public void onNotificationReceived(final NotificationReceivedEvent event) {
		/*
		 * Refresh the form because something new has been added to it like a message.
		 */
		if ((event.notification.parentId != null && event.notification.parentId.equals(entityId))
				|| (event.notification.targetId != null && event.notification.targetId.equals(entityId))) {

			if (event.notification.event.equals("approve_watch_entity")) {
				justApproved = true;
			}
			fetch(FetchMode.AUTO);
		}
	}

	@Subscribe public void onDataResult(final DataQueryResultEvent event) {

		/* Can be called on background thread */
		if (event.tag.equals(System.identityHashCode(this))
				&& (event.entity == null || event.entity.id.equals(entityId))) {

			Logger.v(this, "Data result accepted: " + event.actionType.name());
			if (event.error == null) {
				if (event.actionType == ActionType.ACTION_LINK_INSERT_MEMBER
						|| event.actionType == ActionType.ACTION_LINK_DELETE_MEMBER) {
					fetch(FetchMode.AUTO);
				}
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Actions
	 *--------------------------------------------------------------------------------------------*/

	private void muteAction() {
		Link link = entity.linkFromAppUser(Constants.TYPE_LINK_MEMBER);
		mute(link.mute == null || !link.mute);
	}

	private void tuneAction() {
		Patchr.router.route(this, Command.TUNE, entity, null);
	}

	public void addAction() {

		if (!UserManager.shared().authenticated()) {
			UserManager.shared().showGuestGuard(this, "Sign up for a free account to post messages and more.");
			return;
		}

		if (entity == null) return;

		if (MenuManager.canUserAdd(entity)) {

			Bundle extras = new Bundle();
			extras.putString(Constants.EXTRA_ENTITY_PARENT_ID, entityId);
			extras.putString(Constants.EXTRA_ENTITY_SCHEMA, Constants.SCHEMA_ENTITY_MESSAGE);
			Patchr.router.add(this, Constants.SCHEMA_ENTITY_MESSAGE, extras, true);
		}
	}

	private void inviteAction() {

		if (!UserManager.shared().authenticated()) {
			UserManager.shared().showGuestGuard(this, "Sign up for a free account to send patch invites and more.");
			return;
		}

		if (entity != null) {
			share();
		}
	}

	protected void joinAction() {

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
				join(false /* delete */);
			}
		}
		else if (watchStatus == WatchStatus.REQUESTED) {
			join(false /* delete */);
		}
		else if (watchStatus == WatchStatus.NONE) {
			join(true /* insert */);
		}
	}

	private void memberListAction() {
		if (entity != null) {
			Bundle extras = new Bundle();
			extras.putInt(Constants.EXTRA_LIST_ITEM_RESID, R.layout.temp_listitem_user);
			extras.putString(Constants.EXTRA_LIST_LINK_DIRECTION, Link.Direction.in.name());
			extras.putString(Constants.EXTRA_LIST_LINK_SCHEMA, Constants.SCHEMA_ENTITY_USER);
			extras.putString(Constants.EXTRA_LIST_LINK_TYPE, Constants.TYPE_LINK_MEMBER);
			extras.putInt(Constants.EXTRA_LIST_TITLE_RESID, R.string.form_title_member_list);
			extras.putInt(Constants.EXTRA_TRANSITION_TYPE, TransitionType.DRILL_TO);
			Patchr.router.route(this, Command.ENTITY_LIST, entity, extras);
		}
	}

	public void editAction() {
		Bundle extras = new Bundle();
		Patchr.router.edit(this, entity, extras, true);
	}

	public void deleteAction() {
		confirmDelete();
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			entityId = extras.getString(Constants.EXTRA_ENTITY_ID);
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

		this.listPresenter = new RecyclePresenter(this);
		this.listPresenter.recycleView = (RecyclerView) this.rootView.findViewById(R.id.entity_list);
		this.listPresenter.listItemResId = R.layout.temp_listitem_message;
		this.listPresenter.busyPresenter = new BusyPresenter();
		this.listPresenter.busyPresenter.setProgressBar(this.rootView.findViewById(R.id.list_progress));
		this.listPresenter.emptyPresenter = new EmptyPresenter(this.rootView.findViewById(R.id.list_message));
		this.listPresenter.emptyPresenter.setLabel(StringManager.getString(R.string.button_list_share));
		this.listPresenter.headerView = this.header;

		this.listPresenter.query = EntitiesQueryEvent.build(ActionType.ACTION_GET_ENTITIES
				, Maps.asMap("enabled", true)
				, Link.Direction.in.name()
				, Constants.TYPE_LINK_CONTENT
				, Constants.SCHEMA_ENTITY_MESSAGE
				, this.entityId);

		/* Inject swipe refresh component - listController performs operations that impact swipe behavior */
		SwipeRefreshLayout swipeRefresh = (SwipeRefreshLayout) this.rootView.findViewById(R.id.swipe);
		if (swipeRefresh != null) {
			swipeRefresh.setColorSchemeColors(Colors.getColor(R.color.brand_accent));
			swipeRefresh.setProgressBackgroundColorSchemeResource(UI.getResIdForAttribute(this, R.attr.refreshColorBackground));
			swipeRefresh.setOnRefreshListener(this);
			swipeRefresh.setRefreshing(false);
			swipeRefresh.setEnabled(true);
			this.listPresenter.busyPresenter.setSwipeRefresh(swipeRefresh);
		}

		View footer = LayoutInflater.from(this).inflate(R.layout.widget_list_footer_message, null);
		listPresenter.footerView = footer;

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
		return R.layout.patch_screen;
	}

	@Override public void configureStandardMenuItems(final Menu menu) {

		super.configureStandardMenuItems(menu);
		MenuItem menuItem = menu.findItem(R.id.leave_patch);
		if (menuItem != null) {
			if (entity != null) {
				Integer watchStatus = ((Patch) entity).watchStatus();
				menuItem.setVisible(watchStatus == WatchStatus.WATCHING);
			}
		}
	}

	public void fetch(final FetchMode mode) {
		/*
		 * Called on main thread.
		 */
		Logger.v(this, "Fetching: " + mode.name().toString());

		EntityQueryEvent request = new EntityQueryEvent();
		request.setLinkProfile(LinkSpecType.LINKS_FOR_PATCH)
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
		if (this.entity != null) {

			watchStatus = ((Patch) entity).watchStatus();
			header.bind(this.entity);

			if (listPresenter != null) {
				listPresenter.bind();
			}

			/* Bind action button */
			{
				ViewGroup actionView = (ViewGroup) header.findViewById(R.id.action_group);

				if (actionView != null && entity != null) {

					Patch patch = (Patch) entity;
					Boolean owner = (UserManager.shared().authenticated() && patch.ownerId != null && patch.ownerId.equals(UserManager.currentUser.id));
					Boolean hasMessaged = (entity.linkByAppUser(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_MESSAGE) != null);
					Boolean isPublic = (patch.privacy != null
							&& patch.privacy.equals(Constants.PRIVACY_PUBLIC)
							&& patch.isVisibleToCurrentUser());

					TextView buttonAlert = (TextView) actionView.findViewById(R.id.action_button);
					if (buttonAlert == null) return;

					Count requestCount = entity.getCount(Constants.TYPE_LINK_MEMBER, null, false, Link.Direction.in);

					if (watchStatus == WatchStatus.NONE) {
						buttonAlert.setText(R.string.button_list_watch_request);
						buttonAlert.setTag(R.id.join_button);
						return;
					}

					/* Owner */

					if (owner) {
						/*
						 * - Member requests then alert to handle
						 * - No messages then alert to invite
						 */
						if (requestCount != null) {
							String requests = getResources().getQuantityString(R.plurals.button_pending_requests, requestCount.count.intValue(), requestCount.count.intValue());
							buttonAlert.setText(requests);
							buttonAlert.setTag(R.id.members_button);
						}
						else {
							buttonAlert.setText(StringManager.getString(R.string.button_list_share));
							buttonAlert.setTag(R.id.invite);
						}
					}

					/* Members */

					else {
						if (isPublic) {
							if (!hasMessaged) {
								buttonAlert.setText(StringManager.getString(R.string.button_no_message));
								buttonAlert.setTag(R.id.fab);
							}
							else {
								buttonAlert.setText(StringManager.getString(R.string.button_list_share));
								buttonAlert.setTag(R.id.invite);
							}
						}
						else {
							if (watchStatus == WatchStatus.REQUESTED) {
								buttonAlert.setText(R.string.button_list_watch_request_cancel);
								buttonAlert.setTag(R.id.join_button);
							}
							else if (watchStatus == WatchStatus.WATCHING) {
								if (!hasMessaged) {
									buttonAlert.setText(StringManager.getString(R.string.button_no_message));
									buttonAlert.setTag(R.id.fab);
								}
								else {
									buttonAlert.setText(StringManager.getString(R.string.button_list_share));
									buttonAlert.setTag(R.id.invite);
								}
							}

							if (justApproved) {  // We add a little sugar by using a flag set by an 'approved' notification
								if (hasMessaged) {
									buttonAlert.setText(StringManager.getString(R.string.button_just_approved));
									buttonAlert.setTag(R.id.invite);
								}
								else {
									buttonAlert.setText(StringManager.getString(R.string.button_just_approved_no_message));
									buttonAlert.setTag(R.id.fab);
								}
							}
						}
					}
				}
			}
		}
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

	public void join(final boolean activate) {

		final boolean enabled = !(((Patch) entity).isRestrictedForCurrentUser());

		if (activate) {

			/* Used as part of link management */
			Shortcut fromShortcut = UserManager.currentUser.getAsShortcut();
			Shortcut toShortcut = entity.getAsShortcut();

			LinkInsertEvent update = new LinkInsertEvent()
					.setFromId(UserManager.currentUser.id)
					.setToId(entity.id)
					.setType(Constants.TYPE_LINK_MEMBER)
					.setEnabled(enabled)
					.setFromShortcut(fromShortcut)
					.setToShortcut(toShortcut)
					.setActionEvent(((Patch) entity).isVisibleToCurrentUser() ? "watch_entity_patch" : "request_watch_entity")
					.setSkipCache(false);

			update.setActionType(ActionType.ACTION_LINK_INSERT_MEMBER)
					.setTag(System.identityHashCode(this));

			Dispatcher.getInstance().post(update);
		}
		else {

			LinkDeleteEvent update = new LinkDeleteEvent()
					.setFromId(UserManager.currentUser.id)
					.setToId(entity.id)
					.setType(Constants.TYPE_LINK_MEMBER)
					.setEnabled(enabled)
					.setSchema(entity.schema)
					.setActionEvent("unwatch_entity_" + entity.schema.toLowerCase(Locale.US));

			update.setActionType(ActionType.ACTION_LINK_DELETE_MEMBER)
					.setTag(System.identityHashCode(this));

			Dispatcher.getInstance().post(update);
		}
	}

	public void mute(final Boolean mute) {

		final Link link = entity.linkFromAppUser(Constants.TYPE_LINK_MEMBER);
		final String actionEvent = mute ? "mute_watch_entity" : "unmute_watch_entity";

		new AsyncTask() {

			@Override protected void onPreExecute() {
				if (header != null) {
					ViewAnimator animator = (ViewAnimator) header.bannerView.muteButton;
					if (animator != null) {
						animator.setDisplayedChild(1);  // Turned off in drawButtons
					}
				}
			}

			@Override protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncMuteLink");
				return DataController.getInstance().muteLink(link.id, mute, actionEvent);
			}

			@Override protected void onPostExecute(Object response) {
				fetch(FetchMode.AUTO);
			}
		}.executeOnExecutor(Constants.EXECUTOR);
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
									join(true /* activate */);
								}
							}
						}, null);
				dialog.setCanceledOnTouchOutside(false);
			}
		});
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
							join(false /* delete */);
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
							joinAction();
						}
					});
				}
				else {
					authenticatedForInvite = false;
					((TextView) view.findViewById(R.id.action1_button)).setText("LOG IN");
					((Button) view.findViewById(R.id.action1_button)).setOnClickListener(new View.OnClickListener() {
						@Override public void onClick(View v) {
							Patchr.router.route(PatchScreen.this, Command.LOGIN, null, null);
						}
					});
					((TextView) view.findViewById(R.id.action2_button)).setText("SIGN UP");
					((Button) view.findViewById(R.id.action2_button)).setOnClickListener(new View.OnClickListener() {
						@Override public void onClick(View v) {
							Patchr.router.route(PatchScreen.this, Command.SIGNUP, null, null);
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