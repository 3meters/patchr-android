package com.aircandi.components;

import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.objects.Notification;
import com.aircandi.utilities.Booleans;

public class PreferenceManager {

	public Boolean notificationEnabled(String triggerCategory, String eventCategory) {

		if (triggerCategory.equals(Notification.TriggerCategory.NEARBY)) {
			if (!Patchr.settings.getBoolean(StringManager.getString(R.string.pref_messages_nearby)
					, Booleans.getBoolean(R.bool.pref_notifications_nearby_default)))
				return false;
		}
		else if (triggerCategory.equals(Notification.TriggerCategory.OWN)) {
			if (!Patchr.settings.getBoolean(StringManager.getString(R.string.pref_messages_own)
					, Booleans.getBoolean(R.bool.pref_notifications_own_default)))
				return false;
		}
		else if (triggerCategory.equals(Notification.TriggerCategory.WATCH)) {
			if (!Patchr.settings.getBoolean(StringManager.getString(R.string.pref_messages_watch)
					, Booleans.getBoolean(R.bool.pref_notifications_watch_default)))
				return false;
		}
		else if (eventCategory.equals(Notification.EventCategory.LIKE)) {
			if (!Patchr.settings.getBoolean(StringManager.getString(R.string.pref_likes)
					, Booleans.getBoolean(R.bool.pref_notifications_like_default)))
				return false;
		}
		else if (eventCategory.equals(Notification.EventCategory.SHARE)) {
			if (!Patchr.settings.getBoolean(StringManager.getString(R.string.pref_messages_share)
					, Booleans.getBoolean(R.bool.pref_notifications_share_default)))
				return false;
		}

		return true;
	}
}
