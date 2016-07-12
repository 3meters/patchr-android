package com.patchr.ui.collections;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.arlib.floatingsearchview.FloatingSearchView;
import com.patchr.Constants;
import com.patchr.R;
import com.patchr.model.Photo;
import com.patchr.model.RealmEntity;
import com.patchr.objects.enums.Suggest;
import com.patchr.objects.enums.TransitionType;
import com.patchr.ui.BaseScreen;
import com.patchr.ui.components.EntitySuggestController;
import com.patchr.utilities.Colors;

public class SearchScreen extends BaseScreen {

	private String             suggestScope;
	private String             searchPhrase;
	private FloatingSearchView searchView;

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
		searchView = (FloatingSearchView) findViewById(R.id.search_view);

		searchView.setSearchHint("Search...");
		searchView.setLeftActionMode(FloatingSearchView.LEFT_ACTION_MODE_SHOW_HOME);
		searchView.setDimBackground(false);
		searchView.setBackgroundColor(Colors.getColor(R.color.transparent));
		searchView.setSearchFocusable(true);
		searchView.setSearchFocused(true);
		searchView.setOnHomeActionClickListener(new FloatingSearchView.OnHomeActionClickListener() {
			@Override public void onHomeClicked() {
				onBackPressed();
			}
		});

		EntitySuggestController entitySuggest = new EntitySuggestController(this);
		entitySuggest.searchView = searchView;
		entitySuggest.recyclerView = recyclerView;
		entitySuggest.suggestScope = this.suggestScope;
		entitySuggest.initialize();

		//getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
	}

	@Override protected int getLayoutId() {
		return R.layout.screen_search;
	}

	@Override protected int getTransitionBack(int transitionType) {
		return TransitionType.FORM_BACK;
	}

	public void bind() { }
}