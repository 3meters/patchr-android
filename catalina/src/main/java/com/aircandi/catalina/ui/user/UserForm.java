package com.aircandi.catalina.ui.user;

import android.os.Bundle;
import android.view.View;

import com.aircandi.Aircandi;
import com.aircandi.catalina.Constants;
import com.aircandi.catalina.R;
import com.aircandi.catalina.objects.LinkProfile;
import com.aircandi.catalina.ui.MessageListFragment;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ModelResult;
import com.aircandi.monitors.EntityMonitor;
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

		Boolean currentUser = Aircandi.getInstance().getCurrentUser().id.equals(mEntityId);
		mLinkProfile = currentUser ? LinkProfile.LINKS_FOR_USER_CURRENT : LinkProfile.LINKS_FOR_USER;
		mDrawStats = null;
	}

	@Override
	public void afterDatabind(final BindingMode mode, ModelResult result) {
		super.afterDatabind(mode, result);
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
	public void drawButtons() {
		super.drawButtons();

		UI.setVisibility(findViewById(R.id.button_edit), View.GONE);
		if (Aircandi.getInstance().getMenuManager().canUserEdit(mEntity)) {
			UI.setVisibility(findViewById(R.id.button_edit), View.VISIBLE);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/ 	@SuppressWarnings("ucd")
	public void onMoreButtonClick(View view) {
		mListFragment.onMoreButtonClick(view);
	}

	public void onEditButtonClick(View view) {
		Aircandi.dispatch.route(this, Route.EDIT, mEntity, null, null);
	}

	@Override
	protected int getLayoutId() {
		return R.layout.user_form_with_list;
	}
}