package com.patchr.components;

import com.patchr.Constants;
import com.patchr.model.RealmEntity;
import com.patchr.objects.Command;
import com.patchr.objects.MemberStatus;

public class MenuManager {

	public static Boolean canUserEdit(RealmEntity entity) {
		if (entity.schema.equals(Constants.SCHEMA_ENTITY_MESSAGE)) {
			if (entity.type != null && entity.type.equals("share")) {
				return false;
			}
		}
		return (UserManager.shared().authenticated() && entity.isOwnedByCurrentUser());
	}

	public static Boolean canUserDelete(RealmEntity entity) {
		return UserManager.shared().authenticated() && entity != null && (entity.isOwnedByCurrentUser());
	}

	public static Boolean canUserRemoveFromPatch(RealmEntity entity) {
		if (!UserManager.shared().authenticated()) return false;
		if (entity == null) return false;
		if (entity.type != null && entity.type.equals(Constants.TYPE_LINK_SHARE)) return false;

		return entity.patch != null && entity.patch.ownerId != null
				&& entity.patch.ownerId.equals(UserManager.currentUser.id)
				&& !entity.ownerId.equals(UserManager.currentUser.id);
	}

	public static Boolean canUserAdd(RealmEntity entity) {
		if (!UserManager.shared().authenticated()) return false;
		if (entity == null) return true;

		/* Current user is owner */
		if (entity.isOwnedByCurrentUser()) return true;

		return true;
	}

	public static Boolean canUserShare(RealmEntity entity) {
		/*
		 * Must be owner or member to share a private patch. Anyone
		 * can share a public patch. For messages, we assume that if
		 * you can see the message you have the ability to share it.
		 */
		if (!UserManager.shared().authenticated()) return false;
		if (entity == null) return false;

		if (entity.schema.equals(Constants.SCHEMA_ENTITY_MESSAGE)) {
			if (entity.type != null && entity.type.equals("share")) {
				return false;
			}
		}

		if (!(entity.schema.equals(Constants.SCHEMA_ENTITY_PATCH))) {
			return true;
		}
		else {
			return (entity.userMemberStatus.equals(MemberStatus.Member) || entity.isOwnedByCurrentUser());
		}
	}

	public static boolean showAction(Integer route, RealmEntity entity) {

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
