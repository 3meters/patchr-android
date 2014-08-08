package com.aircandi.catalina.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.aircandi.catalina.Catalina;
import com.aircandi.catalina.Constants;
import com.aircandi.catalina.objects.EventType;
import com.aircandi.catalina.ui.AircandiForm;
import com.aircandi.objects.ServiceMessage;
import com.aircandi.ui.base.BaseFragment;

public class GcmIntentService extends com.aircandi.components.GcmIntentService {

	@Override
	protected Boolean showingActivities() {
		android.app.Activity currentActivity = Catalina.getInstance().getCurrentActivity();
		if (currentActivity != null && currentActivity.getClass().equals(AircandiForm.class)) {
			BaseFragment fragment = ((AircandiForm) currentActivity).getCurrentFragment();
			if (fragment != null && fragment.isActivityStream()) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected Boolean isValidSchema(ServiceMessage message) {
		String[] validSchemas = {Constants.SCHEMA_ENTITY_MESSAGE, Constants.SCHEMA_ENTITY_PLACE};
		String[] validToSchemas = {Constants.SCHEMA_ENTITY_MESSAGE, Constants.SCHEMA_ENTITY_PLACE, Constants.SCHEMA_ENTITY_USER};

		if (message.action.entity != null) {
			if (!Arrays.asList(validSchemas).contains(message.action.entity.schema)) return false;
		}
		if (message.action.toEntity != null) {
			if (!Arrays.asList(validToSchemas).contains(message.action.toEntity.schema))
				return false;
		}

		return true;
	}

	@Override
	protected Boolean isValidEvent(ServiceMessage message) {
		List<String> events = new ArrayList<String>();
		events.add(EventType.INSERT_PLACE);
		events.add(EventType.INSERT_MESSAGE);
		events.add(EventType.INSERT_MESSAGE_SHARE);

		if (message.action.entity != null) {
			if (!events.contains(message.action.event)) return false;
		}

		return true;
	}

}
