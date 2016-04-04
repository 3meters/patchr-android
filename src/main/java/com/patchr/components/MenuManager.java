package com.patchr.components;

import com.patchr.Constants;
import com.patchr.objects.Command;
import com.patchr.objects.Entity;
import com.patchr.objects.Link;
import com.patchr.objects.Link.Direction;
import com.patchr.objects.Message;
import com.patchr.objects.Patch;

public class MenuManager {

	public static Boolean canUserEdit(Entity entity) {
		if (entity instanceof Message) {
			Message message = (Message) entity;
			if (message.type != null && message.type.equals("share")) {
				return false;
			}
		}
		return UserManager.shared().authenticated() && entity != null && (entity.isOwnedByCurrentUser());
	}

	public static Boolean canUserDelete(Entity entity) {
		return UserManager.shared().authenticated() && entity != null && (entity.isOwnedByCurrentUser());
	}

	public static Boolean canUserRemoveFromPatch(Entity entity) {
		if (!UserManager.shared().authenticated()) return false;
		if (entity == null) return false;
		if (entity.type != null && entity.type.equals(Constants.TYPE_LINK_SHARE)) return false;

		Link patchLink = entity.getLink(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_PATCH, entity.patchId, Direction.out);
		return patchLink != null
				&& patchLink.ownerId.equals(UserManager.currentUser.id)
				&& !entity.ownerId.equals(UserManager.currentUser.id);
	}

	public static Boolean canUserAdd(Entity entity) {
		if (!UserManager.shared().authenticated()) return false;
		if (entity == null) return true;

		/* Current user is owner */
		if (entity.isOwnedByCurrentUser()) return true;

		return true;
	}

	public static Boolean canUserShare(Entity entity) {
		/*
		 * Must be owner or member to share a private patch. Anyone
		 * can share a public patch. For messages, we assume that if
		 * you can see the message you have the ability to share it.
		 */
		if (!UserManager.shared().authenticated()) return false;
		if (entity == null) return false;

		if (entity instanceof Message) {
			Message message = (Message) entity;
			if (message.type != null && message.type.equals("share")) {
				return false;
			}
		}

		if (!(entity instanceof Patch)) {
			return true;
		}
		else {
			Patch patch = (Patch) entity;
			return (patch.isVisibleToCurrentUser() || patch.isOwnedByCurrentUser());
		}
	}

	public static boolean showAction(Integer route, Entity entity) {

		if (entity == null)
			return false;
		else if (route == Command.REMOVE)
			return canUserRemoveFromPatch(entity);
		else if (route == Command.EDIT)
			return canUserEdit(entity);
		else if (route == Command.DELETE)
			return canUserDelete(entity);
		else if (route == Command.SHARE)
			return canUserShare(entity);

		return false;
	}
}
