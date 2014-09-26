package com.aircandi.catalina.ui.user;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.aircandi.Aircandi;
import com.aircandi.catalina.Constants;
import com.aircandi.catalina.R;
import com.aircandi.catalina.ui.MessageListFragment;
import com.aircandi.components.ModelResult;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.StringManager;
import com.aircandi.monitors.EntityMonitor;
import com.aircandi.objects.Count;
import com.aircandi.objects.Link.Direction;
import com.aircandi.objects.Route;
import com.aircandi.queries.EntitiesQuery;
import com.aircandi.ui.EntityListFragment;
import com.aircandi.ui.EntityListFragment.ViewType;
import com.aircandi.utilities.Integers;
import com.aircandi.utilities.UI;

@SuppressWarnings("ucd")
public class UserForm extends com.aircandi.ui.user.UserForm {

	private EntityListFragment mListFragment;
	private TextView           mButtonWatching;
	private TextView           mButtonCreated;
	private View               mButtonEdit;

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
		mListFragment = new MessageListFragment();

		EntityMonitor monitor = new EntityMonitor(mEntityId);
		EntitiesQuery query = new EntitiesQuery();

		query.setEntityId(mEntityId)
		     .setLinkDirection(Direction.out.name())
		     .setLinkType(Constants.TYPE_LINK_CREATE)
		     .setPageSize(Integers.getInteger(R.integer.page_size_messages))
		     .setSchema(Constants.SCHEMA_ENTITY_MESSAGE);

		mListFragment.setQuery(query)
		             .setMonitor(monitor)
		             .setListViewType(ViewType.LIST)
		             .setListLayoutResId(R.layout.entity_list_fragment)
		             .setListLoadingResId(R.layout.temp_list_item_loading)
		             .setListItemResId(R.layout.temp_listitem_message)
		             .setListEmptyMessageResId(R.string.label_sent_empty)
		             .setHeaderViewResId(R.layout.widget_list_header_user)
		             .setSelfBindingEnabled(false);

		getFragmentManager().beginTransaction().add(R.id.fragment_holder, mListFragment).commit();
	}

	@Override
	public void afterDatabind(final BindingMode mode, ModelResult result) {
		super.afterDatabind(mode, result);
		Boolean currentUser = Aircandi.getInstance().getCurrentUser().id.equals(mEntityId);
		if (!currentUser) return;
		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			if (mEntityMonitor.changed) {
				mListFragment.bind(BindingMode.MANUAL);
			}
			else {
				mListFragment.bind(mode);
			}
		}
	}

	@Override
	public void drawButtons(View view) {
		super.drawButtons(view);

		mButtonWatching = (TextView) findViewById(R.id.button_watching);
		mButtonCreated = (TextView) findViewById(R.id.button_created);
		mButtonEdit = findViewById(R.id.button_edit);

		UI.setVisibility(mButtonEdit, View.GONE);
		if (Aircandi.getInstance().getMenuManager().canUserEdit(mEntity)) {
			UI.setVisibility(mButtonEdit, View.VISIBLE);
		}

		Count watching = mEntity.getCount(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_PLACE, true, Direction.out);
		Count created = mEntity.getCount(Constants.TYPE_LINK_CREATE, Constants.SCHEMA_ENTITY_PLACE, true, Direction.out);

		if (watching != null) {
			mButtonWatching.setText(StringManager.getString(R.string.label_user_watching) + ": " + String.valueOf(watching.count.intValue()));
		}
		if (created != null) {
			mButtonCreated.setText(StringManager.getString(R.string.label_user_created) + ": " + String.valueOf(created.count.intValue()));
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@SuppressWarnings("ucd")
	public void onMoreButtonClick(View view) {
		mListFragment.onMoreButtonClick(view);
	}

	public void onEditButtonClick(View view) {
		Aircandi.dispatch.route(this, Route.EDIT, mEntity, null, null);
	}

	public void onPlaceListButtonClick(View view) {

		String linkType = (String) view.getTag();
		Integer titleResId = null;
		if (linkType.equals(Constants.TYPE_LINK_WATCH)) {
			titleResId = R.string.label_drawer_item_watch;
		}
		else if (linkType.equals(Constants.TYPE_LINK_CREATE)) {
			titleResId = R.string.label_drawer_item_create;
		}

		Bundle extras = new Bundle();
		extras.putString(Constants.EXTRA_LIST_LINK_TYPE, linkType);
		extras.putInt(Constants.EXTRA_LIST_TITLE_RESID, titleResId);

		Aircandi.dispatch.route(this, Route.PLACE_LIST, mEntity, null, extras);
	}

	@Override
	protected int getLayoutId() {
		return R.layout.user_form_with_list;
	}
}