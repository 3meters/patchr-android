package com.patchr.ui.collections;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.facebook.CallbackManager;
import com.flipboard.bottomsheet.BottomSheetLayout;
import com.flipboard.bottomsheet.commons.MenuSheetView;
import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.AnimationManager;
import com.patchr.components.BranchProvider;
import com.patchr.components.FacebookProvider;
import com.patchr.components.Logger;
import com.patchr.components.MediaManager;
import com.patchr.components.StringManager;
import com.patchr.components.UserManager;
import com.patchr.events.NotificationReceivedEvent;
import com.patchr.model.Photo;
import com.patchr.model.RealmEntity;
import com.patchr.objects.QuerySpec;
import com.patchr.objects.enums.AnalyticsCategory;
import com.patchr.objects.enums.FetchMode;
import com.patchr.objects.enums.LinkType;
import com.patchr.objects.enums.MemberStatus;
import com.patchr.objects.enums.MessageType;
import com.patchr.objects.enums.QueryName;
import com.patchr.objects.enums.State;
import com.patchr.objects.enums.TransitionType;
import com.patchr.service.RestClient;
import com.patchr.ui.MapScreen;
import com.patchr.ui.components.CircleTransform;
import com.patchr.ui.components.InsetViewTransformer;
import com.patchr.ui.components.ListScrollListener;
import com.patchr.ui.edit.MessageEdit;
import com.patchr.ui.edit.PatchEdit;
import com.patchr.ui.edit.ShareEdit;
import com.patchr.ui.views.PatchDetailView;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Reporting;
import com.patchr.utilities.UI;
import com.segment.analytics.Properties;
import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.util.LinkProperties;

@SuppressLint("Registered")
public class PatchScreen extends BaseListScreen {

	private final Handler handler = new Handler();

	protected BottomSheetLayout bottomSheetLayout;
	protected ViewGroup         actionView;

	protected boolean processingCommand;

	protected CallbackManager callbackManager;      // For facebook
	protected String          branchLink;           // Uri
	protected boolean         showReferrerWelcome;
	protected String          referrerName;
	protected String          referrerPhotoUrl;
	protected boolean         autoJoin;
	protected boolean         justApproved;               // Set in onMessage via notification

	@Override public void onResume() {
		super.onResume();

		/* Check for invitation */
		if (entity != null) {
			if (referrerName != null) {     // Active invitation
				if (this.bottomSheetLayout.isSheetShowing()) {
					if (!entity.userMemberStatus.equals(MemberStatus.NonMember)) {
						this.bottomSheetLayout.dismissSheet();
					}
					else {
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
			autoJoin = false;
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public boolean onCreateOptionsMenu(Menu menu) {

		/* Shown for owner */
		getMenuInflater().inflate(R.menu.menu_edit, menu);
		getMenuInflater().inflate(R.menu.menu_delete, menu);

		/* Shown for everyone */
		getMenuInflater().inflate(R.menu.menu_invite, menu);
		getMenuInflater().inflate(R.menu.menu_leave_patch, menu);
		getMenuInflater().inflate(R.menu.menu_report, menu);        // base

		getMenuInflater().inflate(R.menu.menu_login, menu);         // base
		getMenuInflater().inflate(R.menu.menu_map, menu);           // base

		return super.onCreateOptionsMenu(menu);
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
		else if (item.getItemId() == R.id.leave_patch) {
			joinAction();
		}
		else if (item.getItemId() == R.id.map) {
			mapAction();
		}
		else {
			return super.onOptionsItemSelected(item);   // home, report, logout
		}
		return true;
	}

	@Override public void onClick(View view) {
		/*
		 * Base activity broadcasts view clicks that target onViewClick. There are actions
		 * that should be handled at the activity level like add a new entity.
		 */
		if (!processingCommand) {

			processingCommand = true;
			Integer id = view.getId();

			/* Action button redirects based on tag */
			if (id == R.id.action_button) {
				id = (Integer) view.getTag();
			}

			/* Async */
			if (id == R.id.mute_button) {
				muteAction();
			}

			/* Synchronous */
			else {
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
				else if (view.getTag() != null) {
					if (view.getTag() instanceof Photo) {
						Photo photo = (Photo) view.getTag();
						UI.browsePhoto(photo, this);
					}
					else if (view.getTag() instanceof RealmEntity) {
						final RealmEntity entity = (RealmEntity) view.getTag();
						UI.browseEntity(entity.id, this);
					}
				}
				processingCommand = false;
			}
		}
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
				}
			}
			else if (resultCode == Constants.RESULT_USER_LOGGED_IN) {
				this.listWidget.onRefresh();
			}
			else if (requestCode == Constants.ACTIVITY_ENTITY_INSERT) {
				if (intent != null && intent.getExtras() != null) {
					String schema = intent.getExtras().getString(Constants.EXTRA_ENTITY_SCHEMA);
					if (schema != null && schema.equals(Constants.SCHEMA_ENTITY_MESSAGE)) {
						if (entity.userMemberStatus.equals(MemberStatus.NonMember) && entity.visibility.equals(Constants.PRIVACY_PUBLIC) && !entity.userHasMessaged) {
							autoJoin = true;
						}
					}
				}
			}
			else if (requestCode == Constants.ACTIVITY_SHARE) {
				Reporting.track(AnalyticsCategory.EDIT, "Sent Patch Invitation", new Properties().putValue("network", "Android"));
				UI.toast("Patch invites sent");
			}

			callbackManager.onActivityResult(requestCode, resultCode, intent);
		}
		super.onActivityResult(requestCode, resultCode, intent);
	}

	/*--------------------------------------------------------------------------------------------
	 * Notifications
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onNotificationReceived(final NotificationReceivedEvent event) {
		/* Refresh the list because something happened with the list parent. */
		if ((event.notification.parentId != null && event.notification.parentId.equals(this.entity.id))
			|| (event.notification.targetId != null && event.notification.targetId.equals(this.entity.id))) {
			if (event.notification.event.equals("approve_watch_entity")) {
				justApproved = true;
			}
			fetch(FetchMode.AUTO);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Actions
	 *--------------------------------------------------------------------------------------------*/

	private void muteAction() {
		mute(!entity.userMemberMuted);
	}

	public void addAction() {

		/* Add action not accessible if user can't add */
		if (entity != null) {
			Intent intent = new Intent(this, MessageEdit.class);
			intent.putExtra(Constants.EXTRA_ENTITY_PARENT_ID, entityId);
			intent.putExtra(Constants.EXTRA_ENTITY_PARENT_NAME, entity.name);
			intent.putExtra(Constants.EXTRA_STATE, State.Creating);
			startActivityForResult(intent, Constants.ACTIVITY_ENTITY_INSERT);
			AnimationManager.doOverridePendingTransition(this, TransitionType.FORM_TO);
		}
	}

	private void inviteAction() {
		if (entity != null) {
			share();
		}
	}

	protected void joinAction() {

		if (entity == null) return;

		/* Cancel request */
		if (entity.userMemberStatus.equals(MemberStatus.Member)) {
			if (entity.isRestrictedForCurrentUser()) {
				confirmLeave();
			}
			else {
				join(false /* delete */);
			}
		}
		else if (entity.userMemberStatus.equals(MemberStatus.Pending)) {
			join(false /* delete */);
		}
		else if (entity.userMemberStatus.equals(MemberStatus.NonMember)) {
			join(true /* insert */);
		}
	}

	private void memberListAction() {
		if (entity != null) {
			Intent intent = new Intent(this, MemberListScreen.class);
			intent.putExtra(Constants.EXTRA_ENTITY_ID, entityId);
			intent.putExtra(Constants.EXTRA_QUERY_NAME, QueryName.MembersForPatch);
			startActivity(intent);
			AnimationManager.doOverridePendingTransition(this, TransitionType.FORM_TO);
		}
	}

	private void mapAction() {
		if (entity != null) {
			Intent intent = new Intent(this, MapScreen.class);
			intent.putExtra(Constants.EXTRA_ENTITY_ID, entityId);
			startActivity(intent);
			AnimationManager.doOverridePendingTransition(this, TransitionType.FORM_TO);
		}
	}

	public void editAction() {
		Intent intent = new Intent(this, PatchEdit.class);
		intent.putExtra(Constants.EXTRA_ENTITY_ID, entityId);
		intent.putExtra(Constants.EXTRA_STATE, State.Editing);
		startActivityForResult(intent, Constants.ACTIVITY_ENTITY_EDIT);
		AnimationManager.doOverridePendingTransition(this, TransitionType.FORM_TO);
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
			this.referrerName = extras.getString(Constants.EXTRA_REFERRER_NAME);
			this.referrerPhotoUrl = extras.getString(Constants.EXTRA_REFERRER_PHOTO_URL);
		}
	}

	@Override public void initialize(Bundle savedInstanceState) {
		this.header = new PatchDetailView(this);
		this.querySpec = QuerySpec.Factory(QueryName.MessagesForPatch);
		this.actionView = (ViewGroup) header.findViewById(R.id.action_group);

		super.initialize(savedInstanceState);

		this.bottomSheetLayout = (BottomSheetLayout) findViewById(R.id.bottomsheet);
		this.callbackManager = CallbackManager.Factory.create();

		if (this.bottomSheetLayout != null)
			this.bottomSheetLayout.setPeekOnDismiss(true);

		this.listWidget.recyclerView.addOnScrollListener(new ListScrollListener() {
			@Override public void onMoved(int distance) {
				((PatchDetailView) header).bannerView.photoView.imageView.setTranslationY(distance / 2);    // Parallax
			}
		});
	}

	@Override public void bind() {
		super.bind();

		/* Bind action button */
		if (this.entity != null) {
			updateActiveView();
			this.entity.addChangeListener(user -> {
				updateActiveView();
			});
		}
	}

	public void updateActiveView() {
		if (entity != null) {

			Boolean owner = entity.isOwnedByCurrentUser();
			Boolean hasMessaged = entity.userHasMessaged;
			Boolean isPublic = (entity.visibility != null
				&& entity.visibility.equals(Constants.PRIVACY_PUBLIC)
				&& entity.isVisibleToCurrentUser());

			TextView buttonAlert = (TextView) actionView.findViewById(R.id.action_button);
			if (buttonAlert == null) return;

			int requestCount = entity.countPending;

			/* Owner */

			if (owner) {
				/*
				 * - Member requests then alert to handle
				 * - No messages then alert to invite
				 */
				if (requestCount > 0) {
					String requests = getResources().getQuantityString(R.plurals.button_pending_requests, requestCount, requestCount);
					buttonAlert.setText(requests);
					buttonAlert.setTag(R.id.members_button);
				}
				else if (entity.userMemberStatus.equals(MemberStatus.NonMember)) {
					buttonAlert.setText(R.string.button_list_watch_request);
					buttonAlert.setTag(R.id.join_button);
				}
				else {
					buttonAlert.setText(StringManager.getString(R.string.button_list_share));
					buttonAlert.setTag(R.id.invite);
				}
			}

			/* Members */

			else {
				if (entity.userMemberStatus.equals(MemberStatus.NonMember)) {
					buttonAlert.setText(R.string.button_list_watch_request);
					buttonAlert.setTag(R.id.join_button);
					return;
				}

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
					if (entity.userMemberStatus.equals(MemberStatus.Pending)) {
						buttonAlert.setText(R.string.button_list_watch_request_cancel);
						buttonAlert.setTag(R.id.join_button);
					}
					else if (entity.userMemberStatus.equals(MemberStatus.Member)) {
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

	@Override protected int getLayoutId() {
		return R.layout.screen_patch;
	}

	@Override public void configureStandardMenuItems(final Menu menu) {
		super.configureStandardMenuItems(menu);

		MenuItem menuItem = menu.findItem(R.id.leave_patch);
		if (menuItem != null) {
			if (entity != null) {
				menuItem.setVisible(entity.userMemberStatus.equals(MemberStatus.Member));
			}
		}
	}

	public void share() {

		final String patchName = (entity.name != null) ? entity.name : StringManager.getString(R.string.container_singular_lowercase);
		final String title = String.format(StringManager.getString(R.string.label_patch_share_title), patchName);
		final Activity activity = this;

		MenuSheetView menuSheetView = new MenuSheetView(this
			, MenuSheetView.MenuType.GRID
			, "Invite friends using...", (item) -> {

			bottomSheetLayout.addOnSheetDismissedListener(bottomSheetLayout -> {

				if (item.getItemId() == R.id.invite_using_patchr) {
						/*
						 * Go to patchr share directly but looks just like an external share
						 */
					final Intent intent = new Intent(activity, ShareEdit.class);
					intent.putExtra(Constants.EXTRA_STATE, State.Creating);
					intent.putExtra(Constants.EXTRA_MESSAGE_TYPE, MessageType.Invite);
					intent.putExtra(Constants.EXTRA_SHARE_SOURCE, getPackageName());
					intent.putExtra(Constants.EXTRA_SHARE_ENTITY_ID, entityId);
					intent.putExtra(Constants.EXTRA_SHARE_SCHEMA, Constants.SCHEMA_ENTITY_PATCH);
					intent.setAction(Intent.ACTION_SEND);
					activity.startActivity(intent);
					AnimationManager.doOverridePendingTransition(activity, TransitionType.FORM_TO);
				}
				else if (item.getItemId() == R.id.invite_using_facebook) {
					FacebookProvider provider = new FacebookProvider();
					provider.invite(title, entity, PatchScreen.this, callbackManager);
				}
				else if (item.getItemId() == R.id.invite_using_other) {
					BranchProvider provider = new BranchProvider();
					Reporting.track(AnalyticsCategory.ACTION, "Started Patch Invitation", new Properties().putValue("network", "Android"));
					provider.invite(title, entity, PatchScreen.this);
				}
			});

			bottomSheetLayout.dismissSheet();
			return true;
		});

		menuSheetView.inflateMenu(R.menu.menu_invite_sheet);
		bottomSheetLayout.setPeekOnDismiss(true);
		bottomSheetLayout.showWithSheetView(menuSheetView, new InsetViewTransformer());
	}

	public void join(final boolean activate) {

		if (!entity.userMemberStatus.equals(MemberStatus.NonMember)) {  // Member or pending
			RestClient.getInstance().deleteLinkById(entity.userMemberId)
				.subscribe(
					response -> {
						Reporting.track(AnalyticsCategory.EDIT, "Left patch");
						realm.executeTransaction(realm -> {
							entity.userMemberId = null;
							if (entity.userMemberStatus.equals(MemberStatus.Member)) {
								entity.countMembers--;
							}
							entity.userMemberStatus = MemberStatus.NonMember;
						});
						MediaManager.playSound(MediaManager.SOUND_DEBUG_POP, 1.0f, 1);
						UI.toast("You have left this patch");
						fetch(FetchMode.AUTO);
					},
					error -> {
						Logger.w(this, error.getLocalizedMessage());
						processingCommand = false;
					});
		}
		else {
			RestClient.getInstance().insertLink(UserManager.userId, entity.id, LinkType.Watch)
				.subscribe(
					response -> {
						if (response.data != null && response.count.intValue() == 1) {
							RealmEntity link = response.data.get(0);
							realm.executeTransaction(realm -> {
								entity.userMemberId = link.id;
								if (link.enabled) {
									entity.userMemberStatus = MemberStatus.Member;
									entity.countMembers++;
									Reporting.track(AnalyticsCategory.EDIT, "Joined patch");
									UI.toast("You are now a member of this patch");
								}
								else {
									entity.userMemberStatus = MemberStatus.Pending;
									Reporting.track(AnalyticsCategory.EDIT, "Joined patch");
								}
							});
						}
						MediaManager.playSound(MediaManager.SOUND_DEBUG_POP, 1.0f, 1);
						fetch(FetchMode.AUTO);
					},
					error -> {
						Logger.w(this, error.getLocalizedMessage());
						processingCommand = false;
					});
		}
	}

	public void mute(final Boolean mute) {

		final ViewAnimator animator = (ViewAnimator) ((PatchDetailView) header).bannerView.muteButton;
		animator.setDisplayedChild(1);  // Turned off in drawButtons

		String entityId = entity.id;
		String linkId = entity.userMemberId;

		RestClient.getInstance().muteLinkById(entityId, linkId, mute)
			.subscribe(
				response -> {
					Reporting.track(AnalyticsCategory.EDIT, mute ? "Muted Patch" : "Unmuted Patch");
					fetch(FetchMode.MANUAL, true);  // Need to refetch the patch, header only
					processingCommand = false;
				},
				error -> {
					Logger.w(this, error.getLocalizedMessage());
					processingCommand = false;
				});
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
			, this
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
		BranchUniversalObject applink = provider.buildApplink(this.entity);
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

		handler.postDelayed(new Runnable() {

			@Override public void run() {

				View view = LayoutInflater.from(PatchScreen.this).inflate(R.layout.view_onboarding, null, false);

				View buttonGroup = view.findViewById(R.id.buttons);
				TextView button1 = (TextView) view.findViewById(R.id.action1_button);
				TextView button2 = (TextView) view.findViewById(R.id.action2_button);
				TextView member = (TextView) view.findViewById(R.id.member);
				TextView message = (TextView) view.findViewById(R.id.message);
				ImageView imageView = (ImageView) view.findViewById(R.id.user_photo);

				String heading = (referrerName != null)
				                 ? String.format("%1$s invites you to join this patch.", referrerName)
				                 : "A friend invites you to join this patch.";
				message.setText(heading);

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

				if (entity.userMemberStatus.equals(MemberStatus.Member)) {
					buttonGroup.setVisibility(View.GONE);
					member.setText("You are a member of this patch!");
				}
				else if (entity.userMemberStatus.equals(MemberStatus.Pending)) {
					buttonGroup.setVisibility(View.GONE);
					member.setText("Requested");
				}
				else {
					member.setVisibility(View.GONE);
					button2.setVisibility(View.GONE);
					button1.setText("JOIN");
					button1.setOnClickListener(new View.OnClickListener() {
						@Override public void onClick(View view) {
							bottomSheetLayout.dismissSheet();
							joinAction();
						}
					});
				}

				bottomSheetLayout.setPeekOnDismiss(true);
				bottomSheetLayout.showWithSheetView(view, new InsetViewTransformer(0.2f, 0.95f));
				handler.removeCallbacks(this);
			}
		}, delay);
	}
}