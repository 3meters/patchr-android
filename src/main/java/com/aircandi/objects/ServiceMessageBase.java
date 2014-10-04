package com.aircandi.objects;

import android.content.Intent;

import com.aircandi.service.Expose;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public abstract class ServiceMessageBase extends ServiceObject implements Cloneable, Serializable {

	private static final long serialVersionUID = -9162254814199461867L;

	@Expose
	public String trigger;                                        // create, watch
	@Expose
	public Action action;

	/* client only */
	public Intent intent;
	public String title;
	public String subtitle;
	public String description;
	public Photo  photoBy;
	public Photo  photoOne;

	public static ServiceMessageBase setPropertiesFromMap(ServiceMessageBase base, Map map, Boolean nameMapping) {
	    /*
	     * Need to include any properties that need to survive encode/decoded between activities.
		 */
		base.trigger = (String) map.get("trigger");

		if (map.get("action") != null) {
			base.action = Action.setPropertiesFromMap(new Action(), (HashMap<String, Object>) map.get("action"), nameMapping);
		}

		return base;
	}

	public String getTriggerCategory() {
		if (this.trigger.contains("nearby")) return MessageTriggers.TriggerCategory.NEARBY;
		if (this.trigger.contains("watch")) return MessageTriggers.TriggerCategory.WATCH;
		if (this.trigger.contains("own")) return MessageTriggers.TriggerCategory.OWN;
		return null;
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/
}