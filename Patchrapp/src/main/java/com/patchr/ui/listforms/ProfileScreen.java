package com.patchr.ui.listforms;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.UserManager;
import com.patchr.objects.Command;
import com.patchr.objects.Link;
import com.patchr.objects.Query;
import com.patchr.objects.QueryName;
import com.patchr.ui.collections.BaseListScreen;
import com.patchr.ui.edit.ProfileEdit;
import com.patchr.ui.views.UserDetailView;
import com.patchr.utilities.Json;

public class ProfileScreen extends BaseListScreen {

	private boolean isCurrentUser;

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public boolean onCreateOptionsMenu(Menu menu) {

		if (UserManager.shared().authenticated()) {
			/* Shown for owner */
			getMenuInflater().inflate(R.menu.menu_logout, menu);    // base

			/* Shown for everyone */
			getMenuInflater().inflate(R.menu.menu_report, menu);    // base
		}

		return super.onCreateOptionsMenu(menu);
	}

	@Override public boolean onOptionsItemSelected(MenuItem item) {
		return super.onOptionsItemSelected(item);   // home, report, logout
	}

	@Override public void onClick(View view) {

		if (view.getId() == R.id.fab) {
			final String jsonEntity = Json.objectToJson(this.entity);
			startActivity(new Intent(this, ProfileEdit.class).putExtra(Constants.EXTRA_ENTITY, jsonEntity));
		}
		else if (view.getId() == R.id.member_of_button || view.getId() == R.id.owner_of_button) {
			int titleResId = 0;
			int emptyResId = 0;
			String linkType = (String) view.getTag();

			if (view.getId() == R.id.member_of_button) {
				titleResId = R.string.label_drawer_item_watch;
				emptyResId = R.string.label_profile_member_of_empty;
			}
			else if (view.getId() == R.id.owner_of_button) {
				titleResId = R.string.label_drawer_item_create;
				emptyResId = R.string.label_profile_owner_of_empty;
			}

			Bundle extras = new Bundle();
			extras.putInt(Constants.EXTRA_LIST_ITEM_RESID, R.layout.listitem_patch);
			extras.putString(Constants.EXTRA_LIST_LINK_DIRECTION, Link.Direction.out.name());
			extras.putString(Constants.EXTRA_LIST_LINK_SCHEMA, Constants.SCHEMA_ENTITY_PATCH);
			extras.putString(Constants.EXTRA_LIST_LINK_TYPE, linkType);
			extras.putInt(Constants.EXTRA_LIST_EMPTY_RESID, emptyResId);
			extras.putInt(Constants.EXTRA_LIST_TITLE_RESID, titleResId);
			Patchr.router.route(this, Command.ENTITY_LIST, this.entity, extras);
		}
		else {
			super.onClick(view);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override public void initialize(Bundle savedInstanceState) {
		this.header = new UserDetailView(this);
		this.boundHeader = true;
		this.query = Query.Factory(QueryName.MessagesByUser, entityId);

		super.initialize(savedInstanceState);

		this.isCurrentUser = UserManager.shared().authenticated() && UserManager.currentRealmUser.id.equals(this.entityId);

		/* Override */
		this.fab = (FloatingActionButton) this.header.findViewById(R.id.fab);
	}

	@Override protected int getLayoutId() {
		return R.layout.screen_profile;
	}

	@Override protected String getScreenName() {
		return this.isCurrentUser ? "ProfileScreen" : "UserScreen";
	}
}