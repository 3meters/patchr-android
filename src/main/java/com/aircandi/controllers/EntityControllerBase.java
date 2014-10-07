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
import com.aircandi.Patch;
import com.aircandi.R;
import com.aircandi.ServiceConstants;
import com.aircandi.components.EntityManager;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.StringManager;
import com.aircandi.interfaces.IEntityController;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Link.Direction;
import com.aircandi.objects.LinkProfile;
import com.aircandi.objects.NotificationType;
import com.aircandi.objects.Photo;
import com.aircandi.objects.Photo.PhotoSource;
import com.aircandi.objects.Place;
import com.aircandi.objects.ServiceMessage;
import com.aircandi.objects.TransitionType;
import com.aircandi.objects.ViewHolder;
import com.aircandi.ui.EntityList;
import com.aircandi.ui.EntityListFragment.ViewType;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.ui.widgets.CandiView;
import com.aircandi.ui.widgets.ComboButton;
import com.aircandi.ui.widgets.UserView;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Integers;
import com.aircandi.utilities.UI;

public abstract class EntityControllerBase implements IEntityController {

	protected int mColorPrimary = R.color.holo_orange_dark;
	protected String mSchema;

	protected Class<?> mBrowseClass;

	protected Class<?> mEditClass;
	protected Class<?> mNewClass;
	protected Class<?> mListClass = EntityList.class;

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
			context.startActivity(intent);
			Patch.getInstance().getAnimationManager().doOverridePendingTransition((Activity) context, TransitionType.PAGE_TO_PAGE);
		}

		return intent;
	}

	/*
	 * Browse a set of child entities for a particular parent entity.
	 */
	@Override
	public Intent viewFor(Context context
			, Entity entity
			, String entityId
			, String linkType
			, Direction direction
			, String title
			, Boolean newEnabled
			, Boolean start) {

		IntentBuilder intentBuilder = new IntentBuilder(context, mListClass);
		intentBuilder
				.setEntityId(entityId)
				.setListTitle(title)
				.setListLinkType(linkType)
				.setListLinkDirection((direction != null) ? direction.name() : Direction.in.name())
				.setListLinkSchema(mSchema)
				.setListPageSize(mPageSize)
				.setListViewType(mListViewType)
				.setListItemResId(mListItemResId)
				.setLayoutResId(mListLayoutResId)
				.setListLoadingResId(mListLoadingResId)
				.setListNewMessageResId(mListNewMessageResId)
				.setListNewEnabled(newEnabled);

		Intent intent = intentBuilder.create();

		if (start) {
			context.startActivity(intentBuilder.create());
			Patch.getInstance().getAnimationManager().doOverridePendingTransition((Activity) context, TransitionType.PAGE_TO_PAGE);
		}

		return intent;
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
			Patch.getInstance().getAnimationManager().doOverridePendingTransition((Activity) context, TransitionType.PAGE_TO_PAGE);
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
			Patch.getInstance().getAnimationManager().doOverridePendingTransition((Activity) context, TransitionType.PAGE_TO_PAGE);
		}

		return intent;
	}

	public void bind(Entity entity, View view) {

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
			holder.candiView.databind(entity, new CandiView.IndicatorOptions());
			UI.setVisibility(holder.candiView, View.VISIBLE);
			return;
		}

		/* Checkbox */

		UI.setVisibility(holder.checked, View.GONE);
		if (holder.checked != null && entity.checked != null) {
			holder.checked.setChecked(entity.checked);
			holder.checked.setTag(entity);
			UI.setVisibility(holder.checked, View.VISIBLE);
		}

		/* Overflow button */

		UI.setVisibility(holder.overflow, View.GONE);
		if (holder.overflow != null) {
			holder.overflow.setTag(entity);
			UI.setVisibility(holder.overflow, View.VISIBLE);
		}

		/* Name */

		UI.setVisibility(holder.name, View.GONE);
		if (holder.name != null && entity.name != null && entity.name.length() > 0) {
			holder.name.setText(entity.name);
			UI.setVisibility(holder.name, View.VISIBLE);
		}

		/* Subtitle */

		UI.setVisibility(holder.subtitle, View.GONE);
		if (entity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
			Place place = (Place) entity;
			if (holder.subtitle != null) {
				if (place.subtitle != null) {
					holder.subtitle.setText(place.subtitle);
					UI.setVisibility(holder.subtitle, View.VISIBLE);
				}
				else {
					if (place.category != null && !TextUtils.isEmpty(place.category.name)) {
						holder.subtitle.setText(Html.fromHtml(place.category.name));
						UI.setVisibility(holder.subtitle, View.VISIBLE);
					}
				}
			}
		}
		else {
			if (holder.subtitle != null && entity.subtitle != null && !entity.subtitle.equals("")) {
				holder.subtitle.setText(entity.subtitle);
				UI.setVisibility(holder.subtitle, View.VISIBLE);
			}
		}

		/* Description */

		UI.setVisibility(holder.description, View.GONE);
		if (holder.description != null && entity.description != null && entity.description.length() > 0) {
			holder.description.setText(entity.description);
			UI.setVisibility(holder.description, View.VISIBLE);
		}

		/* Place context */

		UI.setVisibility(holder.placeName, View.GONE);
		if (holder.placeName != null) {
			Entity parentEntity = entity.place;
			if (parentEntity == null) {
				parentEntity = EntityManager.getCacheEntity(entity.placeId);
			}
			if (parentEntity != null) {
				holder.placeName.setText(parentEntity.name);
				UI.setVisibility(holder.placeName, View.VISIBLE);
			}
		}

		/* Creator */

		UI.setVisibility(holder.creator, View.GONE);
		if (holder.creator != null && entity.creator != null) {
			if (!entity.ownerId.equals(ServiceConstants.ADMIN_USER_ID)
					&& !entity.ownerId.equals(ServiceConstants.ANONYMOUS_USER_ID)) {
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

		/* Created date */

		UI.setVisibility(holder.createdDate, View.GONE);
		if (holder.createdDate != null && entity.createdDate != null) {
			String compactAgo = DateTime.dateStringAt(entity.createdDate.longValue());
			holder.createdDate.setText(compactAgo);
			UI.setVisibility(holder.createdDate, View.VISIBLE);
		}

		/* Photo */

		UI.setVisibility(holder.photoView, View.GONE);
		if (holder.photoView != null) {
			final Photo photo = entity.getPhoto();
			if (photo != null) {
				if (holder.photoView.getPhoto() == null || !photo.getUri().equals(holder.photoView.getPhoto().getUri())) {
					UI.drawPhoto(holder.photoView, photo);
				}
				UI.setVisibility(holder.photoView, View.VISIBLE);
			}
		}
	}

	public void bindHolder(View view, ViewHolder holder) {

		holder.candiView = (CandiView) view.findViewById(R.id.candi_view);
		holder.photoView = (AirImageView) view.findViewById(R.id.entity_photo);
		holder.name = (TextView) view.findViewById(R.id.name);
		holder.subtitle = (TextView) view.findViewById(R.id.subtitle);
		holder.description = (TextView) view.findViewById(R.id.description);
		holder.creator = (UserView) view.findViewById(R.id.creator);
		holder.area = (TextView) view.findViewById(R.id.area);
		holder.createdDate = (TextView) view.findViewById(R.id.created_date);
		holder.comments = (TextView) view.findViewById(R.id.comments);
		holder.checked = (CheckBox) view.findViewById(R.id.checked);
		holder.overflow = (ComboButton) view.findViewById(R.id.button_overflow);
		holder.share = (ViewGroup) view.findViewById(R.id.share);
		holder.alert = (ImageView) view.findViewById(R.id.alert_indicator);

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

		holder.userPhotoView = (AirImageView) view.findViewById(R.id.user_photo);
		holder.userName = (TextView) view.findViewById(R.id.user_name);
		holder.placeName = (TextView) view.findViewById(R.id.place_name);
		holder.toName = (TextView) view.findViewById(R.id.to_name);
	}

	@Override
	public Photo getDefaultPhoto(String type) {
		return EntityControllerBase.getDefaultPhoto();
	}

	@Override
	public Photo getPlaceholderPhoto(String type) {
		return getDefaultPhoto(type);
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
	public Integer getNotificationType(Entity entity) {
		return NotificationType.NORMAL;
	}

	@Override
	public String getNotificationTicker(ServiceMessage message, String eventCategory) {
		return StringManager.getString(R.string.label_notification_ticker);
	}

	@Override
	public abstract Entity makeNew();

	@Override
	public Integer getLinkProfile() {
		return LinkProfile.NO_LINKS;
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

	@Override
	public IEntityController setListClass(Class<?> listClass) {
		mListClass = listClass;
		return this;
	}

	public static Photo getDefaultPhoto() {
		String prefix = (Patch.themeTone.equals(Patch.ThemeTone.LIGHT)) ? "img_placeholder_bw_light" : "img_placeholder_bw_dark";
		String source = PhotoSource.resource;
		Photo photo = new Photo(prefix, null, null, null, source);
		return photo;
	}
}
