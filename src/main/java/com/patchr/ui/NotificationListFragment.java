package com.patchr.ui;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.patchr.R;
import com.patchr.components.Dispatcher;
import com.patchr.components.NotificationManager;
import com.patchr.components.UserManager;
import com.patchr.events.ActionEvent;
import com.patchr.events.DataErrorEvent;
import com.patchr.events.DataNoopEvent;
import com.patchr.events.DataResultEvent;
import com.patchr.events.NotificationReceivedEvent;
import com.patchr.events.NotificationsRequestEvent;
import com.patchr.objects.Cursor;
import com.patchr.objects.Notification;
import com.patchr.utilities.Colors;
import com.patchr.utilities.Maps;
import com.patchr.utilities.UI;
import com.squareup.otto.Subscribe;

import java.util.Map;

public class NotificationListFragment extends EntityListFragment
		implements SwipeRefreshLayout.OnRefreshListener {

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);

		/* Change swipe colors and redirect swipe listener to self */
		if (view != null) {
			SwipeRefreshLayout swipeRefresh = (SwipeRefreshLayout) view.findViewById(R.id.swipe);
			if (swipeRefresh != null) {
				swipeRefresh.setColorSchemeColors(Colors.getColor(UI.getResIdForAttribute(getActivity(), R.attr.refreshColorNotifications)));
				swipeRefresh.setProgressBackgroundColorSchemeResource(UI.getResIdForAttribute(getActivity(), R.attr.refreshColorBackgroundNotifications));
				swipeRefresh.setOnRefreshListener(this);
			}
		}

		return view;
	}

	@Override public void onRefresh() {
		super.onRefresh();
	}

	/*--------------------------------------------------------------------------------------------
	 * Notifications
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe public void onDataResult(final DataResultEvent event) {
		super.onDataResult(event);
	}

	@Subscribe public void onDataError(DataErrorEvent event) {
		super.onDataError(event);
	}

	@Subscribe public void onDataNoop(DataNoopEvent event) {
		super.onDataNoop(event);
	}

	@Subscribe public void onViewClick(ActionEvent event) {
		/*
		 * Base activity broadcasts view clicks that target onViewClick. This lets
		 * us handle view clicks inside fragments if we want.
		 */
		if (event.view != null) {
			Integer id = event.view.getId();

			if (id == R.id.button_more_notifications) {
				onNextPageClick(event.view);
			}
		}
	}

	@Subscribe public void onNotificationReceived(final NotificationReceivedEvent event) {

		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (mEntities.contains(event.notification)) {
					mEntities.remove(event.notification);
				}
				mListController.getMessageController().fadeOut();
				mAdapter.insert(event.notification, 0);
				mAdapter.notifyDataSetChanged();
			}
		});
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override public void bind(final BindingMode mode) {

		/* Need to be signed in to see notifications */
		if (!UserManager.getInstance().authenticated()) {
			mEntities.clear();
			mAdapter.setNotifyOnChange(false);
			mAdapter.clear();
		}
		else {
			super.bind(mode);
		}
	}

	@Override public void fetch(Integer skip, Integer limit, BindingMode mode) {

		Integer skipCount = ((int) Math.ceil((double) skip / mPageSize) * mPageSize);
		Cursor cursor = new Cursor()
				.setLimit(limit)
				.setSort(Maps.asMap("modifiedDate", -1))
				.setSkip(skipCount);

		NotificationsRequestEvent request = new NotificationsRequestEvent()
				.setCursor(cursor);

		request.setActionType(mActionType)
				.setEntityId(mScopingEntityId)
				.setTag(System.identityHashCode(this));

		if (mBound && mScopingEntity != null && mode != BindingMode.MANUAL) {
			request.setCacheStamp(mScopingEntity.getCacheStamp());
		}

		Dispatcher.getInstance().post(request);
	}

	@Override protected void injectEntities(ListAdapter adapter) {
		/* Nearby notifications are local only so inject them */
		for (Map.Entry<String, Notification> entry : NotificationManager.getInstance().getNotifications().entrySet()) {
			if (entry.getValue().getTriggerCategory().equals(Notification.TriggerCategory.NEARBY)) {
				mAdapter.add(entry.getValue());
			}
		}
	}
}