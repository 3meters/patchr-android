package com.patchr.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ShareCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.share.model.AppInviteContent;
import com.facebook.share.widget.AppInviteDialog;
import com.flipboard.bottomsheet.BottomSheetLayout;
import com.flipboard.bottomsheet.commons.IntentPickerSheetView;
import com.flipboard.bottomsheet.commons.MenuSheetView;
import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.AndroidManager;
import com.patchr.components.AnimationManager;
import com.patchr.components.DataController.ActionType;
import com.patchr.components.DownloadManager;
import com.patchr.components.IntentBuilder;
import com.patchr.components.Logger;
import com.patchr.components.MenuManager;
import com.patchr.components.StringManager;
import com.patchr.components.UserManager;
import com.patchr.events.ActionEvent;
import com.patchr.events.DataResultEvent;
import com.patchr.events.ProcessingCompleteEvent;
import com.patchr.events.WatchStatusChangedEvent;
import com.patchr.objects.AirLocation;
import com.patchr.objects.Link.Direction;
import com.patchr.objects.Message;
import com.patchr.objects.Patch;
import com.patchr.objects.Photo;
import com.patchr.objects.PhotoSizeCategory;
import com.patchr.objects.Route;
import com.patchr.objects.TransitionType;
import com.patchr.objects.WatchStatus;
import com.patchr.ui.base.BaseActivity;
import com.patchr.ui.base.BaseFragment;
import com.patchr.ui.components.CircleTransform;
import com.patchr.ui.components.ListController;
import com.patchr.ui.edit.MessageEdit;
import com.patchr.ui.widgets.InsetViewTransformer;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Integers;
import com.patchr.utilities.Reporting;
import com.patchr.utilities.Type;
import com.patchr.utilities.UI;
import com.patchr.utilities.Utils;
import com.squareup.otto.Subscribe;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.util.LinkProperties;

@SuppressLint("Registered")
public class PatchForm extends BaseActivity {

	private final Handler mHandler = new Handler();

	protected EntityFormFragment mHeaderFragment;
	protected String             mListLinkType;
	protected String             mNotificationId;
	protected BottomSheetLayout  mBottomSheetLayout;
	protected CallbackManager    mCallbackManager;
	protected String             mInviterName;
	protected String             mInviterPhotoUrl;
	protected Boolean mShowInviterWelcome     = false;
	protected Boolean mAuthenticatedForInvite = false;

	@Override public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mEntityId = extras.getString(Constants.EXTRA_ENTITY_ID);
			mListLinkType = extras.getString(Constants.EXTRA_LIST_LINK_TYPE);
			mTransitionType = extras.getInt(Constants.EXTRA_TRANSITION_TYPE, TransitionType.FORM_TO);
			mNotificationId = extras.getString(Constants.EXTRA_NOTIFICATION_ID);
			mInviterName = extras.getString(Constants.EXTRA_INVITER_NAME);
			mInviterPhotoUrl = extras.getString(Constants.EXTRA_INVITER_PHOTO_URL);
		}
	}

	@Override protected void onResume() {
		super.onResume();
		if (!isFinishing()) {
			Patchr.getInstance().setCurrentPatch(mEntity);
		}

		if (mEntity != null && mEntity instanceof Patch) {
			Patch patch = (Patch) mEntity;
			if (mInviterName != null) {     // Active invitation
				if (mBottomSheetLayout.isSheetShowing()) {
					if (UserManager.getInstance().authenticated() && patch.watchStatus() != WatchStatus.NONE) {
						mBottomSheetLayout.dismissSheet();
						return;
					}
					else if (UserManager.getInstance().authenticated() != mAuthenticatedForInvite) {
						showInviteWelcome(1000);
					}
				}
				else {
					showInviteWelcome(1500);
				}
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe public void onDataResult(final DataResultEvent event) {
		/*
		 * Cherry pick the entity so we can add some wrapper functionality.
		 */
		if (event.entity != null && event.entity.id.equals(mEntityId)) {
			Boolean firstBind = (mEntity == null);
			mEntity = event.entity;
			if (firstBind && mInviterName != null) {     // Active invitation
				showInviteWelcome(1500);
			}
			Patchr.getInstance().setCurrentPatch(mEntity);
			invalidateOptionsMenu();    // In case user authenticated
		}
	}

	@Subscribe public void onViewClick(ActionEvent event) {
		/*
		 * Base activity broadcasts view clicks that target onViewClick. There are actions
		 * that should be handled at the activity level like add a new entity.
		 */
		if (mProcessing) return;

		if (event.view != null) {
			mProcessing = true;
			Integer id = event.view.getId();

			/* Dynamic button we need to redirect */
			if (id == R.id.button_alert) {
				id = (Integer) event.view.getTag();
			}

			if (id == R.id.add || id == R.id.list_fab) {
				onAdd(new Bundle());
			}
			else {
				mProcessing = false;
				super.onViewClick(event);
			}
			mProcessing = false;
		}
	}

	@Subscribe public void onProcessingComplete(ProcessingCompleteEvent event) {
		/*
		 * Gets called direct at the activity level and receives
		 * events from fragments.
		 */
		mProcessing = false;
		mUiController.getBusyController().hide(false);

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				/*
				 * Non-members can't add messages to private patches.
				 */
				if (mEntity != null && mEntity instanceof Patch) {
					Patch patch = (Patch) mEntity;
					ListController controller = ((EntityListFragment) mCurrentFragment).getListController();
					if (patch.privacy != null
							&& patch.privacy.equals(Constants.PRIVACY_PRIVATE)
							&& !patch.isVisibleToCurrentUser()) {
						controller.getFloatingActionController().fadeOut();
					}
					else {
						controller.getFloatingActionController().fadeIn();
					}
				}
			}
		});
	}

	@Subscribe public void onWatchStatusChangedEvent(WatchStatusChangedEvent event) {
		onRefresh();
	}

	@Override public void onRefresh() {
		/*
		 * Called from swipe refresh or routing. Always treated
		 * as an aggresive refresh.
		 */
		if (mHeaderFragment != null) {
			mHeaderFragment.onRefresh();
		}
		if (mCurrentFragment != null && mCurrentFragment instanceof EntityListFragment) {
			((EntityListFragment) mCurrentFragment).onRefresh();
		}
	}

	@Override public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.leave_patch) {
			((PatchFormFragment) mHeaderFragment).onWatchButtonClick(null);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override public void onAdd(Bundle extras) {

		if (!UserManager.getInstance().authenticated()) {
			UserManager.getInstance().showGuestGuard(this, "Sign up for a free account to post messages and more.");
			return;
		}

		if (mEntity == null) return;

		if (MenuManager.canUserAdd(mEntity)) {

			String message = StringManager.getString(R.string.label_message_new_message);
			if (!TextUtils.isEmpty(mEntity.name)) {
				message = String.format(StringManager.getString(R.string.label_message_new_to_message), mEntity.name);
			}

			extras.putString(Constants.EXTRA_MESSAGE, message);
			extras.putString(Constants.EXTRA_ENTITY_PARENT_ID, mEntityId);
			extras.putString(Constants.EXTRA_MESSAGE_TYPE, Message.MessageType.ROOT);
			extras.putString(Constants.EXTRA_ENTITY_SCHEMA, Constants.SCHEMA_ENTITY_MESSAGE);

			Patchr.router.route(this, Route.NEW, null, extras);
		}
		else if (Type.isTrue(((Patch) mEntity).locked)) {
			Dialogs.locked(this, mEntity);
		}
	}

	@Override public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (mBottomSheetLayout.isSheetShowing()) {
			mBottomSheetLayout.peekSheet();
		}
	}

	@Override public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (resultCode != Activity.RESULT_CANCELED || Patchr.resultCode != Activity.RESULT_CANCELED) {
			if (requestCode == Constants.ACTIVITY_ENTITY_EDIT) {
				if (resultCode == Constants.RESULT_ENTITY_DELETED || resultCode == Constants.RESULT_ENTITY_REMOVED) {
					finish();
					AnimationManager.doOverridePendingTransition(this, TransitionType.PAGE_TO_RADAR_AFTER_DELETE);
				}
			}
			else if (resultCode == Constants.RESULT_USER_SIGNED_IN && UserManager.getInstance().authenticated()) {
				onRefresh();
			}
			mCallbackManager.onActivityResult(requestCode, resultCode, intent);
		}
		super.onActivityResult(requestCode, resultCode, intent);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mCurrentFragment = new EntityListFragment();
		mHeaderFragment = new PatchFormFragment();
		mBottomSheetLayout = (BottomSheetLayout) findViewById(R.id.bottomsheet);
		mBottomSheetLayout.setPeekOnDismiss(true);
		mCallbackManager = CallbackManager.Factory.create();

		mHeaderFragment
				.setEntityId(mEntityId)
				.setListLinkType(mListLinkType)
				.setTransitionType(mTransitionType)
				.setNotificationId(mNotificationId)
				.setParallax(true)
				.setLayoutResId(R.layout.widget_list_header_patch);

		((EntityListFragment) mCurrentFragment)
				.setScopingEntityId(mEntityId)
				.setActionType(ActionType.ACTION_GET_ENTITIES)
				.setLinkSchema(Constants.SCHEMA_ENTITY_MESSAGE)
				.setLinkType(Constants.TYPE_LINK_CONTENT)
				.setLinkDirection(Direction.in.name())
				.setPageSize(Integers.getInteger(R.integer.page_size_messages))
				.setHeaderFragment(mHeaderFragment)
				.setHeaderViewResId(R.layout.entity_form)
				.setFooterViewResId(R.layout.widget_list_footer_message)
				.setListItemResId(R.layout.temp_listitem_message)
				.setListLayoutResId(R.layout.message_list_patch_fragment)
				.setListLoadingResId(R.layout.temp_listitem_loading)
				.setListViewType(EntityListFragment.ViewType.LIST)
				.setBubbleButtonMessageResId(R.string.button_list_share);

		((BaseFragment) mCurrentFragment).getMenuResIds().add(R.menu.menu_sign_in);
		((BaseFragment) mCurrentFragment).getMenuResIds().add(R.menu.menu_invite);
		((BaseFragment) mCurrentFragment).getMenuResIds().add(R.menu.menu_map);
		((BaseFragment) mCurrentFragment).getMenuResIds().add(R.menu.menu_refresh);
		((BaseFragment) mCurrentFragment).getMenuResIds().add(R.menu.menu_edit_patch);
		((BaseFragment) mCurrentFragment).getMenuResIds().add(R.menu.menu_leave_patch);
		((BaseFragment) mCurrentFragment).getMenuResIds().add(R.menu.menu_delete);
		((BaseFragment) mCurrentFragment).getMenuResIds().add(R.menu.menu_qrcode);
		((BaseFragment) mCurrentFragment).getMenuResIds().add(R.menu.menu_report_patch);

		getSupportFragmentManager()
				.beginTransaction()
				.replace(R.id.fragment_holder, mCurrentFragment)
				.commit();
	}

	@Override public void share() {

		final String patchName = (mEntity.name != null) ? mEntity.name : StringManager.getString(R.string.container_singular_lowercase);
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
					intent.putExtra(Constants.EXTRA_SHARE_ID, mEntityId);
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

				mBottomSheetLayout.dismissSheet();
				return true;
			}
		});

		menuSheetView.inflateMenu(R.menu.menu_invite_sheet);
		mBottomSheetLayout.setPeekOnDismiss(true);
		mBottomSheetLayout.showWithSheetView(menuSheetView, new InsetViewTransformer());
	}

	@Override public void configureActionBar() {
		super.configureActionBar();
		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayShowTitleEnabled(false);  // Dont show title
		}
	}

	@Override protected int getLayoutId() {
		return R.layout.patch_form;
	}

	private void showSmartSharePicker(String title) {

		final Intent shareIntent = new Intent(Intent.ACTION_SEND);
		shareIntent.setType("text/plain");
		shareIntent.putExtra(Intent.EXTRA_SUBJECT, String.format(StringManager.getString(R.string.label_patch_share_subject)
				, (mEntity.name != null) ? mEntity.name : "A"));
		shareIntent.putExtra(Intent.EXTRA_TEXT, String.format(StringManager.getString(R.string.label_patch_share_body), mEntityId));
		shareIntent.putExtra(Constants.EXTRA_SHARE_SOURCE, getPackageName());
		shareIntent.putExtra(Constants.EXTRA_SHARE_ID, mEntityId);
		shareIntent.putExtra(Constants.EXTRA_SHARE_SCHEMA, Constants.SCHEMA_ENTITY_PATCH);

		IntentPickerSheetView intentPickerSheet = new IntentPickerSheetView(this, shareIntent, "Invite using...", new IntentPickerSheetView.OnIntentPickedListener() {
			@Override
			public void onIntentPicked(IntentPickerSheetView.ActivityInfo activityInfo) {
				mBottomSheetLayout.dismissSheet();
				startActivity(activityInfo.getConcreteIntent(shareIntent));
			}
		});

		/* Filter out built in sharing options such as bluetooth and beam. */
		intentPickerSheet.setFilter(new IntentPickerSheetView.Filter() {
			@Override
			public boolean include(IntentPickerSheetView.ActivityInfo info) {
				return !info.componentName.getPackageName().startsWith("com.android");
			}
		});

		mBottomSheetLayout.showWithSheetView(intentPickerSheet);
	}

	private void showInviteWelcome(int delay) {

		final Patch patch = (Patch) mEntity;

		/* Don't show invite if already a member */
		if (UserManager.getInstance().authenticated() && patch.watchStatus() != WatchStatus.NONE) {
			return;
		}

		mHandler.postDelayed(new Runnable() {

			@Override public void run() {

				View view = LayoutInflater.from(PatchForm.this).inflate(R.layout.onboarding_view, null, false);

				String heading = (mInviterName != null)
				                 ? String.format("%1$s invites you to join this patch.", mInviterName)
				                 : "A friend invites you to join this patch.";
				((TextView) view.findViewById(R.id.message)).setText(heading);

				ImageView imageView = (ImageView) view.findViewById(R.id.user_photo);

				if (mInviterPhotoUrl != null) {
					DownloadManager
							.with(Patchr.applicationContext)
							.load(Uri.parse(mInviterPhotoUrl))
							.config(Bitmap.Config.RGB_565)
							.transform(new CircleTransform())
							.into(imageView);
				}
				else {
					imageView.setVisibility(View.GONE);
				}

				if (UserManager.getInstance().authenticated()) {

					mAuthenticatedForInvite = true;
					((TextView) view.findViewById(R.id.action2_button)).setVisibility(View.GONE);
					((TextView) view.findViewById(R.id.action1_button)).setText("JOIN");
					((Button) view.findViewById(R.id.action1_button)).setOnClickListener(new View.OnClickListener() {
						@Override public void onClick(View view) {
							mBottomSheetLayout.dismissSheet();
							PatchFormFragment header = (PatchFormFragment) mHeaderFragment;
							header.onWatchButtonClick(view);
						}
					});
				}
				else {
					mAuthenticatedForInvite = false;
					((TextView) view.findViewById(R.id.action1_button)).setText("LOG IN");
					((Button) view.findViewById(R.id.action1_button)).setOnClickListener(new View.OnClickListener() {
						@Override public void onClick(View v) {
							Patchr.router.route(PatchForm.this, Route.LOGIN, null, null);
						}
					});
					((TextView) view.findViewById(R.id.action2_button)).setText("SIGN UP");
					((Button) view.findViewById(R.id.action2_button)).setOnClickListener(new View.OnClickListener() {
						@Override public void onClick(View v) {
							Patchr.router.route(PatchForm.this, Route.SIGNUP, null, null);
						}
					});
				}

				mBottomSheetLayout.showWithSheetView(view, new InsetViewTransformer(0.2f, 0.95f));
				mHandler.removeCallbacks(this);
			}
		}, delay);
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	public class BranchProvider {

		public void invite(final String title) {

			final String patchName = (mEntity.name != null) ? mEntity.name : StringManager.getString(R.string.container_singular_lowercase);
			final String referrerName = UserManager.getInstance().getCurrentUser().name;
			final String referrerId = UserManager.getInstance().getCurrentUser().id;
			final String ownerName = mEntity.owner.name;
			final String path = "patch/" + mEntityId;

			BranchUniversalObject applink = new BranchUniversalObject()
					.setCanonicalIdentifier(path)
					.setTitle(String.format("Invite by %1$s to the %2$s patch", referrerName, patchName)) // $og_title
					.addContentMetadata("entityId", mEntityId)
					.addContentMetadata("entitySchema", Constants.SCHEMA_ENTITY_PATCH)
					.addContentMetadata("referrerName", referrerName)
					.addContentMetadata("referrerId", referrerId)
					.addContentMetadata("ownerName", ownerName)
					.addContentMetadata("patchName", patchName);

			if (UserManager.getInstance().getCurrentUser().getPhoto() != null) {
				Photo photo = UserManager.getInstance().getCurrentUser().getPhoto();
				applink.addContentMetadata("referrerPhotoUrl", photo.getUri(PhotoSizeCategory.PROFILE));
			}

			if (mEntity.photo != null) {
				Photo photo = mEntity.getPhoto();
				String settings = "h=500&crop&fit=crop&q=50";
				String photoUrl = String.format("https://3meters-images.imgix.net/%1$s?%2$s", photo.prefix, settings);
				applink.setContentImageUrl(photoUrl);  // $og_image_url
			}

			if (mEntity.description != null) {
				applink.setContentDescription(mEntity.description); // $og_description
			}

			LinkProperties linkProperties = new LinkProperties()
					.setChannel("patchr-android")
					.setFeature(Branch.FEATURE_TAG_INVITE);

			applink.generateShortUrl(PatchForm.this, linkProperties, new Branch.BranchLinkCreateListener() {

				@Override
				public void onLinkCreate(String url, BranchError error) {

					if (error == null) {
						ShareCompat.IntentBuilder builder = ShareCompat.IntentBuilder.from(PatchForm.this);
						builder.setChooserTitle(title);
						builder.setType("text/plain");
					/*
					 * subject: Invitation to the \'%1$s\' patch
					 * body: %1$s has invited you to the %2$s patch! %3$s
					 */
						builder.setSubject(String.format(StringManager.getString(R.string.label_patch_share_subject), patchName));
						builder.setText(String.format(StringManager.getString(R.string.label_patch_share_body), referrerName, patchName, url));

						builder.getIntent().putExtra(Constants.EXTRA_SHARE_SOURCE, getPackageName());
						builder.getIntent().putExtra(Constants.EXTRA_SHARE_ID, mEntityId);
						builder.getIntent().putExtra(Constants.EXTRA_SHARE_SCHEMA, Constants.SCHEMA_ENTITY_PATCH);

						builder.startChooser();
					}
				}
			});
		}
	}

	public class FacebookProvider {

		public void invite(final String title) {

			AppInviteDialog inviteDialog = new AppInviteDialog(PatchForm.this);

			if (AppInviteDialog.canShow()) {

				String patchName = (mEntity.name != null) ? mEntity.name : StringManager.getString(R.string.container_singular_lowercase);
				String patchPhotoUrl = null;
				String referrerNameEncoded = Utils.encode(UserManager.getInstance().getCurrentUser().name);
				String referrerPhotoUrl = "";

				if (UserManager.getInstance().getCurrentUser().getPhoto() != null) {
					Photo photo = UserManager.getInstance().getCurrentUser().getPhoto();
					String photoUrlEncoded = Utils.encode(photo.getUri(PhotoSizeCategory.PROFILE));
					referrerPhotoUrl = String.format("&referrerPhotoUrl=%1$s", photoUrlEncoded);
				}

				String queryString = String.format("entityId=%1$s&entitySchema=%2$s&referrerName=%3$s%4$s", mEntity.id, mEntity.schema, referrerNameEncoded, referrerPhotoUrl);
				Uri applink = Uri.parse(String.format("https://fb.me/934234473291708?%1$s", queryString));

				if (mEntity.photo != null) {
					Photo photo = mEntity.getPhoto();
					String patchNameEncoded = Utils.encode(patchName);
					String settings = "w=1200&h=628&crop&fit=crop&q=25&txtsize=96&txtalign=left,bottom&txtcolor=fff&txtshad=5&txtpad=60&txtfont=Helvetica%20Neue%20Light";
					patchPhotoUrl = String.format("https://3meters-images.imgix.net/%1$s?%2$s&txt=%3$s", photo.prefix, settings, patchNameEncoded);
				}

				AppInviteContent.Builder builder = new AppInviteContent.Builder();
				builder.setApplinkUrl(applink.toString());
				if (patchPhotoUrl != null) {
					builder.setPreviewImageUrl(patchPhotoUrl);
				}

				inviteDialog.registerCallback(mCallbackManager, new FacebookCallback<AppInviteDialog.Result>() {

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
				AppInviteDialog.show(PatchForm.this, builder.build());
			}
		}
	}
}