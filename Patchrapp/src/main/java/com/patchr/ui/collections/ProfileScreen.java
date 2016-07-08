package com.patchr.ui.collections;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.patchr.Constants;
import com.patchr.R;
import com.patchr.components.AnimationManager;
import com.patchr.components.UserManager;
import com.patchr.objects.QueryName;
import com.patchr.objects.QuerySpec;
import com.patchr.objects.TransitionType;
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
		else if (view.getId() == R.id.member_of_button) {
			Intent intent = new Intent(this, BaseListScreen.class);
			intent.putExtra(Constants.EXTRA_ENTITY_ID, entityId);
			intent.putExtra(Constants.EXTRA_QUERY_NAME, QueryName.PatchesUserMemberOf);
			startActivity(intent);
			AnimationManager.doOverridePendingTransition(this, TransitionType.FORM_TO);
		}
		else if (view.getId() == R.id.owner_of_button) {
			Intent intent = new Intent(this, BaseListScreen.class);
			intent.putExtra(Constants.EXTRA_ENTITY_ID, entityId);
			intent.putExtra(Constants.EXTRA_QUERY_NAME, QueryName.PatchesOwnedByUser);
			startActivity(intent);
			AnimationManager.doOverridePendingTransition(this, TransitionType.FORM_TO);
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
		this.querySpec = QuerySpec.Factory(QueryName.MessagesByUser);

		super.initialize(savedInstanceState);

		this.isCurrentUser = UserManager.shared().authenticated() && UserManager.currentUser.id.equals(this.entityId);
		this.fab = (FloatingActionButton) this.header.findViewById(R.id.fab); /* Override */
	}

	@Override protected int getLayoutId() {
		return R.layout.screen_profile;
	}

	@Override protected String getScreenName() {
		return this.isCurrentUser ? "ProfileScreen" : "UserScreen";
	}
}