package com.patchr.objects;

import java.util.Map;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class ServiceEntry extends ServiceBase {

	private static final long serialVersionUID = -4468666434251114969L;

	public static ServiceEntry setPropertiesFromMap(ServiceEntry serviceEntry, Map map, Boolean nameMapping) {

		serviceEntry = (ServiceEntry) ServiceBase.setPropertiesFromMap(serviceEntry, map, nameMapping);
		return serviceEntry;
	}

	@Override
	public String getCollection() {
		return null;
	}

}