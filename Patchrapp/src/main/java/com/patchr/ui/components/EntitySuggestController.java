package com.patchr.ui.components;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.arlib.floatingsearchview.FloatingSearchView;
import com.patchr.R;
import com.patchr.components.LocationManager;
import com.patchr.components.Logger;
import com.patchr.components.UserManager;
import com.patchr.model.RealmEntity;
import com.patchr.objects.Suggest;
import com.patchr.service.RestClient;
import com.patchr.ui.widgets.RecipientsCompletionView;
import com.patchr.utilities.UI;
import com.tokenautocomplete.TokenCompleteTextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

	public  EditText           searchInput;
	public  FloatingSearchView searchView;
	private boolean            suggestInProgress;
	public  String             suggestScope;
	public  Subscription       subscription;

	private SimpleTextWatcher                   textWatcher;
	private String                              suggestInput;
	private String                              prefix;
	public  TokenCompleteTextView.TokenListener tokenListener;

	private int mAutoCompleteDelay = DEFAULT_AUTOCOMPLETE_DELAY;

	private final Handler handler = new SuggestHandler(this);

	public EntitySuggestController(Context context) {

		this.context = context;
		this.entities = new ArrayList<>();
		this.adapter = new SuggestArrayAdapter(this.entities);
		this.suggestScope = Suggest.Patches;
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void initialize() {

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

	public void suggestCall(final String chars) {

		if (!suggestInProgress) {
			suggestInProgress = true;

			if (busyPresenter != null) {
				busyPresenter.show(BusyController.BusyAction.Scanning_Empty);
			}

			if (searchView != null) {
				searchView.showProgress();
			}

			AsyncTask.execute(() -> {
				this.subscription = RestClient.getInstance().suggest(chars.toString().trim()
					, suggestScope
					, UserManager.shared().authenticated() ? UserManager.currentUser.id : null
					, LocationManager.getInstance().getLocationLocked()
					, LIMIT)
					.doOnTerminate(() -> {
						if (busyPresenter != null) {
							busyPresenter.hide(false);
						}
						if (searchView != null) {
							searchView.hideProgress();
						}
					})
					.subscribe(
						response -> {
							suggestInProgress = false;
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
							Logger.e(this, error.getLocalizedMessage());
						});
			});
		}
	}

	public void clear() {
		this.entities.clear();
		this.bindDropdown();
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	private class SuggestArrayAdapter extends RecyclerView.Adapter<RealmRecyclerViewHolder> {

		private List<RealmEntity> entities;
		private LayoutInflater    inflater;

		private SuggestArrayAdapter(List<RealmEntity> entities) {
			this.entities = entities;
			this.inflater = LayoutInflater.from(context);
		}

		@Override
		public RealmRecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View view = inflater.inflate(R.layout.listitem_search, parent, false);
			return new RealmRecyclerViewHolder(view);
		}

		@Override public void onBindViewHolder(RealmRecyclerViewHolder holder, int position) {
			RealmEntity entity = this.entities.get(position);
			holder.bind(entity);
		}

		@Override public int getItemCount() {
			return this.entities.size();
		}
	}

	public static class SortByScoreAndDistance implements Comparator<RealmEntity> {

		@Override public int compare(@NonNull RealmEntity object1, @NonNull RealmEntity object2) {

			if (object1.score.floatValue() > object2.score.floatValue())
				return -1;
			else if (object2.score.floatValue() < object1.score.floatValue())
				return 1;
			else {
				if (object1.distance == null || object2.distance == null)
					return 0;
				else if (object1.distance < object2.distance.intValue())
					return -1;
				else if (object1.distance.intValue() > object2.distance.intValue())
					return 1;
				else
					return 0;
			}
		}
	}

	private static class SuggestHandler extends Handler {
		private final WeakReference<EntitySuggestController> suggestController;

		private SuggestHandler(EntitySuggestController suggestController) {
			this.suggestController = new WeakReference<EntitySuggestController>(suggestController);
		}

		@Override public void handleMessage(Message msg) {
			EntitySuggestController suggestController = this.suggestController.get();
			if (suggestController != null) {
				suggestController.suggestCall((String) msg.obj);
			}
		}
	}
}
