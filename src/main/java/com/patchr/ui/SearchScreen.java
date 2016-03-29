package com.patchr.ui;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
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

	private EntitySuggestController entitySuggest;
	private String                  suggestScope;
	private SearchView              searchView;
	private String                  searchPhrase;
	private RecyclerView            listView;

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

		this.listView = (RecyclerView) findViewById(R.id.results_list);

		this.searchView = new SearchView(this);
		this.searchView.setIconified(false);
		this.searchView.setFocusable(true);
		this.searchView.requestFocusFromTouch();
		this.searchView.setOnCloseListener(new SearchView.OnCloseListener() {

			@Override public boolean onClose() {
				cancelAction(false);
				return false;
			}
		});
		this.actionBar.setCustomView(this.searchView);
		this.actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		this.actionBar.setDisplayShowTitleEnabled(false);

		this.entitySuggest = new EntitySuggestController(this);
		this.entitySuggest.searchView = this.searchView;
		this.entitySuggest.busyPresenter = this.busyPresenter;
		this.entitySuggest.listView = this.listView;
		this.entitySuggest.suggestScope = this.suggestScope;
		this.entitySuggest.initialize();

		if (this.searchPhrase != null) {
			this.searchView.setQuery(this.searchPhrase, true);
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