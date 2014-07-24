package com.aircandi.components;

import com.aircandi.Aircandi;
import com.aircandi.R;
import com.aircandi.objects.ActivityBase.TriggerType;
import com.aircandi.utilities.Booleans;

public class PreferenceManager {

	public Boolean notificationEnabled(String triggerCategory, String entitySchema) {

		if (triggerCategory.equals(TriggerType.NEARBY)) {
			if (!Aircandi.settings.getBoolean(StringManager.getString(R.string.pref_messages_nearby)
					, Booleans.getBoolean(R.bool.pref_notifications_nearby_default)))
				return false;
		}
		else if (triggerCategory.equals(TriggerType.OWN)) {
			if (!Aircandi.settings.getBoolean(StringManager.getString(R.string.pref_messages_own)
					, Booleans.getBoolean(R.bool.pref_notifications_own_default)))
				return false;
		}
		else if (triggerCategory.equals(TriggerType.WATCH)) {
			if (!Aircandi.settings.getBoolean(StringManager.getString(R.string.pref_messages_watch)
					, Booleans.getBoolean(R.bool.pref_notifications_watch_default)))
				return false;
		}

		return true;
	}
}
