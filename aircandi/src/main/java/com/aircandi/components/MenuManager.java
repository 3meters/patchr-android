package com.aircandi.components;

import android.app.Activity;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuInflater;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Link;
import com.aircandi.objects.Link.Direction;
import com.aircandi.objects.Route;
import com.aircandi.utilities.Type;

public class MenuManager {

	public boolean onCreatePopupMenu(Activity activity, android.view.Menu menu, Entity entity) {

		/* Browsing */

		android.view.MenuInflater inflater = activity.getMenuInflater();
		inflater.inflate(R.menu.menu_report, menu);

		return true;
	}

	public boolean onCreateOptionsMenu(Activity activity, Menu menu) {

		/* Browsing */

		String activityName = activity.getClass().getSimpleName();
		final FragmentActivity fragmentActivity = (FragmentActivity) activity;
		MenuInflater menuInflater = fragmentActivity.getMenuInflater();

		/* Editing */

		if (activityName.equals("ReportEdit")
				|| activityName.equals("FeedbackEdit")
				|| activityName.equals("InviteEdit")) {
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
				|| activityName.contains("SignInEdit")) {
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
			else if (activityName.equals("AircandiForm")) {
				menuInflater.inflate(R.menu.menu_base, menu);
				return true;
			}
			else if (activityName.equals("PlaceForm")) {
				menuInflater.inflate(R.menu.menu_refresh, menu);
				menuInflater.inflate(R.menu.menu_add, menu);
				menuInflater.inflate(R.menu.menu_base, menu);

				if (Type.isTrue(Aircandi.getInstance().getCurrentUser().developer)
						&& Aircandi.settings.getBoolean(StringManager.getString(R.string.pref_enable_dev), false)) {
					menuInflater.inflate(R.menu.menu_save_beacon, menu);
				}

				menuInflater.inflate(R.menu.menu_help, menu);
				return true;
			}
			else if (activityName.equals("PictureForm")) {
				menuInflater.inflate(R.menu.menu_refresh, menu);
				menuInflater.inflate(R.menu.menu_add, menu);
				menuInflater.inflate(R.menu.menu_edit, menu);
				menuInflater.inflate(R.menu.menu_base, menu);
				return true;
			}
			else if (activityName.equals("CommentForm")) {
				menuInflater.inflate(R.menu.menu_cancel, menu);
				menuInflater.inflate(R.menu.menu_base, menu);
				menuInflater.inflate(R.menu.menu_help, menu);
				return true;
			}
			else if (activityName.equals("UserForm")) {
				menuInflater.inflate(R.menu.menu_refresh, menu);
				menuInflater.inflate(R.menu.menu_base, menu);
				menuInflater.inflate(R.menu.menu_edit, menu);
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
				menuInflater.inflate(R.menu.menu_base, menu);
				return true;
			}
			else if (activityName.equals("MapForm")) {
				menuInflater.inflate(R.menu.menu_navigate, menu);
				menuInflater.inflate(R.menu.menu_base, menu);
				return true;
			}
			else {
				menuInflater.inflate(R.menu.menu_base, menu);
				return true;
			}
		}
	}

	public Boolean canUserEdit(Entity entity) {
		if (entity == null) return false;

		if (entity.isOwnedByCurrentUser() || entity.isOwnedBySystem()) return true;
		return Aircandi.settings.getBoolean(StringManager.getString(R.string.pref_enable_dev), false)
				&& Type.isTrue(Aircandi.getInstance().getCurrentUser().developer);

	}

	public Boolean canUserDelete(Entity entity) {
		if (entity == null) return false;

		if (entity.isOwnedByCurrentUser()) return true;
		return Aircandi.settings.getBoolean(StringManager.getString(R.string.pref_enable_dev), false)
				&& Type.isTrue(Aircandi.getInstance().getCurrentUser().developer);

	}

	@SuppressWarnings("ucd")
	public Boolean canUserRemoveFromPlace(Entity entity) {
		if (entity == null) return false;
		if (entity.type.equals(Constants.TYPE_LINK_SHARE)) return false;

		Link placeLink = entity.getLink(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_PLACE, entity.placeId, Direction.out);
		return placeLink != null
				&& placeLink.ownerId.equals(Aircandi.getInstance().getCurrentUser().id)
				&& !entity.ownerId.equals(Aircandi.getInstance().getCurrentUser().id);

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
		else if (route == Route.EDIT)
			return Aircandi.getInstance().getMenuManager().canUserEdit(entity);
		else if (route == Route.DELETE)
			return Aircandi.getInstance().getMenuManager().canUserDelete(entity);
		else if (route == Route.REMOVE)
			return Aircandi.getInstance().getMenuManager().canUserRemoveFromPlace(entity);
		else if (route == Route.SHARE)
			return Aircandi.getInstance().getMenuManager().canUserShare(entity);
		else if (route == Route.ADD) {
			if (entity != null && (entity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)
					|| entity.schema.equals(Constants.SCHEMA_ENTITY_PICTURE)))
				return true;
		}

		return false;
	}
}
