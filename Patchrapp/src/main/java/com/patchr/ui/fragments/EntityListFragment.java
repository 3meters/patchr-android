package com.patchr.ui.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.patchr.R;
import com.patchr.objects.QuerySpec;
import com.patchr.ui.widgets.ListWidget;

import io.realm.Realm;

/**
 * Fragment lifecycle
 * <p>
 * - onAttach (activity may not be fully initialized but fragment has been associated with it)
 * - onCreate
 * - onCreateView
 * - onViewCreated
 * - onActivityCreated (views created, safe to use findById)
 * - onViewStateRestored
 * - onStart (fragment becomes visible)
 * - onResume
 * <p>
 * - onPause
 * - onStop
 * - onSaveInstanceState
 * - onDestroyView
 * - onDestroy
 * - onDetach
 */
public class EntityListFragment extends Fragment {

	public QuerySpec  querySpec;   /* Required injection */
	public Integer    headerResId; /* Optional injection */
	public ListWidget listWidget;
	public String     contextEntityId;

	public Integer topPadding = 0;  // Hack to handle ui tweaking
	public Realm realm;  // Always on main thread

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		/* Force complete initialization if being recreated by the system */
		boolean recreated = (savedInstanceState != null && !savedInstanceState.isEmpty());
		if (recreated) {
			getActivity().finish();
		}
		this.realm = Realm.getDefaultInstance();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		/* Called every time the fragment is used/reused */
		super.onCreateView(inflater, container, savedInstanceState);
		return inflater.inflate(R.layout.fragment_entity_list, container, false);
	}

	@Override public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		/*
		 * Called when the fragment's activity has been created and this
		 * fragment's view hierarchy instantiated.
		 */
		super.onActivityCreated(savedInstanceState);
		View view = getView();
		if (view != null) {
			initialize(view);
			this.listWidget.bind(this.querySpec, this.contextEntityId);   // Triggers display of cached entities
		}
	}

	@Override public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
		super.onViewStateRestored(savedInstanceState);
		this.listWidget.onViewStateRestored(savedInstanceState);
	}

	@Override public void onStart() {
		super.onStart();
		listWidget.onStart();
	}

	@Override public void onResume() {
		super.onResume();
		doOnResume();       // Triggers service fetch
	}

	@Override public void onStop() {
		super.onStop();
		listWidget.onStop();
	}

	@Override public void onDestroy() {
		super.onDestroy();
		if (!this.realm.isClosed()) {
			this.realm.close();
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	private void initialize(View view) {
		this.listWidget = (ListWidget) view.findViewById(R.id.list_view);
		if (this.listWidget != null) {
			this.listWidget.setRealm(this.realm);
			this.listWidget.listGroup.setPadding(0, this.topPadding, 0, 0);
		}
		if (this.headerResId != null) {
			View header = LayoutInflater.from(getContext()).inflate(this.headerResId, null, false);
			this.listWidget.setHeader(header);
		}
	}

	protected void doOnResume() {
		/* To provide more control to subclasses like NearbyListFragment */
		listWidget.onResume();      // Triggers service fetch
	}
}