package com.patchr.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.objects.Route;
import com.patchr.objects.TransitionType;
import com.patchr.ui.views.UserDetailView;

public class UserListFormFragment extends EntityListFragment implements View.OnClickListener {

	private UserDetailView header;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		return inflater.inflate(R.layout.entity_list_fragment, container, false);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public void onClick(View view) {
		if (mProcessing) return;

		int titleResId = 0;
		int emptyResId = 0;
		String linkType = (String) view.getTag();

		if (view.getId() == R.id.button_member) {
			titleResId = R.string.label_drawer_item_watch;
			emptyResId = R.string.label_watching_empty;
		}
		else if (view.getId() == R.id.button_owner) {
			titleResId = R.string.label_drawer_item_create;
			emptyResId = R.string.label_created_empty;
		}

		Bundle extras = new Bundle();
		extras.putString(Constants.EXTRA_LIST_LINK_TYPE, linkType);
		extras.putInt(Constants.EXTRA_LIST_TITLE_RESID, titleResId);
		extras.putInt(Constants.EXTRA_LIST_EMPTY_RESID, emptyResId);
		extras.putInt(Constants.EXTRA_TRANSITION_TYPE, TransitionType.DRILL_TO);

		Patchr.router.route(getActivity(), Route.PATCH_LIST, mScopingEntity, extras);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void initialize() {

	}


}
