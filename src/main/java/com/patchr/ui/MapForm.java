package com.patchr.ui;

import android.os.Bundle;

import com.patchr.Constants;
import com.patchr.R;
import com.patchr.components.MapManager;
import com.patchr.objects.Entity;
import com.patchr.ui.base.BaseActivity;
import com.patchr.utilities.Json;

import java.util.ArrayList;
import java.util.List;

public class MapForm extends BaseActivity {

	@Override
	public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			final String jsonEntity = extras.getString(Constants.EXTRA_ENTITY);
			if (jsonEntity != null) {
				mEntity = (Entity) Json.jsonToObject(jsonEntity, Json.ObjectType.ENTITY);
			}
		}
	}

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		List<Entity> entities = new ArrayList<Entity>();
		entities.add(mEntity);

		mCurrentFragment = new MapListFragment();

		((MapListFragment) mCurrentFragment)
				.setEntities(entities)
				.setZoomLevel(MapManager.ZOOM_SCALE_NEARBY)
				.draw();

		getSupportFragmentManager()
				.beginTransaction()
				.replace(R.id.fragment_holder, mCurrentFragment)
				.commit();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected int getLayoutId() {
		return R.layout.map_form;
	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/
}