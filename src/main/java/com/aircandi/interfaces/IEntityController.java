package com.aircandi.interfaces;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.aircandi.objects.Entity;
import com.aircandi.objects.Link.Direction;
import com.aircandi.objects.Photo;
import com.aircandi.objects.ViewHolder;

import java.util.Map;

@SuppressWarnings("ucd")
public interface IEntityController {

	public Intent view(Context context, Entity entity, String entityId, String parentId, String linkType, Bundle extras, Boolean start);

	public Intent viewFor(Context context, Entity entity, String entityId, String linkType, Direction direction, String title, Boolean newEnabled,
	                      Boolean start);

	public Intent edit(Context context, Entity entity, Bundle extras, Boolean start);

	public Intent insert(Context context, Bundle extras, Boolean start);

	public void bind(Entity entity, View view);

	public void bindHolder(View view, ViewHolder viewHolder);

	public Entity makeNew();

	public Entity makeFromMap(Map<String, Object> map, Boolean nameMapping);

	public Photo getDefaultPhoto(String type);

	public Photo getPlaceholderPhoto(String type);

	public Integer getNotificationType(Entity entity);

	public String getName(Boolean plural);

	public String getType(Entity entity, Boolean verbose);

	public Integer getColorPrimary();

	public Integer getLinkProfile();

	public IEntityController setBrowseClass(Class<?> browseClass);

	public IEntityController setEditClass(Class<?> editClass);

	public IEntityController setNewClass(Class<?> newClass);

	public IEntityController setListClass(Class<?> listClass);

}
