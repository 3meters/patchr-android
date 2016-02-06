package com.patchr.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ShareCompat;
import android.text.TextUtils;
import android.view.MenuItem;
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
import com.patchr.components.AnimationManager;
import com.patchr.components.DataController.ActionType;
import com.patchr.components.IntentBuilder;
import com.patchr.components.Logger;
import com.patchr.components.MenuManager;
import com.patchr.components.StringManager;
import com.patchr.events.ActionEvent;
import com.patchr.events.DataResultEvent;
import com.patchr.events.ProcessingCompleteEvent;
import com.patchr.objects.Link.Direction;
import com.patchr.objects.Message;
import com.patchr.objects.Patch;
import com.patchr.objects.Photo;
import com.patchr.objects.Route;
import com.patchr.objects.TransitionType;
import com.patchr.ui.base.BaseActivity;
import com.patchr.ui.base.BaseFragment;
import com.patchr.ui.components.ListController;
import com.patchr.ui.edit.MessageEdit;
import com.patchr.ui.widgets.InsetViewTransformer;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Integers;
import com.patchr.utilities.Reporting;
import com.patchr.utilities.Type;
import com.patchr.utilities.UI;
import com.squareup.otto.Subscribe;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.util.LinkProperties;

@SuppressLint("Registered")
public class PatchForm extends BaseActivity {

	protected EntityFormFragment mHeaderFragment;
	protected String             mListLinkType;
	protected String             mNotificationId;
	protected BottomSheetLayout  mBottomSheetLayout;
	protected CallbackManager    mCallbackManager;

	@Override
	public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mEntityId = extras.getString(Constants.EXTRA_ENTITY_ID);
			mListLinkType = extras.getString(Constants.EXTRA_LIST_LINK_TYPE);
			mTransitionType = extras.getInt(Constants.EXTRA_TRANSITION_TYPE, TransitionType.FORM_TO);
			mNotificationId = extras.getString(Constants.EXTRA_NOTIFICATION_ID);
		}
	}

	@Override
	public void initialize(Bundle savedInstanceState) {
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
		((BaseFragment) mCurrentFragment).getMenuResIds().add(R.menu.menu_delete);
		((BaseFragment) mCurrentFragment).getMenuResIds().add(R.menu.menu_qrcode);
		((BaseFragment) mCurrentFragment).getMenuResIds().add(R.menu.menu_report);

		getSupportFragmentManager()
				.beginTransaction()
				.replace(R.id.fragment_holder, mCurrentFragment)
				.commit();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe
	public void onDataResult(final DataResultEvent event) {
		/*
		 * Cherry pick the entity so we can add some wrapper functionality.
		 */
		if (event.entity != null && event.entity.id.equals(mEntityId)) {
			mEntity = event.entity;
			Patchr.getInstance().setCurrentPatch(mEntity);
		}
	}

	@Subscribe
	public void onViewClick(ActionEvent event) {
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

	@Subscribe
	public void onProcessingComplete(ProcessingCompleteEvent event) {
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

	@Override
	public void onRefresh() {
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

	@Override
	public void onAdd(Bundle extras) {

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

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (resultCode != Activity.RESULT_CANCELED || Patchr.resultCode != Activity.RESULT_CANCELED) {
			if (requestCode == Constants.ACTIVITY_ENTITY_EDIT) {
				if (resultCode == Constants.RESULT_ENTITY_DELETED || resultCode == Constants.RESULT_ENTITY_REMOVED) {
					finish();
					AnimationManager.doOverridePendingTransition(this, TransitionType.PAGE_TO_RADAR_AFTER_DELETE);
				}
			}
			mCallbackManager.onActivityResult(requestCode, resultCode, intent);
		}
		super.onActivityResult(requestCode, resultCode, intent);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void share() {

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
					mBottomSheetLayout.dismissSheet();
				}
				else if (item.getItemId() == R.id.invite_using_facebook) {

					LinkProperties linkProperties = new LinkProperties()
							.setChannel("facebook")
							.setFeature(Branch.FEATURE_TAG_INVITE);
					facebookInvite(title, configureApplink(), linkProperties);
					mBottomSheetLayout.dismissSheet();
				}
				else if (item.getItemId() == R.id.invite_using_other) {

					LinkProperties linkProperties = new LinkProperties()
							.setChannel("patchr-android")
							.setFeature(Branch.FEATURE_TAG_INVITE);
					androidInvite(title, configureApplink(), linkProperties);
					mBottomSheetLayout.dismissSheet();
				}
				else {
					mBottomSheetLayout.dismissSheet();
					UI.showToastNotification(item.getTitle().toString(), Toast.LENGTH_SHORT);
				}
				return true;
			}
		});

		menuSheetView.inflateMenu(R.menu.menu_invite_sheet);
		mBottomSheetLayout.showWithSheetView(menuSheetView, new InsetViewTransformer());
	}

	public void facebookInvite(final String title, final BranchUniversalObject applink, LinkProperties linkProperties) {

		final String patchName = (mEntity.name != null) ? mEntity.name : StringManager.getString(R.string.container_singular_lowercase);

		applink.generateShortUrl(this, linkProperties, new Branch.BranchLinkCreateListener() {

			@Override
			public void onLinkCreate(String url, BranchError error) {

				if (error == null) {

					AppInviteDialog inviteDialog = new AppInviteDialog(PatchForm.this);

					if (inviteDialog.canShow()) {

						String photoUrl = null;

						if (mEntity.photo != null) {
							Photo photo = mEntity.getPhoto();
							String patchNameEncoded = "";
							try {
								patchNameEncoded = URLEncoder.encode(patchName, "UTF-8");
							}
							catch (UnsupportedEncodingException e) {
								Reporting.logException(e);
							}
							String settings = "w=1200&h=628&crop&fit=crop&q=25&txtsize=96&txtalign=left,bottom&txtcolor=fff&txtshad=5&txtpad=60&txtfont=Helvetica%20Neue%20Light";
							photoUrl = String.format("https://3meters-images.imgix.net/%1$s?%2$s&txt=%3$s", photo.prefix, settings, patchNameEncoded);
						}

						AppInviteContent.Builder builder = new AppInviteContent.Builder();
						builder.setApplinkUrl(url);
						if (photoUrl != null) {
							builder.setPreviewImageUrl(photoUrl);
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
						inviteDialog.show(PatchForm.this, builder.build());
					}
				}
			}
		});
	}

	public void androidInvite(final String title, final BranchUniversalObject applink, LinkProperties linkProperties) {

		final String patchName = (mEntity.name != null) ? mEntity.name : StringManager.getString(R.string.container_singular_lowercase);
		final String referrerName = Patchr.getInstance().getCurrentUser().name;

		applink.generateShortUrl(this, linkProperties, new Branch.BranchLinkCreateListener() {

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

	public BranchUniversalObject configureApplink() {

		final String patchName = (mEntity.name != null) ? mEntity.name : StringManager.getString(R.string.container_singular_lowercase);
		final String referrerName = Patchr.getInstance().getCurrentUser().name;
		final String ownerName = mEntity.owner.name;
		final String path = "patch/" + mEntityId;

		BranchUniversalObject applink = new BranchUniversalObject()
				.setCanonicalIdentifier(path)
				.setTitle(String.format("Invite by %1$s to the %2$s patch", referrerName, patchName)) // $og_title
				.addContentMetadata("entityId", mEntityId)
				.addContentMetadata("entitySchema", Constants.SCHEMA_ENTITY_PATCH)
				.addContentMetadata("referrerName", referrerName)
				.addContentMetadata("ownerName", ownerName)
				.addContentMetadata("patchName", patchName);

		if (mEntity.photo != null) {
			Photo photo = mEntity.getPhoto();
			String settings = "h=500&crop&fit=crop&q=50";
			String photoUrl = String.format("https://3meters-images.imgix.net/%1$s?%2$s", photo.prefix, settings);
			applink.setContentImageUrl(photoUrl);  // $og_image_url
		}

		if (mEntity.description != null) {
			applink.setContentDescription(mEntity.description); // $og_description
		}

		return applink;
	}

	public void showSmartSharePicker(String title) {

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

	@Override
	public void configureActionBar() {
		super.configureActionBar();
		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayShowTitleEnabled(false);  // Dont show title
		}
	}

	@Override
	protected int getLayoutId() {
		return R.layout.patch_form;
	}

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected void onResume() {
		super.onResume();
		if (!isFinishing()) {
			Patchr.getInstance().setCurrentPatch(mEntity);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/
}