package com.aircandi.objects;

import com.aircandi.service.Expose;

import java.io.Serializable;
import java.util.Map;

public class ServiceMessage extends ServiceMessageBase implements Cloneable, Serializable {

	private static final long serialVersionUID = -6550475791491989605L;

	@Expose
	public Number sentDate;

	public static ServiceMessage setPropertiesFromMap(ServiceMessage serviceMessage, Map map, Boolean nameMapping) {

		serviceMessage = (ServiceMessage) ServiceMessageBase.setPropertiesFromMap(serviceMessage, map, nameMapping);
		serviceMessage.sentDate = (Number) map.get("sentDate");

		return serviceMessage;
	}

}
