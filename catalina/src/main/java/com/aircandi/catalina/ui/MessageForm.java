package com.aircandi.catalina.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ShareCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.aircandi.Aircandi;
import com.aircandi.R.color;
import com.aircandi.catalina.Constants;
import com.aircandi.catalina.R;
import com.aircandi.catalina.components.EntityManager;
import com.aircandi.catalina.objects.LinkProfile;
import com.aircandi.catalina.objects.Message;
import com.aircandi.catalina.objects.Message.MessageType;
import com.aircandi.components.Logger;
import com.aircandi.components.MessagingManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ModelResult;
import com.aircandi.components.StringManager;
import com.aircandi.controllers.IEntityController;
import com.aircandi.events.EntitiesLoadedEvent;
import com.aircandi.events.MessageEvent;
import com.aircandi.monitors.EntityMonitor;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Link;
import com.aircandi.objects.Link.Direction;
import com.aircandi.objects.Photo;
import com.aircandi.objects.Route;
import com.aircandi.objects.TransitionType;
import com.aircandi.queries.EntitiesQuery;
import com.aircandi.ui.EntityListFragment;
import com.aircandi.ui.EntityListFragment.Highlight;
import com.aircandi.ui.EntityListFragment.ViewType;
import com.aircandi.ui.base.BaseEntityForm;
import com.aircandi.ui.base.IBusy.BusyAction;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.ui.widgets.EntityView;
import com.aircandi.ui.widgets.FlowLayout;
import com.aircandi.utilities.Colors;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.Integers;
import com.aircandi.utilities.UI;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.List;

public class MessageForm extends BaseEntityForm {

	private EntityListFragment mListFragment;
	private String             mChildId;
	private Highlight          mHighlight;
	private List<Entity> mTos = new ArrayList<Entity>();

	@Override
	public void unpackIntent() {
		super.unpackIntent();

		Intent intent = getIntent();
		if (intent != null) {
			final Bundle extras = intent.getExtras();
			if (extras != null) {

                /* Used when browse target is a reply */
				mChildId = extras.getString(Constants.EXTRA_ENTITY_CHILD_ID);

                /* Provides message context. Could be a patch or a user */
				mForId = extras.getString(Constants.EXTRA_ENTITY_FOR_ID);
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

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mLinkProfile = LinkProfile.LINKS_FOR_MESSAGE;

		mListFragment = new MessageListFragment();

		EntityMonitor monitor = new EntityMonitor(mEntityId);
		EntitiesQuery query = new EntitiesQuery();

		query.setEntityId(mEntityId)
		     .setLinkDirection(Direction.in.name())
		     .setLinkType(Constants.TYPE_LINK_CONTENT)
		     .setPageSize(Integers.getInteger(R.integer.page_size_replies))
		     .setSchema(Constants.SCHEMA_ENTITY_MESSAGE);

		mListFragment.setQuery(query)
		             .setMonitor(monitor)
		             .setListItemResId(R.layout.temp_listitem_message)
		             .setListViewType(ViewType.LIST)
		             .setListLayoutResId(R.layout.entity_list_fragment)
		             .setListLoadingResId(R.layout.temp_list_item_loading)
		             .setHeaderViewResId(R.layout.widget_list_header_message)
		             .setFooterViewResId(R.layout.widget_list_footer_message)
		             .setBackgroundResId(R.drawable.selector_item)
		             .setReverseSort(true)
		             .setSelfBindingEnabled(false)
		             .setButtonSpecialEnabled(false);

		if (mChildId != null) {
			mHighlight = new Highlight(true);
			mListFragment.getHighlightEntities().put(mChildId, mHighlight);
		}

		getFragmentManager().beginTransaction().replace(R.id.fragment_holder, mListFragment).commit();
	}

	@Override
	public void afterDatabind(final BindingMode mode, ModelResult result) {
		super.afterDatabind(mode, result);
		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {

            /* Clear notifications and activity indicator */
			MessagingManager.getInstance().setNewActivity(false);
			MessagingManager.getInstance().clearCounts();
			if (mEntityMonitor.changed) {
				mListFragment.bind(BindingMode.MANUAL);
			}
			else {
				mListFragment.bind(mode);
			}
		}
	}

	@SuppressWarnings("ConstantConditions")
	@Override
	public void draw(View view) {
	    /*
	     * For now, we assume that the candi form isn't recycled.
		 * 
		 * We leave most of the views visible by default so they are visible in the layout editor.
		 * 
		 * - WebImageView primary image is visible by default
		 * - WebImageView child views are gone by default
		 * - Header views are visible by default
		 */
		if (view == null) {
			view = findViewById(android.R.id.content);
		}
		mFirstDraw = false;

		final AirImageView photoView = (AirImageView) findViewById(R.id.entity_photo);
		final View holderUser = findViewById(R.id.holder_user);
		final View holderPlace = findViewById(R.id.holder_place);
		final TextView description = (TextView) findViewById(R.id.description);
		final AirImageView userPhotoView = (AirImageView) findViewById(R.id.user_photo);
		final TextView userName = (TextView) findViewById(R.id.user_name);
		final TextView toName = (TextView) findViewById(R.id.to_name);
		final TextView placeName = (TextView) findViewById(R.id.place_name);
		final TextView createdDate = (TextView) findViewById(R.id.created_date);
		final FlowLayout flowLayout = (FlowLayout) findViewById(R.id.flow_recipients);
		final ViewGroup shareHolder = (ViewGroup) findViewById(R.id.share_holder);
		final ViewGroup shareFrame = (ViewGroup) findViewById(R.id.share);
		final ViewGroup toHolder = (ViewGroup) findViewById(R.id.to_holder);

		//		final ComboButton messageButton = (ComboButton) findViewById(R.id.footer_holder);
		//		if (messageButton != null && mEntity.creator != null && !TextUtils.isEmpty(mEntity.creator.name)) {
		//			messageButton.setLabel(String.format(StringManager.getString(R.string.button_reply_message), mEntity.creator.name));
		//		}
		//		UI.setVisibility(messageButton, View.VISIBLE);

		/* Delete button */

		//		UI.setVisibility(findViewById(R.id.button_delete), View.GONE);
		//		UI.setVisibility(findViewById(R.id.button_remove), View.GONE);
		//
		//		/*
		//		 * Only the message owner can delete the seed or reply message.
		//		 */
		//		if (mEntity.ownerId != null && Aircandi.getInstance().getCurrentUser() != null
		//				&& (mEntity.ownerId.equals(Aircandi.getInstance().getCurrentUser().id)
		//				|| (Aircandi.settings.getBoolean(StringManager.getString(R.string.pref_enable_dev), false)
		//						&& Aircandi.getInstance().getCurrentUser().developer != null
		//						&& Aircandi.getInstance().getCurrentUser().developer))) {
		//			UI.setVisibility(findViewById(R.id.button_delete), View.VISIBLE);
		//		}
		//		/*
		//		 * If a seed message is linked to a patch owned by the current user, they have the
		//		 * right to remove it. We do not let a seed message owner remove a reply.
		//		 */
		//		else if (mEntity.type.equals(MessageType.ROOT)) {
		//			Link placeLink = mEntity.getLink(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_PLACE, mEntity.placeId, Direction.out);
		//			if (placeLink != null && placeLink.ownerId.equals(Aircandi.getInstance().getCurrentUser().id)) {
		//				findViewById(R.id.button_remove).setTag(mEntity.placeId);
		//				UI.setVisibility(findViewById(R.id.button_remove), View.VISIBLE);
		//			}
		//		}
		//
		//		/* Edit button */
		//
		//		UI.setVisibility(findViewById(R.id.button_edit), View.GONE);
		//		if (mEntity != null && mEntity.ownerId != null && Aircandi.getInstance().getCurrentUser() != null
		//				&& (mEntity.ownerId.equals(Aircandi.getInstance().getCurrentUser().id)
		//				|| (Aircandi.settings.getBoolean(StringManager.getString(R.string.pref_enable_dev), false)
		//						&& Aircandi.getInstance().getCurrentUser().developer != null
		//						&& Aircandi.getInstance().getCurrentUser().developer))) {
		//			UI.setVisibility(findViewById(R.id.button_edit), View.VISIBLE);
		//		}

        /* Share */

		Boolean share = (mEntity.type != null && mEntity.type.equals(Constants.TYPE_LINK_SHARE));

		if (share) {

			mEntity.shareable = false;
			UI.setVisibility(findViewById(R.id.footer_holder), View.GONE);
			UI.setVisibility(findViewById(R.id.button_share), View.GONE);
			UI.setVisibility(findViewById(R.id.divider_replies), View.GONE);

			if (mEntity.ownerId.equals(Aircandi.getInstance().getCurrentUser().id)) {
				UI.setVisibility(toHolder, View.VISIBLE);

				flowLayout.setSpacingHorizontal(UI.getRawPixelsForDisplayPixels(4f));
				flowLayout.setSpacingVertical(UI.getRawPixelsForDisplayPixels(4f));
				flowLayout.setClickable(false);

            /* Check for recipients */
				List<Link> links = mEntity.getLinks(Constants.TYPE_LINK_SHARE, Constants.SCHEMA_ENTITY_USER, null, Direction.out);
				for (Link link : links) {
					mTos.add(link.shortcut.getAsEntity());
				}

				for (Entity entity : mTos) {

					EntityView entityView = new EntityView(this);
					entityView.setLayout(R.layout.widget_token_view);
					entityView.initialize();
					entityView.databind(entity);
					entityView.setClickable(false);

					FlowLayout.LayoutParams params = new FlowLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
					params.setCenterHorizontal(false);
					entityView.setLayoutParams(params);

					flowLayout.addView(entityView);
				}
			}
		}

		/* Message place context */

		UI.setVisibility(holderPlace, View.GONE);
		if (holderPlace != null) {
			if (share) {
				placeName.setText(StringManager.getString(R.string.label_message_shared));
				UI.setVisibility(holderPlace, View.VISIBLE);
				UI.setEnabled(holderPlace, false);
			}
			else {
				Link linkPlace = mEntity.getParentLink(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_PLACE);
				if (linkPlace != null) {
					holderPlace.setTag(mEntity);
					placeName.setText(linkPlace.shortcut.name);
					UI.setVisibility(holderPlace, View.VISIBLE);
				}
			}
		}

		/* Message 'to' context */

		UI.setVisibility(toName, View.GONE);
		UI.setVisibility(findViewById(R.id.symbol_at), View.GONE);

		if (mEntity.type.equals(MessageType.REPLY)) {

			if (toName != null) {

				Message message = (Message) mEntity;
				Link linkMessage = mEntity.getParentLink(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_MESSAGE);

				String toLabel;
				if (message.replyTo != null) {
					if (!mEntity.creator.name.equals(message.replyTo.name)) {
						toLabel = ((Message) mEntity).replyTo.name;
					}
					else {
						toLabel = "Added";
					}
				}
				else {
					if (mEntity.creator != null && mEntity.creator.name != null) {

						if (linkMessage != null
								&& linkMessage.shortcut.creator != null
								&& linkMessage.shortcut.creator.name != null) {

							if (!mEntity.creator.name.equals(linkMessage.shortcut.creator.name)) {
								toLabel = linkMessage.shortcut.creator.name;
							}
							else {
								toLabel = "Added";
							}
						}
						else {
							toLabel = "[Removed]";
						}
					}
					else {
						toLabel = "[Unknown]";
					}
				}

				if (linkMessage != null) {
					Entity linkEntity = linkMessage.shortcut.getAsEntity();
					toName.setTag(linkEntity);
					toName.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View view) {
							Entity entity = (Entity) view.getTag();
							Aircandi.dispatch.route(MessageForm.this, Route.BROWSE, entity, null, null);
						}
					});
				}

				if (toLabel != null) {
					toName.setText(toLabel);
					UI.setVisibility(toName, View.VISIBLE);
					UI.setVisibility(findViewById(R.id.symbol_at), View.VISIBLE);
				}
			}
		}

		/* User holder */

		if (holderUser != null && mEntity.creator != null) {
			holderUser.setTag(mEntity.creator);
		}

		/* User photo */

		UI.setVisibility(userPhotoView, View.GONE);
		if (userPhotoView != null && mEntity.creator != null) {
			Photo photo = mEntity.creator.getPhoto();
			if (userPhotoView.getPhoto() == null || !userPhotoView.getPhoto().getUri().equals(photo.getUri())) {
				UI.drawPhoto(userPhotoView, photo);
			}
			UI.setVisibility(userPhotoView, View.VISIBLE);
		}

		/* User name */

		UI.setVisibility(userName, View.GONE);
		if (userName != null && mEntity.creator != null && mEntity.creator.name != null && mEntity.creator.name.length() > 0) {
			userName.setText(mEntity.creator.name);
			UI.setVisibility(userName, View.VISIBLE);
		}

		/* Created date */

		UI.setVisibility(createdDate, View.GONE);
		if (createdDate != null && mEntity.createdDate != null) {
			createdDate.setText(DateTime.dateStringAt(mEntity.createdDate.longValue()));
			UI.setVisibility(createdDate, View.VISIBLE);
		}

		/* Message text */

		UI.setVisibility(description, View.GONE);
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
		}

        /* Shared entity */

		UI.setVisibility(shareHolder, View.GONE);
		UI.setVisibility(photoView, View.GONE);
		Entity shareEntity = null;
		if (share) {
			Link linkEntity = mEntity.getParentLink(Constants.TYPE_LINK_SHARE, Constants.SCHEMA_ENTITY_PLACE);
			if (linkEntity != null) {
				shareEntity = linkEntity.shortcut.getAsEntity();
			}
			if (shareEntity == null) {
				linkEntity = mEntity.getParentLink(Constants.TYPE_LINK_SHARE, Constants.SCHEMA_ENTITY_MESSAGE);
				if (linkEntity != null) {
					shareEntity = linkEntity.shortcut.getAsEntity();
				}
			}
		}

		if (shareEntity != null) {

            /* Message that shares an entity */

			int layoutResId = 0;
			if (shareEntity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
				layoutResId = R.layout.temp_button_share_place;
			}
			else if (shareEntity.schema.equals(Constants.SCHEMA_ENTITY_MESSAGE)) {
				layoutResId = R.layout.temp_button_share_message;
			}

			shareFrame.removeAllViews();
			View shareView = LayoutInflater.from(this).inflate(layoutResId, null, false);
			IEntityController controller = Aircandi.getInstance().getControllerForSchema(shareEntity.schema);
			controller.bind(shareEntity, shareView);
			if (shareEntity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
				shareEntity.autowatchable = true;
			}
			shareFrame.setTag(shareEntity);
			shareFrame.addView(shareView);

			UI.setVisibility(findViewById(R.id.divider_buttons), View.GONE);
			UI.setVisibility(shareHolder, View.VISIBLE);
		}
		else {

		    /* Message that includes a photo */

			UI.setVisibility(findViewById(R.id.divider_buttons), View.VISIBLE);
			if (photoView != null) {
				if (!Photo.same(photoView.getPhoto(), mEntity.getPhoto())) {
					if (mEntity.photo != null) {
						Photo photo = mEntity.getPhoto();
						photoView.setTag(photo);
						photoView.setCenterCrop(false);
						UI.drawPhoto(photoView, photo);
					}
				}
				if (mEntity.photo != null) {
					UI.setVisibility(photoView, View.VISIBLE);
					UI.setVisibility(findViewById(R.id.divider_buttons), View.GONE);
				}
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe
	@SuppressWarnings("ucd")
	public void onEntitiesLoaded(final EntitiesLoadedEvent event) {
		if (mHighlight != null && !mHighlight.hasFired()) {
			mListFragment.setListPositionToEntity(mChildId);
		}
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onMessage(final MessageEvent event) {
        /*
         * Refresh the form because something new has been added to it
		 * like a comment or post.
		 */
		if (related(event.message.action.toEntity.id)) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					mListFragment.bind(BindingMode.AUTO);
				}
			});
		}
	}

	@SuppressWarnings("ucd")
	public void onReplyClick(View view) {
		Bundle extras = new Bundle();

		String rootId = mEntity.type.equals(MessageType.ROOT) ? mEntity.id : ((Message) mEntity).rootId;

		extras.putString(Constants.EXTRA_MESSAGE_ROOT_ID, rootId);
		extras.putString(Constants.EXTRA_ENTITY_PARENT_ID, rootId);
		extras.putString(Constants.EXTRA_MESSAGE_TYPE, MessageType.REPLY);
		extras.putString(Constants.EXTRA_PLACE_ID, mEntity.placeId);

		if (mEntity.creator != null) {
			extras.putString(Constants.EXTRA_MESSAGE_REPLY_TO_ID, mEntity.creator.id);
			if (!TextUtils.isEmpty(mEntity.creator.name)) {
				extras.putString(Constants.EXTRA_MESSAGE_REPLY_TO_NAME, mEntity.creator.name);
			}
		}

		onAdd(extras);
	}

	@SuppressWarnings("ucd")
	public void onPlaceClick(View view) {
		Entity entity = (Entity) view.getTag();
		Aircandi.dispatch.route(MessageForm.this, Route.BROWSE, entity.place, null, null);
	}

	@SuppressWarnings("ucd")
	public void onEditClick(View view) {
		Bundle extras = new Bundle();
		Aircandi.dispatch.route(this, Route.EDIT, mEntity, null, extras);
	}

	@SuppressWarnings("ucd")
	public void onDeleteClick(View view) {
		Aircandi.dispatch.route(this, Route.DELETE, mEntity, null, null);
	}

	@SuppressWarnings("ucd")
	public void onRemoveClick(View view) {
		Bundle extras = new Bundle();
		extras.putString(Constants.EXTRA_ENTITY_PARENT_ID, (String) view.getTag());
		Aircandi.dispatch.route(this, Route.REMOVE, mEntity, null, extras);
	}

	@SuppressWarnings("ucd")
	public void onShareClick(View view) {
		share();
	}

	@Override
	public void onAdd(Bundle extras) {
		extras.putString(Constants.EXTRA_ENTITY_SCHEMA, Constants.SCHEMA_ENTITY_MESSAGE);
		super.onAdd(extras);
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
			if (requestCode == Constants.ACTIVITY_ENTITY_INSERT) {
				mChildId = intent.getStringExtra(Constants.EXTRA_ENTITY_CHILD_ID);
				if (mChildId != null) {
					/* If reply to reply then finish */
					if (mEntity.type != null && mEntity.type.equals(MessageType.REPLY)) {
						finish();
						Aircandi.getInstance().getAnimationManager().doOverridePendingTransition(this, TransitionType.PAGE_TO_RADAR_AFTER_DELETE);
					}
					else {
						mHighlight = new Highlight(true);
						mListFragment.getHighlightEntities().put(mChildId, mHighlight);
					}
				}
			}
			else {
				super.onActivityResult(requestCode, resultCode, intent);
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void share() {

		ShareCompat.IntentBuilder builder = ShareCompat.IntentBuilder.from(this);

		builder.setSubject(String.format(StringManager.getString(R.string.label_message_share_subject)
				, Aircandi.getInstance().getCurrentUser().name));

		builder.setType("text/plain");
		builder.setText(String.format(StringManager.getString(R.string.label_message_share_body), mEntityId));
		builder.setChooserTitle(String.format(StringManager.getString(R.string.label_message_share_title)
				, (mEntity.name != null) ? mEntity.name : StringManager.getString(R.string.container_singular_lowercase)));

		builder.getIntent().putExtra(Constants.EXTRA_SHARE_SOURCE, getPackageName());
		builder.getIntent().putExtra(Constants.EXTRA_SHARE_ID, mEntityId);
		builder.getIntent().putExtra(Constants.EXTRA_SHARE_SCHEMA, Constants.SCHEMA_ENTITY_MESSAGE);

		builder.startChooser();
	}

	@Override
	protected void configureActionBar() {
		super.configureActionBar();

		if (mActionBar != null) {
			Drawable icon = Aircandi.applicationContext.getResources().getDrawable(R.drawable.img_comment_temp);
			icon.setColorFilter(Colors.getColor(color.white), PorterDuff.Mode.SRC_ATOP);
			mActionBar.setIcon(icon);
		}
	}

	@Override
	public Boolean related(String entityId) {
		Boolean related = super.related(entityId);
		if (!related) {
			if (mEntity != null && mEntity.placeId != null && mEntity.placeId.equals(entityId)) {
				return true;
			}
		}
		return related;
	}

	@Override
	public void confirmDelete() {

		String message = String.format(StringManager.getString(R.string.alert_delete_message_message_no_name), mEntity.name);
		if (mEntity.type.equals(MessageType.ROOT)) {
			Link linkPlace = mEntity.getParentLink(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_PLACE);
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

	@Override
	protected void delete() {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mBusy.showBusy(BusyAction.ActionWithMessage, mDeleteProgressResId);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncDeleteEntity");
				String seedParentId = mEntity.type.equals(MessageType.ROOT) ? mEntity.placeId : null;
				final ModelResult result = ((EntityManager) Aircandi.getInstance().getEntityManager()).deleteMessage(mEntity.id, false, seedParentId);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				mBusy.hideBusy(true);
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					Logger.i(this, "Deleted entity: " + mEntity.id);
					/*
					 * We either go back to a list or to radar.
					 */
					UI.showToastNotification(StringManager.getString(mDeletedResId), Toast.LENGTH_SHORT);
					setResultCode(Constants.RESULT_ENTITY_DELETED);
					finish();
					Aircandi.getInstance().getAnimationManager().doOverridePendingTransition(MessageForm.this, TransitionType.FORM_TO_PAGE_AFTER_DELETE);
				}
				else {
					Errors.handleError(MessageForm.this, result.serviceResponse);
				}
			}

		}.execute();
	}

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
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Misc
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected int getLayoutId() {
		return R.layout.message_form;
	}

}