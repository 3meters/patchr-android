package com.patchr.components;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.view.Menu;
import android.view.MenuInflater;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.objects.Entity;
import com.patchr.objects.Link;
import com.patchr.objects.Link.Direction;
import com.patchr.objects.Patch;
import com.patchr.objects.Route;
import com.patchr.ui.base.BaseEdit;
import com.patchr.ui.base.BaseEntityEdit;
import com.patchr.utilities.Type;

public class MenuManager {

	@NonNull
	public static Boolean onCreateOptionsMenu(Activity activity, Menu menu) {

		/* Browsing */

		String activityName = activity.getClass().getSimpleName();
		MenuInflater menuInflater = activity.getMenuInflater();

		/*
		 * Fragments set menu items when they are configured which are
		 * later added in BaseFragment.onCreateOptionsMenu.
		 */
		if (activityName.equals("AircandiForm")) {
			if (!Patchr.getInstance().getCurrentUser().isAnonymous()) {
				menuInflater.inflate(R.menu.menu_notifications, menu);
			}
			return true;
		}
		else if (activityName.equals("MapForm")) {
			menuInflater.inflate(R.menu.menu_sign_in, menu);
			menuInflater.inflate(R.menu.menu_navigate, menu);
			return true;
		}
		else if (activityName.equals("MessageForm")) {
			/*
			 * These are included but actual visibility is handled in BaseActivity.onPrepareOptionsMenu
			 * which gets called everytime the overflow menu is displayed. If these are shown as actions
			 * there might be a timing problem.
			 */
			menuInflater.inflate(R.menu.menu_edit_message, menu);
			menuInflater.inflate(R.menu.menu_delete, menu);
			menuInflater.inflate(R.menu.menu_remove, menu);
			/*
			 * Shown for everyone
			 */
			menuInflater.inflate(R.menu.menu_share_message, menu);
			menuInflater.inflate(R.menu.menu_refresh, menu);
			menuInflater.inflate(R.menu.menu_report, menu);
			return true;
		}
		else if (activityName.equals("SearchForm")) {
			menuInflater.inflate(R.menu.menu_search_view, menu);
			return true;
		}
		else if (activityName.equals("AboutForm")) {
			return true;
		}
		else if (activityName.equals("PhotoForm")) {
			menuInflater.inflate(R.menu.menu_share_photo, menu);
			return true;
		}

		/* Editing */

		if (activityName.contains("PatchEdit")) {
			if (((BaseEntityEdit) activity).isEditing()) {
				menuInflater.inflate(R.menu.menu_save, menu);
			}
			menuInflater.inflate(R.menu.menu_delete_patch, menu);
			return true;
		}
		else if (activityName.contains("MessageEdit")) {
			Boolean editing = ((BaseEdit) activity).isEditing();
			if (editing) {
				menuInflater.inflate(R.menu.menu_save, menu);
				menuInflater.inflate(R.menu.menu_delete, menu);
			}
			else {
				menuInflater.inflate(R.menu.menu_send, menu);
			}
			return true;
		}
		else if (activityName.equals("SignInEdit")) {
			menuInflater.inflate(R.menu.menu_sign_in, menu);
			menuInflater.inflate(R.menu.menu_accept, menu);
			return true;
		}
		else if (activityName.equals("ReportEdit")
				|| activityName.equals("FeedbackEdit")) {
			menuInflater.inflate(R.menu.menu_send, menu);
			return true;
		}
		else if (activityName.equals("UserEdit")
				|| activityName.equals("PasswordEdit")
				|| activityName.contains("LocationPicker")) {
			menuInflater.inflate(R.menu.menu_accept, menu);
			return true;
		}
		else if (activityName.equals("ResetEdit")) {
			menuInflater.inflate(R.menu.menu_accept, menu);
			return true;
		}
		else if (activityName.equals("RegisterEdit")) {
			return true;
		}
		else if (activityName.contains("Builder")) {
			menuInflater.inflate(R.menu.menu_accept, menu);
			return true;
		}
		else if (activityName.contains("PhotoPicker")) {
			menuInflater.inflate(R.menu.menu_search_start, menu);
			return true;
		}
		else if (activityName.contains("Picker")) {
			menuInflater.inflate(R.menu.menu_accept, menu);
			return true;
		}
		return false;
	}

	@NonNull
	public static Boolean canUserEdit(Entity entity) {
		return entity != null
				&& (entity.isOwnedByCurrentUser()
				|| entity.isOwnedBySystem()
				|| (Patchr.settings.getBoolean(StringManager.getString(R.string.pref_enable_dev), false)
				&& Type.isTrue(Patchr.getInstance().getCurrentUser().developer)));
	}

	@NonNull
	public static Boolean canUserDelete(Entity entity) {
		return entity != null
				&& (entity.isOwnedByCurrentUser()
				|| (Patchr.settings.getBoolean(StringManager.getString(R.string.pref_enable_dev), false)
				&& Type.isTrue(Patchr.getInstance().getCurrentUser().developer)));
	}

	@NonNull
	public static Boolean canUserRemoveFromPatch(Entity entity) {
		if (entity == null) return false;
		if (entity.type != null && entity.type.equals(Constants.TYPE_LINK_SHARE)) return false;

		Link patchLink = entity.getLink(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_PATCH, entity.patchId, Direction.out);
		return patchLink != null
				&& patchLink.ownerId.equals(Patchr.getInstance().getCurrentUser().id)
				&& !entity.ownerId.equals(Patchr.getInstance().getCurrentUser().id);
	}

	@NonNull
	public static Boolean canUserAdd(Entity entity) {
		if (entity == null) return true;

		/* Current user is owner */
		if (entity.isOwnedByCurrentUser() || entity.isOwnedBySystem()) return true;

		/* Locked */
		if (entity.schema.equals(Constants.SCHEMA_ENTITY_PATCH)) {
			return Type.isFalse(((Patch) entity).locked);
		}

		return true;
	}

	@NonNull
	public static Boolean canUserShare(Entity entity) {
		/*
		 * Must be owner or member to share a private patch. Anyone
		 * can share a public patch. For messages, we assume that if
		 * you can see the message you have the ability to share it.
		 */
		if (entity == null) return false;
		if (!(entity instanceof Patch)) {
			return true;
		}
		else {
			Patch patch = (Patch) entity;
			return (patch.isVisibleToCurrentUser() || patch.isOwnedByCurrentUser());
		}
	}

	@NonNull
	public static Boolean showAction(Integer route, Entity entity, String forId) {

		if (entity == null)
			return false;
		else if (route == Route.ADD) {
			if (entity.schema.equals(Constants.SCHEMA_ENTITY_MESSAGE))
				return true;
		}
		else if (route == Route.REMOVE) {
			if (forId == null) return false;
			String forSchema = Entity.getSchemaForId(forId);
			/*
			 * Message can be listed for patches or current user.
		     */
			if (forSchema == null || forSchema.equals(Constants.SCHEMA_ENTITY_USER))
				return false;
			else
				return canUserRemoveFromPatch(entity);
		}
		else if (route == Route.EDIT)
			return canUserEdit(entity);
		else if (route == Route.DELETE)
			return canUserDelete(entity);
		else if (route == Route.SHARE)
			return canUserShare(entity);

		return false;
	}
}
