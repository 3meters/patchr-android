package com.patchr.ui.collections;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.FloatingActionButton;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.AnimationManager;
import com.patchr.components.UserManager;
import com.patchr.objects.QuerySpec;
import com.patchr.objects.enums.QueryName;
import com.patchr.objects.enums.State;
import com.patchr.objects.enums.TransitionType;
import com.patchr.ui.edit.ProfileEdit;
import com.patchr.ui.views.UserDetailView;
import com.patchr.utilities.UI;

public class ProfileScreen extends BaseListScreen {

	private boolean isCurrentUser;

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public boolean onCreateOptionsMenu(Menu menu) {

		getMenuInflater().inflate(R.menu.menu_overflow, menu);
		getMenuInflater().inflate(R.menu.menu_logout, menu);

		return super.onCreateOptionsMenu(menu);
	}

	@Override public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.overflow) {
			bottomSheetDialog = new BottomSheetDialog(this);
			View view = getLayoutInflater().inflate(R.layout.dialog_profile, null);
			bottomSheetDialog.setContentView(view);
			bottomSheetDialog.getWindow().setDimAmount(0.3f);
			bottomSheetDialog.setOnDismissListener(dialogInterface -> bottomSheetDialog = null);
			bottomSheetDialog.show();
		}
		else if (item.getItemId() == R.id.logout) {
			UserManager.shared().logout();
			UI.routeLobby(Patchr.applicationContext);
		}
		else {
			return super.onOptionsItemSelected(item);   // home, report, logout
		}
		return true;
	}

	@Override public void onClick(View view) {

		if (view.getId() == R.id.fab) {
			Intent intent = new Intent(this, ProfileEdit.class);
			intent.putExtra(Constants.EXTRA_ENTITY_ID, entityId);
			intent.putExtra(Constants.EXTRA_STATE, State.Editing);
			startActivityForResult(intent, Constants.ACTIVITY_ENTITY_EDIT);
			AnimationManager.doOverridePendingTransition(this, TransitionType.FORM_TO);
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
		else if (view.getId() == R.id.report) {
			bottomSheetDialog.dismiss();
			String message = String.format("Report on user id: %1$s\n\nPlease add some detail on why you are reporting this user.\n", entityId);
			Intent email = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:report@patchr.com"));
			email.putExtra(Intent.EXTRA_SUBJECT, "Report on user");
			email.putExtra(Intent.EXTRA_TEXT, message);
			startActivity(Intent.createChooser(email, "Send report using:"));
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

		this.isCurrentUser = UserManager.currentUser.id.equals(this.entityId);
		this.fab = (FloatingActionButton) this.header.findViewById(R.id.fab); /* Override */
	}

	@Override protected int getLayoutId() {
		return R.layout.screen_profile;
	}

	@Override protected String getScreenName() {
		return this.isCurrentUser ? "ProfileScreen" : "UserScreen";
	}
}