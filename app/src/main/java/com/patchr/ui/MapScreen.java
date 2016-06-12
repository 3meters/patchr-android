package com.patchr.ui;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.patchr.Constants;
import com.patchr.R;
import com.patchr.components.AndroidManager;
import com.patchr.components.MapManager;
import com.patchr.objects.AirLocation;
import com.patchr.objects.Entity;
import com.patchr.ui.fragments.MapListFragment;
import com.patchr.utilities.Json;

import java.util.ArrayList;
import java.util.List;

public class MapScreen extends BaseScreen {

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		bind();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public boolean onCreateOptionsMenu(Menu menu) {

		AirLocation location = entity.getLocation();
		if (location != null) {
			getMenuInflater().inflate(R.menu.menu_navigate, menu);
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override public boolean onOptionsItemSelected(MenuItem item) {

		if (item.getItemId() == R.id.navigate) {
			AirLocation location = entity.getLocation();
			if (location == null) {
				throw new IllegalArgumentException("Tried to navigate without a location");
			}

			AndroidManager.getInstance().callMapNavigation(this
					, location.lat.doubleValue()
					, location.lng.doubleValue()
					, null
					, entity.name);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			final String jsonEntity = extras.getString(Constants.EXTRA_ENTITY);
			if (jsonEntity != null) {
				entity = (Entity) Json.jsonToObject(jsonEntity, Json.ObjectType.ENTITY);
			}
		}
	}

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		List<Entity> entities = new ArrayList<Entity>();
		entities.add(entity);

		MapListFragment fragment = new MapListFragment();

		fragment.entities = entities;
		fragment.zoomLevel = MapManager.ZOOM_SCALE_NEARBY;

		currentFragment = fragment;

		getSupportFragmentManager()
				.beginTransaction()
				.replace(R.id.fragment_holder, currentFragment)
				.commit();
	}

	@Override protected int getLayoutId() {
		return R.layout.screen_map;
	}

	public void bind() {
		((MapListFragment) currentFragment).bind();
	}
}