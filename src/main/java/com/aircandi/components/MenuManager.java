package com.aircandi.components;

import android.app.Activity;
import android.view.Menu;
import android.view.MenuInflater;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Link;
import com.aircandi.objects.Link.Direction;
import com.aircandi.objects.Patch;
import com.aircandi.objects.Route;
import com.aircandi.ui.base.BaseActivity;
import com.aircandi.ui.base.BaseEdit;
import com.aircandi.utilities.Type;

public class MenuManager {

	public boolean onCreateOptionsMenu(Activity activity, Menu menu) {

		/* Browsing */

		String activityName = activity.getClass().getSimpleName();
		MenuInflater menuInflater = activity.getMenuInflater();
		Entity entity = ((BaseActivity) activity).getEntity();

		if (activityName.equals("AircandiForm")) {
			/*
			 * Fragments set menu items when they are configured which are
			 * later added in BaseFragment.onCreateOptionsMenu.
			 */
			menuInflater.inflate(R.menu.menu_notifications, menu);
			menuInflater.inflate(R.menu.menu_sign_in, menu);
			return true;
		}
		else if (activityName.equals("PatchForm")) {
			menuInflater.inflate(R.menu.menu_sign_in, menu);
			menuInflater.inflate(R.menu.menu_invite, menu);
			menuInflater.inflate(R.menu.menu_map, menu);
			return true;
		}
		else if (activityName.equals("PlaceForm")) {
			menuInflater.inflate(R.menu.menu_sign_in, menu);
			menuInflater.inflate(R.menu.menu_map, menu);
			return true;
		}
		else if (activityName.equals("UserForm")) {
			menuInflater.inflate(R.menu.menu_refresh, menu);
			if (canUserEdit(entity)) {
				menuInflater.inflate(R.menu.menu_edit_user, menu);
				menuInflater.inflate(R.menu.menu_sign_out, menu);
			}
			menuInflater.inflate(R.menu.menu_report, menu);
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
		else if (activityName.equals("EntityList")) {
			menuInflater.inflate(R.menu.menu_refresh, menu);
			menuInflater.inflate(R.menu.menu_add, menu);
			menuInflater.inflate(R.menu.menu_sign_in, menu);
			return true;
		}
		else if (activityName.equals("MapForm")) {
			menuInflater.inflate(R.menu.menu_navigate, menu);
			menuInflater.inflate(R.menu.menu_sign_in, menu);
			return true;
		}

		/* Editing */

		if (activityName.contains("PatchEdit")) {
			menuInflater.inflate(R.menu.menu_save, menu);
			menuInflater.inflate(R.menu.menu_delete_patch, menu);
			return true;
		}
		else if (activityName.contains("MessageEdit")) {
			Boolean editing = ((BaseEdit) activity).isEditing();
			if (editing) {
				menuInflater.inflate(R.menu.menu_accept, menu);
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
			menuInflater.inflate(R.menu.menu_accept, menu);
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

	public Boolean canUserEdit(Entity entity) {
		if (entity == null) return false;

		if (entity.isOwnedByCurrentUser() || entity.isOwnedBySystem()) return true;
		return Patchr.settings.getBoolean(StringManager.getString(R.string.pref_enable_dev), false)
				&& Type.isTrue(Patchr.getInstance().getCurrentUser().developer);
	}

	public Boolean canUserDelete(Entity entity) {
		if (entity == null) return false;

		if (entity.isOwnedByCurrentUser()) return true;
		return Patchr.settings.getBoolean(StringManager.getString(R.string.pref_enable_dev), false)
				&& Type.isTrue(Patchr.getInstance().getCurrentUser().developer);
	}

	@SuppressWarnings("ucd")
	public Boolean canUserRemoveFromPlace(Entity entity) {
		if (entity == null) return false;
		if (entity.type.equals(Constants.TYPE_LINK_SHARE)) return false;

		Link placeLink = entity.getLink(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_PATCH, entity.patchId, Direction.out);
		return placeLink != null
				&& placeLink.ownerId.equals(Patchr.getInstance().getCurrentUser().id)
				&& !entity.ownerId.equals(Patchr.getInstance().getCurrentUser().id);
	}

	public Boolean canUserAdd(Entity entity) {
		if (entity == null) return true;

		/* Current user is owner */
		if (entity.isOwnedByCurrentUser() || entity.isOwnedBySystem()) return true;

		/* Locked */
		return Type.isFalse(entity.locked);
	}

	public Boolean canUserShare(Entity entity) {
		/*
		 * Must be owner or member to share a private patch. Anyone
		 * can share a public patch. For messages, we assume that if
		 * you can see the message you have the ability to share it.
		 */
		if (entity == null) return false;
		if (!entity.schema.equals(Constants.SCHEMA_ENTITY_PATCH)) {
			return true;
		}
		else {
			Patch patch = (Patch) entity;
			return (patch.isVisibleToCurrentUser() || patch.isOwnedByCurrentUser());
		}
	}

	public Boolean showAction(Integer route, Entity entity, String forId) {

		if (entity == null)
			return false;
		else if (route == Route.ADD) {
			if (entity.schema.equals(Constants.SCHEMA_ENTITY_MESSAGE))
				return true;
		}
		else if (route == Route.REMOVE) {
			if (forId == null) return false;
			String forSchema = com.aircandi.objects.Entity.getSchemaForId(forId);
			/*
			 * Message can be listed for places or current user.
		 */
			if (forSchema.equals(Constants.SCHEMA_ENTITY_USER))
				return false;
			else
				return Patchr.getInstance().getMenuManager().canUserRemoveFromPlace(entity);
		}
		else if (route == Route.EDIT)
			return Patchr.getInstance().getMenuManager().canUserEdit(entity);
		else if (route == Route.DELETE)
			return Patchr.getInstance().getMenuManager().canUserDelete(entity);
		else if (route == Route.SHARE)
			return Patchr.getInstance().getMenuManager().canUserShare(entity);

		return false;
	}
}
