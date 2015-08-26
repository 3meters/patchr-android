package com.aircandi.controllers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.AnimationManager;
import com.aircandi.components.DataController;
import com.aircandi.components.IntentBuilder;
import com.aircandi.interfaces.IEntityController;
import com.aircandi.objects.Entity;
import com.aircandi.objects.LinkSpecType;
import com.aircandi.objects.Notification;
import com.aircandi.objects.Patch;
import com.aircandi.objects.Photo;
import com.aircandi.objects.Place;
import com.aircandi.objects.TransitionType;
import com.aircandi.objects.ViewHolder;
import com.aircandi.ui.EntityListFragment.ViewType;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.ui.widgets.CandiView;
import com.aircandi.ui.widgets.UserView;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Integers;
import com.aircandi.utilities.UI;

import java.util.Locale;

public abstract class EntityControllerBase implements IEntityController {

	protected int mColorPrimary = R.color.holo_orange_dark;
	protected String mSchema;

	protected Class<?> mBrowseClass;

	protected Class<?> mEditClass;
	protected Class<?> mNewClass;

	protected String  mListViewType = ViewType.LIST;
	protected Integer mPageSize     = Integers.getInteger(R.integer.page_size_entities);

	protected Integer mListItemResId = R.layout.temp_listitem_entity;
	protected Integer mListLayoutResId;
	protected Integer mListLoadingResId;
	protected Integer mListNewMessageResId;

	/*
	 * Browse an entity.
	 */
	@Override
	public Intent view(Context context
			, Entity entity
			, String entityId
			, String parentId
			, String linkType
			, Bundle extras
			, Boolean start) {

		return viewForm(context, entity, entityId, parentId, mSchema, mBrowseClass, linkType, extras, start);
	}

	public static Intent viewForm(Context context
			, Entity entity
			, String entityId
			, String parentId
			, String schema
			, Class<?> browseClass
			, String linkType
			, Bundle extras
			, Boolean start) {

		IntentBuilder intentBuilder = new IntentBuilder(context, browseClass);
		intentBuilder
				.setEntityId(entityId)
				.setEntityParentId(parentId)
				.setListLinkType(linkType)
				.setListLinkSchema(schema)
				.addExtras(extras);

		if (entity != null) {
			intentBuilder.setEntityType(entity.type);
			if (entityId == null) {
				intentBuilder.setEntityId(entity.id);
			}
		}

		Intent intent = intentBuilder.create();

		if (start) {
			Integer transitionType = TransitionType.FORM_TO;
			if (extras != null) {
				transitionType = extras.getInt(Constants.EXTRA_TRANSITION_TYPE, TransitionType.FORM_TO);
			}
			context.startActivity(intent);
			AnimationManager.doOverridePendingTransition((Activity) context, transitionType);
		}

		return intent;
	}

	public boolean supportsEditUi() {
		return (mEditClass != null);
	}

	public boolean supportsInsertUi() {
		return (mNewClass != null);
	}

	public boolean supportsNew() {
		return true;
	}

	@Override
	public Intent edit(Context context, Entity entity, Bundle extras, Boolean start) {

		IntentBuilder intentBuilder = new IntentBuilder(context, mEditClass);
		intentBuilder
				.setEntity(entity)
				.addExtras(extras);
		Intent intent = intentBuilder.create();

		if (start) {
			((Activity) context).startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_ENTITY_EDIT);
			AnimationManager.doOverridePendingTransition((Activity) context, TransitionType.FORM_TO);
		}

		return intent;
	}

	@Override
	public Intent insert(Context context, Bundle extras, Boolean start) {

		IntentBuilder intentBuilder = new IntentBuilder(context, mNewClass);
		intentBuilder
				.setEntitySchema(mSchema)
				.addExtras(extras);
		Intent intent = intentBuilder.create();

		if (start) {
			((Activity) context).startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_ENTITY_INSERT);
			AnimationManager.doOverridePendingTransition((Activity) context, TransitionType.FORM_TO);
		}

		return intent;
	}

	public void bind(Entity entity, View view, String groupTag) {

        /* Configure holder if we didn't get one ready to go */
		ViewHolder holder = (ViewHolder) view.getTag();
		if (holder == null) {
			holder = new ViewHolder();
			bindHolder(view, holder);
			view.setTag(holder);
		}
		holder.data = entity;

		/* Candi View */

		UI.setVisibility(holder.candiView, View.GONE);
		if (holder.candiView != null) {
			holder.candiView.databind(entity, new CandiView.IndicatorOptions(), groupTag);
			UI.setVisibility(holder.candiView, View.VISIBLE);
			return;
		}

		/* Checkbox */

		UI.setVisibility(holder.checked, View.GONE);
		if (holder.checked != null) {
			holder.checked.setChecked(entity.checked);
			holder.checked.setTag(entity);
			UI.setVisibility(holder.checked, View.VISIBLE);
		}

		/* Index */
		UI.setVisibility(holder.index, View.GONE);
		if (holder.index != null && entity.index != null) {
			holder.index.setText(String.valueOf(entity.index.intValue()));
			UI.setVisibility(holder.index, View.VISIBLE);
		}

		/* Name */

		if (holder.name != null && entity.name != null && entity.name.length() > 0) {
			holder.name.setText(entity.name);
			UI.setVisibility(holder.name, View.VISIBLE);
		}
		else {
			UI.setVisibility(holder.name, View.GONE);
		}

		/* Subhead */

		UI.setVisibility(holder.subhead, View.GONE);
		if (holder.subhead != null && !TextUtils.isEmpty(entity.subtitle)) {
			holder.subhead.setText(Html.fromHtml(entity.subtitle));
			UI.setVisibility(holder.subhead, View.VISIBLE);
		}

		/* Category */

		UI.setVisibility(holder.categoryName, View.GONE);
		if (entity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
			Place place = (Place) entity;
			if (holder.categoryName != null) {
				if (place.category != null && !TextUtils.isEmpty(place.category.name)) {
					holder.categoryName.setText((place.category.name).toUpperCase(Locale.US));
					UI.setVisibility(holder.categoryName, View.VISIBLE);
				}
			}
		}

		/* Type */

		UI.setVisibility(holder.type, View.GONE);
		if (entity.schema.equals(Constants.SCHEMA_ENTITY_PATCH)) {
			@SuppressWarnings("ConstantConditions") Patch patch = (Patch) entity;
			if (holder.type != null) {
				if (patch.type != null && !TextUtils.isEmpty(patch.type)) {
					holder.type.setText((patch.type + " patch").toUpperCase(Locale.US));
					UI.setVisibility(holder.type, View.VISIBLE);
				}
			}
		}

		/* Description */

		UI.setVisibility(holder.description, View.GONE);
		if (holder.description != null && entity.description != null && entity.description.length() > 0) {
			holder.description.setText(entity.description);
			UI.setVisibility(holder.description, View.VISIBLE);
		}

		/* Parent patch photo (if one) */

		Entity parentEntity = null;
		if (entity instanceof Notification) {
			if (((Notification) entity).parentId != null) {
				parentEntity = DataController.getStoreEntity(((Notification) entity).parentId);
			}
		}
		else {
			parentEntity = entity.patch;
			if (parentEntity == null && entity.patchId != null) {
				parentEntity = DataController.getStoreEntity(entity.patchId);
			}
		}

		UI.setVisibility(holder.patchPhotoView, View.GONE);
		if (holder.patchPhotoView != null && parentEntity != null) {
			Photo photo = parentEntity.photo;
			if (photo == null) {
				photo = Entity.getDefaultPhoto(Constants.SCHEMA_ENTITY_PATCH);
			}
			if (holder.patchPhotoView.getPhoto() == null || !holder.patchPhotoView.getPhoto().getUri().equals(photo.getUri())) {
				holder.patchPhotoView.setTag(parentEntity);
				UI.drawPhoto(holder.patchPhotoView, photo);
			}
			UI.setVisibility(holder.patchPhotoView, View.VISIBLE);
		}

		/* Patch name */

		UI.setVisibility(holder.patchName, View.GONE);
		if (holder.patchName != null && parentEntity != null) {
			holder.patchName.setText(parentEntity.name);
			UI.setVisibility(holder.patchName, View.VISIBLE);
		}

		/* Creator */

		UI.setVisibility(holder.creator, View.GONE);
		if (holder.creator != null && entity.creator != null) {
			if (!entity.ownerId.equals(Constants.ADMIN_USER_ID)
					&& !entity.ownerId.equals(Constants.ANONYMOUS_USER_ID)) {
				holder.creator.databind(entity.creator, entity.modifiedDate.longValue());
				UI.setVisibility(holder.creator, View.VISIBLE);
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
				holder.userPhotoView.setTag(entity.creator);
				UI.drawPhoto(holder.userPhotoView, photo);
			}
			UI.setVisibility(holder.userPhotoView, View.VISIBLE);
		}

		/* User name */

		UI.setVisibility(holder.userName, View.GONE);
		if (holder.userName != null && entity.creator != null && entity.creator.name != null && entity.creator.name.length() > 0) {
			holder.userName.setText(entity.creator.name);
			UI.setVisibility(holder.userName, View.VISIBLE);
		}

		/* Modified date */

		UI.setVisibility(holder.modifiedDate, View.GONE);
		if (holder.modifiedDate != null && entity.modifiedDate != null) {
			String compactAgo = DateTime.dateStringAt(entity.modifiedDate.longValue());
			holder.modifiedDate.setText(compactAgo);
			UI.setVisibility(holder.modifiedDate, View.VISIBLE);
		}

		/* Created date */

		UI.setVisibility(holder.createdDate, View.GONE);
		if (holder.createdDate != null && entity.createdDate != null) {
			String compactAgo = DateTime.dateStringAt(entity.createdDate.longValue());
			holder.createdDate.setText(compactAgo);
			UI.setVisibility(holder.createdDate, View.VISIBLE);
		}

		/* Photo */

		if (holder.photoView != null) {
			final Photo photo = entity.getPhoto();
			if (holder.photoView.getPhoto() == null || !photo.getUri().equals(holder.photoView.getPhoto().getUri())) {
				UI.drawPhoto(holder.photoView, photo);
			}
			UI.setVisibility(holder.photoView, View.VISIBLE);
		}
	}

	public void bindHolder(View view, ViewHolder holder) {

		holder.candiView = (CandiView) view.findViewById(R.id.candi_view);
		holder.photoView = (AirImageView) view.findViewById(R.id.photo);
		holder.name = (TextView) view.findViewById(R.id.name);
		holder.subhead = (TextView) view.findViewById(R.id.subhead);
		holder.summary = (TextView) view.findViewById(R.id.summary);
		holder.description = (TextView) view.findViewById(R.id.description);
		holder.creator = (UserView) view.findViewById(R.id.creator);
		holder.area = (TextView) view.findViewById(R.id.area);
		holder.createdDate = (TextView) view.findViewById(R.id.created_date);
		holder.modifiedDate = (TextView) view.findViewById(R.id.modified_date);
		holder.comments = (TextView) view.findViewById(R.id.comments);
		holder.share = (ViewGroup) view.findViewById(R.id.share_entity);
		holder.alert = (ImageView) view.findViewById(R.id.alert_indicator);
		holder.photoViewBig = (AirImageView) view.findViewById(R.id.photo_big);
		holder.photoType = (ImageView) view.findViewById(R.id.photo_type);

		if (holder.checked != null) {
			holder.checked.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View view) {
					final CheckBox checkBox = (CheckBox) view;
					final Entity entity = (Entity) checkBox.getTag();
					entity.checked = checkBox.isChecked();
				}
			});
		}
		holder.index = (TextView) view.findViewById(R.id.index);
		holder.userPhotoView = (AirImageView) view.findViewById(R.id.user_photo);
		holder.userName = (TextView) view.findViewById(R.id.user_name);
		holder.patchPhotoView = (AirImageView) view.findViewById(R.id.patch_photo);
		holder.patchName = (TextView) view.findViewById(R.id.patch_name);
		holder.categoryName = (TextView) view.findViewById(R.id.category_name);
		holder.type = (TextView) view.findViewById(R.id.type);
	}

	@Override
	public String getName(Boolean plural) {
		return (plural ? mSchema + "s" : mSchema);
	}

	@Override
	public String getType(Entity entity, Boolean verbose) {
		return null;
	}

	@Override
	public Entity makeNew() {
		return null;
	}

	@Override
	public Integer getLinkProfile() {
		return LinkSpecType.NO_LINKS;
	}

	@Override
	public Integer getColorPrimary() {
		return mColorPrimary;
	}

	@Override
	public IEntityController setBrowseClass(Class<?> browseClass) {
		mBrowseClass = browseClass;
		return this;
	}

	@Override
	public IEntityController setEditClass(Class<?> editClass) {
		mEditClass = editClass;
		return this;
	}

	@Override
	public IEntityController setNewClass(Class<?> newClass) {
		mNewClass = newClass;
		return this;
	}
}
