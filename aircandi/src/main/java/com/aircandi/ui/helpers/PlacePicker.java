package com.aircandi.ui.helpers;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.ListView;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.StringManager;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Place;
import com.aircandi.objects.Place.ReasonType;
import com.aircandi.service.RequestListener;
import com.aircandi.ui.base.BaseActivity;
import com.aircandi.ui.components.PlaceSuggestController;
import com.aircandi.ui.widgets.AirEditText;
import com.aircandi.utilities.Json;

/*
 * We often will get duplicates because the ordering of images isn't
 * guaranteed while paging.
 */
public class PlacePicker extends BaseActivity {

	private PlaceSuggestController	mPlaceSuggest;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
		
		mPlaceSuggest = new PlaceSuggestController(this, new RequestListener() {

			@Override
			public void onComplete(Object response) {
				Place place = (Place) response;
				final Intent intent = new Intent();
				final String jsonEntity = Json.objectToJson(place);
				intent.putExtra(Constants.EXTRA_ENTITY, jsonEntity);
				setResultCode(Activity.RESULT_OK, intent);
				finish();
			}
		});

		setActivityTitle(StringManager.getString(R.string.dialog_place_picker_search_title));
		bind(BindingMode.AUTO);
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Lifecycle
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.place_picker;
	}

	@Override
	public void bind(BindingMode mode) {
		Entity currentPlace = Aircandi.getInstance().getCurrentPlace();
		if (currentPlace != null) {
			((Place) currentPlace).reason = ReasonType.LOCATION;
			((Place) currentPlace).score = 20;
			mPlaceSuggest.getEntitiesInjected().add(currentPlace);
		}
		mPlaceSuggest.setInput((AirEditText) findViewById(R.id.input));
		mPlaceSuggest.setListView((ListView) findViewById(R.id.list));		
		mPlaceSuggest.init();
	}

	// --------------------------------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------------------------------
}