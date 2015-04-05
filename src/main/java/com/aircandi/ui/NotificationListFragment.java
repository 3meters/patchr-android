package com.aircandi.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.components.Dispatcher;
import com.aircandi.components.NotificationManager;
import com.aircandi.events.DataErrorEvent;
import com.aircandi.events.DataNoopEvent;
import com.aircandi.events.DataResultEvent;
import com.aircandi.events.NotificationReceivedEvent;
import com.aircandi.events.NotificationsRequestEvent;
import com.aircandi.interfaces.IEntityController;
import com.aircandi.objects.Cursor;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Notification;
import com.aircandi.objects.Route;
import com.aircandi.objects.TransitionType;
import com.aircandi.objects.ViewHolder;
import com.aircandi.utilities.Colors;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Maps;
import com.aircandi.utilities.UI;
import com.squareup.otto.Subscribe;

import java.util.Map;

public class NotificationListFragment extends EntityListFragment
		implements SwipeRefreshLayout.OnRefreshListener {

	@Override
	public void bind(final BindingMode mode) {

		/* Need to be signed in to see notifications */
		if (Patchr.getInstance().getCurrentUser().isAnonymous()) {
			mEntities.clear();
			mAdapter.setNotifyOnChange(false);
			mAdapter.clear();
		}
		else {
			super.bind(mode);
		}
	}

	public void fetch(Integer skip, Integer limit, BindingMode mode) {

		Integer skipCount = ((int) Math.ceil((double) skip / mPageSize) * mPageSize);
		Cursor cursor = new Cursor()
				.setLimit(limit)
				.setSort(Maps.asMap("modifiedDate", -1))
				.setSkip(skipCount);

		NotificationsRequestEvent request = new NotificationsRequestEvent()
				.setCursor(cursor);

		request.setActionType(mActionType)
		       .setEntityId(mMonitorEntityId)
		       .setTag(System.identityHashCode(this));

		if (mBound && mMonitorEntity != null && mode != BindingMode.MANUAL) {
			request.setCacheStamp(mMonitorEntity.getCacheStamp());
		}

		Dispatcher.getInstance().post(request);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@SuppressLint("ResourceAsColor")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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

	@Override
	public void onRefresh() {
		super.onRefresh();
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onNotificationReceived(final NotificationReceivedEvent event) {

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

	@Subscribe
	public void onDataResult(final DataResultEvent event) {
		super.onDataResult(event);
	}

	@Subscribe
	public void onDataError(DataErrorEvent event) {
		super.onDataError(event);
	}

	@Subscribe
	public void onDataNoop(DataNoopEvent event) {
		super.onDataNoop(event);
	}

	@Override
	public void onClick(View v) {
		final Entity entity = (Entity) ((ViewHolder) v.getTag()).data;
		if (entity != null) {
			Notification notification = (Notification) entity;

			Bundle extras = new Bundle();
			extras.putString(Constants.EXTRA_ENTITY_SCHEMA, Entity.getSchemaForId(notification.targetId));
			extras.putString(Constants.EXTRA_ENTITY_ID, notification.targetId);
			extras.putString(Constants.EXTRA_ENTITY_PARENT_ID, notification.parentId);

			extras.putInt(Constants.EXTRA_TRANSITION_TYPE, TransitionType.DRILL_TO);
			Patchr.router.route(getActivity(), Route.BROWSE, null, extras);

			if (NotificationManager.getInstance().getNotifications().containsKey(entity.id)) {
				NotificationManager.getInstance().getNotifications().get(entity.id).read = true;
				mAdapter.notifyDataSetChanged();
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	protected void bindListItem(Entity entity, View view, String groupTag) {

		IEntityController controller = Patchr.getInstance().getControllerForEntity(entity);
		controller.bind(entity, view, groupTag);

		ViewHolder holder = (ViewHolder) view.getTag();
		Notification notification = (Notification) entity;

		/* Alert indicator */
		if (holder.alert != null) {
			UI.setVisibility(holder.alert, View.INVISIBLE);
			if (NotificationManager.getInstance().getNotifications().containsKey(notification.id)) {
				boolean read = NotificationManager.getInstance().getNotifications().get(notification.id).read;
				if (!read) {
					UI.setVisibility(holder.alert, View.VISIBLE);
				}
			}
		}

		/* Summary */
		UI.setVisibility(holder.summary, View.GONE);
		if (holder.summary != null && !TextUtils.isEmpty(((Notification) entity).summary)) {
			holder.summary.setText(Html.fromHtml(((Notification) entity).summary));
			UI.setVisibility(holder.summary, View.VISIBLE);
		}

		/* Big photo */
		if (holder.photoViewBig != null) {
			UI.setVisibility(holder.photoViewBig, View.GONE);
			if (notification.photoBig != null) {
				if (holder.photoViewBig.getPhoto() == null || !notification.photoBig.getUri().equals(holder.photoViewBig.getPhoto().getUri())) {
					holder.photoViewBig.setTag(notification.photoBig);
					holder.photoViewBig.setCenterCrop(false);
					UI.drawPhoto(holder.photoViewBig, notification.photoBig);
				}
				UI.setVisibility(holder.photoViewBig, View.VISIBLE);
			}
		}

		/* Revise the date formatting */
		if (holder.modifiedDate != null && entity.modifiedDate != null) {
			String compactAgo = DateTime.interval(entity.sortDate.longValue(), DateTime.nowDate().getTime(), DateTime.IntervalContext.PAST);
			holder.modifiedDate.setText(compactAgo);
		}

		/* Type photo */
		if (entity.type != null) {
			UI.setVisibility(holder.photoType, View.GONE);
			Integer drawableResId = null;
			if (entity.type.equals(Notification.NotificationType.WATCH)) {
				drawableResId = UI.getResIdForAttribute(getActivity(), R.attr.iconWatch);
			}
			else if (entity.type.equals(Notification.NotificationType.PLACE)) {
				drawableResId = UI.getResIdForAttribute(getActivity(), R.attr.iconPatch);
			}
			else if (entity.type.equals(Notification.NotificationType.MESSAGE)) {
				drawableResId = UI.getResIdForAttribute(getActivity(), R.attr.iconMessage);
			}
			else if (entity.type.equals(Notification.NotificationType.MEDIA)) {
				drawableResId = UI.getResIdForAttribute(getActivity(), R.attr.iconMediaMessage);
			}
			else if (entity.type.equals(Notification.NotificationType.SHARE)) {
				drawableResId = UI.getResIdForAttribute(getActivity(), R.attr.iconShare);
			}
			else if (entity.type.equals(Notification.NotificationType.LIKE)) {
				drawableResId = UI.getResIdForAttribute(getActivity(), R.attr.iconLike);
			}
			if (drawableResId != null) {
				holder.photoType.setImageResource(drawableResId);
				UI.setVisibility(holder.photoType, View.VISIBLE);
			}
		}
	}

	protected void injectEntities(ListAdapter adapter) {
		/* Nearby notifications are local only so inject them */
		for (Map.Entry<String, Notification> entry : NotificationManager.getInstance().getNotifications().entrySet()) {
			if (entry.getValue().getTriggerCategory().equals(Notification.TriggerCategory.NEARBY)) {
				mAdapter.add(entry.getValue());
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/
}