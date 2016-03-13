package com.patchr.ui;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ShareCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.flipboard.bottomsheet.BottomSheetLayout;
import com.flipboard.bottomsheet.commons.MenuSheetView;
import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.AnimationManager;
import com.patchr.components.DataController;
import com.patchr.components.IntentBuilder;
import com.patchr.components.Logger;
import com.patchr.components.ModelResult;
import com.patchr.components.NetworkManager;
import com.patchr.components.NetworkManager.ResponseCode;
import com.patchr.components.StringManager;
import com.patchr.components.UserManager;
import com.patchr.events.ActionEvent;
import com.patchr.events.DataErrorEvent;
import com.patchr.events.DataNoopEvent;
import com.patchr.events.DataResultEvent;
import com.patchr.events.NotificationReceivedEvent;
import com.patchr.interfaces.IBusy.BusyAction;
import com.patchr.objects.Entity;
import com.patchr.objects.Link;
import com.patchr.objects.LinkSpecType;
import com.patchr.objects.Message;
import com.patchr.objects.Message.MessageType;
import com.patchr.objects.Photo;
import com.patchr.objects.Route;
import com.patchr.objects.TransitionType;
import com.patchr.ui.base.BaseEntityForm;
import com.patchr.ui.components.InsetViewTransformer;
import com.patchr.ui.edit.MessageEdit;
import com.patchr.ui.views.ImageLayout;
import com.patchr.ui.views.MessageView;
import com.patchr.ui.views.PatchView;
import com.patchr.utilities.DateTime;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Errors;
import com.patchr.utilities.UI;
import com.squareup.otto.Subscribe;

import java.util.List;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.util.LinkProperties;

@SuppressWarnings("ConstantConditions")
public class MessageForm extends BaseEntityForm {

	protected BottomSheetLayout mBottomSheetLayout;

	@Override public void unpackIntent() {
		super.unpackIntent();

		Intent intent = getIntent();
		if (intent != null) {
			final Bundle extras = intent.getExtras();

			if (extras != null) {
				mForId = extras.getString(Constants.EXTRA_ENTITY_FOR_ID); // Provides message context. Could be a patch or a user
			}

			if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_VIEW)) {
				Uri uri = intent.getData();
				if (uri != null) {
					if (uri.getPath().contains("/message/")) {
						mEntityId = uri.getPath().replace("/message/", "");
					}
				}
			}
		}
	}

	@Override protected void onResume() {
		super.onResume();
		draw(null);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	public void onPatchClick(View view) {
		Entity entity = (Entity) view.getTag();
		Patchr.router.route(MessageForm.this, Route.BROWSE, entity.patch, null);
	}

	public void onEditClick(View view) {
		Bundle extras = new Bundle();
		Patchr.router.route(this, Route.EDIT, mEntity, extras);
	}

	public void onDeleteClick(View view) {
		Patchr.router.route(this, Route.DELETE, mEntity, null);
	}

	public void onRemoveClick(View view) {
		Bundle extras = new Bundle();
		extras.putString(Constants.EXTRA_ENTITY_PARENT_ID, (String) view.getTag());
		Patchr.router.route(this, Route.REMOVE, mEntity, extras);
	}

	public void onLikeButtonClick(View view) {

		if (mProcessing) return;
		mProcessing = true;

		if (!UserManager.getInstance().authenticated()) {
			mProcessing = false;
			UserManager.getInstance().showGuestGuard(this, "Sign up for a free account to like messages and more.");
			return;
		}

		Link linkLike = mEntity.linkFromAppUser(Constants.TYPE_LINK_LIKE);
		like(linkLike == null);
	}

	public void onLikesListButtonClick(View view) {
		if (mEntity != null) {
			Bundle extras = new Bundle();
			extras.putString(Constants.EXTRA_LIST_LINK_TYPE, Constants.TYPE_LINK_LIKE);
			extras.putInt(Constants.EXTRA_LIST_TITLE_RESID, R.string.form_title_likes_list);
			extras.putInt(Constants.EXTRA_LIST_ITEM_RESID, R.layout.temp_listitem_liker);
			extras.putInt(Constants.EXTRA_TRANSITION_TYPE, TransitionType.DRILL_TO);
			extras.putInt(Constants.EXTRA_LIST_EMPTY_RESID, R.string.label_likes_empty);
			Patchr.router.route(this, Route.USER_LIST, mEntity, extras);
		}
	}

	public void onShareClick(View view) {

		if (!UserManager.getInstance().authenticated()) {
			UserManager.getInstance().showGuestGuard(this, "Sign up for a free account to share messages and more.");
			return;
		}

		share();
	}

	/*--------------------------------------------------------------------------------------------
	 * Notifications
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe public void onDataResult(DataResultEvent event) {
		super.onDataResult(event); // Handles GET_ENTITY, INSERT_LIKE, DELETE_LIKE
	}

	@Subscribe public void onDataError(DataErrorEvent event) {
		super.onDataError(event);
	}

	@Subscribe public void onDataNoop(DataNoopEvent event) {
		super.onDataNoop(event);
	}

	@Subscribe public void onNotificationReceived(final NotificationReceivedEvent event) {
	    /*
	     * Refresh the form because something might have changed e.g. new likes.
		 */
		if ((event.notification.parentId != null && event.notification.parentId.equals(mEntityId))
				|| (event.notification.targetId != null && event.notification.targetId.equals(mEntityId))) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					bind(BindingMode.AUTO);
				}
			});
		}
	}

	@Subscribe public void onViewClick(ActionEvent event) {
		super.onViewClick(event);
	}

	@Override public void onAdd(Bundle extras) {
		extras.putString(Constants.EXTRA_ENTITY_SCHEMA, Constants.SCHEMA_ENTITY_MESSAGE);
		super.onAdd(extras);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mLinkProfile = LinkSpecType.LINKS_FOR_MESSAGE;
		mBottomSheetLayout = (BottomSheetLayout) findViewById(R.id.bottomsheet);
		mBottomSheetLayout.setPeekOnDismiss(true);
	}

	@Override public void draw(View view) {
	    /*
	     * For now, we assume that the candi form isn't recycled.
		 *
		 * We leave most of the views visible by default so they are visible in the layout editor.
		 *
		 * - WebImageView primary image is visible by default
		 * - WebImageView child views are gone by default
		 * - Header views are visible by default
		 */
		if (mEntity == null) return;
		if (view == null) {
			view = findViewById(android.R.id.content);
		}

		final ImageLayout photoView = (ImageLayout) view.findViewById(R.id.image_layout);
		final View holderUser = view.findViewById(R.id.holder_user);
		final View holderPatch = view.findViewById(R.id.holder_patch);
		final TextView description = (TextView) view.findViewById(R.id.description);
		final ImageLayout patchPhotoView = (ImageLayout) view.findViewById(R.id.patch_photo);
		final TextView patchName = (TextView) view.findViewById(R.id.patch_name);
		final ImageLayout userPhotoView = (ImageLayout) view.findViewById(R.id.user_photo);
		final TextView userName = (TextView) view.findViewById(R.id.user_name);
		final TextView createdDate = (TextView) view.findViewById(R.id.created_date);
		final ViewGroup buttonHolder = (ViewGroup) view.findViewById(R.id.toolbar);

		final ViewGroup shareHolder = (ViewGroup) view.findViewById(R.id.share_holder);
		final ViewGroup shareView = (ViewGroup) view.findViewById(R.id.share_entity);

		final ViewGroup shareRecipientsHolder = (ViewGroup) view.findViewById(R.id.share_recipients_holder);
		final TextView shareRecipients = (TextView) view.findViewById(R.id.share_recipients);

        /* Share */

		Boolean share = (mEntity.type != null && mEntity.type.equals(Constants.TYPE_LINK_SHARE));

		/* Message patch context */

		if (holderPatch != null) {
			if (share) {
				UI.setEnabled(holderPatch, false);
				UI.setVisibility(holderPatch, View.VISIBLE);
				UI.setVisibility(patchPhotoView, View.GONE);
			}
			else {
				Link linkPlace = mEntity.getParentLink(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_PATCH);

				if (linkPlace != null) {
					holderPatch.setTag(mEntity);

					/* Name */
					patchName.setText(linkPlace.shortcut.name);
					UI.setVisibility(holderPatch, View.VISIBLE);

					/* Photo */
					if (linkPlace.shortcut.photo != null) {
						patchPhotoView.setImageWithPhoto(linkPlace.shortcut.photo);
					}
					else {
						patchPhotoView.setImageWithText(linkPlace.shortcut.name, false);
					}
					UI.setVisibility(patchPhotoView, View.VISIBLE);
				}
				else {
					UI.setVisibility(holderPatch, View.GONE);
				}
			}
		}

		/* User holder */

		if (holderUser != null && mEntity.creator != null) {
			holderUser.setTag(mEntity.creator);
		}

		/* User photo */

		if (userPhotoView != null) {
			if (mEntity.creator != null) {
				userPhotoView.setImageWithEntity(mEntity.creator);
				UI.setVisibility(userPhotoView, View.VISIBLE);
			}
			else {
				UI.setVisibility(userPhotoView, View.GONE);
			}
		}

		/* User name */

		if (userName != null) {
			if (mEntity.creator != null && mEntity.creator.name != null && mEntity.creator.name.length() > 0) {
				userName.setText(mEntity.creator.name);
				UI.setVisibility(userName, View.VISIBLE);
			}
			else {
				UI.setVisibility(userName, View.GONE);
			}
		}

		/* Created date */

		if (createdDate != null) {
			if (mEntity.createdDate != null) {
				createdDate.setText(DateTime.dateStringAt(mEntity.createdDate.longValue()));
				UI.setVisibility(createdDate, View.VISIBLE);
			}
			else {
				UI.setVisibility(createdDate, View.GONE);
			}
		}

		/* Message text */

		if (description != null) {
			description.setText(null);

			description.setOnLongClickListener(new OnLongClickListener() {

				@Override
				public boolean onLongClick(View v) {
					TextView textView = (TextView) v;
					String text = (String) textView.getText().toString();

					if (!TextUtils.isEmpty(text)) {
						android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
						android.content.ClipData clip = android.content.ClipData.newPlainText("message", text);
						clipboard.setPrimaryClip(clip);
						UI.showToastNotification(StringManager.getString(R.string.alert_copied_to_clipboard), Toast.LENGTH_SHORT);
					}

					return true;
				}
			});

			if (!TextUtils.isEmpty(mEntity.description)) {
				description.setText(mEntity.description);
				UI.setVisibility(description, View.VISIBLE);
			}
			else {
				UI.setVisibility(description, View.GONE);
			}
		}

		UI.setVisibility(photoView, View.GONE);
		UI.setVisibility(shareHolder, View.GONE);
		UI.setVisibility(buttonHolder, View.GONE);
		UI.setVisibility(shareRecipientsHolder, View.GONE);

        /* Shared entity */

		if (share) {

			Entity shareEntity = null;

			Link linkEntity = mEntity.getParentLink(Constants.TYPE_LINK_SHARE, Constants.SCHEMA_ENTITY_PATCH);
			if (linkEntity != null) {
				if (linkEntity.shortcut != null) {
					shareEntity = linkEntity.shortcut.getAsEntity();
				}
			}

			if (shareEntity == null) {
				linkEntity = mEntity.getParentLink(Constants.TYPE_LINK_SHARE, Constants.SCHEMA_ENTITY_MESSAGE);
				if (linkEntity != null) {
					if (linkEntity.shortcut != null) {
						shareEntity = linkEntity.shortcut.getAsEntity();
					}
				}
			}

			if (shareEntity != null) {

				shareView.removeAllViews();

				if (shareEntity.schema.equals(Constants.SCHEMA_ENTITY_PATCH)) {
					patchName.setText(StringManager.getString(R.string.label_message_invite));
					PatchView patchView = new PatchView(this, R.layout.patch_view_attachment);
					patchView.databind(shareEntity);
					CardView cardView = (CardView) shareView;
					int padding = UI.getRawPixelsForDisplayPixels(0f);
					cardView.setContentPadding(padding, padding, padding, padding);
					shareView.setTag(shareEntity);
					shareView.addView(patchView);
				}
				else if (shareEntity.schema.equals(Constants.SCHEMA_ENTITY_MESSAGE)) {
					patchName.setText(StringManager.getString(R.string.label_message_shared));
					MessageView messageView = new MessageView(this, R.layout.message_view_attachment);
					messageView.databind(shareEntity);
					CardView cardView = (CardView) shareView;
					int padding = UI.getRawPixelsForDisplayPixels(8f);
					cardView.setContentPadding(padding, padding, padding, padding);
					shareView.setTag(shareEntity);
					shareView.addView(messageView);
				}

				UI.setVisibility(shareHolder, View.VISIBLE);
			}
			else if (linkEntity != null) {

				/* Message that shares an entity but shortcut was blocked by permissions */

				if (linkEntity.targetSchema.equals(Constants.SCHEMA_ENTITY_MESSAGE)) {

					shareView.removeAllViews();
					View blockView = LayoutInflater.from(this).inflate(R.layout.temp_button_share_message_blocked, null, false);

					Entity message = new Message();
					message.schema = Constants.SCHEMA_ENTITY_MESSAGE;
					message.id = linkEntity.toId;

					shareView.setTag(message);
					shareView.addView(blockView);

					UI.setVisibility(shareHolder, View.VISIBLE);
				}
			}

			/* Show share recipients */

			UI.setVisibility(shareRecipientsHolder, View.VISIBLE);
			StringBuilder recipientsString = new StringBuilder();
			List<Link> links = mEntity.getLinks(Constants.TYPE_LINK_SHARE, Constants.SCHEMA_ENTITY_USER, null, Link.Direction.out);
			for (Link link : links) {
				recipientsString.append(link.shortcut.name);
			}
			shareRecipients.setText(recipientsString);
		}
		else {

			/* A message without a share */

			if (mEntity.photo != null) {
				final Photo photo = mEntity.photo;
				photoView.setImageWithPhoto(photo);
				photoView.setTag(photo);
				UI.setVisibility(photoView, View.VISIBLE);
			}

            /* Likes */
			drawLikeWatch(view);    // Handled in parent class
		}
	}

	@Override public void configureActionBar() {
		super.configureActionBar();
		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayShowTitleEnabled(false);  // Dont show title
		}
	}

	@Override public void share() {

		if (!UserManager.getInstance().authenticated()) {
			UserManager.getInstance().showGuestGuard(this, "Sign up for a free account to share messages and more.");
			return;
		}

		final String entityName = (mEntity.name != null) ? mEntity.name : StringManager.getString(R.string.container_singular_lowercase);
		final String title = StringManager.getString(R.string.label_message_share_title);
		final Activity activity = this;

		MenuSheetView menuSheetView = new MenuSheetView(this, MenuSheetView.MenuType.GRID, "Share using...", new MenuSheetView.OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				if (item.getItemId() == R.id.share_using_patchr) {
					/*
					 * Go to patchr share directly but looks just like an external share
					 */
					mBottomSheetLayout.dismissSheet();
					final IntentBuilder intentBuilder = new IntentBuilder(activity, MessageEdit.class);
					final Intent intent = intentBuilder.create();
					intent.putExtra(Constants.EXTRA_SHARE_SOURCE, getPackageName());
					intent.putExtra(Constants.EXTRA_SHARE_ID, mEntityId);
					intent.putExtra(Constants.EXTRA_SHARE_SCHEMA, Constants.SCHEMA_ENTITY_MESSAGE);
					intent.setAction(Intent.ACTION_SEND);
					activity.startActivity(intent);
					AnimationManager.doOverridePendingTransition(activity, TransitionType.FORM_TO);
				}
				else if (item.getItemId() == R.id.share_using_other) {
					mBottomSheetLayout.dismissSheet();
					showBuiltInSharePicker(title);
				}
				else {
					mBottomSheetLayout.dismissSheet();
					UI.showToastNotification(item.getTitle().toString(), Toast.LENGTH_SHORT);
				}
				return true;
			}
		});

		menuSheetView.inflateMenu(R.menu.menu_share_sheet);
		mBottomSheetLayout.showWithSheetView(menuSheetView, new InsetViewTransformer());
	}

	@Override public void confirmDelete() {

		String message = StringManager.getString(R.string.alert_delete_message_message_no_name);
		if (mEntity.type.equals(MessageType.ROOT)) {
			Link linkPlace = mEntity.getParentLink(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_PATCH);
			if (linkPlace != null) {
				message = String.format(StringManager.getString(R.string.alert_delete_message_message), linkPlace.shortcut.name);
			}
		}
		final AlertDialog dialog = Dialogs.alertDialog(null
				, StringManager.getString(R.string.alert_delete_message_title)
				, message
				, null
				, this
				, android.R.string.ok
				, android.R.string.cancel
				, null
				, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (which == DialogInterface.BUTTON_POSITIVE) {
							delete();
						}
					}
				}
				, null);
		dialog.setCanceledOnTouchOutside(false);
	}

	@Override protected void delete() {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mUiController.getBusyController().show(BusyAction.ActionWithMessage, mDeleteProgressResId, MessageForm.this);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncDeleteEntity");
				String seedParentId = mEntity.type.equals(MessageType.ROOT) ? mEntity.patchId : null;
				return ((DataController) DataController.getInstance()).deleteMessage(mEntity.id, false, seedParentId, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
			}

			@Override
			protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				mUiController.getBusyController().hide(true);
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					Logger.i(this, "Deleted entity: " + mEntity.id);
					/*
					 * We either go back to a list or to radar.
					 */
					UI.showToastNotification(StringManager.getString(mDeletedResId), Toast.LENGTH_SHORT);
					setResultCode(Constants.RESULT_ENTITY_DELETED);
					finish();
					AnimationManager.doOverridePendingTransition(MessageForm.this, TransitionType.FORM_TO_PAGE_AFTER_DELETE);
				}
				else {
					Errors.handleError(MessageForm.this, result.serviceResponse);
				}
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	@Override protected int getLayoutId() {
		return R.layout.message_form;
	}

	public void showBuiltInSharePicker(final String title) {

		final String patchName = (mEntity.patch.name != null) ? mEntity.patch.name : StringManager.getString(R.string.container_singular_lowercase);
		final String referrerName = UserManager.getInstance().getCurrentUser().name;
		final String referrerId = UserManager.getInstance().getCurrentUser().id;
		final String ownerName = mEntity.owner.name;
		final String path = "message/" + mEntityId;

		BranchUniversalObject applink = new BranchUniversalObject()
				.setCanonicalIdentifier(path)
				.addContentMetadata("entityId", mEntityId)
				.addContentMetadata("entitySchema", Constants.SCHEMA_ENTITY_MESSAGE)
				.addContentMetadata("referrerName", referrerName)
				.addContentMetadata("referrerId", referrerId)
				.addContentMetadata("ownerName", ownerName)
				.addContentMetadata("patchName", patchName);

		if (mEntity.photo != null) {
			Photo photo = mEntity.photo;
			String settings = "h=500&crop&fit=crop&q=50";
			String photoUrl = String.format("https://3meters-images.imgix.net/%1$s?%2$s", photo.prefix, settings);
			applink.setContentImageUrl(photoUrl);  // $og_image_url
		}
		else if (mEntity.patch != null) {
			Photo photo = mEntity.patch.photo;
			String settings = "h=500&crop&fit=crop&q=50";
			String photoUrl = String.format("https://3meters-images.imgix.net/%1$s?%2$s", photo.prefix, settings);
			applink.setContentImageUrl(photoUrl);  // $og_image_url
		}

		String description = String.format("%1$s posted a photo to the %2$s patch using Patchr", ownerName, patchName);
		if (mEntity.description != null) {
			description = String.format("%1$s posted: \"%2$s\"", ownerName, mEntity.description);
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
					ShareCompat.IntentBuilder builder = ShareCompat.IntentBuilder.from(MessageForm.this);
					builder.setChooserTitle(title);
					builder.setType("text/plain");
					/*
					 * subject: Invitation to the \'%1$s\' patch
					 * body: %1$s has invited you to the %2$s patch! %3$s
					 */
					builder.setSubject(String.format(StringManager.getString(R.string.label_message_share_subject), ownerName));
					builder.setText(String.format(StringManager.getString(R.string.label_message_share_body), ownerName, patchName, url));

					builder.getIntent().putExtra(Constants.EXTRA_SHARE_SOURCE, getPackageName());
					builder.getIntent().putExtra(Constants.EXTRA_SHARE_ID, mEntityId);
					builder.getIntent().putExtra(Constants.EXTRA_SHARE_SCHEMA, Constants.SCHEMA_ENTITY_MESSAGE);

					builder.startChooser();
				}
			}
		});
	}
}