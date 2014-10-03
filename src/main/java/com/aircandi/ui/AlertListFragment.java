package com.aircandi.ui;

import android.view.View;

import com.aircandi.Patch;
import com.aircandi.components.MessagingManager;
import com.aircandi.events.ProcessingCompleteEvent;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Route;
import com.aircandi.objects.ViewHolder;
import com.squareup.otto.Subscribe;

public class AlertListFragment extends MessageListFragment {

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onClick(View v) {
		final Entity entity = (Entity) ((ViewHolder) v.getTag()).data;
		if (entity != null) {
			Entity place = entity.linksOut.get(0).shortcut.getAsEntity();
			place.id = entity.linksOut.get(0).toId;
			Patch.dispatch.route(getActivity(), Route.BROWSE, place, null, null);
			MessagingManager.getInstance().setNewActivity(false);
			if (MessagingManager.getInstance().getAlerts().containsKey(entity.id)) {
				MessagingManager.getInstance().getAlerts().remove(entity.id);
				mAdapter.notifyDataSetChanged();;
			}
		}
	}

	@Subscribe
	public void onProcessingComplete(ProcessingCompleteEvent event) {
		super.onProcessingComplete(event);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

 	/*--------------------------------------------------------------------------------------------
	 * Menus
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/
}