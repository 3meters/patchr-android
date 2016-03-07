package com.patchr.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.components.Logger;
import com.patchr.objects.Route;
import com.patchr.objects.TransitionType;

import java.util.Map;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.util.LinkProperties;

@SuppressLint("Registered")
public class DeepLinkActivity extends Activity {

	protected void handleDeepLink() {

		if (Patchr.applicationUpdateRequired) {
			Patchr.router.route(DeepLinkActivity.this, Route.SPLASH, null, null);
			finish();
			return;
		}

		Intent intent = this.getIntent();
		Uri uri = intent.getData();

		if (uri != null) {
			Branch branch = Branch.getInstance(this);
			branch.initSession(new Branch.BranchUniversalReferralInitListener() {

				@Override
				public void onInitFinished(BranchUniversalObject branchUniversalObject, LinkProperties linkProperties, BranchError error) {

					if (branchUniversalObject != null) {
						Map metadata = branchUniversalObject.getMetadata();
						Bundle extras = new Bundle();
						extras.putString(Constants.EXTRA_ENTITY_SCHEMA, (String) metadata.get("entitySchema"));
						extras.putString(Constants.EXTRA_ENTITY_ID, (String) metadata.get("entityId"));
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
			}, uri, this);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		handleDeepLink();
	}

	@Override
	public void onNewIntent(Intent intent) {
		this.setIntent(intent);
	}
}