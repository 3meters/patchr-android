package com.aircandi.catalina.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.aircandi.Aircandi;
import com.aircandi.catalina.Catalina;
import com.aircandi.catalina.Constants;
import com.aircandi.catalina.R;
import com.aircandi.catalina.objects.LinkProfile;
import com.aircandi.catalina.objects.Message;
import com.aircandi.catalina.ui.MessageForm;
import com.aircandi.catalina.ui.edit.MessageEdit;
import com.aircandi.components.AirApplication;
import com.aircandi.components.EntityManager;
import com.aircandi.components.StringManager;
import com.aircandi.controllers.EntityControllerBase;
import com.aircandi.controllers.IEntityController;
import com.aircandi.controllers.ViewHolder;
import com.aircandi.objects.Count;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Link;
import com.aircandi.objects.NotificationType;
import com.aircandi.objects.Photo;
import com.aircandi.utilities.Colors;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Integers;
import com.aircandi.utilities.UI;

public class Messages extends EntityControllerBase {

	public Messages() {
		mColorPrimary = R.color.holo_blue_dark;
		mSchema = Constants.SCHEMA_ENTITY_MESSAGE;
		mBrowseClass = MessageForm.class;
		mEditClass = MessageEdit.class;
		mNewClass = MessageEdit.class;
		mPageSize = Integers.getInteger(R.integer.page_size_messages);
		mListLayoutResId = R.layout.entity_list_fragment;
		mListItemResId = R.layout.temp_listitem_message;
		mListLoadingResId = R.layout.temp_list_item_loading;
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
				Link linkMessage = entity.getParentLink(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_MESSAGE);

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
				linkEntity = entity.getParentLink(Constants.TYPE_LINK_SHARE, Constants.SCHEMA_ENTITY_MESSAGE);
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
			else if (shareEntity.schema.equals(Constants.SCHEMA_ENTITY_MESSAGE)) {
				layoutResId = R.layout.temp_button_share_message;
			}

			holder.share.removeAllViews();
			View shareView = LayoutInflater.from(view.getContext()).inflate(layoutResId, null, false);
			IEntityController controller = Aircandi.getInstance().getControllerForSchema(shareEntity.schema);
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
				Count count = entity.getCount(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_MESSAGE, null, Link.Direction.in);
				Integer linkCount = (count != null) ? count.count.intValue() : 0;
				if (linkCount != null && linkCount > 0) {
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
	public Drawable getIcon() {
		Drawable icon = Catalina.applicationContext.getResources().getDrawable(R.drawable.img_comment_temp);
		icon.setColorFilter(Colors.getColor(mColorPrimary), PorterDuff.Mode.SRC_ATOP);
		return icon;
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
	public List<Object> getApplications(String themeTone) {

		final List<Object> listData = new ArrayList<Object>();

		listData.add(new AirApplication(themeTone.equals("light") ? R.drawable.ic_action_picture_light : R.drawable.ic_action_picture_dark
				, StringManager.getString(R.string.dialog_application_picture_new), null, Constants.SCHEMA_ENTITY_PICTURE));

		listData.add(new AirApplication(themeTone.equals("light") ? R.drawable.ic_action_monolog_light : R.drawable.ic_action_monolog_dark
				, StringManager.getString(R.string.dialog_application_comment_new), null, Constants.SCHEMA_ENTITY_COMMENT));

		return listData;
	}

	@Override
	public Entity makeFromMap(Map<String, Object> map, Boolean nameMapping) {
		return Message.setPropertiesFromMap(new Message(), map, nameMapping);
	}

	// --------------------------------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------------------------------

	public static class ViewHolderExtended extends ViewHolder {
		public TextView childCount;
	}

}
