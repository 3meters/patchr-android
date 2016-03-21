package com.patchr.ui;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ShareCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;

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
import com.patchr.components.ModelResult;
import com.patchr.components.NetworkManager;
import com.patchr.components.NetworkManager.ResponseCode;
import com.patchr.components.NotificationManager;
import com.patchr.components.StringManager;
import com.patchr.components.UserManager;
import com.patchr.events.DataQueryResultEvent;
import com.patchr.events.EntityQueryEvent;
import com.patchr.events.EntityQueryResultEvent;
import com.patchr.events.LinkDeleteEvent;
import com.patchr.events.LinkInsertEvent;
import com.patchr.events.NotificationReceivedEvent;
import com.patchr.interfaces.IBusy.BusyAction;
import com.patchr.objects.ActionType;
import com.patchr.objects.Count;
import com.patchr.objects.Entity;
import com.patchr.objects.FetchMode;
import com.patchr.objects.Link;
import com.patchr.objects.LinkSpecType;
import com.patchr.objects.Message;
import com.patchr.objects.Message.MessageType;
import com.patchr.objects.Patch;
import com.patchr.objects.Photo;
import com.patchr.objects.Route;
import com.patchr.objects.Shortcut;
import com.patchr.objects.TransitionType;
import com.patchr.ui.components.InsetViewTransformer;
import com.patchr.ui.edit.MessageEdit;
import com.patchr.ui.views.ImageLayout;
import com.patchr.ui.views.MessageView;
import com.patchr.ui.views.PatchView;
import com.patchr.utilities.Colors;
import com.patchr.utilities.DateTime;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Errors;
import com.patchr.utilities.UI;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;
import java.util.Locale;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.util.LinkProperties;

@SuppressWarnings("ConstantConditions")
public class MessageScreen extends BaseScreen {

	protected BottomSheetLayout bottomSheetLayout;
	protected ImageLayout       photoView;
	protected View              holderUser;
	protected View              holderPatch;
	protected TextView          description;
	protected ImageLayout       patchPhotoView;
	protected TextView          patchName;
	protected ImageLayout       userPhotoView;
	protected TextView          userName;
	protected TextView          createdDate;
	protected ViewGroup         buttonHolder;

	protected ViewGroup shareHolder;
	protected ViewGroup shareView;
	protected ViewGroup shareRecipientsHolder;
	protected TextView  shareRecipients;

	protected boolean bound;
	public    String  parentId;
	protected String  notificationId;

	@Override protected void onStart() {
		super.onStart();
		Dispatcher.getInstance().register(this);
	}

	@Override protected void onResume() {
		super.onResume();
		if (!isFinishing()) {
			if (this.entity != null) {
				bind();
			}
			fetch(FetchMode.AUTO);
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
		Integer id = view.getId();

		if (id == R.id.like_button) {
			likeAction();
		}
		else if (id == R.id.likes_button) {
			likeListAction();
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
	}

	@Override public boolean onCreateOptionsMenu(Menu menu) {

		this.optionMenu = menu;

		/* Shown for owner */
		getMenuInflater().inflate(R.menu.menu_edit, menu);
		getMenuInflater().inflate(R.menu.menu_delete, menu);
		getMenuInflater().inflate(R.menu.menu_remove, menu);

		/* Shown for everyone */
		getMenuInflater().inflate(R.menu.menu_share_message, menu);
		getMenuInflater().inflate(R.menu.menu_refresh, menu);
		getMenuInflater().inflate(R.menu.menu_report, menu);        // base

		configureStandardMenuItems(menu);   // Tweaks based on permissions
		return true;
	}

	@Override public boolean onOptionsItemSelected(MenuItem item) {

		if (item.getItemId() == R.id.delete) {
			deleteAction();
		}
		else if (item.getItemId() == R.id.remove) {
			removeAction();
		}
		else if (item.getItemId() == R.id.edit) {
			editAction();
		}
		else if (item.getItemId() == R.id.refresh) {
			fetch(FetchMode.MANUAL);
		}
		else if (item.getItemId() == R.id.share) {
			shareAction();
		}
		else {
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	@Override public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (resultCode != Activity.RESULT_CANCELED) {
			if (requestCode == Constants.ACTIVITY_ENTITY_EDIT) {
				if (resultCode == Constants.RESULT_ENTITY_DELETED || resultCode == Constants.RESULT_ENTITY_REMOVED) {
					finish();
					AnimationManager.doOverridePendingTransition(this, TransitionType.PAGE_TO_RADAR_AFTER_DELETE);
				}
			}
		}
		super.onActivityResult(requestCode, resultCode, intent);
	}

	public void editAction() {
		Bundle extras = new Bundle();
		Patchr.router.route(this, Route.EDIT, entity, extras);
	}

	public void deleteAction() {
		confirmDelete();
	}

	public void removeAction() {
		confirmRemove(parentId);    // Give activity a chance for remove confirmation
	}

	public void shareAction() {
		if (!UserManager.shared().authenticated()) {
			UserManager.shared().showGuestGuard(this, "Sign up for a free account to share messages and more.");
			return;
		}
		share();
	}

	public void likeAction() {

		if (processing) return;
		processing = true;

		if (!UserManager.shared().authenticated()) {
			processing = false;
			UserManager.shared().showGuestGuard(this, "Sign up for a free account to like messages and more.");
			return;
		}

		Link linkLike = entity.linkFromAppUser(Constants.TYPE_LINK_LIKE);
		like(linkLike == null);
	}

	public void likeListAction() {
		if (entity != null) {
			Bundle extras = new Bundle();
			extras.putInt(Constants.EXTRA_LIST_ITEM_RESID, R.layout.temp_listitem_user);
			extras.putString(Constants.EXTRA_LIST_LINK_DIRECTION, Link.Direction.in.name());
			extras.putString(Constants.EXTRA_LIST_LINK_SCHEMA, Constants.SCHEMA_ENTITY_USER);
			extras.putString(Constants.EXTRA_LIST_LINK_TYPE, Constants.TYPE_LINK_LIKE);
			extras.putInt(Constants.EXTRA_LIST_TITLE_RESID, R.string.form_title_likes_list);
			extras.putInt(Constants.EXTRA_TRANSITION_TYPE, TransitionType.DRILL_TO);
			Patchr.router.route(this, Route.ENTITY_LIST, entity, extras);
		}
	}

	public void onFetchComplete() {
		super.onFetchComplete();
		bind();
	}

	/*--------------------------------------------------------------------------------------------
	 * Notifications
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe(threadMode = ThreadMode.MAIN) public void onEntityResult(final EntityQueryResultEvent event) {

		if (event.actionType == ActionType.ACTION_GET_ENTITY) {

			if (event.entity != null && event.entity.id != null && event.entity.id.equals(entityId)) {

				bound = true;
				if (event.entity != null) {
					entity = event.entity;

					if (parentId != null) {
						entity.toId = parentId;
					}

					if (entity instanceof Patch) {
						Patchr.getInstance().setCurrentPatch(entity);
					}
				}
						/*
						 * Possible to hit this before options menu has been set. If so then
						 * configureStandardMenuItems will be called in onCreateOptionsMenu.
						 */
				if (optionMenu != null) {
					configureStandardMenuItems(optionMenu);
				}

						/* Ensure this is flagged as read */
				if (notificationId != null) {
					if (NotificationManager.getInstance().getNotifications().containsKey(notificationId)) {
						NotificationManager.getInstance().getNotifications().get(notificationId).read = true;
					}
				}

				onFetchComplete();
			}
		}
	}

	@Subscribe(threadMode = ThreadMode.MAIN) public void onDataQueryResult(final DataQueryResultEvent event) {

		if (event.actionType == ActionType.ACTION_LINK_INSERT_LIKE
				|| event.actionType == ActionType.ACTION_LINK_DELETE_LIKE) {
			if (event.entity != null && event.entity.id != null && event.entity.id.equals(entityId)) {
				onFetchComplete();
			}
		}
	}

	@Subscribe public void onNotificationReceived(final NotificationReceivedEvent event) {
	    /*
	     * Refresh the form because something might have changed e.g. new likes.
		 */
		if ((event.notification.parentId != null && event.notification.parentId.equals(entityId))
				|| (event.notification.targetId != null && event.notification.targetId.equals(entityId))) {
			runOnUiThread(new Runnable() {
				@Override public void run() {
					//fetch(FetchMode.AUTO);
				}
			});
		}
	}

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

		bottomSheetLayout = (BottomSheetLayout) findViewById(R.id.bottomsheet);
		bottomSheetLayout.setPeekOnDismiss(true);
	}

	@Override public void confirmDelete() {

		String message = StringManager.getString(R.string.alert_delete_message_message_no_name);
		if (entity.type.equals(MessageType.ROOT)) {
			Link linkPlace = entity.getParentLink(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_PATCH);
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

			@Override protected void onPreExecute() {
				busyPresenter.show(BusyAction.ActionWithMessage, R.string.progress_deleting, MessageScreen.this);
			}

			@Override protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncDeleteEntity");
				String seedParentId = entity.type.equals(MessageType.ROOT) ? entity.patchId : null;
				return ((DataController) DataController.getInstance()).deleteMessage(entity.id, false, seedParentId, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
			}

			@Override protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				busyPresenter.hide(true);
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					Logger.i(this, "Deleted entity: " + entity.id);
					/*
					 * We either go back to a list or to radar.
					 */
					UI.showToastNotification(StringManager.getString(R.string.alert_deleted), Toast.LENGTH_SHORT);
					setResult(Constants.RESULT_ENTITY_DELETED);
					finish();
					AnimationManager.doOverridePendingTransition(MessageScreen.this, TransitionType.FORM_TO_PAGE_AFTER_DELETE);
				}
				else {
					Errors.handleError(MessageScreen.this, result.serviceResponse);
				}
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	@Override protected int getLayoutId() {
		return R.layout.message_screen;
	}

	public void fetch(final FetchMode mode) {
		/*
		 * Called on main thread.
		 */
		Logger.v(this, "Binding: " + mode.name().toString());
		EntityQueryEvent request = new EntityQueryEvent();
		request.setLinkProfile(LinkSpecType.LINKS_FOR_MESSAGE)
				.setActionType(ActionType.ACTION_GET_ENTITY)
				.setFetchMode(mode)
				.setEntityId(entityId)
				.setTag(System.identityHashCode(this));

		if (bound && entity != null && mode != FetchMode.MANUAL) {
			request.setCacheStamp(entity.getCacheStamp());
		}

		Dispatcher.getInstance().post(request);
	}

	public void bind() {
	    /*
	     * For now, we assume that the candi form isn't recycled.
		 *
		 * We leave most of the views visible by default so they are visible in the layout editor.
		 *
		 * - WebImageView primary image is visible by default
		 * - WebImageView child views are gone by default
		 * - Header views are visible by default
		 */
		if (entity == null) return;
		assert rootView != null;

		photoView = (ImageLayout) findViewById(R.id.photo);
		holderUser = findViewById(R.id.holder_user);
		holderPatch = findViewById(R.id.holder_patch);
		description = (TextView) findViewById(R.id.description);
		patchPhotoView = (ImageLayout) findViewById(R.id.patch_photo);
		patchName = (TextView) findViewById(R.id.patch_name);
		userPhotoView = (ImageLayout) findViewById(R.id.user_photo);
		userName = (TextView) findViewById(R.id.user_name);
		createdDate = (TextView) findViewById(R.id.created_date);
		buttonHolder = (ViewGroup) findViewById(R.id.toolbar);

		shareHolder = (ViewGroup) findViewById(R.id.share_holder);
		shareView = (ViewGroup) findViewById(R.id.share_entity);
		shareRecipientsHolder = (ViewGroup) findViewById(R.id.share_recipients_holder);
		shareRecipients = (TextView) findViewById(R.id.share_recipients);

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
				Link linkPlace = entity.getParentLink(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_PATCH);

				if (linkPlace != null) {
					holderPatch.setTag(linkPlace.shortcut.getAsEntity());

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

		if (holderUser != null && entity.creator != null) {
			holderUser.setTag(entity.creator);
		}

		/* User photo */

		if (userPhotoView != null) {
			if (entity.creator != null) {
				userPhotoView.setImageWithEntity(entity.creator);
				UI.setVisibility(userPhotoView, View.VISIBLE);
			}
			else {
				UI.setVisibility(userPhotoView, View.GONE);
			}
		}

		/* User name */

		if (userName != null) {
			if (entity.creator != null && entity.creator.name != null && entity.creator.name.length() > 0) {
				userName.setText(entity.creator.name);
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

			if (!TextUtils.isEmpty(entity.description)) {
				description.setText(entity.description);
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

			Link linkEntity = entity.getParentLink(Constants.TYPE_LINK_SHARE, Constants.SCHEMA_ENTITY_PATCH);
			if (linkEntity != null) {
				if (linkEntity.shortcut != null) {
					shareEntity = linkEntity.shortcut.getAsEntity();
				}
			}

			if (shareEntity == null) {
				linkEntity = entity.getParentLink(Constants.TYPE_LINK_SHARE, Constants.SCHEMA_ENTITY_MESSAGE);
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
			List<Link> links = entity.getLinks(Constants.TYPE_LINK_SHARE, Constants.SCHEMA_ENTITY_USER, null, Link.Direction.out);
			for (Link link : links) {
				recipientsString.append(link.shortcut.name);
			}
			shareRecipients.setText(recipientsString);
		}
		else {

			/* A message without a share */

			if (entity.photo != null) {
				final Photo photo = entity.photo;
				photoView.setImageWithPhoto(photo);
				photoView.setTag(photo);
				UI.setVisibility(photoView, View.VISIBLE);
			}

            /* Likes */
			bindLike();    // Handled in parent class
		}
	}

	public void bindLike() {

		/* We don't support like/watch for users */
		if (entity.schema.equals(Constants.SCHEMA_ENTITY_USER)) {
			UI.setVisibility(findViewById(R.id.toolbar), View.GONE);
			return;
		}

		UI.setVisibility(findViewById(R.id.toolbar), View.VISIBLE);

		/* Like button coloring */
		ViewAnimator like = (ViewAnimator) findViewById(R.id.like_button);
		if (like != null) {
			like.setDisplayedChild(0);
			if (entity instanceof Patch && !((Patch) entity).isVisibleToCurrentUser()) {
				UI.setVisibility(like, View.GONE);
			}
			else {
				Link link = entity.linkFromAppUser(Constants.TYPE_LINK_LIKE);
				ImageView image = (ImageView) like.findViewById(R.id.like_image);
				if (link != null) {
					final int color = Colors.getColor(R.color.brand_primary);
					image.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
					image.setAlpha(1.0f);
				}
				else {
					image.setColorFilter(null);
					image.setAlpha(0.5f);
				}
				UI.setVisibility(like, View.VISIBLE);
			}
		}

		/* Like count */
		View likes = findViewById(R.id.likes_button);
		if (likes != null) {
			Count count = entity.getCount(Constants.TYPE_LINK_LIKE, null, true, Link.Direction.in);
			if (count == null) {
				count = new Count(Constants.TYPE_LINK_LIKE, Constants.SCHEMA_ENTITY_PATCH, null, 0);
			}
			if (count.count.intValue() > 0) {
				TextView likesCount = (TextView) findViewById(R.id.likes_count);
				TextView likesLabel = (TextView) findViewById(R.id.likes_label);
				if (likesCount != null) {
					String label = getResources().getQuantityString(R.plurals.label_likes, count.count.intValue(), count.count.intValue());
					likesCount.setText(String.valueOf(count.count.intValue()));
					likesLabel.setText(label);
					UI.setVisibility(likes, View.VISIBLE);
				}
			}
			else {
				UI.setVisibility(likes, View.GONE);
			}
		}
	}

	public void like(final boolean activate) {

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				ViewAnimator animator = (ViewAnimator) findViewById(R.id.like_button);
				if (animator != null) {
					animator.setDisplayedChild(1);  // Turned off in drawButtons
				}
			}
		});

		if (UserManager.shared().authenticated()) {
			UserManager.currentUser.activityDate = DateTime.nowDate().getTime();
		}

		if (activate) {

			/* Used as part of link management */
			Shortcut fromShortcut = UserManager.currentUser.getAsShortcut();
			Shortcut toShortcut = entity.getAsShortcut();

			LinkInsertEvent update = new LinkInsertEvent()
					.setFromId(UserManager.currentUser.id)
					.setToId(entity.id)
					.setType(Constants.TYPE_LINK_LIKE)
					.setEnabled(true)
					.setFromShortcut(fromShortcut)
					.setToShortcut(toShortcut)
					.setActionEvent("like_entity_" + entity.schema.toLowerCase(Locale.US))
					.setSkipCache(false);

			update.setActionType(ActionType.ACTION_LINK_INSERT_LIKE)
					.setTag(System.identityHashCode(this));

			Dispatcher.getInstance().post(update);
		}
		else {

			LinkDeleteEvent update = new LinkDeleteEvent()
					.setFromId(UserManager.currentUser.id)
					.setToId(entity.id)
					.setType(Constants.TYPE_LINK_LIKE)
					.setSchema(entity.schema)
					.setActionEvent("unlike_entity_" + entity.schema.toLowerCase(Locale.US));

			update.setActionType(ActionType.ACTION_LINK_DELETE_LIKE)
					.setTag(System.identityHashCode(this));

			Dispatcher.getInstance().post(update);
		}
	}

	public void share() {

		final String entityName = (entity.name != null) ? entity.name : StringManager.getString(R.string.container_singular_lowercase);
		final String title = StringManager.getString(R.string.label_message_share_title);
		final Activity activity = this;

		MenuSheetView menuSheetView = new MenuSheetView(this, MenuSheetView.MenuType.GRID, "Share using...", new MenuSheetView.OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				if (item.getItemId() == R.id.share_using_patchr) {
					/*
					 * Go to patchr share directly but looks just like an external share
					 */
					bottomSheetLayout.dismissSheet();
					final IntentBuilder intentBuilder = new IntentBuilder(activity, MessageEdit.class);
					final Intent intent = intentBuilder.create();
					intent.putExtra(Constants.EXTRA_SHARE_SOURCE, getPackageName());
					intent.putExtra(Constants.EXTRA_SHARE_ID, entityId);
					intent.putExtra(Constants.EXTRA_SHARE_SCHEMA, Constants.SCHEMA_ENTITY_MESSAGE);
					intent.setAction(Intent.ACTION_SEND);
					activity.startActivity(intent);
					AnimationManager.doOverridePendingTransition(activity, TransitionType.FORM_TO);
				}
				else if (item.getItemId() == R.id.share_using_other) {
					bottomSheetLayout.dismissSheet();
					showBuiltInSharePicker(title);
				}
				else {
					bottomSheetLayout.dismissSheet();
					UI.showToastNotification(item.getTitle().toString(), Toast.LENGTH_SHORT);
				}
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

		if (entity.photo != null) {
			Photo photo = entity.photo;
			String settings = "h=500&crop&fit=crop&q=50";
			String photoUrl = String.format("https://3meters-images.imgix.net/%1$s?%2$s", photo.prefix, settings);
			applink.setContentImageUrl(photoUrl);  // $og_image_url
		}
		else if (entity.patch != null) {
			Photo photo = entity.patch.photo;
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
					builder.getIntent().putExtra(Constants.EXTRA_SHARE_ID, entityId);
					builder.getIntent().putExtra(Constants.EXTRA_SHARE_SCHEMA, Constants.SCHEMA_ENTITY_MESSAGE);

					builder.startChooser();
				}
			}
		});
	}

}