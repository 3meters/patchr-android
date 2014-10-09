package com.aircandi.controllers;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.components.EntityManager;
import com.aircandi.components.MessagingManager;
import com.aircandi.components.StringManager;
import com.aircandi.interfaces.IEntityController;
import com.aircandi.objects.Action;
import com.aircandi.objects.Count;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Link;
import com.aircandi.objects.LinkProfile;
import com.aircandi.objects.Message;
import com.aircandi.objects.NotificationType;
import com.aircandi.objects.Photo;
import com.aircandi.objects.ServiceMessage;
import com.aircandi.objects.ViewHolder;
import com.aircandi.ui.MessageForm;
import com.aircandi.ui.edit.MessageEdit;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Integers;
import com.aircandi.utilities.UI;

import java.util.Map;

public class Messages extends EntityControllerBase {

	public Messages() {
		mColorPrimary = R.color.holo_blue_dark;
		mSchema = com.aircandi.Constants.SCHEMA_ENTITY_MESSAGE;
		mBrowseClass = MessageForm.class;
		mEditClass = MessageEdit.class;
		mNewClass = MessageEdit.class;
		mPageSize = Integers.getInteger(R.integer.page_size_messages);
		mListLayoutResId = R.layout.entity_list_fragment;
		mListItemResId = R.layout.temp_listitem_message;
		mListLoadingResId = R.layout.temp_listitem_loading;
	}

	@Override
	public Entity makeNew() {
		Entity entity = new Message();
		entity.schema = mSchema;
		entity.id = "temp:" + DateTime.nowString(DateTime.DATE_NOW_FORMAT_FILENAME); // Temporary
		entity.signalFence = -100.0f;
		return entity;
	}

	public void bind(Entity entity, View view) {

        /* Configure holder if we didn't get one ready to go */
		ViewHolder holder = (ViewHolder) view.getTag();
		if (holder == null) {
			holder = new ViewHolderExtended();
			bindHolder(view, holder);
			view.setTag(holder);
		}
		holder.data = entity;

        /* Share */

		Boolean share = (entity.type != null && entity.type.equals(Constants.TYPE_LINK_SHARE));

		/* Place context */

		UI.setVisibility(holder.placeName, View.GONE);
		if (holder.placeName != null) {
			if (share) {
				holder.placeName.setText(StringManager.getString(R.string.label_message_shared));
				UI.setVisibility(holder.placeName, View.VISIBLE);
			}
			else {
				Entity parentEntity = entity.place;
				if (parentEntity == null) {
					parentEntity = EntityManager.getCacheEntity(entity.placeId);
				}
				if (parentEntity != null) {
					holder.placeName.setText(parentEntity.name);
					UI.setVisibility(holder.placeName, View.VISIBLE);
				}
			}
		}

		/* User photo */

		UI.setVisibility(holder.userPhotoView, View.GONE);
		if (holder.userPhotoView != null && entity.creator != null) {
		    /*
             * Acting a cheap proxy for user view so setting photoview to entity instead of photo.
			 */
			Photo photo = entity.creator.getPhoto();
			if (holder.userPhotoView.getPhoto() == null || !holder.userPhotoView.getPhoto().getUri().equals(photo.getUri())) {
				UI.drawPhoto(holder.userPhotoView, photo);
			}
			holder.userPhotoView.setTag(entity.creator);
			UI.setVisibility(holder.userPhotoView, View.VISIBLE);
		}

		/* User name */

		UI.setVisibility(holder.userName, View.GONE);
		if (holder.userName != null && entity.creator != null && entity.creator.name != null && entity.creator.name.length() > 0) {
			holder.userName.setText(entity.creator.name);
			UI.setVisibility(holder.userName, View.VISIBLE);
		}

		/* Message 'to' context */

		UI.setVisibility(holder.toName, View.GONE);
		UI.setVisibility(view.findViewById(R.id.symbol_at), View.GONE);

		if (entity.type != null && entity.type.equals(Message.MessageType.REPLY)) {

			if (holder.toName != null) {

				Message message = (Message) entity;
				Link linkMessage = entity.getParentLink(Constants.TYPE_LINK_CONTENT, com.aircandi.Constants.SCHEMA_ENTITY_MESSAGE);

				String toLabel;
				if (message.replyTo != null) {
					if (!entity.creator.name.equals(message.replyTo.name)) {
						toLabel = message.replyTo.name;
					}
					else {
						toLabel = "Added";
					}
				}
				else {
					if (entity.creator != null && entity.creator.name != null) {

						if (linkMessage != null
								&& linkMessage.shortcut.creator != null
								&& linkMessage.shortcut.creator.name != null) {

							if (!entity.creator.name.equals(linkMessage.shortcut.creator.name)) {
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

				if (toLabel != null) {
					holder.toName.setText(toLabel);
					UI.setVisibility(holder.toName, View.VISIBLE);
					UI.setVisibility(view.findViewById(R.id.symbol_at), View.VISIBLE);
				}
			}
		}

		/* Created date */

		UI.setVisibility(holder.createdDate, View.GONE);
		if (holder.createdDate != null && entity.createdDate != null) {
			String compactAgo = DateTime.intervalCompact(entity.createdDate.longValue(), DateTime.nowDate().getTime(), DateTime.IntervalContext.PAST);
			holder.createdDate.setText(compactAgo);
			UI.setVisibility(holder.createdDate, View.VISIBLE);
		}

		/* Description */

		UI.setVisibility(holder.description, View.GONE);
		if (holder.description != null && entity.description != null && entity.description.length() > 0) {
			holder.description.setText(entity.description);
			UI.setVisibility(holder.description, View.VISIBLE);
		}

		/* Alert indicator */

		if (holder.alert != null) {
			UI.setVisibility(holder.alert, View.INVISIBLE);
			if (entity.type.equals(Constants.TYPE_APP_ALERT)) {
				boolean notification = MessagingManager.getInstance().getAlerts().containsKey(entity.id);
				if (notification) {
					UI.setVisibility(holder.alert, View.VISIBLE);
				}
			}
			else {
				boolean notification = MessagingManager.getInstance().getMessages().containsKey(entity.id);
				if (notification) {
					UI.setVisibility(holder.alert, View.VISIBLE);
				}
			}
		}

        /* Shared entity */

		UI.setVisibility(holder.photoView, View.GONE);
		UI.setVisibility(((ViewHolderExtended) holder).childCount, View.GONE);
		UI.setVisibility(view.findViewById(R.id.share_holder), View.GONE);

		Entity shareEntity = null;

		if (share) {
			Link linkEntity = entity.getParentLink(Constants.TYPE_LINK_SHARE, Constants.SCHEMA_ENTITY_PLACE);
			if (linkEntity != null) {
				shareEntity = linkEntity.shortcut.getAsEntity();
			}
			if (shareEntity == null) {
				linkEntity = entity.getParentLink(Constants.TYPE_LINK_SHARE, com.aircandi.Constants.SCHEMA_ENTITY_MESSAGE);
				if (linkEntity != null) {
					shareEntity = linkEntity.shortcut.getAsEntity();
				}
			}
		}

		if (shareEntity != null) {

			int layoutResId = 0;
			if (shareEntity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
				layoutResId = R.layout.temp_button_share_place;
			}
			else if (shareEntity.schema.equals(com.aircandi.Constants.SCHEMA_ENTITY_MESSAGE)) {
				layoutResId = R.layout.temp_button_share_message;
			}

			holder.share.removeAllViews();
			View shareView = LayoutInflater.from(view.getContext()).inflate(layoutResId, null, false);
			IEntityController controller = Patchr.getInstance().getControllerForSchema(shareEntity.schema);
			controller.bind(shareEntity, shareView);
			if (shareEntity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
				shareEntity.autowatchable = true;
			}
			holder.share.setTag(shareEntity);
			holder.share.addView(shareView);

			UI.setVisibility(view.findViewById(R.id.share_holder), View.VISIBLE);
		}
		else {

		    /* Photo */

			if (holder.photoView != null) {
				final Photo photo = entity.getPhoto();

				if (entity.photo != null) {
					if (holder.photoView.getPhoto() == null || !photo.getUri().equals(holder.photoView.getPhoto().getUri())) {
						holder.photoView.setCenterCrop(false);
						UI.drawPhoto(holder.photoView, photo);
					}
					UI.setVisibility(holder.photoView, View.VISIBLE);
				}
			}

		    /* Info about child links */

			if (((ViewHolderExtended) holder).childCount != null) {
				Count count = entity.getCount(Constants.TYPE_LINK_CONTENT, com.aircandi.Constants.SCHEMA_ENTITY_MESSAGE, null, Link.Direction.in);
				Integer linkCount = (count != null) ? count.count.intValue() : 0;
				if (linkCount > 0) {
					((ViewHolderExtended) holder).childCount.setText(String.valueOf(linkCount) + ((linkCount == 1) ? " reply" : " replies"));
					UI.setVisibility(((ViewHolderExtended) holder).childCount, View.VISIBLE);
				}
			}
		}
	}

	@Override
	public void bindHolder(View view, ViewHolder holder) {
		((ViewHolderExtended) holder).childCount = (TextView) view.findViewById(R.id.child_count);
		super.bindHolder(view, holder);
	}

	@Override
	public Integer getLinkProfile() {
		return LinkProfile.LINKS_FOR_MESSAGE;
	}

	@Override
	public Integer getNotificationType(Entity entity) {
		if (entity.photo != null) {
			return NotificationType.BIG_PICTURE;
		}
		else {
			return NotificationType.BIG_TEXT;
		}
	}

	@Override
	public String getNotificationTicker(ServiceMessage message, String eventCategory) {
		if (eventCategory.equals(Action.EventCategory.INSERT)) {
			if (message.action.entity.photo != null && message.action.toEntity != null) {
				return String.format(StringManager.getString(R.string.label_notification_ticker_photo_insert), message.title, message.action.toEntity.name);
			}
			else if (message.action.entity.description != null) {
				return String.format(StringManager.getString(R.string.label_notification_ticker_message_insert), message.title, message.action.entity.description);
			}
		}
		else if (eventCategory.equals(Action.EventCategory.SHARE)) {
			if (message.action.entity.photo != null) {
				return String.format(StringManager.getString(R.string.label_notification_ticker_photo_share), message.title);
			}
			else {
				return String.format(StringManager.getString(R.string.label_notification_ticker_message_share), message.title);
			}
		}
		return super.getNotificationTicker(message, eventCategory);
	}

	@Override
	public Entity makeFromMap(Map<String, Object> map, Boolean nameMapping) {
		return Message.setPropertiesFromMap(new Message(), map, nameMapping);
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	public static class ViewHolderExtended extends ViewHolder {
		public TextView childCount;
	}
}
