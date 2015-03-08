package com.aircandi.ui;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewSwitcher;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.components.Dispatcher;
import com.aircandi.components.ModelResult;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.NotificationManager;
import com.aircandi.events.NotificationEvent;
import com.aircandi.events.ProcessingFinishedEvent;
import com.aircandi.interfaces.IEntityController;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Notification;
import com.aircandi.objects.Route;
import com.aircandi.objects.TransitionType;
import com.aircandi.objects.ViewHolder;
import com.aircandi.utilities.Colors;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.UI;
import com.squareup.otto.Subscribe;
import com.aircandi.interfaces.IBusy.BusyAction;

import java.util.List;
import java.util.Map;

public class NotificationListFragment extends MessageListFragment {

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@SuppressLint("ResourceAsColor")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);

		/* Change swipe colors */
		if (view != null) {
			SwipeRefreshLayout swipeRefresh = (SwipeRefreshLayout) view.findViewById(R.id.swipe);
			if (swipeRefresh != null) {
				swipeRefresh.setColorSchemeColors(Colors.getColor(UI.getResIdForAttribute(getActivity(), R.attr.refreshColorNotifications)));
				swipeRefresh.setProgressBackgroundColor(UI.getResIdForAttribute(getActivity(), R.attr.refreshColorBackgroundNotifications));
			}
		}

		return view;
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onMessage(final NotificationEvent event) {

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
			Patchr.dispatch.route(getActivity(), Route.BROWSE, null, extras);

			if (NotificationManager.getInstance().getNotifications().containsKey(entity.id)) {
				NotificationManager.getInstance().getNotifications().get(entity.id).read = true;
				mAdapter.notifyDataSetChanged();
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void bind(final BindingMode mode) {
		/*
		 * Overriding bind because notifications have too many special cases.
		 */
		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				if (mode != BindingMode.MANUAL) {
					mListController.getMessageController().showMessage(false);
				}
				mListController.getBusyController().show((mode == BindingMode.MANUAL || mNotEmpty) ? BusyAction.Refreshing : BusyAction.Refreshing_Empty);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncBindEntityList");
				ModelResult result = new ModelResult();
				if (Patchr.getInstance().getCurrentUser().isAnonymous()) {
					return result;
				}
				if (mode == BindingMode.FIRST && mQuery.hasExecuted()) {
					return result;
				}
				else if (mode == BindingMode.MANUAL
						|| (mEntities != null && mEntities.size() == 0)
						|| (mMonitor.isChanged() && mMonitor.activity)) {

					Integer limit = null;

					if (mode == BindingMode.MANUAL && mEntities != null && mEntities.size() > 0) {
						Integer pageSize = mQuery.getPageSize();
						limit = (int) Math.ceil((float) mEntities.size() / pageSize) * pageSize;
					}
					result = mQuery.execute(0, limit);
				}
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				ModelResult result = (ModelResult) response;

				if (result.serviceResponse.responseCode == NetworkManager.ResponseCode.SUCCESS) {
					if (result.data != null || Patchr.getInstance().getCurrentUser().isAnonymous()) {

						mEntities.clear();
						mAdapter.setNotifyOnChange(false);
						mAdapter.clear();

						/* Nearby notifications are local only so inject them */
						for (Map.Entry<String, Notification> entry : NotificationManager.getInstance().getNotifications().entrySet()) {
							if (entry.getValue().getTriggerCategory().equals(Notification.TriggerCategory.NEARBY)) {
								mAdapter.add(entry.getValue());
							}
						}

						if (result.data != null) {
							for (Entity entity : (List<Entity>) result.data) {
								mAdapter.add(entity);
							}
						}

						mAdapter.sort(mReverseSort ? new Entity.SortByPositionSortDateAscending() : new Entity.SortByPositionSortDate());
						draw(null);
					}

					mNotEmpty = (mAdapter != null && mAdapter.getCount() != 0);
					mFirstBind = false;
					Dispatcher.getInstance().post(new ProcessingFinishedEvent(result.serviceResponse.responseCode));
				}
				else {
					Dispatcher.getInstance().post(new ProcessingFinishedEvent(result.serviceResponse.responseCode));
					Errors.handleError(getActivity(), result.serviceResponse);
				}
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

	public void lazyLoad() {

		final ViewSwitcher switcher = (ViewSwitcher) mLoadingView.findViewById(R.id.animator_more);

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				switcher.setDisplayedChild(1);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncLazyLoadList");
				ModelResult result = mQuery.execute(mEntities.size(), null);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				ModelResult result = (ModelResult) response;

				if (result.serviceResponse.responseCode != NetworkManager.ResponseCode.SUCCESS) {
					Errors.handleError(getActivity(), result.serviceResponse);
				}
				else {
					if (result.data != null) {
						for (Entity entity : (List<Entity>) result.data) {
							mAdapter.add(entity);
						}
						mAdapter.sort(mReverseSort ? new Entity.SortByPositionSortDateAscending() : new Entity.SortByPositionSortDate());
						draw(null);
					}
				}
				switcher.setDisplayedChild(0);
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}

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

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/
}