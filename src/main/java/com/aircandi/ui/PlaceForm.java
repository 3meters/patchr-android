package com.aircandi.ui;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.events.ProcessingFinishedEvent;
import com.aircandi.objects.Entity;
import com.aircandi.objects.LinkProfile;
import com.aircandi.objects.Route;
import com.aircandi.ui.base.BaseEntityForm;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.ui.widgets.CandiView;
import com.aircandi.ui.widgets.CandiView.IndicatorOptions;
import com.aircandi.utilities.Json;
import com.squareup.otto.Subscribe;

@SuppressLint("Registered")
public class PlaceForm extends BaseEntityForm {

	@Override
	public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			/*
			 * We handle upsizing if the place entity we want to browse isn't
			 * stored in the service yet.
			 */
			final String jsonEntity = extras.getString(Constants.EXTRA_ENTITY);
			if (jsonEntity != null) {
				mEntity = (Entity) Json.jsonToObject(jsonEntity, Json.ObjectType.ENTITY);
			}
		}
	}

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mEmptyView.setEnabled(false);
		mLinkProfile = LinkProfile.NO_LINKS;
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe
	public void onProcessingFinished(ProcessingFinishedEvent event) {
		mBusy.hideBusy(false);
	}

	@SuppressWarnings("ucd")
	public void onMapButtonClick(View view) {
		if (mEntity != null) {
			Patchr.dispatch.route(this, Route.MAP, mEntity, null);
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		/* Reset the image aspect ratio */
		AirImageView image = (AirImageView) findViewById(R.id.photo);
		TypedValue typedValue = new TypedValue();
		getResources().getValue(R.dimen.aspect_ratio_place_image, typedValue, true);
		image.setAspectRatio(typedValue.getFloat());
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void draw(View view) {

		if (view == null) {
			view = findViewById(android.R.id.content);
		}
		mFirstDraw = false;

		/* Photo overlayed with info */
		final CandiView candiView = (CandiView) view.findViewById(R.id.candi_view);
		if (candiView != null) {
			/*
			 * This is a place entity with a fancy image widget
			 */
			IndicatorOptions options = new IndicatorOptions();
			options.showIfZero = true;
			options.imageSizePixels = 15;
			options.iconsEnabled = false;
			candiView.databind(mEntity, options, null);
		}
	}

	public void configureActionBar() {
		super.configureActionBar();
		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayShowTitleEnabled(false);  // Dont show title
		}
	}

	@Override
	protected int getLayoutId() {
		return R.layout.place_form;
	}
}