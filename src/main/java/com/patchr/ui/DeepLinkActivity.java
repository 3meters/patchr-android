package com.patchr.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.components.Logger;
import com.patchr.objects.Route;
import com.patchr.objects.TransitionType;
import com.patchr.utilities.Dialogs;

import org.json.JSONObject;

import io.branch.referral.Branch;
import io.branch.referral.BranchError;

@SuppressLint("Registered")
public class DeepLinkActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initialize();
	}

	protected void initialize() {

		if (Patchr.applicationUpdateRequired) {
			Patchr.router.route(DeepLinkActivity.this, Route.SPLASH, null, null);
			finish();
			return;
		}

		Branch.getInstance().initSession(new Branch.BranchReferralInitListener() {

			@Override
			public void onInitFinished(JSONObject referringParams, BranchError error) {

				if (referringParams != null && referringParams.has("entitySchema")) {

					Logger.d(this, "Referring params received");
					Bundle extras = new Bundle();
					extras.putString(Constants.EXTRA_ENTITY_SCHEMA, referringParams.optString("entitySchema"));
					extras.putString(Constants.EXTRA_ENTITY_ID, referringParams.optString("entityId"));
					extras.putInt(Constants.EXTRA_TRANSITION_TYPE, TransitionType.DRILL_TO);
					Patchr.router.route(DeepLinkActivity.this, Route.BROWSE, null, extras);
					finish();
					return;
				}

				if (error != null) {
					Logger.w(this, error.getMessage());
				}

				Patchr.router.route(DeepLinkActivity.this, Route.SPLASH, null, null);
				finish();

			}
		}, this.getIntent().getData(), this);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	private void updateRequired() {
		Dialogs.updateApp(this);
	}

	/*--------------------------------------------------------------------------------------------
	 * Dialogs
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onNewIntent(Intent intent) {
		this.setIntent(intent);
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/
}