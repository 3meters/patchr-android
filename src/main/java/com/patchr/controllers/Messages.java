package com.patchr.controllers;

import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.DataController;
import com.patchr.components.StringManager;
import com.patchr.interfaces.IEntityController;
import com.patchr.objects.Count;
import com.patchr.objects.Entity;
import com.patchr.objects.Link;
import com.patchr.objects.LinkSpecType;
import com.patchr.objects.Message;
import com.patchr.objects.Photo;
import com.patchr.objects.ViewHolder;
import com.patchr.ui.MessageForm;
import com.patchr.ui.edit.MessageEdit;
import com.patchr.utilities.DateTime;
import com.patchr.utilities.Integers;
import com.patchr.utilities.UI;

import java.util.Map;

public class Messages extends EntityControllerBase {

	public Messages() {
		mColorPrimary = R.color.brand_accent;
		mSchema = Constants.SCHEMA_ENTITY_MESSAGE;
		mBrowseClass = MessageForm.class;
		mEditClass = MessageEdit.class;
		mNewClass = MessageEdit.class;
		mPageSize = Integers.getInteger(R.integer.page_size_messages);
		mListLayoutResId = R.layout.entity_list_fragment;
		mListItemResId = R.layout.temp_listitem_message;
		mListLoadingResId = R.layout.temp_listitem_loading;
	}

	@NonNull
	@Override
	public Entity makeNew() {
		Entity entity = new Message();
		entity.schema = mSchema;
		entity.id = "temp:" + DateTime.nowString(DateTime.DATE_NOW_FORMAT_FILENAME); // Temporary
		return entity;
	}

	public void bind(Entity entity, View view, String groupTag) {

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

		/* Patch context */

		UI.setVisibility(holder.patchName, View.GONE);
		if (holder.patchName != null) {
			if (share) {
				holder.patchName.setText(StringManager.getString(R.string.label_message_shared));
				UI.setVisibility(holder.patchName, View.VISIBLE);
			}
			else {
				Entity parentEntity = entity.patch;
				if (parentEntity == null) {
					if (entity.patchId != null) {
						parentEntity = DataController.getStoreEntity(entity.patchId);
					}
				}
				if (parentEntity != null) {
					holder.patchName.setText(parentEntity.name);
					UI.setVisibility(holder.patchName, View.VISIBLE);
				}
			}
		}

		/* User photo */

		UI.setVisibility(holder.userPhotoView, View.GONE);
		if (holder.userPhotoView != null && entity.creator != null) {
			holder.userPhotoView.databind(entity.creator);
			holder.userPhotoView.setGroupTag(groupTag);
			holder.userPhotoView.setTag(entity.creator);
			UI.setVisibility(holder.userPhotoView, View.VISIBLE);
		}

		/* User name */

		UI.setVisibility(holder.userName, View.GONE);
		if (holder.userName != null && entity.creator != null && entity.creator.name != null && entity.creator.name.length() > 0) {
			holder.userName.setText(entity.creator.name);
			UI.setVisibility(holder.userName, View.VISIBLE);
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
		UI.setVisibility(((ViewHolderExtended) holder).shareHolder, View.GONE);
		UI.setVisibility(((ViewHolderExtended) holder).buttonLikes, View.GONE);

		Entity shareEntity = null;

		if (share) {
			Link linkEntity = entity.getParentLink(Constants.TYPE_LINK_SHARE, Constants.SCHEMA_ENTITY_PATCH);
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
			if (shareEntity.schema.equals(Constants.SCHEMA_ENTITY_PATCH)) {
				layoutResId = R.layout.temp_button_share_patch;
			}
			else if (shareEntity.schema.equals(Constants.SCHEMA_ENTITY_MESSAGE)) {
				layoutResId = R.layout.temp_button_share_message;
			}

			holder.share.removeAllViews();
			View shareView = LayoutInflater.from(view.getContext()).inflate(layoutResId, null, false);
			IEntityController controller = Patchr.getInstance().getControllerForSchema(shareEntity.schema);
			controller.bind(shareEntity, shareView, groupTag);
			holder.share.setTag(shareEntity);
			holder.share.addView(shareView);

			UI.setVisibility(((ViewHolderExtended) holder).shareHolder, View.VISIBLE);
		}
		else {

		    /* Photo */

			if (holder.photoView != null) {
				if (entity.photo != null) {
					final Photo photo = entity.getPhoto();
					if (holder.photoView.getPhoto() == null || !photo.getDirectUri().equals(holder.photoView.getPhoto().getDirectUri())) {
						holder.photoView.setTag(photo);
						holder.photoView.setGroupTag(groupTag);
						UI.drawPhoto(holder.photoView, photo);
					}
					UI.setVisibility(holder.photoView, View.VISIBLE);
				}
			}

		    /* Likes */

			if (((ViewHolderExtended) holder).buttonLikes != null) {
				Count count = entity.getCount(Constants.TYPE_LINK_LIKE, null, null, Link.Direction.in);
				if (count == null) {
					count = new Count(Constants.TYPE_LINK_LIKE, Constants.SCHEMA_ENTITY_PATCH, null, 0);
				}
				if (count.count.intValue() > 0) {
					if (((ViewHolderExtended) holder).likesCount != null) {
						String label = view.getResources().getQuantityString(R.plurals.label_likes, count.count.intValue(), count.count.intValue());
						((ViewHolderExtended) holder).likesCount.setText(String.valueOf(count.count.intValue()));
						((ViewHolderExtended) holder).likesLabel.setText(label);
						UI.setVisibility(((ViewHolderExtended) holder).buttonLikes, View.VISIBLE);
					}
				}
			}
		}
	}

	@Override
	public void bindHolder(View view, ViewHolder holder) {
		((ViewHolderExtended) holder).likesCount = (TextView) view.findViewById(R.id.likes_count);
		((ViewHolderExtended) holder).likesLabel = (TextView) view.findViewById(R.id.likes_label);
		((ViewHolderExtended) holder).buttonLikes = view.findViewById(R.id.button_message_likes);
		((ViewHolderExtended) holder).shareHolder = view.findViewById(R.id.share_holder);
		super.bindHolder(view, holder);
	}

	@Override
	public Integer getLinkProfile() {
		return LinkSpecType.LINKS_FOR_MESSAGE;
	}

	@Override
	public Entity makeFromMap(Map<String, Object> map, Boolean nameMapping) {
		return Message.setPropertiesFromMap(new Message(), map, nameMapping);
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	public static class ViewHolderExtended extends ViewHolder {
		public TextView likesCount;
		public TextView likesLabel;
		public View     buttonLikes;
		public View     shareHolder;
	}
}
