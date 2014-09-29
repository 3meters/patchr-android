package com.aircandi.components;

import com.aircandi.Patch;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.objects.MessageTriggerType;
import com.aircandi.objects.Entity;
import com.aircandi.utilities.Booleans;

public class PreferenceManager {

	public Boolean notificationEnabled(String triggerCategory, Entity entity) {

		if (entity.type != null && entity.type.equals(Constants.TYPE_LINK_SHARE)) {
			if (!Patch.settings.getBoolean(StringManager.getString(R.string.pref_messages_share)
					, Booleans.getBoolean(R.bool.pref_notifications_share_default)))
				return false;
		}
		else {
			if (triggerCategory.equals(MessageTriggerType.TriggerType.NEARBY)) {
				if (!Patch.settings.getBoolean(StringManager.getString(R.string.pref_messages_nearby)
						, Booleans.getBoolean(R.bool.pref_notifications_nearby_default)))
					return false;
			}
			else if (triggerCategory.equals(MessageTriggerType.TriggerType.OWN)) {
				if (!Patch.settings.getBoolean(StringManager.getString(R.string.pref_messages_own)
						, Booleans.getBoolean(R.bool.pref_notifications_own_default)))
					return false;
			}
			else if (triggerCategory.equals(MessageTriggerType.TriggerType.WATCH)) {
				if (!Patch.settings.getBoolean(StringManager.getString(R.string.pref_messages_watch)
						, Booleans.getBoolean(R.bool.pref_notifications_watch_default)))
					return false;
			}
		}

		return true;
	}
}