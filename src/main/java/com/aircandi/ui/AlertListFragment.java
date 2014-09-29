package com.aircandi.ui;

import android.view.View;

import com.aircandi.Patch;
import com.aircandi.objects.ViewHolder;
import com.aircandi.events.ProcessingCompleteEvent;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Route;
import com.aircandi.objects.User;
import com.squareup.otto.Subscribe;

public class AlertListFragment extends MessageListFragment {

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onClick(View v) {
		final Entity entity = (Entity) ((ViewHolder) v.getTag()).data;
		if (entity.creator != null) {
			User user = entity.creator;
			Patch.dispatch.route(getActivity(), Route.BROWSE, user, null, null);
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