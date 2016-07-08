package com.patchr.ui.components;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.Editable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.patchr.Constants;
import com.patchr.R;
import com.patchr.components.DataController;
import com.patchr.components.LocationManager;
import com.patchr.components.ModelResult;
import com.patchr.components.NetworkManager;
import com.patchr.components.UserManager;
import com.patchr.objects.Entity;
import com.patchr.objects.Suggest;
import com.patchr.ui.widgets.RecipientsCompletionView;
import com.patchr.utilities.UI;
import com.tokenautocomplete.TokenCompleteTextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class EntitySuggestController implements SearchView.OnQueryTextListener {
	/*
	 * Used by SearchForm and MessageEdit.
	 */
	private static       Integer LIMIT                      = 10;
	private static final int     MESSAGE_TEXT_CHANGED       = 1337;
	private static final int     DEFAULT_AUTOCOMPLETE_DELAY = 750;

	private List<Entity>         entities;
	private RecyclerView.Adapter adapter;
	private Context              context;
	public  RecyclerView         recyclerView;
	public  BusyController       busyPresenter;

	public  EditText   searchInput;
	public  SearchView searchView;
	private View       searchProgress;
	private View       searchImage;
	private boolean    suggestInProgress;
	public  String     suggestScope;

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

	@Override public boolean onQueryTextSubmit(String query) {
		textChanged(query);
		return false;
	}

	@Override public boolean onQueryTextChange(String newText) {
		textChanged(newText);
		return false;
	}

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
			searchView.setOnQueryTextListener(this);
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
			new AsyncTask() {

				@Override protected void onPreExecute() {
					if (busyPresenter != null) {
						busyPresenter.show(BusyController.BusyAction.Scanning_Empty);
					}

					if (searchProgress != null) {
						searchImage.setVisibility(View.INVISIBLE);
						searchProgress.setVisibility(View.VISIBLE);
					}
				}

				@Override protected Object doInBackground(Object... params) {
					Thread.currentThread().setName("AsyncSuggestQuery");

					ModelResult result = DataController.getInstance().suggest(chars.toString().trim()
							, suggestScope
							, UserManager.shared().authenticated() ? UserManager.currentUser.id : null
							, LocationManager.getInstance().getLocationLocked()
							, LIMIT, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);

					return result;
				}

				@Override protected void onPostExecute(Object response) {
					final ModelResult result = (ModelResult) response;

					if (busyPresenter != null) {
						busyPresenter.hide(false);
					}

					if (searchProgress != null) {
						searchImage.setVisibility(View.VISIBLE);
						searchProgress.setVisibility(View.INVISIBLE);
					}

					if (result.serviceResponse.responseCode == NetworkManager.ResponseCode.SUCCESS) {
						final List<Entity> suggestions = (List<Entity>) result.data;
						entities.clear();
						entities.addAll(suggestions);
						Collections.sort(entities, new SortByScoreAndDistance());
						adapter.notifyDataSetChanged();
						bindDropdown();
					}

					suggestInProgress = false;
				}
			}.executeOnExecutor(Constants.EXECUTOR);
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

		private List<Entity>   entities;
		private LayoutInflater inflater;

		private SuggestArrayAdapter(List<Entity> entities) {
			this.entities = entities;
			this.inflater = LayoutInflater.from(context);
		}

		@Override public RealmRecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View view = inflater.inflate(R.layout.listitem_search, parent, false);
			return new RealmRecyclerViewHolder(view);
		}

		@Override public void onBindViewHolder(RealmRecyclerViewHolder holder, int position) {
			Entity entity = this.entities.get(position);
			//holder.bind(entity);
		}

		@Override public int getItemCount() {
			return this.entities.size();
		}
	}

	public static class SortByScoreAndDistance implements Comparator<Entity> {

		@Override public int compare(@NonNull Entity object1, @NonNull Entity object2) {

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
