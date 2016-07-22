package com.patchr.ui.components;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.arlib.floatingsearchview.FloatingSearchView;
import com.patchr.components.LocationManager;
import com.patchr.components.UserManager;
import com.patchr.model.RealmEntity;
import com.patchr.objects.enums.Suggest;
import com.patchr.service.RestClient;
import com.patchr.ui.widgets.RecipientsCompletionView;
import com.patchr.utilities.Errors;
import com.patchr.utilities.UI;
import com.tokenautocomplete.TokenCompleteTextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rx.Subscription;

public class EntitySuggestController {
	/*
	 * Used by SearchForm and MessageEdit.
	 */
	private static       Integer LIMIT                      = 10;
	private static final int     MESSAGE_TEXT_CHANGED       = 1337;
	private static final int     DEFAULT_AUTOCOMPLETE_DELAY = 750;

	private List<RealmEntity>    entities;
	private RecyclerView.Adapter adapter;
	private Context              context;
	public  RecyclerView         recyclerView;
	public  BusyController       busyPresenter;

	public EditText           searchInput;
	public FloatingSearchView searchView;
	public String             suggestScope;
	public Subscription       subscription;

	private SimpleTextWatcher                   textWatcher;
	private String                              suggestInput;
	private String                              prefix;
	public  TokenCompleteTextView.TokenListener tokenListener;
	public  boolean                             processing;

	private int mAutoCompleteDelay = DEFAULT_AUTOCOMPLETE_DELAY;

	private final Handler handler = new SuggestHandler(this);

	public EntitySuggestController(Context context) {
		initialize();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	public void suggestAction(final String chars) {
		if (!processing) {
			processing = true;
			suggestCall(chars);
		}
	}

	public void onStop() {
		if (subscription != null && !subscription.isUnsubscribed()) {
			subscription.unsubscribe();
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void initialize() {
		this.context = context;
		this.entities = new ArrayList<>();
		this.adapter = new SuggestArrayAdapter(context, this.entities);
		this.suggestScope = Suggest.Patches;
	}

	public void bind() {

		/* Bind to adapter */
		if (searchInput instanceof RecipientsCompletionView) {
			RecipientsCompletionView recipientsField = (RecipientsCompletionView) searchInput;
			recipientsField.allowDuplicates(false);
			recipientsField.setDeletionStyle(TokenCompleteTextView.TokenDeleteStyle.Clear);
			recipientsField.setTokenClickStyle(TokenCompleteTextView.TokenClickStyle.Select);
		}

		if (searchInput instanceof EditText && recyclerView != null) {
			recyclerView.setLayoutManager(new LinearLayoutManager(context));
			recyclerView.setAdapter(adapter);

			final ViewGroup.LayoutParams params = recyclerView.getLayoutParams();
			params.height = 0;
			recyclerView.setLayoutParams(params);

			searchInput.addTextChangedListener(new SimpleTextWatcher() {
				@Override public void afterTextChanged(@NonNull Editable s) {
					textChanged(((RecipientsCompletionView) searchInput).currentCompletionText());
				}
			});
		}
		else if (searchView != null && recyclerView != null) {
			recyclerView.setLayoutManager(new LinearLayoutManager(context));
			recyclerView.setAdapter(adapter);
			searchView.setOnQueryChangeListener(new FloatingSearchView.OnQueryChangeListener() {
				@Override public void onSearchTextChanged(String oldQuery, String newQuery) {
					textChanged(newQuery);
				}
			});
		}
	}

	public void bindDropdown() {
		final ViewGroup.LayoutParams params = recyclerView.getLayoutParams();
		params.height = (int) adapter.getItemCount() * UI.getRawPixelsForDisplayPixels(56f);
		recyclerView.setLayoutParams(params);
		recyclerView.setVisibility(adapter.getItemCount() > 0 ? View.VISIBLE : View.GONE);
	}

	public void textChanged(@NonNull String input) {
		if (!TextUtils.isEmpty(input) && input.length() >= 2) {
			handler.removeMessages(MESSAGE_TEXT_CHANGED);
			handler.sendMessageDelayed(handler.obtainMessage(MESSAGE_TEXT_CHANGED, input), DEFAULT_AUTOCOMPLETE_DELAY);
		}
		else {
			entities.clear();
			adapter.notifyDataSetChanged();
		}
	}

	public void suggestComplete() {
		if (busyPresenter != null) {
			busyPresenter.hide(false);
		}
		if (searchView != null) {
			searchView.hideProgress();
		}
	}

	public void suggestCall(final String chars) {

		if (busyPresenter != null) {
			busyPresenter.show(BusyController.BusyAction.Scanning_Empty);
		}

		if (searchView != null) {
			searchView.showProgress();
		}

		subscription = RestClient.getInstance().suggest(chars.toString().trim()
			, suggestScope
			, UserManager.currentUser.id
			, LocationManager.getInstance().getLocationLocked()
			, LIMIT)
			.subscribe(
				response -> {
					processing = false;
					suggestComplete();
					entities.clear();
					if (response.data != null) {
						final List<RealmEntity> suggestions = (List<RealmEntity>) response.data;
						entities.addAll(suggestions);
						Collections.sort(entities, new SortByScoreAndDistance());
					}
					adapter.notifyDataSetChanged();
					bindDropdown();
				},
				error -> {
					processing = false;
					suggestComplete();
					Errors.handleError(context, error);
				});
	}

	public void clear() {
		this.entities.clear();
		this.bindDropdown();
	}
}
