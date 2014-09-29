package com.aircandi.ui.helpers;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.StringManager;
import com.aircandi.objects.Place;
import com.aircandi.ui.base.BaseEntityEdit;
import com.aircandi.utilities.Json;
import com.aircandi.utilities.Type;

public class AddressBuilder extends BaseEntityEdit {

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		((EditText) findViewById(R.id.phone)).setImeOptions(EditorInfo.IME_ACTION_DONE);
		((EditText) findViewById(R.id.phone)).setOnEditorActionListener(new OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					onAccept();
					return true;
				}
				return false;
			}
		});
	}

	@Override
	public void draw(View view) {

		Place place = (Place) mEntity;

		if (place.address != null) {
			((EditText) findViewById(R.id.address)).setText(place.address);
		}
		if (place.city != null) {
			((EditText) findViewById(R.id.city)).setText(place.city);
		}
		if (place.region != null) {
			((EditText) findViewById(R.id.state)).setText(place.region);
		}
		if (place.postalCode != null) {
			((EditText) findViewById(R.id.zip_code)).setText(place.postalCode);
		}
		if (place.phone != null) {
			((EditText) findViewById(R.id.phone)).setText(place.phone);
		}
		setActivityTitle(StringManager.getString(R.string.label_about_title));
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected void gather() {
		Place place = (Place) mEntity;
		place.phone = Type.emptyAsNull(((EditText) findViewById(R.id.phone)).getEditableText().toString());
		place.address = Type.emptyAsNull(((EditText) findViewById(R.id.address)).getEditableText().toString());
		place.city = Type.emptyAsNull(((EditText) findViewById(R.id.city)).getEditableText().toString());
		place.region = Type.emptyAsNull(((EditText) findViewById(R.id.state)).getEditableText().toString());
		place.postalCode = Type.emptyAsNull(((EditText) findViewById(R.id.zip_code)).getEditableText().toString());
	}

	@Override
	public void onAccept() {
		gather();
		save();
	}

	/*--------------------------------------------------------------------------------------------
	 * Services
	 *--------------------------------------------------------------------------------------------*/

	private void save() {
		final Intent intent = new Intent();
		if (mEntity != null) {
			final String jsonEntity = Json.objectToJson(mEntity);
			intent.putExtra(Constants.EXTRA_PLACE, jsonEntity);
		}
		setResultCode(Activity.RESULT_OK, intent);
		finish();
	}

	/*--------------------------------------------------------------------------------------------
	 * Misc
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected int getLayoutId() {
		return R.layout.address_builder;
	}
}