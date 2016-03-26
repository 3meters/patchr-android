package com.patchr.ui.components;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.Editable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.DataController;
import com.patchr.components.LocationManager;
import com.patchr.components.ModelResult;
import com.patchr.components.NetworkManager;
import com.patchr.components.StringManager;
import com.patchr.components.UserManager;
import com.patchr.objects.Entity;
import com.patchr.ui.widgets.AirTokenCompleteTextView;
import com.patchr.ui.widgets.TokenCompleteTextView;
import com.patchr.utilities.Json;
import com.patchr.utilities.Reporting;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class EntitySuggestController implements SearchView.OnQueryTextListener, TokenCompleteTextView.TokenListener {
	/*
	 * Used by SearchForm and MessageEdit.
	 */
	private static Integer LIMIT = 10;

	private List<Entity>         entities;
	private RecyclerView.Adapter adapter;
	private Context              context;
	public  RecyclerView         listView;
	public  BusyPresenter        busyPresenter;

	public  EditText   searchInput;
	public  SearchView searchView;
	private View       searchProgress;
	private View       searchImage;
	private boolean    suggestInProgress;

	private Integer                             watchResId;
	private Integer                             locationResId;
	private Integer                             userResId;
	private SimpleTextWatcher                   textWatcher;
	private String                              suggestInput;
	private String                              prefix;
	public  TokenCompleteTextView.TokenListener tokenListener;

	public DataController.SuggestScope suggestScope;

	public EntitySuggestController(Context context) {

		this.context = context;
		this.entities = new ArrayList<>();
		this.adapter = new SuggestArrayAdapter(this.entities);
		this.suggestScope = DataController.SuggestScope.PATCHES;

		final TypedValue resourceName = new TypedValue();
		if (context.getTheme().resolveAttribute(R.attr.iconWatch, resourceName, true)) {
			watchResId = resourceName.resourceId;
		}
		if (context.getTheme().resolveAttribute(R.attr.iconLocation, resourceName, true)) {
			locationResId = resourceName.resourceId;
		}
		if (context.getTheme().resolveAttribute(R.attr.iconUser, resourceName, true)) {
			userResId = resourceName.resourceId;
		}
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

	@Override public void onTokenAdded(Object o) {
		Entity entity = (Entity) o;

		/* Add patch to auto complete array */
		try {
			org.json.JSONObject jsonSearchMap = new org.json.JSONObject(Patchr.settings.getString(
					StringManager.getString(R.string.setting_patch_searches), "{}"));
			final String jsonEntity = Json.objectToJson(entity);
			jsonSearchMap.put(entity.id, jsonEntity);
			Patchr.settingsEditor.putString(StringManager.getString(R.string.setting_patch_searches), jsonSearchMap.toString());
			Patchr.settingsEditor.commit();
		}
		catch (JSONException e) {
			Reporting.logException(e);
		}
	}

	@Override public void onTokenRemoved(Object o) {}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void initialize() {

		/* Bind to adapter */
		if (searchInput instanceof AirTokenCompleteTextView) {
			AirTokenCompleteTextView input = (AirTokenCompleteTextView) searchInput;
			input.allowDuplicates(false);
			input.setDeletionStyle(TokenCompleteTextView.TokenDeleteStyle.Clear);
			input.setTokenClickStyle(TokenCompleteTextView.TokenClickStyle.Select);
			input.setTokenListener(tokenListener != null ? tokenListener : this);
		}

		if (searchInput instanceof EditText && listView != null) {
			((RecyclerView) listView).setAdapter(adapter);
			searchInput.addTextChangedListener(new SimpleTextWatcher() {
				@Override public void afterTextChanged(@NonNull Editable s) {
					textChanged(s.toString());
				}
			});
		}
		else if (searchView != null && listView != null) {
			listView.setLayoutManager(new LinearLayoutManager(context));
			listView.setAdapter(adapter);
			searchView.setOnQueryTextListener(this);
		}
	}

	public void textChanged(@NonNull String input) {
		if (!TextUtils.isEmpty(input) && input.length() >= 3) {
			suggestCall(input);
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
						busyPresenter.startProgressBar();
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
							, LocationManager.getInstance().getAirLocationLocked()
							, LIMIT, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);

					return result;
				}

				@Override protected void onPostExecute(Object response) {
					final ModelResult result = (ModelResult) response;

					if (busyPresenter != null) {
						busyPresenter.stopProgressBar();
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
					}

					suggestInProgress = false;
				}
			}.executeOnExecutor(Constants.EXECUTOR);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	private class SuggestArrayAdapter extends RecyclerView.Adapter<ViewHolder> {

		private List<Entity>   entities;
		private LayoutInflater inflater;

		private SuggestArrayAdapter(List<Entity> entities) {
			this.entities = entities;
			this.inflater = LayoutInflater.from(context);
		}

		@Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View view = inflater.inflate(R.layout.listitem_search, parent, false);
			return new ViewHolder(view);
		}

		@Override public void onBindViewHolder(ViewHolder holder, int position) {
			Entity entity = this.entities.get(position);
			holder.bind(entity);
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
}
