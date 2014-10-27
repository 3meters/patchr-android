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
import com.aircandi.objects.Route;
import com.aircandi.ui.base.BaseActivity;
import com.aircandi.ui.base.BaseEdit;
import com.aircandi.utilities.Type;

public class MenuManager {

	public boolean onCreatePopupMenu(Activity activity, android.view.Menu menu, Entity entity) {

		/* Browsing */

		String activityName = activity.getClass().getSimpleName();
		android.view.MenuInflater inflater = activity.getMenuInflater();

		if (activityName.equals("PlaceForm")) {
			if (canUserEdit(entity)) {
				inflater.inflate(R.menu.menu_edit_place, menu);
			}
			inflater.inflate(R.menu.menu_report, menu);
		}
		else if (activityName.equals("UserForm")) {
			if (canUserEdit(entity)) {
				inflater.inflate(R.menu.menu_edit_user, menu);
				inflater.inflate(R.menu.menu_sign_out, menu);
			}
			else {
				inflater.inflate(R.menu.menu_report, menu);
			}
		}
		else if (activityName.equals("AircandiForm")
				&& entity != null
				&& entity.schema.equals(Constants.SCHEMA_ENTITY_USER)) {
			if (canUserEdit(entity)) {
				inflater.inflate(R.menu.menu_edit_user, menu);
				inflater.inflate(R.menu.menu_sign_out, menu);
			}
		}
		else
			inflater.inflate(R.menu.menu_report, menu);

		return true;
	}

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
			menuInflater.inflate(R.menu.menu_sign_in, menu);
			return true;
		}
		else if (activityName.equals("PlaceForm")) {
			menuInflater.inflate(R.menu.menu_sign_in, menu);
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
		else if (activityName.contains("MessageEdit")) {
			Boolean editing = ((BaseEdit) activity).isEditing();
			if (editing) {
				menuInflater.inflate(R.menu.menu_accept, menu);
			}
			else {
				menuInflater.inflate(R.menu.menu_send, menu);
			}
			menuInflater.inflate(R.menu.menu_cancel, menu);
			return true;
		}

		/* Editing */

		else if (activityName.equals("ReportEdit")
				|| activityName.equals("FeedbackEdit")) {
			menuInflater.inflate(R.menu.menu_cancel, menu);
			menuInflater.inflate(R.menu.menu_send, menu);
			return true;
		}
		else if (activityName.equals("ApplinkListEdit")) {
			menuInflater.inflate(R.menu.menu_refresh, menu);
			menuInflater.inflate(R.menu.menu_cancel, menu);
			menuInflater.inflate(R.menu.menu_accept, menu);
			return true;
		}
		else if (activityName.equals("CommentEdit")
				|| activityName.equals("ApplinkEdit")
				|| activityName.equals("UserEdit")
				|| activityName.equals("PasswordEdit")
				|| activityName.equals("TuningEdit")
				|| activityName.contains("SignInEdit")
				|| activityName.contains("LocationPicker")) {
			menuInflater.inflate(R.menu.menu_cancel, menu);
			menuInflater.inflate(R.menu.menu_accept, menu);
			return true;
		}
		else if (activityName.equals("ResetEdit")) {
			menuInflater.inflate(R.menu.menu_cancel, menu);
			return true;
		}
		else if (activityName.contains("Edit")) {
			menuInflater.inflate(R.menu.menu_cancel, menu);
			menuInflater.inflate(R.menu.menu_accept, menu);
			menuInflater.inflate(R.menu.menu_delete, menu);
			return true;
		}
		else if (activityName.contains("Builder")) {
			menuInflater.inflate(R.menu.menu_cancel, menu);
			menuInflater.inflate(R.menu.menu_accept, menu);
			return true;
		}
		else if (activityName.contains("Picker")) {
			menuInflater.inflate(R.menu.menu_cancel, menu);
			return true;
		}
		else {

			/* BROWSE */

			if (activityName.equals("HelpForm") || activityName.equals("AboutForm")) {
				menuInflater.inflate(R.menu.menu_cancel, menu);
				return true;
			}
			else if (activityName.equals("PhotoForm")) {
				menuInflater.inflate(R.menu.menu_cancel, menu);
				menuInflater.inflate(R.menu.menu_share, menu);
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
			else {
				menuInflater.inflate(R.menu.menu_sign_in, menu);
				return true;
			}
		}
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

		Link placeLink = entity.getLink(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_PLACE, entity.placeId, Direction.out);
		return placeLink != null
				&& placeLink.ownerId.equals(Patchr.getInstance().getCurrentUser().id)
				&& !entity.ownerId.equals(Patchr.getInstance().getCurrentUser().id);
	}

	public Boolean canUserAdd(Entity entity) {
		if (entity == null) return true;

		/* Current user is owner */
		if (entity.isOwnedByCurrentUser() || entity.isOwnedBySystem()) return true;

		/* Locked */
		return !entity.locked;
	}

	public Boolean canUserShare(Entity entity) {
		if (entity == null || entity.shareable == null) return false;
		return entity.shareable;
	}

	public Boolean showAction(Integer route, Entity entity, String forId) {

		if (entity == null)
			return false;
		else if (route == Route.ADD) {
			if (entity.schema.equals(com.aircandi.Constants.SCHEMA_ENTITY_MESSAGE))
				return true;
		}
		else if (route == Route.REMOVE) {
			if (forId == null) return false;
			String forSchema = com.aircandi.objects.Entity.getSchemaForId(forId);
			if (forSchema.equals(com.aircandi.Constants.SCHEMA_ENTITY_USER))
				return false;
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
