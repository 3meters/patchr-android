package com.aircandi.ui;

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

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.components.EntityController.SuggestScope;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Route;
import com.aircandi.ui.base.BaseActivity;
import com.aircandi.ui.components.EntitySuggestController;
import com.aircandi.utilities.Json;

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
		setResultCode(Activity.RESULT_OK);
		finish();
		Patchr.getInstance().getAnimationManager().doOverridePendingTransition(this, getExitTransitionType());
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

	@Override
	public void bind(BindingMode mode) {

		if (mSearchView != null) {
			mEntitySuggest
					.setSearchView(mSearchView)
					.setUiController(mUiController)
					.setListView((ListView) findViewById(R.id.list))
					.setSuggestScope(mSuggestScope)
					.init();
		}

		((ListView) findViewById(R.id.list)).setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Entity entity = (Entity) mEntitySuggest.getAdapter().getItem(position);
				final Intent intent = new Intent();
				final String json = Json.objectToJson(entity);
				if (mReturnEntity) {
					intent.putExtra(Constants.EXTRA_ENTITY, json);
					setResultCode(Activity.RESULT_OK, intent);
					finish();
					Patchr.getInstance().getAnimationManager().doOverridePendingTransition(SearchForm.this, getExitTransitionType());
				}
				else {
					Patchr.dispatch.route(SearchForm.this, Route.BROWSE, entity, null);
				}
			}
		});

		if (mQuery != null) {
			mSearchView.setQuery(mQuery, true);
		}

		draw(null);
	}

	public void configureActionBar() {
		super.configureActionBar();
		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayShowTitleEnabled(false);  // Dont show title
		}
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
					setResultCode(Activity.RESULT_CANCELED);
					finish();
					Patchr.getInstance().getAnimationManager().doOverridePendingTransition(SearchForm.this, getExitTransitionType());
					return false;
				}
			});
		}

		bind(BindingMode.AUTO);

		return true;
	}
}