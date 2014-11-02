package com.aircandi.ui;

import android.os.Bundle;
import android.view.View;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.components.EntityManager;
import com.aircandi.events.NotificationEvent;
import com.aircandi.events.ProcessingFinishedEvent;
import com.aircandi.interfaces.IEntityController;
import com.aircandi.monitors.EntityMonitor;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Route;
import com.aircandi.queries.EntitiesQuery;
import com.aircandi.ui.base.BaseActivity;
import com.squareup.otto.Subscribe;

public class EntityList extends BaseActivity {

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

		mCurrentFragment = new EntityListFragment();

		EntityMonitor monitor = new EntityMonitor(mParams.getEntityId());

		EntitiesQuery query = new EntitiesQuery();
		query.setEntityId(mParams.getEntityId())
		     .setLinkDirection(mParams.getListLinkDirection())
		     .setLinkType(mParams.getListLinkType())
		     .setPageSize(mParams.getListPageSize())
		     .setSchema(mParams.getListLinkSchema());

		((EntityListFragment)mCurrentFragment).setQuery(query)
		             .setMonitor(monitor)
		             .setListItemResId(mParams.getListItemResId())
		             .setListViewType(mParams.getListViewType())
		             .setBubbleButtonMessageResId(mParams.getListNewMessageResId())
		             .setListLayoutResId(mParams.getListLayoutResId())
		             .setListLoadingResId(mParams.getListLoadingResId())
		             .setSelfBindingEnabled(true);

		getFragmentManager().beginTransaction().replace(R.id.fragment_holder, mCurrentFragment).commit();
		draw(null);
	}

	public void draw(View view) {
		IEntityController controller = Patchr.getInstance().getControllerForSchema(mParams.getListLinkSchema());
		setActivityTitle((mParams.getListTitle() != null) ? mParams.getListTitle() : controller.getName(true));
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe
	@SuppressWarnings("ucd")
	public void onMessage(final NotificationEvent event) {
		/*
		 * Refresh the form because something new has been added to it
		 * like a comment or post.
		 */
		if (event.notification.parentId != null
				&& mParams.getEntityId().equals(event.notification.parentId)) {

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
		if (mCurrentFragment != null) {
			((EntityListFragment)mCurrentFragment).onRefresh();
		}
	}

	@Subscribe
	public void onProcessingFinished(ProcessingFinishedEvent event) {
	}

	@SuppressWarnings("ucd")
	public void onNewEntityButtonClick(View view) {
		onAdd(new Bundle());
	}

	@SuppressWarnings("ucd")
	public void onMoreButtonClick(View view) {
		((EntityListFragment)mCurrentFragment).onMoreButtonClick(view);
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
		Patchr.dispatch.route(this, Route.BROWSE, entity, null, null);
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