package com.aircandi.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ShareCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.components.EntityManager;
import com.aircandi.components.Logger;
import com.aircandi.components.ModelResult;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.StringManager;
import com.aircandi.events.EntitiesLoadedEvent;
import com.aircandi.events.NotificationEvent;
import com.aircandi.events.ProcessingFinishedEvent;
import com.aircandi.interfaces.IBusy.BusyAction;
import com.aircandi.interfaces.IEntityController;
import com.aircandi.monitors.EntityMonitor;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Link;
import com.aircandi.objects.Link.Direction;
import com.aircandi.objects.LinkProfile;
import com.aircandi.objects.Message;
import com.aircandi.objects.Message.MessageType;
import com.aircandi.objects.Photo;
import com.aircandi.objects.Route;
import com.aircandi.objects.TransitionType;
import com.aircandi.queries.EntitiesQuery;
import com.aircandi.ui.EntityListFragment.Highlight;
import com.aircandi.ui.EntityListFragment.ViewType;
import com.aircandi.ui.base.BaseEntityForm;
import com.aircandi.ui.base.BaseFragment;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.ui.widgets.EntityView;
import com.aircandi.ui.widgets.FlowLayout;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.Integers;
import com.aircandi.utilities.UI;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.List;

public class MessageForm extends BaseEntityForm {

	private String    mChildId;
	private Highlight mHighlight;
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

		mEmptyView.setEnabled(false);
		mLinkProfile = LinkProfile.LINKS_FOR_MESSAGE;

		mCurrentFragment = new MessageListFragment();

		EntityMonitor monitor = new EntityMonitor(mEntityId);
		EntitiesQuery query = new EntitiesQuery();

		query.setEntityId(mEntityId)
		     .setLinkDirection(Direction.in.name())
		     .setLinkType(Constants.TYPE_LINK_CONTENT)
		     .setPageSize(Integers.getInteger(R.integer.page_size_replies))
		     .setSchema(Constants.SCHEMA_ENTITY_MESSAGE);

		((EntityListFragment) mCurrentFragment)
				.setMonitor(monitor)
				.setQuery(query)
				.setHeaderViewResId(R.layout.widget_list_header_message)
				.setFooterViewResId(R.layout.widget_list_footer_message)
				.setListItemResId(R.layout.temp_listitem_message)
				.setListLayoutResId(R.layout.entity_list_fragment)
				.setListLoadingResId(R.layout.temp_listitem_loading)
				.setListViewType(ViewType.LIST)
				.setBackgroundResId(R.drawable.selector_item)
				.setReverseSort(true)
				.setSelfBindingEnabled(false);

		if (mChildId != null) {
			mHighlight = new Highlight(true);
			((MessageListFragment) mCurrentFragment).getHighlightEntities().put(mChildId, mHighlight);
		}

		getFragmentManager()
				.beginTransaction()
				.replace(R.id.fragment_holder, mCurrentFragment)
				.commit();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe
	@SuppressWarnings("ucd")
	public void onEntitiesLoaded(final EntitiesLoadedEvent event) {
		if (mHighlight != null && !mHighlight.hasFired()) {
			((EntityListFragment) mCurrentFragment).setListPositionToEntity(mChildId);
		}
	}

	@Subscribe
	public void onProcessingFinished(ProcessingFinishedEvent event) {

		runOnUiThread(new Runnable() {
			@Override
			public void run() {

				mBusy.hideBusy(false);
				((BaseFragment) mCurrentFragment).onProcessingFinished();

				mEmptyView.fadeOut();
				Boolean share = (mEntity != null && mEntity.type != null && mEntity.type.equals(Constants.TYPE_LINK_SHARE));
				if (share) {
					mFab.fadeOut();
				}
				else {
					mFab.fadeIn();
				}
			}
		});
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onMessage(final NotificationEvent event) {
	    /*
	     * Refresh the form because something new has been added to it
		 * like a comment or post.
		 */
		if (related(event.notification.parentId)) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					((EntityListFragment) mCurrentFragment).bind(BindingMode.AUTO);
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
		extras.putString(Constants.EXTRA_PATCH_ID, mEntity.patchId);

		if (mEntity.creator != null) {
			extras.putString(Constants.EXTRA_MESSAGE_REPLY_TO_ID, mEntity.creator.id);
			if (!TextUtils.isEmpty(mEntity.creator.name)) {
				extras.putString(Constants.EXTRA_MESSAGE_REPLY_TO_NAME, mEntity.creator.name);
			}
		}

		onAdd(extras);
	}

	@SuppressWarnings("ucd")
	public void onPatchClick(View view) {
		Entity entity = (Entity) view.getTag();
		Patchr.dispatch.route(MessageForm.this, Route.BROWSE, entity.patch, null);
	}

	@SuppressWarnings("ucd")
	public void onEditClick(View view) {
		Bundle extras = new Bundle();
		Patchr.dispatch.route(this, Route.EDIT, mEntity, extras);
	}

	@SuppressWarnings("ucd")
	public void onDeleteClick(View view) {
		Patchr.dispatch.route(this, Route.DELETE, mEntity, null);
	}

	@SuppressWarnings("ucd")
	public void onRemoveClick(View view) {
		Bundle extras = new Bundle();
		extras.putString(Constants.EXTRA_ENTITY_PARENT_ID, (String) view.getTag());
		Patchr.dispatch.route(this, Route.REMOVE, mEntity, extras);
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
		if (resultCode != Activity.RESULT_CANCELED || Patchr.resultCode != Activity.RESULT_CANCELED) {
			if (requestCode == Constants.ACTIVITY_ENTITY_INSERT) {
				if (intent != null) {
					mChildId = intent.getStringExtra(Constants.EXTRA_ENTITY_CHILD_ID);
					if (mChildId != null) {
					/* If reply to reply then finish */
						if (mEntity.type != null && mEntity.type.equals(MessageType.REPLY)) {
							finish();
							Patchr.getInstance().getAnimationManager().doOverridePendingTransition(this, TransitionType.PAGE_TO_RADAR_AFTER_DELETE);
						}
						else {
							mHighlight = new Highlight(true);
							((MessageListFragment) mCurrentFragment).getHighlightEntities().put(mChildId, mHighlight);
						}
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

		final AirImageView photoView = (AirImageView) view.findViewById(R.id.photo);
		final View holderUser = view.findViewById(R.id.holder_user);
		final View holderPatch = view.findViewById(R.id.holder_patch);
		final TextView description = (TextView) view.findViewById(R.id.description);
		final AirImageView patchPhotoView = (AirImageView) view.findViewById(R.id.patch_photo);
		final TextView patchName = (TextView) view.findViewById(R.id.patch_name);
		final AirImageView userPhotoView = (AirImageView) view.findViewById(R.id.user_photo);
		final TextView userName = (TextView) view.findViewById(R.id.user_name);
		final TextView toName = (TextView) view.findViewById(R.id.to_name);
		final TextView createdDate = (TextView) view.findViewById(R.id.created_date);
		final FlowLayout flowLayout = (FlowLayout) view.findViewById(R.id.flow_recipients);
		final ViewGroup shareHolder = (ViewGroup) view.findViewById(R.id.share_holder);
		final ViewGroup shareFrame = (ViewGroup) view.findViewById(R.id.share_entity);
		final ViewGroup toHolder = (ViewGroup) view.findViewById(R.id.to_holder);

        /* Share */

		Boolean share = (mEntity.type != null && mEntity.type.equals(Constants.TYPE_LINK_SHARE));

		if (share) {

			UI.setVisibility(findViewById(R.id.divider_replies), View.GONE);

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

		/* Message patch context */

		UI.setVisibility(holderPatch, View.GONE);
		if (holderPatch != null) {
			if (share) {
				patchName.setText(StringManager.getString(R.string.label_message_shared));
				UI.setVisibility(holderPatch, View.VISIBLE);
				UI.setEnabled(holderPatch, false);
			}
			else {
				Link linkPlace = mEntity.getParentLink(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_PATCH);
				if (linkPlace != null) {
					holderPatch.setTag(mEntity);

					/* Name */
					patchName.setText(linkPlace.shortcut.name);
					UI.setVisibility(holderPatch, View.VISIBLE);

					/* Photo */
					Photo photo = linkPlace.shortcut.photo;
					if (photo == null) {
						photo = Entity.getDefaultPhoto(Constants.SCHEMA_ENTITY_PATCH);
					}
					if (patchPhotoView.getPhoto() == null || !patchPhotoView.getPhoto().getUri().equals(photo.getUri())) {
						UI.drawPhoto(patchPhotoView, photo);
					}
					UI.setVisibility(patchPhotoView, View.VISIBLE);
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
								&& linkMessage.shortcut != null
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

				if (linkMessage != null && linkMessage.shortcut != null) {
					Entity linkEntity = linkMessage.shortcut.getAsEntity();
					toName.setTag(linkEntity);
					toName.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View view) {
							Entity entity = (Entity) view.getTag();
							Patchr.dispatch.route(MessageForm.this, Route.BROWSE, entity, null);
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
		Link linkEntity = null;
		if (share) {
			linkEntity = mEntity.getParentLink(Constants.TYPE_LINK_SHARE, Constants.SCHEMA_ENTITY_PATCH);
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
		}

		if (shareEntity != null) {

            /* Message that shares an entity */

			int layoutResId = 0;
			if (shareEntity.schema.equals(Constants.SCHEMA_ENTITY_PATCH)) {
				layoutResId = R.layout.temp_button_share_patch;
			}
			else if (shareEntity.schema.equals(Constants.SCHEMA_ENTITY_MESSAGE)) {
				layoutResId = R.layout.temp_button_share_message;
			}

			shareFrame.removeAllViews();
			View shareView = LayoutInflater.from(this).inflate(layoutResId, null, false);
			IEntityController controller = Patchr.getInstance().getControllerForSchema(shareEntity.schema);
			controller.bind(shareEntity, shareView, null);
			if (shareEntity.schema.equals(Constants.SCHEMA_ENTITY_PATCH)) {
				shareEntity.autowatchable = true;
			}
			shareFrame.setTag(shareEntity);
			shareFrame.addView(shareView);

			UI.setVisibility(shareHolder, View.VISIBLE);
		}
		else if (shareEntity == null && linkEntity != null) {

			/* Message that shares an entity but shortcut was blocked */

			if (linkEntity.targetSchema.equals(Constants.SCHEMA_ENTITY_MESSAGE)) {

				shareFrame.removeAllViews();
				View shareView = LayoutInflater.from(this).inflate(R.layout.temp_button_share_message_blocked, null, false);

				Entity entity = new Message();
				entity.schema = Constants.SCHEMA_ENTITY_MESSAGE;
				entity.id = linkEntity.toId;

				shareFrame.setTag(entity);
				shareFrame.addView(shareView);

				UI.setVisibility(shareHolder, View.VISIBLE);
			}
		}
		else {

		    /* Message that includes a photo */

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
				}
			}
		}
	}

	public void configureActionBar() {
		super.configureActionBar();
		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayShowTitleEnabled(false);  // Dont show title
		}
	}

	@Override
	public void afterDatabind(final BindingMode mode, ModelResult result) {
		super.afterDatabind(mode, result);
		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			if (mEntityMonitor.changed) {
				((EntityListFragment) mCurrentFragment).bind(BindingMode.MANUAL);
			}
			else {
				((EntityListFragment) mCurrentFragment).bind(mode);
			}
		}
	}

	@Override
	public void share() {

		ShareCompat.IntentBuilder builder = ShareCompat.IntentBuilder.from(this);

		builder.setChooserTitle(String.format(StringManager.getString(R.string.label_message_share_title)
				, (mEntity.name != null) ? mEntity.name : StringManager.getString(R.string.container_singular_lowercase)));
		builder.setType("text/plain");
		builder.setSubject(String.format(StringManager.getString(R.string.label_message_share_subject), Patchr.getInstance().getCurrentUser().name));
		builder.setText(String.format(StringManager.getString(R.string.label_message_share_body), mEntityId));

		builder.getIntent().putExtra(Constants.EXTRA_SHARE_SOURCE, getPackageName());
		builder.getIntent().putExtra(Constants.EXTRA_SHARE_ID, mEntityId);
		builder.getIntent().putExtra(Constants.EXTRA_SHARE_SCHEMA, Constants.SCHEMA_ENTITY_MESSAGE);

		builder.startChooser();
	}

	@Override
	public Boolean related(String entityId) {
		Boolean related = super.related(entityId);
		if (!related) {
			if (mEntity != null && mEntity.patchId != null && mEntity.patchId.equals(entityId)) {
				return true;
			}
		}
		return related;
	}

	@Override
	public void confirmDelete() {

		String message = String.format(StringManager.getString(R.string.alert_delete_message_message_no_name), mEntity.name);
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
				String seedParentId = mEntity.type.equals(MessageType.ROOT) ? mEntity.patchId : null;
				final ModelResult result = ((EntityManager) Patchr.getInstance().getEntityManager()).deleteMessage(mEntity.id, false, seedParentId);
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
					Patchr.getInstance().getAnimationManager().doOverridePendingTransition(MessageForm.this, TransitionType.FORM_TO_PAGE_AFTER_DELETE);
				}
				else {
					Errors.handleError(MessageForm.this, result.serviceResponse);
				}
			}
		}.execute();
	}

	@Override
	protected int getLayoutId() {
		return R.layout.message_form;
	}

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/
}