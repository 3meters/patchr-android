package com.aircandi.catalina.ui;

import android.view.View;

import com.aircandi.Aircandi;
import com.aircandi.catalina.Constants;
import com.aircandi.catalina.objects.Message.MessageType;
import com.aircandi.components.Extras;
import com.aircandi.controllers.ViewHolder;
import com.aircandi.events.ProcessingCompleteEvent;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Link;
import com.aircandi.objects.Route;
import com.aircandi.objects.User;
import com.aircandi.ui.base.BaseActivity;
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
			Aircandi.dispatch.route(getActivity(), Route.BROWSE, user, null, null);
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