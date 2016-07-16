package com.patchr.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.facebook.CallbackManager;
import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.AnimationManager;
import com.patchr.components.BranchProvider;
import com.patchr.components.FacebookProvider;
import com.patchr.components.IntentBuilder;
import com.patchr.components.StringManager;
import com.patchr.model.RealmEntity;
import com.patchr.objects.enums.AnalyticsCategory;
import com.patchr.objects.enums.MessageType;
import com.patchr.objects.enums.TransitionType;
import com.patchr.ui.edit.ShareEdit;
import com.patchr.utilities.Reporting;
import com.segment.analytics.Properties;

public class InviteScreen extends BaseScreen {

	private   RealmEntity     patch;
	private   String          patchJson;
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

	public void onClick(View view) {

		Integer id = view.getId();
		if (id == R.id.signup_button) {
			submitAction();
		}

		final String title = String.format(StringManager.getString(R.string.label_patch_share_title), this.patch.name);

		if (id == R.id.patchr_button) {
			/*
			 * Go to patchr share directly but looks just like an external share
			 */
			final IntentBuilder intentBuilder = new IntentBuilder(this, ShareEdit.class);
			final Intent intent = intentBuilder.build();

			intent.putExtra(Constants.EXTRA_MESSAGE_TYPE, MessageType.Invite);
			intent.putExtra(Constants.EXTRA_SHARE_SOURCE, getPackageName());
			intent.putExtra(Constants.EXTRA_SHARE_ID, this.patch.id);
			intent.putExtra(Constants.EXTRA_SHARE_PATCH, this.patchJson);
			intent.putExtra(Constants.EXTRA_SHARE_SCHEMA, Constants.SCHEMA_ENTITY_PATCH);
			intent.setAction(Intent.ACTION_SEND);
			this.startActivity(intent);
			AnimationManager.doOverridePendingTransition(this, TransitionType.FORM_TO);
		}
		else if (id == R.id.facebook_button) {
			FacebookProvider provider = new FacebookProvider();
			provider.invite(title, this.patch, this, callbackManager);
		}
		else if (id == R.id.more_button) {
			BranchProvider provider = new BranchProvider();
			Reporting.track(AnalyticsCategory.ACTION, "Started Patch Invitation", new Properties().putValue("network", "Android"));
			provider.invite(title, this.patch, this);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			this.patchJson = extras.getString(Constants.EXTRA_ENTITY);
			if (this.patchJson != null) {
				this.patch = Patchr.gson.fromJson(this.patchJson, RealmEntity.class);
			}
		}
	}

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
		callbackManager = CallbackManager.Factory.create();
	}

	@Override protected int getLayoutId() {
		return R.layout.screen_invite;
	}

	@Override public void submitAction() {
		Patchr.router.browse(this, patch.id, null, true);
		finish();
	}

	public void bind() {}
}