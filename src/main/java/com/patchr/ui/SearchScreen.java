package com.patchr.ui;

import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import com.patchr.Constants;
import com.patchr.R;
import com.patchr.components.DataController.SuggestScope;
import com.patchr.objects.Entity;
import com.patchr.objects.Photo;
import com.patchr.ui.components.EntitySuggestController;

public class SearchScreen extends BaseScreen {

	private EntitySuggestController entitySuggest;
	private SuggestScope            suggestScope;
	private SearchView              searchView;
	private String                  searchPhrase;
	private RecyclerView            listView;

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	public void onClick(View view) {
		Integer id = view.getId();

		if (view.getTag() != null) {
			if (view.getTag() instanceof Photo) {
				Photo photo = (Photo) view.getTag();
				navigateToPhoto(photo);
			}
			else if (view.getTag() instanceof Entity) {
				final Entity entity = (Entity) view.getTag();
				navigateToEntity(entity);
			}
		}
	}

	@Override public boolean onCreateOptionsMenu(Menu menu) {
		/*
		 * This is the best event to do the work of setting up the search stuff.
		 */
		this.optionMenu = menu;
		getMenuInflater().inflate(R.menu.menu_search_view, menu);
		bind();
		return true;
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			searchPhrase = extras.getString(Constants.EXTRA_SEARCH_PHRASE);
			suggestScope = SuggestScope.values()[extras.getInt(Constants.EXTRA_SEARCH_SCOPE, SuggestScope.PATCHES.ordinal())];
		}
	}

	@Override public void initialize(Bundle savedInstanceState) {

		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
		actionBar.setDisplayShowTitleEnabled(false);
		entitySuggest = new EntitySuggestController(this);
	}

	@Override protected int getLayoutId() {
		return R.layout.search_screen;
	}

	public void bind() {

		final MenuItem searchItem = this.optionMenu.findItem(R.id.search_view);

		if (searchItem != null) {

			searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
			searchView.setIconified(false);
			searchView.setOnCloseListener(new SearchView.OnCloseListener() {

				@Override public boolean onClose() {
					cancelAction(false);
					return false;
				}
			});
		}

		listView = (RecyclerView) findViewById(R.id.results_list);

		entitySuggest.searchView = this.searchView;
		entitySuggest.busyPresenter = this.busyPresenter;
		entitySuggest.listView = this.listView;
		entitySuggest.suggestScope = this.suggestScope;
		entitySuggest.initialize();

		if (searchPhrase != null) {
			searchView.setQuery(searchPhrase, true);
		}
	}
}