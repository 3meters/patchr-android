package com.patchr.ui.collections;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.view.LayoutInflater;
import android.view.View;

import com.patchr.Constants;
import com.patchr.R;
import com.patchr.components.Logger;
import com.patchr.model.Photo;
import com.patchr.model.RealmEntity;
import com.patchr.objects.QuerySpec;
import com.patchr.objects.enums.FetchMode;
import com.patchr.objects.enums.FetchStrategy;
import com.patchr.service.RestClient;
import com.patchr.ui.BaseScreen;
import com.patchr.ui.views.BaseView;
import com.patchr.ui.widgets.ListWidget;
import com.patchr.utilities.Errors;
import com.patchr.utilities.UI;
import com.patchr.utilities.Utils;

import io.realm.RealmChangeListener;

@SuppressWarnings("ucd")
public class BaseListScreen extends BaseScreen implements AppBarLayout.OnOffsetChangedListener {

	public QuerySpec  querySpec;   /* Required injection by subclass */
	public Integer    headerResId; /* Optional injection by subclass */
	public ListWidget listWidget;

	public Integer topPadding = 0;  // Hack to handle ui tweaking
	public BaseView            header;
	public RealmChangeListener changeListener;

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		bind();                                         // Display header from cache
		listWidget.bind(this.querySpec.name, this.entityId); // Display list from cache
	}

	@Override protected void onStart() {
		super.onStart();
		listWidget.onStart();
	}

	@Override public void onResume() {
		super.onResume();
		fetch(FetchMode.AUTO);  // Check for fresh stuff
		if (this.appBarLayout != null) {
			this.appBarLayout.addOnOffsetChangedListener(this);
		}
	}

	@Override protected void onPause() {
		super.onPause();
		if (this.appBarLayout != null) {
			this.appBarLayout.removeOnOffsetChangedListener(this);
		}
	}

	@Override protected void onStop() {
		super.onStop();
		listWidget.onStop();
	}

	/*--------------------------------------------------------------------------------------------
	 * Notifications
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	public void onClick(View view) {
		if (view.getTag() != null) {
			if (view.getTag() instanceof Photo) {
				Photo photo = (Photo) view.getTag();
				UI.browsePhoto(photo, this);
			}
			else if (view.getTag() instanceof RealmEntity) {
				final RealmEntity entity = (RealmEntity) view.getTag();
				UI.browseEntity(entity.id, this);
			}
		}
	}

	@Override public void onOffsetChanged(AppBarLayout appBarLayout, int i) {
		if (this.listWidget.swipeRefresh != null) {
			this.listWidget.swipeRefresh.setEnabled(i == 0);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void unpackIntent() {
		super.unpackIntent();
		/*
		 * Unpack all the inputs.
		 */
		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			String queryName = extras.getString(Constants.EXTRA_QUERY_NAME);
			if (queryName != null) {
				this.querySpec = QuerySpec.Factory(queryName);
			}
		}
	}

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
		/* Call triggered by BaseScreen */

		Utils.guard(this.querySpec != null, "Query cannot be null");

		/* UI tweak hack */
		if (querySpec.listItemResId != null && querySpec.listItemResId == R.layout.listitem_patch) {
			topPadding = UI.getRawPixelsForDisplayPixels(6f);
		}

		if (querySpec.listTitleResId != null) {
			actionBarTitle.setText(querySpec.listTitleResId);
		}

		listWidget = (ListWidget) findViewById(R.id.list_view);
		if (listWidget != null) {
			listWidget.setRealm(realm);
			listWidget.listGroup.setPadding(0, this.topPadding, 0, 0);
		}
		if (headerResId != null) {
			View header = LayoutInflater.from(this).inflate(headerResId, null, false);
		}
		if (header != null && listWidget != null) {
			listWidget.setHeader(header);
		}
	}

	public void bind() {

		if (this.header != null) {
			this.entity = realm.where(RealmEntity.class).equalTo("id", this.entityId).findFirst();
			if (this.entity != null) {
				supportInvalidateOptionsMenu();     // In case user authenticated
				this.header.bind(this.entity);
				this.changeListener = new RealmChangeListener() {
					@Override public void onChange(Object element) {
						header.draw();
						supportInvalidateOptionsMenu();     // In case user authenticated
					}
				};
				this.entity.addChangeListener(this.changeListener);
			}
		}
	}

	public void fetch(final FetchMode mode) {
		fetch(mode, false);
	}

	public void fetch(final FetchMode mode, Boolean headerOnly) {

		if (header == null) {
			listWidget.fetch(mode);
		}
		else {
			Logger.v(this, "Fetching form entity: " + mode.name().toString());
			FetchStrategy strategy = (mode == FetchMode.MANUAL) ? FetchStrategy.IgnoreCache : FetchStrategy.UseCacheAndVerify;

			AsyncTask.execute(() -> {

				if (!headerOnly && entity != null) {
					/* We can do both in parallel */
					listWidget.fetch(mode);
				}

				subscription = RestClient.getInstance().fetchEntity(entityId, strategy)
					.subscribe(
						response -> {
							processing = false;
							busyController.hide(true);
							if (entity == null) {
								bind();
								if (!headerOnly) {
									listWidget.fetch(mode); // Has it's own processing flag
								}
							}
							supportInvalidateOptionsMenu();     // In case user authenticated
						},
						error -> {
							processing = false;
							busyController.hide(true);
							Errors.handleError(this, error);
						});
			});
		}
	}

	public void draw() {
		listWidget.draw();
		if (header != null) {
			header.invalidate();
		}
	}

	@Override protected int getLayoutId() {
		return R.layout.screen_list;
	}
}