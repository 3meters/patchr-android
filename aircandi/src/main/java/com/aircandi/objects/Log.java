package com.aircandi.objects;

import java.io.Serializable;
import java.util.Map;

import com.aircandi.service.Expose;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class Log extends ServiceBase implements Cloneable, Serializable {

	private static final long serialVersionUID = 4622842664683047258L;

	@Expose
	public String category;
	@Expose
	public String label;
	@Expose
	public Number value;

	public static Log setPropertiesFromMap(Log log, Map map, Boolean nameMapping) {
		/*
		 * Properties involved with editing are copied from one entity to another.
		 */
		log.category = (String) map.get("category");
		log.label = (String) map.get("label");
		log.value = (Number) map.get("value");

		return log;
	}

	@Override
	public String getCollection() {
		return "logs";
	}

	public static class LogCategory {
		public static String TIMING = "timing";
	}
}