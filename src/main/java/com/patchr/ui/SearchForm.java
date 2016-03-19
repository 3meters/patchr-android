package com.patchr.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.AnimationManager;
import com.patchr.components.DataController.SuggestScope;
import com.patchr.objects.FetchMode;
import com.patchr.objects.Entity;
import com.patchr.objects.Route;
import com.patchr.objects.TransitionType;
import com.patchr.ui.components.EntitySuggestController;
import com.patchr.utilities.Json;

public class SearchForm extends BaseActivity {

	private EntitySuggestController mEntitySuggest;
	private Boolean mReturnEntity    = false;
	private Boolean mShowClearButton = false;
	private String mClearButtonText;
	private SuggestScope mSuggestScope = SuggestScope.PATCHES;
	private SearchView   mSearchView   = null;
	private String mQuery;

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	public void onClearButtonClick(View view) {
		setResult(Activity.RESULT_OK);
		finish();
		AnimationManager.doOverridePendingTransition(this, TransitionType.FORM_BACK);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mSuggestScope = SuggestScope.values()[extras.getInt(Constants.EXTRA_SEARCH_SCOPE, SuggestScope.PATCHES.ordinal())];
			mReturnEntity = extras.getBoolean(Constants.EXTRA_SEARCH_RETURN_ENTITY, false);
			mShowClearButton = extras.getBoolean(Constants.EXTRA_SEARCH_CLEAR_BUTTON, false);
			mClearButtonText = extras.getString(Constants.EXTRA_SEARCH_CLEAR_BUTTON_MESSAGE);
			mQuery = extras.getString(Constants.EXTRA_SEARCH_PHRASE);
		}
	}

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
		mEntitySuggest = new EntitySuggestController(this);
		if (mShowClearButton) {
			Button button = (Button) findViewById(R.id.button_clear);
			View holder = findViewById(R.id.holder_button_clear);
			if (button != null) {
				button.setText(mClearButtonText);
				holder.setVisibility(View.VISIBLE);
			}
		}
	}

	public void bind(FetchMode mode) {

		if (mSearchView != null) {
			mEntitySuggest
					.setSearchView(mSearchView)
					.setUiController(uiController)
					.setListView((ListView) findViewById(R.id.entity_list))
					.setSuggestScope(mSuggestScope)
					.init();
		}

		((ListView) findViewById(R.id.entity_list)).setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Entity entity = (Entity) mEntitySuggest.getAdapter().getItem(position);
				final Intent intent = new Intent();
				final String json = Json.objectToJson(entity);
				if (mReturnEntity) {
					intent.putExtra(Constants.EXTRA_ENTITY, json);
					setResult(Activity.RESULT_OK, intent);
					finish();
					AnimationManager.doOverridePendingTransition(SearchForm.this, TransitionType.FORM_BACK);
				}
				else {
					Patchr.router.route(SearchForm.this, Route.BROWSE, entity, null);
				}
			}
		});

		if (mQuery != null) {
			mSearchView.setQuery(mQuery, true);
		}
	}

	public void configureActionBar() {
		actionBar.setDisplayShowTitleEnabled(false);
	}

	@Override
	protected int getLayoutId() {
		return R.layout.search_form;
	}

	/*--------------------------------------------------------------------------------------------
	 * Menus
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		/*
		 * This is the best event to do the work of setting up the search stuff.
		 */
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.menu_search_view, menu);
		final MenuItem searchItem = menu.findItem(R.id.search_view);

		if (searchItem != null) {
			final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
			mSearchView = searchView;
			mSearchView.setIconified(false);

			mSearchView.setOnCloseListener(new SearchView.OnCloseListener() {
				@Override
				public boolean onClose() {
					setResult(Activity.RESULT_CANCELED);
					finish();
					AnimationManager.doOverridePendingTransition(SearchForm.this, TransitionType.FORM_BACK);
					return false;
				}
			});
		}

		bind(FetchMode.AUTO);

		return true;
	}
}