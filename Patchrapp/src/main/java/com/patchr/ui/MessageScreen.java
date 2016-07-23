package com.patchr.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.app.ShareCompat;
import android.support.v7.widget.CardView;
import android.text.Html;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.flipboard.bottomsheet.BottomSheetLayout;
import com.flipboard.bottomsheet.OnSheetDismissedListener;
import com.flipboard.bottomsheet.commons.MenuSheetView;
import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.AnimationManager;
import com.patchr.components.Logger;
import com.patchr.components.MenuManager;
import com.patchr.components.StringManager;
import com.patchr.components.UserManager;
import com.patchr.model.Photo;
import com.patchr.model.RealmEntity;
import com.patchr.objects.enums.AnalyticsCategory;
import com.patchr.objects.enums.FetchMode;
import com.patchr.objects.enums.FetchStrategy;
import com.patchr.objects.enums.LinkType;
import com.patchr.objects.enums.MessageType;
import com.patchr.objects.enums.QueryName;
import com.patchr.objects.enums.State;
import com.patchr.objects.enums.TransitionType;
import com.patchr.service.RestClient;
import com.patchr.ui.collections.BaseListScreen;
import com.patchr.ui.components.InsetViewTransformer;
import com.patchr.ui.edit.MessageEdit;
import com.patchr.ui.edit.ShareEdit;
import com.patchr.ui.views.MessageView;
import com.patchr.ui.views.PatchView;
import com.patchr.ui.widgets.ImageWidget;
import com.patchr.utilities.Colors;
import com.patchr.utilities.DateTime;
import com.patchr.utilities.Errors;
import com.patchr.utilities.Reporting;
import com.patchr.utilities.UI;
import com.segment.analytics.Properties;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.util.LinkProperties;

@SuppressWarnings("ConstantConditions")
public class MessageScreen extends BaseScreen {

	protected BottomSheetLayout bottomSheetLayout;
	protected ImageWidget       photoView;
	protected View              holderUser;
	protected View              holderPatch;
	protected TextView          descriptionView;
	protected ImageWidget       patchPhotoView;
	protected TextView          patchName;
	protected ImageWidget       userPhotoView;
	protected TextView          userName;
	protected TextView          createdDate;
	protected ViewGroup         buttonToolbar;
	protected ViewAnimator      likeAnimator;

	protected ViewGroup shareHolder;
	protected ViewGroup shareView;
	protected ViewGroup shareRecipientsHolder;
	protected TextView  shareRecipients;

	public String parentId;

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		bind();
	}

	@Override protected void onResume() {
		super.onResume();
		if (!isFinishing()) {
			if (!processing) {
				processing = true;
				fetch(FetchMode.AUTO);
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public boolean onCreateOptionsMenu(Menu menu) {

		getMenuInflater().inflate(R.menu.menu_overflow, menu);
		getMenuInflater().inflate(R.menu.menu_edit, menu);              // Owner
		getMenuInflater().inflate(R.menu.menu_share_message, menu);

		return super.onCreateOptionsMenu(menu);
	}

	@Override public boolean onOptionsItemSelected(MenuItem item) {

		if (item.getItemId() == R.id.overflow) {
			bottomSheetDialog = new BottomSheetDialog(this);
			View view = getLayoutInflater().inflate(R.layout.dialog_message, null);
			bottomSheetDialog.setContentView(view);
			bottomSheetDialog.getWindow().setDimAmount(0.3f);
			bottomSheetDialog.setOnDismissListener(dialogInterface -> {
				bottomSheetDialog = null;
			});

			if (!MenuManager.canUserDelete(entity)) {
				view.findViewById(R.id.delete_group).setVisibility(View.GONE);
			}
			if (!MenuManager.canUserRemoveFromPatch(entity)) {
				view.findViewById(R.id.remove_group).setVisibility(View.GONE);
			}

			bottomSheetDialog.show();
		}
		else if (item.getItemId() == R.id.edit) {
			editAction();
		}
		else if (item.getItemId() == R.id.share) {
			shareAction();
		}
		else {
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	public void onClick(View view) {
		Integer id = view.getId();

		if (id == R.id.like_button) {
			likeAction();
		}
		else if (id == R.id.report) {
			bottomSheetDialog.dismiss();
			String message = String.format("Report on message id: %1$s\n\nPlease add some detail on why you are reporting this message.\n", entityId);
			Intent email = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:report@patchr.com"));
			email.putExtra(Intent.EXTRA_SUBJECT, "Report on message content");
			email.putExtra(Intent.EXTRA_TEXT, message);
			startActivity(Intent.createChooser(email, "Send report using:"));
		}
		else if (id == R.id.remove) {
			bottomSheetDialog.dismiss();
			removeAction();
		}
		else if (id == R.id.delete) {
			bottomSheetDialog.dismiss();
			deleteAction();
		}
		else if (id == R.id.likes_button) {
			likeListAction();
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
	}

	@Override public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (resultCode != Activity.RESULT_CANCELED) {
			if (requestCode == Constants.ACTIVITY_ENTITY_EDIT) {
				if (resultCode == Constants.RESULT_ENTITY_DELETED || resultCode == Constants.RESULT_ENTITY_REMOVED) {
					finish();
				}
			}
		}
		super.onActivityResult(requestCode, resultCode, intent);
	}

	public void editAction() {
		Intent intent = new Intent(this, MessageEdit.class)
			.putExtra(Constants.EXTRA_ENTITY_ID, entityId)
			.putExtra(Constants.EXTRA_STATE, State.Editing);
		startActivityForResult(intent, Constants.ACTIVITY_ENTITY_EDIT);
		AnimationManager.doOverridePendingTransition(this, TransitionType.FORM_TO);
	}

	public void deleteAction() {
		if (!processing) {
			processing = true;
			confirmDelete();
		}
	}

	public void removeAction() {
		if (!processing) {
			processing = true;
			confirmRemove(parentId);    // Give activity a chance for remove confirmation
		}
	}

	public void shareAction() {
		share();
	}

	public void likeAction() {
		if (!processing) {
			processing = true;
			like(!entity.userLikes);
		}
	}

	public void likeListAction() {
		if (entity != null) {
			Intent intent = new Intent(this, BaseListScreen.class);
			intent.putExtra(Constants.EXTRA_ENTITY_ID, entityId);
			intent.putExtra(Constants.EXTRA_QUERY_NAME, QueryName.LikesForMessage);
			startActivity(intent);
			AnimationManager.doOverridePendingTransition(this, TransitionType.FORM_TO);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Notifications
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override public void unpackIntent() {
		super.unpackIntent();

		Intent intent = getIntent();
		if (intent != null) {
			final Bundle extras = intent.getExtras();

			if (extras != null) {
				this.entityId = extras.getString(Constants.EXTRA_ENTITY_ID);
			}

			if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_VIEW)) {
				Uri uri = intent.getData();
				if (uri != null) {
					if (uri.getPath().contains("/message/")) {
						entityId = uri.getPath().replace("/message/", "");
					}
				}
			}
		}
	}

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		photoView = (ImageWidget) findViewById(R.id.photo);
		holderUser = findViewById(R.id.holder_user);
		holderPatch = findViewById(R.id.patch_group);
		descriptionView = (TextView) findViewById(R.id.description);
		patchPhotoView = (ImageWidget) findViewById(R.id.patch_photo);
		patchName = (TextView) findViewById(R.id.patch_name);
		userPhotoView = (ImageWidget) findViewById(R.id.user_photo);
		userName = (TextView) findViewById(R.id.user_name);
		createdDate = (TextView) findViewById(R.id.created_date);
		buttonToolbar = (ViewGroup) findViewById(R.id.button_toolbar);
		likeAnimator = (ViewAnimator) findViewById(R.id.like_button);

		shareHolder = (ViewGroup) findViewById(R.id.share_holder);
		shareView = (ViewGroup) findViewById(R.id.share_entity);
		shareRecipientsHolder = (ViewGroup) findViewById(R.id.share_recipients_holder);
		shareRecipients = (TextView) findViewById(R.id.share_recipients);

		bottomSheetLayout = (BottomSheetLayout) findViewById(R.id.bottomsheet);
		bottomSheetLayout.setPeekOnDismiss(true);
	}

	@Override protected int getLayoutId() {
		return R.layout.screen_message;
	}

	public void bind() {

		this.entity = realm.where(RealmEntity.class).equalTo("id", this.entityId).findFirst();
		if (entity != null) {
			draw();
			supportInvalidateOptionsMenu();     // In case user authenticated
			this.entity.addChangeListener(element -> {
				draw();
				supportInvalidateOptionsMenu();     // In case user authenticated
			});
		}
	}

	public void fetch(final FetchMode mode) {

		Logger.v(this, "Fetching entity: " + mode.name().toString());
		final FetchStrategy strategy = (mode != FetchMode.AUTO || !executed) ? FetchStrategy.IgnoreCache : FetchStrategy.UseCacheAndVerify;

		subscription = RestClient.getInstance().fetchEntity(this.entityId, strategy)
			.subscribe(
				response -> {
					processing = false;
					busyController.hide(true);
					executed = true;
					if (this.entity == null) {
						bind();
					}
				},
				error -> {
					processing = false;
					busyController.hide(true);
					Errors.handleError(this, error);
				});
	}

	public void draw() {

		if (entity != null) {
			/* Share */
			Boolean share = (entity.type != null && entity.type.equals(Constants.TYPE_LINK_SHARE));

			/* Message patch context */

			if (holderPatch != null) {
				if (share) {
					UI.setEnabled(holderPatch, false);
					UI.setVisibility(holderPatch, View.VISIBLE);
					UI.setVisibility(patchPhotoView, View.GONE);
				}
				else {
					if (entity.patch != null) {
						holderPatch.setTag(entity.patch);

						/* Name */
						patchName.setText(entity.patch.name);
						UI.setVisibility(holderPatch, View.VISIBLE);

						/* Photo */
						if (entity.patch.getPhoto() != null) {
							patchPhotoView.setImageWithPhoto(entity.patch.getPhoto(), null, null);
						}
						else {
							patchPhotoView.setImageWithText(entity.patch.name, false);
						}
						UI.setVisibility(patchPhotoView, View.VISIBLE);
					}
					else {
						UI.setVisibility(holderPatch, View.GONE);
					}
				}
			}

			/* User holder */
			if (holderUser != null && entity.owner != null) {
				holderUser.setTag(entity.owner);
			}

			/* User photo */
			if (userPhotoView != null) {
				if (entity.owner != null) {
					userPhotoView.setImageWithEntity(entity.owner, null);
					UI.setVisibility(userPhotoView, View.VISIBLE);
				}
				else {
					UI.setVisibility(userPhotoView, View.GONE);
				}
			}

			/* User name */
			if (userName != null) {
				if (entity.owner != null && entity.owner.name != null && entity.owner.name.length() > 0) {
					userName.setText(entity.owner.name);
					UI.setVisibility(userName, View.VISIBLE);
				}
				else {
					UI.setVisibility(userName, View.GONE);
				}
			}

			/* Created date */
			if (createdDate != null) {
				if (entity.createdDate != null) {
					createdDate.setText(DateTime.dateStringAt(entity.createdDate.longValue()));
					UI.setVisibility(createdDate, View.VISIBLE);
				}
				else {
					UI.setVisibility(createdDate, View.GONE);
				}
			}

			/* Message text */
			if (descriptionView != null) {
				descriptionView.setText(null);

				descriptionView.setOnLongClickListener(view -> {
					TextView textView = (TextView) view;
					String text = (String) textView.getText().toString();

					if (!TextUtils.isEmpty(text)) {
						android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
						android.content.ClipData clip = android.content.ClipData.newPlainText("message", text);
						clipboard.setPrimaryClip(clip);
						UI.toast(StringManager.getString(R.string.alert_copied_to_clipboard));
					}

					return true;
				});

				if (!TextUtils.isEmpty(entity.description)) {
					descriptionView.setText(Html.fromHtml(entity.description));
					UI.setVisibility(descriptionView, View.VISIBLE);
				}
				else {
					UI.setVisibility(descriptionView, View.GONE);
				}
			}

			UI.setVisibility(photoView, View.GONE);
			UI.setVisibility(shareHolder, View.GONE);
			UI.setVisibility(buttonToolbar, View.GONE);
			UI.setVisibility(shareRecipientsHolder, View.GONE);

            /* Shared entity */

			if (share) {

				RealmEntity shareEntity = null;

				if (entity.patch != null) {
					shareEntity = entity.patch;
				}

				if (shareEntity == null && entity.message != null) {
					shareEntity = entity.message;
				}

				if (shareEntity != null) {

					shareView.removeAllViews();

					if (shareEntity.schema.equals(Constants.SCHEMA_ENTITY_PATCH)) {
						patchName.setText(StringManager.getString(R.string.label_message_invite));
						PatchView patchView = new PatchView(this, R.layout.view_patch_attachment);
						patchView.bind(shareEntity);
						CardView cardView = (CardView) shareView;
						int padding = UI.getRawPixelsForDisplayPixels(0f);
						cardView.setContentPadding(padding, padding, padding, padding);
						shareView.setTag(shareEntity);
						shareView.addView(patchView);
					}
					else if (shareEntity.schema.equals(Constants.SCHEMA_ENTITY_MESSAGE)) {
						patchName.setText(StringManager.getString(R.string.label_message_shared));
						MessageView messageView = new MessageView(this, R.layout.view_message_attachment);
						messageView.bind(shareEntity, null);
						CardView cardView = (CardView) shareView;
						int padding = UI.getRawPixelsForDisplayPixels(8f);
						cardView.setContentPadding(padding, padding, padding, padding);
						shareView.setTag(shareEntity);
						shareView.addView(messageView);
					}

					UI.setVisibility(shareHolder, View.VISIBLE);
				}

				/* Show share recipients */
				if (entity.recipients != null && entity.recipients.size() > 0) {
					UI.setVisibility(shareRecipientsHolder, View.VISIBLE);
					StringBuilder recipientsString = new StringBuilder();
					for (RealmEntity recipient : entity.recipients) {
						recipientsString.append(recipient.name);
					}
					shareRecipients.setText(recipientsString);
				}
			}
			else {

				/* Photo */
				if (entity.getPhoto() != null) {
					final Photo photo = entity.getPhoto();
					photoView.setImageWithPhoto(photo, null, null);
					photoView.setTag(photo);
					UI.setVisibility(photoView, View.VISIBLE);
				}

	            /* Likes */
				if (entity.schema.equals(Constants.SCHEMA_ENTITY_USER)) {
					UI.setVisibility(findViewById(R.id.button_toolbar), View.GONE);
					return;
				}

				UI.setVisibility(buttonToolbar, View.VISIBLE);

				/* Like button coloring */
				if (likeAnimator != null) {
					likeAnimator.setDisplayedChild(0);
					if (!entity.isVisibleToCurrentUser()) {
						UI.setVisibility(likeAnimator, View.GONE);
					}
					else {
						ImageView image = (ImageView) likeAnimator.findViewById(R.id.like_image);
						if (entity.userLikes) {
							final int color = Colors.getColor(R.color.brand_primary);
							image.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
							image.setAlpha(1.0f);
						}
						else {
							image.setColorFilter(null);
							image.setAlpha(0.5f);
						}
						UI.setVisibility(likeAnimator, View.VISIBLE);
					}
				}

				/* Like count */
				View likes = findViewById(R.id.likes_button);

				if (likes != null) {
					if (entity.countLikes > 0) {
						TextView likesCount = (TextView) findViewById(R.id.likes_count);
						TextView likesLabel = (TextView) findViewById(R.id.likes_label);
						if (likesCount != null) {
							String label = getResources().getQuantityString(R.plurals.label_likes, entity.countLikes, entity.countLikes);
							likesCount.setText(String.valueOf(entity.countLikes));
							likesLabel.setText(label);
							UI.setVisibility(likes, View.VISIBLE);
						}
					}
					else {
						UI.setVisibility(likes, View.GONE);
					}
				}
			}
		}
	}

	public void like(final boolean activate) {

		likeAnimator.setDisplayedChild(1);  // Turned off in draw

		UserManager.currentUser.activityDate = DateTime.nowDate().getTime();

		if (activate) {
			subscription = RestClient.getInstance().insertLink(UserManager.userId, entityId, LinkType.Like)
				.subscribe(
					response -> {
						fetch(FetchMode.MANUAL);
					},
					error -> {
						processing = false;
						Patchr.mainThreadHandler.postDelayed(() -> {
							likeAnimator.setDisplayedChild(0);
						}, 1000);
						Errors.handleError(this, error);
					});
		}
		else {
			String linkId = entity.userLikesId;
			subscription = RestClient.getInstance().deleteLinkById(linkId)
				.subscribe(
					response -> {
						fetch(FetchMode.MANUAL);
					},
					error -> {
						processing = false;
						Patchr.mainThreadHandler.postDelayed(() -> {
							likeAnimator.setDisplayedChild(0);
						}, 1000);
						Errors.handleError(this, error);
					});
		}
	}

	public void share() {

		final String title = StringManager.getString(R.string.label_message_share_title);
		final Activity activity = this;

		MenuSheetView menuSheetView = new MenuSheetView(this, MenuSheetView.MenuType.GRID, "Share using...", new MenuSheetView.OnMenuItemClickListener() {

			@Override public boolean onMenuItemClick(final MenuItem item) {

				bottomSheetLayout.addOnSheetDismissedListener(new OnSheetDismissedListener() {

					@Override public void onDismissed(BottomSheetLayout bottomSheetLayout) {
						if (item.getItemId() == R.id.share_using_patchr) {
							/* Go to patchr share directly but looks just like an external share*/
							final Intent intent = new Intent(activity, ShareEdit.class);
							intent.putExtra(Constants.EXTRA_STATE, State.Inserting);
							intent.putExtra(Constants.EXTRA_MESSAGE_TYPE, MessageType.Share);
							intent.putExtra(Constants.EXTRA_SHARE_SOURCE, getPackageName());
							intent.putExtra(Constants.EXTRA_SHARE_ENTITY_ID, entityId);
							intent.putExtra(Constants.EXTRA_SHARE_SCHEMA, Constants.SCHEMA_ENTITY_MESSAGE);
							intent.setAction(Intent.ACTION_SEND);
							activity.startActivity(intent);
							AnimationManager.doOverridePendingTransition(activity, TransitionType.FORM_TO);
						}
						else if (item.getItemId() == R.id.share_using_other) {
							Reporting.track(AnalyticsCategory.ACTION, "Started Message Share", new Properties().putValue("network", "Android"));
							showBuiltInSharePicker(title);
						}
					}
				});

				bottomSheetLayout.dismissSheet();
				return true;
			}
		});

		menuSheetView.inflateMenu(R.menu.menu_share_sheet);
		bottomSheetLayout.showWithSheetView(menuSheetView, new InsetViewTransformer());
	}

	public void showBuiltInSharePicker(final String title) {

		final String patchName = (entity.patch.name != null) ? entity.patch.name : StringManager.getString(R.string.container_singular_lowercase);
		final String referrerName = UserManager.currentUser.name;
		final String referrerId = UserManager.currentUser.id;
		final String ownerName = entity.owner.name;
		final String path = "message/" + entityId;

		BranchUniversalObject applink = new BranchUniversalObject()
			.setCanonicalIdentifier(path)
			.addContentMetadata("entityId", entityId)
			.addContentMetadata("entitySchema", Constants.SCHEMA_ENTITY_MESSAGE)
			.addContentMetadata("referrerName", referrerName)
			.addContentMetadata("referrerId", referrerId)
			.addContentMetadata("ownerName", ownerName)
			.addContentMetadata("patchName", patchName);

		if (entity.getPhoto() != null) {
			Photo photo = entity.getPhoto();
			String settings = "h=500&crop&fit=crop&q=50";
			String photoUrl = String.format("https://3meters-images.imgix.net/%1$s?%2$s", photo.prefix, settings);
			applink.setContentImageUrl(photoUrl);  // $og_image_url
		}
		else if (entity.patch != null) {
			Photo photo = entity.patch.getPhoto();
			String settings = "h=500&crop&fit=crop&q=50";
			String photoUrl = String.format("https://3meters-images.imgix.net/%1$s?%2$s", photo.prefix, settings);
			applink.setContentImageUrl(photoUrl);  // $og_image_url
		}

		String description = String.format("%1$s posted a photo to the %2$s patch using Patchr", ownerName, patchName);
		if (entity.description != null) {
			description = String.format("%1$s posted: \"%2$s\"", ownerName, entity.description);
		}

		applink.setTitle(String.format("Shared by %1$s", referrerName));    // $og_title
		applink.setContentDescription(description);                 // $og_description

		LinkProperties linkProperties = new LinkProperties()
			.setChannel("patchr-android")
			.setFeature(Branch.FEATURE_TAG_SHARE);

		applink.generateShortUrl(this, linkProperties, new Branch.BranchLinkCreateListener() {

			@Override
			public void onLinkCreate(String url, BranchError error) {

				if (error == null) {
					ShareCompat.IntentBuilder builder = ShareCompat.IntentBuilder.from(MessageScreen.this);
					builder.setChooserTitle(title);
					builder.setType("text/plain");
					/*
					 * subject: Invitation to the \'%1$s\' patch
					 * body: %1$s has invited you to the %2$s patch! %3$s
					 */
					builder.setSubject(String.format(StringManager.getString(R.string.label_message_share_subject), ownerName));
					builder.setText(String.format(StringManager.getString(R.string.label_message_share_body), ownerName, patchName, url));

					builder.getIntent().putExtra(Constants.EXTRA_SHARE_SOURCE, getPackageName());
					builder.getIntent().putExtra(Constants.EXTRA_SHARE_ENTITY_ID, entityId);
					builder.getIntent().putExtra(Constants.EXTRA_SHARE_SCHEMA, Constants.SCHEMA_ENTITY_MESSAGE);

					builder.startChooser();
				}
			}
		});
	}
}