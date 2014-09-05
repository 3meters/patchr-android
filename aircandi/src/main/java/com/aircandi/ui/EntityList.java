package com.aircandi.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.EntityManager;
import com.aircandi.controllers.IEntityController;
import com.aircandi.events.MessageEvent;
import com.aircandi.monitors.EntityMonitor;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Route;
import com.aircandi.queries.EntitiesQuery;
import com.aircandi.ui.base.BaseActivity;
import com.squareup.otto.Subscribe;

public class EntityList extends BaseActivity {

	private EntityListFragment mListFragment;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mParams.setExtras(extras);
			mEntity = EntityManager.getCacheEntity(mParams.getEntityId());
		}
	}

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
		if (this.isFinishing()) return;

		mListFragment = new EntityListFragment();

		EntityMonitor monitor = new EntityMonitor(mParams.getEntityId());

		EntitiesQuery query = new EntitiesQuery();
		query.setEntityId(mParams.getEntityId())
		     .setLinkDirection(mParams.getListLinkDirection())
		     .setLinkType(mParams.getListLinkType())
		     .setPageSize(mParams.getListPageSize())
		     .setSchema(mParams.getListLinkSchema());

		mListFragment.setQuery(query)
		             .setMonitor(monitor)
		             .setListItemResId(mParams.getListItemResId())
		             .setListViewType(mParams.getListViewType())
		             .setListButtonMessageResId(mParams.getListNewMessageResId())
		             .setListLayoutResId(mParams.getListLayoutResId())
		             .setListLoadingResId(mParams.getListLoadingResId())
		             .setSelfBindingEnabled(true);

		getSupportFragmentManager().beginTransaction().replace(R.id.fragment_holder, mListFragment).commit();
	}

	@Override
	protected void configureActionBar() {
		super.configureActionBar();
		/*
		 * Navigation setup for action bar icon and title
		 */
		IEntityController controller = Aircandi.getInstance().getControllerForSchema(mParams.getListLinkSchema());
		Drawable icon = controller.getIcon();
		mActionBar.setIcon(icon);
		setActivityTitle((mParams.getListTitle() != null) ? mParams.getListTitle() : controller.getName(true));
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe
	@SuppressWarnings("ucd")
	public void onMessage(final MessageEvent event) {

		/*
		 * Refresh the form because something new has been added to it
		 * like a comment or post.
		 */
		if (event.message.action.toEntity != null
				&& mParams.getEntityId().equals(event.message.action.toEntity.id)) {

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					onRefresh();
				}
			});
		}
	}

	@Override
	public void onRefresh() {
		if (mListFragment != null) {
			mListFragment.onRefresh();
		}
	}

	@SuppressWarnings("ucd")
	public void onNewEntityButtonClick(View view) {
		onAdd(new Bundle());
	}

	@SuppressWarnings("ucd")
	public void onMoreButtonClick(View view) {
		mListFragment.onMoreButtonClick(view);
	}

	@Override
	public void onAdd(Bundle extras) {
		/*
		 * The new entity button is visible even if the entity is locked. Now we do
		 * the actual hard core check.
		 */
		extras.putString(Constants.EXTRA_ENTITY_PARENT_ID, mParams.getEntityId());
		extras.putString(Constants.EXTRA_ENTITY_SCHEMA, mParams.getListLinkSchema());
		super.onAdd(extras);
	}

	@SuppressWarnings("ucd")
	public void onEntityClick(View view) {
		Entity entity = (Entity) view.getTag();
		Aircandi.dispatch.route(this, Route.BROWSE, entity, null, null);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected int getLayoutId() {
		return R.layout.entity_list;
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/
}