package com.aircandi.ui.helpers;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;

import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.EntityManager;
import com.aircandi.objects.Entity;
import com.aircandi.ui.base.BaseActivity;
import com.aircandi.ui.components.EntitySuggestController;
import com.aircandi.utilities.Json;

public class PlacePicker extends BaseActivity {

	private EntitySuggestController mEntitySuggest;

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	public void onClearButtonClick(View view) {
		setResultCode(Activity.RESULT_OK);
		finish();
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
		mEntitySuggest = new EntitySuggestController(this);
		bind(BindingMode.AUTO);
	}

	@Override
	public void bind(BindingMode mode) {

		mEntitySuggest
				.setSearchInput((EditText) findViewById(R.id.search_input))
				.setSearchImage(findViewById(R.id.search_image))
				.setSearchProgress(findViewById(R.id.search_progress))
				.setListView((ListView) findViewById(R.id.list))
				.setSuggestScope(EntityManager.SuggestScope.PLACES)
				.init();

		((ListView) findViewById(R.id.list)).setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Entity entity = (Entity) mEntitySuggest.getAdapter().getItem(position);
				final Intent intent = new Intent();
				final String json = Json.objectToJson(entity);
				intent.putExtra(Constants.EXTRA_ENTITY, json);
				setResultCode(Activity.RESULT_OK, intent);
				finish();
			}
		});

		draw(null);
	}

	@Override
	protected int getLayoutId() {
		return R.layout.place_picker;
	}
}