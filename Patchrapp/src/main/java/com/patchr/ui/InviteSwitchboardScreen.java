package com.patchr.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.facebook.CallbackManager;
import com.patchr.Constants;
import com.patchr.R;
import com.patchr.components.AnimationManager;
import com.patchr.components.BranchProvider;
import com.patchr.components.FacebookProvider;
import com.patchr.components.StringManager;
import com.patchr.model.RealmEntity;
import com.patchr.objects.enums.AnalyticsCategory;
import com.patchr.objects.enums.MessageType;
import com.patchr.objects.enums.State;
import com.patchr.objects.enums.TransitionType;
import com.patchr.ui.edit.ShareEdit;
import com.patchr.utilities.Reporting;
import com.patchr.utilities.UI;
import com.segment.analytics.Properties;

public class InviteSwitchboardScreen extends BaseScreen {

	protected CallbackManager callbackManager;      // For facebook

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		bind();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_done, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.submit) {
			submitAction();
		}
		else {
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	@Override public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (resultCode != Activity.RESULT_CANCELED) {
			callbackManager.onActivityResult(requestCode, resultCode, intent);
		}
		super.onActivityResult(requestCode, resultCode, intent);
	}

	@Override public void submitAction() {
		UI.browseEntity(entityId, this);
		finish();
	}

	public void onClick(View view) {

		Integer id = view.getId();
		if (id == R.id.signup_button) {
			submitAction();
		}

		final String title = String.format(StringManager.getString(R.string.label_patch_share_title), entity.name);

		if (id == R.id.patchr_button) {
			/* Go to patchr share directly but looks just like an external share*/
			final Intent intent = new Intent(this, ShareEdit.class);
			intent.putExtra(Constants.EXTRA_STATE, State.Inserting);
			intent.putExtra(Constants.EXTRA_MESSAGE_TYPE, MessageType.Invite);
			intent.putExtra(Constants.EXTRA_SHARE_SOURCE, getPackageName());
			intent.putExtra(Constants.EXTRA_SHARE_ENTITY_ID, entityId);
			intent.putExtra(Constants.EXTRA_SHARE_SCHEMA, Constants.SCHEMA_ENTITY_PATCH);
			intent.setAction(Intent.ACTION_SEND);
			startActivity(intent);
			AnimationManager.doOverridePendingTransition(this, TransitionType.FORM_TO);
		}
		else if (id == R.id.facebook_button) {
			FacebookProvider provider = new FacebookProvider();
			provider.invite(title, entity, this, callbackManager);
		}
		else if (id == R.id.more_button) {
			BranchProvider provider = new BranchProvider();
			Reporting.track(AnalyticsCategory.ACTION, "Started Patch Invitation", new Properties().putValue("network", "Android"));
			provider.invite(title, entity, this);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
		callbackManager = CallbackManager.Factory.create();
	}

	public void bind() {
		entity = realm.where(RealmEntity.class).equalTo("id", entityId).findFirst();
	}

	@Override protected int getLayoutId() {
		return R.layout.screen_invite;
	}
}