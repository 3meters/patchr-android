package com.aircandi.catalina.components;

import android.app.Activity;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuInflater;

import com.aircandi.catalina.Constants;
import com.aircandi.catalina.R;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Route;
import com.aircandi.ui.base.BaseActivity;
import com.aircandi.ui.base.BaseEdit;

public class MenuManager extends com.aircandi.components.MenuManager {

	@Override
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
			return (super.onCreatePopupMenu(activity, menu, entity));

		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Activity activity, Menu menu) {

		String activityName = activity.getClass().getSimpleName();
		final FragmentActivity sherlock = (FragmentActivity) activity;
		MenuInflater menuInflater = sherlock.getMenuInflater();
		Entity entity = ((BaseActivity) activity).getEntity();

		if (activityName.equals("AircandiForm")) {
			/*
			 * Fragments set menu items when they are configured which are
			 * later added in BaseFragment.onCreateOptionsMenu.
			 */
			menuInflater.inflate(R.menu.menu_base, menu);
			return true;
		}
		else if (activityName.equals("PlaceForm")) {
			menuInflater.inflate(R.menu.menu_refresh, menu);
			menuInflater.inflate(R.menu.menu_share_place, menu);
			if (canUserEdit(entity)) {
				menuInflater.inflate(R.menu.menu_edit_place, menu);
			}
			menuInflater.inflate(R.menu.menu_delete, menu);
			menuInflater.inflate(R.menu.menu_report, menu);
			menuInflater.inflate(R.menu.menu_base, menu);
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
		else
			return (super.onCreateOptionsMenu(activity, menu));
	}

	@Override
	public Boolean showAction(Integer route, Entity entity, String forId) {
		if (route == Route.ADD) {
			if (entity != null && (entity.schema.equals(Constants.SCHEMA_ENTITY_MESSAGE)))
				return true;
		}
		else if (route == Route.REMOVE) {
			if (forId == null) return false;
			String forSchema = com.aircandi.catalina.objects.Entity.getSchemaForId(forId);
			if (forSchema.equals(com.aircandi.Constants.SCHEMA_ENTITY_USER))
				return false;
		}
		return super.showAction(route, entity, forId);
	}
}
