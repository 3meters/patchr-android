package com.patchr.components;

import com.patchr.Constants;
import com.patchr.model.RealmEntity;
import com.patchr.objects.enums.Command;
import com.patchr.objects.enums.MemberStatus;

public class MenuManager {

	public static Boolean canUserEdit(RealmEntity entity) {
		if (entity.schema.equals(Constants.SCHEMA_ENTITY_MESSAGE)) {
			if (entity.type != null && entity.type.equals("share")) {
				return false;
			}
		}
		return (entity.isOwnedByCurrentUser());
	}

	public static Boolean canUserDelete(RealmEntity entity) {
		return (entity != null && (entity.isOwnedByCurrentUser()));
	}

	public static Boolean canUserRemoveFromPatch(RealmEntity entity) {
		if (entity == null) return false;
		//noinspection SimplifiableIfStatement
		if (Constants.TYPE_ENTITY_SHARE.equals(entity.type)) return false;

		return entity.patch != null && entity.patch.ownerId != null
				&& entity.patch.ownerId.equals(UserManager.currentUser.id)
				&& !entity.ownerId.equals(UserManager.currentUser.id);
	}

	public static Boolean canUserShare(RealmEntity entity) {
		/*
		 * Must be owner or member to share a private patch. Anyone
		 * can share a public patch. For messages, we assume that if
		 * you can see the message you have the ability to share it.
		 */
		if (entity == null) return false;

		if (entity.schema.equals(Constants.SCHEMA_ENTITY_MESSAGE)) {
			if (entity.type != null && entity.type.equals("share")) {
				return false;
			}
		}

		return !(entity.schema.equals(Constants.SCHEMA_ENTITY_PATCH)) || (entity.userMemberStatus.equals(MemberStatus.Member) || entity.isOwnedByCurrentUser());
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
