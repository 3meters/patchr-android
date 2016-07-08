package com.patchr.ui.collections;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.View;
import android.view.WindowManager;

import com.patchr.Constants;
import com.patchr.R;
import com.patchr.model.Photo;
import com.patchr.model.RealmEntity;
import com.patchr.objects.Suggest;
import com.patchr.objects.TransitionType;
import com.patchr.ui.BaseScreen;
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
		if (view.getTag() != null) {
			if (view.getTag() instanceof Photo) {
				Photo photo = (Photo) view.getTag();
				navigateToPhoto(photo);
			}
			else if (view.getTag() instanceof RealmEntity) {
				final RealmEntity entity = (RealmEntity) view.getTag();
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
			suggestScope = extras.getString(Constants.EXTRA_SEARCH_SCOPE, Suggest.Patches);
		}
	}

	@Override public void initialize(Bundle savedInstanceState) {

		RecyclerView recyclerView = (RecyclerView) findViewById(R.id.results_list);

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
		entitySuggest.busyPresenter = this.busyController;
		entitySuggest.recyclerView = recyclerView;
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