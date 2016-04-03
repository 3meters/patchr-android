package com.patchr.ui;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.View;
import android.view.WindowManager;

import com.patchr.Constants;
import com.patchr.R;
import com.patchr.components.DataController;
import com.patchr.objects.Entity;
import com.patchr.objects.Photo;
import com.patchr.objects.TransitionType;
import com.patchr.ui.components.EntitySuggestController;

public class SearchScreen extends BaseScreen {

	private String                  suggestScope;
	private String                  searchPhrase;

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		bind();
	}

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

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			searchPhrase = extras.getString(Constants.EXTRA_SEARCH_PHRASE);
			suggestScope = extras.getString(Constants.EXTRA_SEARCH_SCOPE, DataController.Suggest.Patches);
		}
	}

	@Override public void initialize(Bundle savedInstanceState) {

		RecyclerView listView = (RecyclerView) findViewById(R.id.results_list);

		SearchView searchView = new SearchView(this);
		searchView.setIconified(false);
		searchView.setQueryHint("Search...");
		searchView.setFocusable(true);
		searchView.requestFocusFromTouch();
		searchView.setOnCloseListener(new SearchView.OnCloseListener() {

			@Override public boolean onClose() {
				cancelAction(false);
				return false;
			}
		});
		this.actionBar.setCustomView(searchView);
		this.actionBar.setDisplayShowCustomEnabled(true);
		this.actionBar.setDisplayShowHomeEnabled(true);
		this.actionBar.setDefaultDisplayHomeAsUpEnabled(true);
		this.actionBar.setDisplayShowTitleEnabled(false);

		EntitySuggestController entitySuggest = new EntitySuggestController(this);
		entitySuggest.searchView = searchView;
		entitySuggest.busyPresenter = this.busyPresenter;
		entitySuggest.listView = listView;
		entitySuggest.suggestScope = this.suggestScope;
		entitySuggest.initialize();

		if (this.searchPhrase != null) {
			searchView.setQuery(this.searchPhrase, true);
		}

		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
	}

	@Override protected int getLayoutId() {
		return R.layout.screen_search;
	}

	@Override protected int getTransitionBack(int transitionType) {
		return TransitionType.VIEW_BACK;
	}

	public void bind() { }
}