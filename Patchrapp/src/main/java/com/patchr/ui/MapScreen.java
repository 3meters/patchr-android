package com.patchr.ui;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.patchr.Constants;
import com.patchr.R;
import com.patchr.components.AndroidManager;
import com.patchr.model.Location;
import com.patchr.model.RealmEntity;
import com.patchr.ui.fragments.MapListFragment;

import io.realm.RealmList;

public class MapScreen extends BaseScreen {

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		bind();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public boolean onCreateOptionsMenu(Menu menu) {

		Location location = entity.getLocation();
		if (location != null) {
			getMenuInflater().inflate(R.menu.menu_navigate, menu);
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override public boolean onOptionsItemSelected(MenuItem item) {

		if (item.getItemId() == R.id.navigate) {
			Location location = entity.getLocation();
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

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		MapListFragment fragment = new MapListFragment();
		fragment.zoomLevel = Constants.ZOOM_SCALE_NEARBY;
		currentFragment = fragment;

		getSupportFragmentManager()
			.beginTransaction()
			.replace(R.id.fragment_holder, currentFragment)
			.commit();
	}

	public void bind() {
		this.actionBarTitle.setText(R.string.screen_title_map_form);
		this.entity = realm.where(RealmEntity.class).equalTo("id", this.entityId).findFirst();
		if (this.entity != null) {
			RealmList<RealmEntity> entities = new RealmList<RealmEntity>();
			entities.add(entity);
			MapListFragment fragment = (MapListFragment) currentFragment;
			fragment.entities = entities;
			fragment.bind();
		}
	}

	@Override protected int getLayoutId() {
		return R.layout.screen_map;
	}
}