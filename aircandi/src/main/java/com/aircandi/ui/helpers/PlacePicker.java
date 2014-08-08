package com.aircandi.ui.helpers;

import android.os.Bundle;
import android.view.WindowManager;

import com.aircandi.Aircandi;
import com.aircandi.R;
import com.aircandi.components.StringManager;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Place;
import com.aircandi.objects.Place.ReasonType;
import com.aircandi.ui.base.BaseActivity;
import com.aircandi.ui.components.EntitySuggestController;
import com.aircandi.ui.widgets.AirTokenCompleteTextView;

/*
 * We often will get duplicates because the ordering of images isn't
 * guaranteed while paging.
 */
public class PlacePicker extends BaseActivity {

	private EntitySuggestController  mEntitySuggest;
	private AirTokenCompleteTextView mTo;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

		mTo = (AirTokenCompleteTextView) this.findViewById(R.id.to);
		mEntitySuggest = new EntitySuggestController(this);

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
			mEntitySuggest.getSeedEntities().add(currentPlace);
		}
		mEntitySuggest.setInput((AirTokenCompleteTextView) findViewById(R.id.to));
		mEntitySuggest.init();
	}
}