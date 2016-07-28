package com.patchr.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.patchr.Constants;
import com.patchr.model.RealmEntity;

import java.lang.reflect.Type;

public class EntitySerializer implements JsonSerializer<RealmEntity> {
	@Override
	public JsonElement serialize(RealmEntity entity, Type typeOfSrc, JsonSerializationContext context) {

		final JsonObject jsonEntity = new JsonObject();

		if (entity.id != null) {
			jsonEntity.addProperty("_id", entity.id);
		}
		jsonEntity.addProperty("type", entity.type);
		jsonEntity.addProperty("schema", entity.schema);
		jsonEntity.addProperty("name", entity.name);
		jsonEntity.addProperty("description", entity.description);
		jsonEntity.add("photo", context.serialize(entity.getPhoto()));

		if (entity.schema.equals(Constants.SCHEMA_ENTITY_PATCH)) {
			jsonEntity.addProperty("visibility", entity.visibility);
			jsonEntity.add("location", context.serialize(entity.getLocation()));
		}
		else if (entity.schema.equals(Constants.SCHEMA_ENTITY_USER)) {
			jsonEntity.addProperty("area", entity.area);
			jsonEntity.addProperty("email", entity.email);
			jsonEntity.addProperty("role", entity.role);
		}
		else if (entity.schema.equals(Constants.SCHEMA_ENTITY_MESSAGE)) {
			if (Constants.TYPE_LINK_SHARE.equals(entity.type)) {
				final JsonArray jsonLinksArray = new JsonArray();
				if (entity.message != null) {
					JsonObject jsonLink = new JsonObject();
					jsonLink.addProperty("type", Constants.TYPE_LINK_SHARE);
					jsonLink.addProperty("_to", entity.message.id);
					jsonLinksArray.add(jsonLink);
				}
				else if (entity.patch != null) {
					JsonObject jsonLink = new JsonObject();
					jsonLink.addProperty("type", Constants.TYPE_LINK_SHARE);
					jsonLink.addProperty("_to", entity.patch.id);
					jsonLinksArray.add(jsonLink);
				}
				if (entity.recipients != null && entity.recipients.size() > 0) {
					for (RealmEntity recipient : entity.recipients) {
						JsonObject jsonLink = new JsonObject();
						jsonLink.addProperty("type", Constants.TYPE_LINK_SHARE);
						jsonLink.addProperty("_to", recipient.id);
						jsonLinksArray.add(jsonLink);
					}
				}
				jsonEntity.add("links", jsonLinksArray);
			}
		}

		return jsonEntity;
	}
}
