package com.patchr.objects;

import java.io.Serializable;
import java.util.Map;

/**
 * @author Jayma
 */

@SuppressWarnings("ucd")
public class Document extends ServiceBase implements Cloneable, Serializable {

	private static final long   serialVersionUID = 694133954499515095L;

	public static Document setPropertiesFromMap(Document document, Map map) {
		/*
		 * Properties involved with editing are copied from one entity to another.
		 */
		document = (Document) ServiceBase.setPropertiesFromMap(document, map);

		return document;
	}
}