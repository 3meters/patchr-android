package com.aircandi.controllers;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.IntentBuilder;
import com.aircandi.objects.Count;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Link;
import com.aircandi.objects.Link.Direction;
import com.aircandi.objects.LinkProfile;
import com.aircandi.objects.Links;
import com.aircandi.objects.NotificationType;
import com.aircandi.objects.Photo;
import com.aircandi.objects.Photo.PhotoSource;
import com.aircandi.objects.Shortcut;
import com.aircandi.objects.TransitionType;
import com.aircandi.ui.EntityList;
import com.aircandi.ui.EntityListFragment.ViewType;
import com.aircandi.utilities.Integers;

public abstract class EntityControllerBase implements IEntityController {

	protected int		mColorPrimary	= R.color.holo_orange_dark;
	protected String	mSchema;

	protected Class<?>	mBrowseClass;

	protected Class<?>	mEditClass;
	protected Class<?>	mNewClass;
	protected Class<?>	mListClass		= EntityList.class;

	protected String	mListViewType	= ViewType.LIST;
	protected Integer	mPageSize		= Integers.getInteger(R.integer.page_size_entities);

	protected Integer	mListItemResId	= R.layout.temp_listitem_entity;
	protected Integer	mListLayoutResId;
	protected Integer	mListLoadingResId;
	protected Integer	mListNewMessageResId;

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

		IntentBuilder intentBuilder = new IntentBuilder(context, mBrowseClass);
		intentBuilder
				.setEntityId(entityId)
				.setEntityParentId(parentId)
				.setListLinkType(linkType)
				.setListLinkSchema(mSchema)
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
			Aircandi.getInstance().getAnimationManager().doOverridePendingTransition((Activity) context, TransitionType.PAGE_TO_PAGE);
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
			Aircandi.getInstance().getAnimationManager().doOverridePendingTransition((Activity) context, TransitionType.PAGE_TO_PAGE);
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
			Aircandi.getInstance().getAnimationManager().doOverridePendingTransition((Activity) context, TransitionType.PAGE_TO_PAGE);
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
			Aircandi.getInstance().getAnimationManager().doOverridePendingTransition((Activity) context, TransitionType.PAGE_TO_PAGE);
		}

		return intent;
	}

	@Override
	public void decorate(Entity entity, Links linkOptions) {

		List<Shortcut> shortcuts = new ArrayList<Shortcut>();

		Aircandi.getInstance().getShortcutManager().getClientShortcuts(shortcuts, entity);

		if (entity.linksIn == null) {
			entity.linksIn = new ArrayList<Link>();
		}
		if (entity.linksInCounts == null) {
			entity.linksInCounts = new ArrayList<Count>();
		}
		else if (entity.getCount(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_APPLINK, null, Direction.in) == null) {
			entity.linksInCounts.add(new Count(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_APPLINK, null, shortcuts.size()));
		}
		else {
			entity.getCount(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_APPLINK, null, Direction.in).count = entity.getCount(
					Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_APPLINK, null, Direction.in).count.intValue()
					+ shortcuts.size();
		}
		for (Shortcut shortcut : shortcuts) {
			Link link = new Link(shortcut.getId(), entity.id, Constants.TYPE_LINK_CONTENT, shortcut.schema);
			link.shortcut = shortcut;
			entity.linksIn.add(link);
		}
		entity.shortcuts = (linkOptions != null) ? linkOptions.getShortcuts() : false;
	}

	@Override
	public Drawable getIcon() {
		Drawable icon = Aircandi.applicationContext.getResources().getDrawable(R.drawable.img_logo_dark);
		//icon.setColorFilter(Colors.getColor(mColorPrimary), PorterDuff.Mode.SRC_ATOP);
		return icon;
	}

	@Override
	public Photo getDefaultPhoto(String type) {
		return EntityControllerBase.getDefaultPhoto();
	}

	@Override
	public Drawable getDefaultDrawable(String type) {
		Drawable drawable = Aircandi.applicationContext.getResources().getDrawable(R.drawable.img_placeholder_logo_bw);
		return drawable;
	}

	@Override
	public Photo getPlaceholderPhoto(String type) {
		return getDefaultPhoto(type);
	}

	@Override
	public Drawable getPlaceholderDrawable(String type) {
		return getDefaultDrawable(type);
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
	public List<Object> getApplications(String themeTone) {
		return new ArrayList<Object>();
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
		String prefix = "img_placeholder_logo_bw";
		String source = PhotoSource.resource;
		Photo photo = new Photo(prefix, null, null, null, source);
		return photo;
	}
}
