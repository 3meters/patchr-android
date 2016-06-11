package com.patchr.utilities;

import android.support.annotation.NonNull;

import com.patchr.Constants;
import com.patchr.objects.AirLocation;
import com.patchr.objects.CacheStamp;
import com.patchr.objects.Category;
import com.patchr.objects.Count;
import com.patchr.objects.Document;
import com.patchr.objects.ImageResult;
import com.patchr.objects.Install;
import com.patchr.objects.Link;
import com.patchr.objects.Message;
import com.patchr.objects.Notification;
import com.patchr.objects.Patch;
import com.patchr.objects.PhoneNumber;
import com.patchr.objects.Photo;
import com.patchr.objects.ServiceBase.UpdateScope;
import com.patchr.objects.ServiceData;
import com.patchr.objects.ServiceEntry;
import com.patchr.objects.ServiceObject;
import com.patchr.objects.Session;
import com.patchr.objects.Shortcut;
import com.patchr.objects.User;
import com.patchr.service.Expose;
import com.patchr.service.SerializedName;

import net.minidev.json.JSONValue;
import net.minidev.json.parser.ContainerFactory;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Json {

	/*
	 * We expect serialization/deserializstion to work because all json is coming from trusted sources:
	 *
	 * - Internal transfer of objects using intents.
	 * - Internal storage of objects in shared preferences, files, resources, etc.
	 * - Objects returned by patchr service.
	 * - Objects returned by Bing search.
	 */

	@NonNull
	public static Object jsonToObject(@NonNull final String json, @NonNull Json.ObjectType objectType) {
		/*
		 * Caller will get back either an array of objectType or a single objectType.
		 */
		return Json.jsonToObject(json, objectType, Json.ServiceDataWrapper.FALSE);
	}

	@NonNull
	public static Object jsonToObject(@NonNull final String json
			, @NonNull Json.ObjectType objectType
			, @NonNull Json.ServiceDataWrapper serviceDataWrapper) {
		/*
		 * serviceDataWrapper
		 * 
		 * true: Caller will get back a ServiceData object with a data property that is
		 * either an array of objectType or a single objectType.
		 * 
		 * false: Caller will get back either an array of objectType or a single objectType.
		 */
		final Object object = Json.jsonToObjects(json, objectType, serviceDataWrapper);
		if (serviceDataWrapper == Json.ServiceDataWrapper.FALSE) {
			if (object instanceof List) {
				final List<Object> array = (List<Object>) object;
				if (array.size() > 0) return array.get(0);
			}
		}
		else {
			ServiceData serviceData = (ServiceData) object;
			if (serviceData.data instanceof List) {
				final List<Object> array = (List<Object>) serviceData.data;
				if (array.size() > 0) {
					serviceData.data = array.get(0);
					return serviceData;
				}
			}
		}
		return object;
	}

	@NonNull
	public static Object jsonToObjects(@NonNull final String json
			, @NonNull final Json.ObjectType objectType
			, @NonNull Json.ServiceDataWrapper serviceDataWrapper) {

		/*
		 * serviceDataWrapper
		 * 
		 * true: Caller will get back a ServiceData object with a data property that is
		 * either an array of objectType or a single objectType.
		 * 
		 * false: Caller will get back either an array of objectType or a single objectType.
		 */
		try {
			List<LinkedHashMap<String, Object>> maps;

			JSONParser parser = new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE);

			/* Lets us direct which implementations to use for lists and maps */
			ContainerFactory containerFactory = new ContainerFactory() {
				@NonNull
				@Override
				public Map createObjectContainer() {
					return new LinkedHashMap();
				}

				@NonNull
				@Override
				public List<Object> createArrayContainer() {
					return new ArrayList<Object>();
				}
			};

			if (objectType == ObjectType.OBJECT) {

				Map<String, Object> rootMap = (LinkedHashMap<String, Object>) parser.parse(json, containerFactory);
				return rootMap;
			}
			else if (serviceDataWrapper == ServiceDataWrapper.FALSE) {

				Map<String, Object> rootMap = (LinkedHashMap<String, Object>) parser.parse(json, containerFactory);
				maps = new ArrayList<LinkedHashMap<String, Object>>();
				maps.add((LinkedHashMap<String, Object>) rootMap);

				List<Object> object = Json.mapsToObjects(maps, objectType, false);

				return object;
			}
			else {

				Map<String, Object> rootMap = (LinkedHashMap<String, Object>) parser.parse(json, containerFactory);
				ServiceData serviceData = ServiceData.setPropertiesFromMap(new ServiceData(), rootMap, true);
				/*
				 * The data property of ServiceData is always an array even
				 * if the request could only expect to return a single object.
				 */
				if (serviceData.d != null) {

					/* It's the results of a bing query */
					rootMap = (LinkedHashMap<String, Object>) serviceData.d;
					if (objectType == Json.ObjectType.IMAGE_RESULT) {

						/* Array of objects */
						maps = (List<LinkedHashMap<String, Object>>) rootMap.get("results");
						final List<Object> list = new ArrayList<Object>();
						for (Map<String, Object> map : maps) {
							list.add(ImageResult.setPropertiesFromMap(new ImageResult(), map, true));
						}
						serviceData.data = list;
					}
				}
				else if (serviceData.data != null) {

					if (serviceData.data instanceof List) {
						/* The data property is an array of objects */
						maps = (List<LinkedHashMap<String, Object>>) serviceData.data;
					}
					else {

						/* The data property is an object and we put it in an array */
						final Map<String, Object> map = (LinkedHashMap<String, Object>) serviceData.data;
						maps = new ArrayList<LinkedHashMap<String, Object>>();
						maps.add((LinkedHashMap<String, Object>) map);
					}
					serviceData.data = Json.mapsToObjects(maps, objectType, true);
				}
				return serviceData;
			}
		}
		catch (ParseException e) {
			Reporting.breadcrumb(json);
			Reporting.logException(e);
		}
		catch (Exception e) {
			Reporting.breadcrumb(json);
			Reporting.logException(e);
		}
		throw new IllegalArgumentException("Unable to deserialize json: " + objectType.toString());
	}

	@NonNull
	public static List<Object> mapsToObjects(@NonNull List<LinkedHashMap<String, Object>> maps
			, @NonNull final Json.ObjectType objectType
			, @NonNull Boolean nameMapping) {

		final List<Object> list = new ArrayList<Object>();

		try {
			/* Decode each map into an object and add to an array */
			for (Map<String, Object> map : maps) {
				if (objectType == Json.ObjectType.SERVICE_ENTRY) {
					list.add(ServiceEntry.setPropertiesFromMap(new ServiceEntry(), map, nameMapping));
				}
				else if (objectType == Json.ObjectType.ENTITY) {
					String schema = (String) map.get("schema");
					if (schema != null) {
						if (schema.equals(Constants.SCHEMA_ENTITY_PATCH)) {
							list.add(Patch.setPropertiesFromMap(new Patch(), map, nameMapping));
						}
						else if (schema.equals(Constants.SCHEMA_ENTITY_MESSAGE)) {
							list.add(Message.setPropertiesFromMap(new Message(), map, nameMapping));
						}
						else if (schema.equals(Constants.SCHEMA_ENTITY_NOTIFICATION)) {
							list.add(Notification.setPropertiesFromMap(new Notification(), map, nameMapping));
						}
						else if (schema.equals(Constants.SCHEMA_ENTITY_USER)) {
							list.add(User.setPropertiesFromMap(new User(), map, nameMapping));
						}
					}
				}
				else if (objectType == Json.ObjectType.SESSION) {
					list.add(Session.setPropertiesFromMap(new Session(), map, nameMapping));
				}
				else if (objectType == Json.ObjectType.SHORTCUT) {
					list.add(Shortcut.setPropertiesFromMap(new Shortcut(), map, nameMapping));
				}
				else if (objectType == Json.ObjectType.RESULT) {
					list.add(CacheStamp.setPropertiesFromMap(new CacheStamp(), map, nameMapping));
				}
				else if (objectType == Json.ObjectType.AIR_LOCATION) {
					list.add(AirLocation.setPropertiesFromMap(new AirLocation(), map, nameMapping));
				}
				else if (objectType == Json.ObjectType.LINK) {
					list.add(Link.setPropertiesFromMap(new Link(), map, nameMapping));
				}
				else if (objectType == Json.ObjectType.IMAGE_RESULT) {
					list.add(ImageResult.setPropertiesFromMap(new ImageResult(), map, nameMapping));
				}
				else if (objectType == Json.ObjectType.PHOTO) {
					list.add(Photo.setPropertiesFromMap(new Photo(), map, nameMapping));
				}
				else if (objectType == ObjectType.PHONE) {
					list.add(PhoneNumber.setPropertiesFromMap(new PhoneNumber(), map, nameMapping));
				}
				else if (objectType == Json.ObjectType.COUNT) {
					list.add(Count.setPropertiesFromMap(new Count(), map, nameMapping));
				}
				else if (objectType == Json.ObjectType.CATEGORY) {
					list.add(Category.setPropertiesFromMap(new Category(), map, nameMapping));
				}
				else if (objectType == Json.ObjectType.INSTALL) {
					list.add(Install.setPropertiesFromMap(new Install(), map, nameMapping));
				}
				else if (objectType == Json.ObjectType.DOCUMENT) {
					list.add(Document.setPropertiesFromMap(new Document(), map, nameMapping));
				}
			}
			return list;
		}
		catch (ClassCastException exception) {
			/*
			 * Sometimes we get back something that isn't a json object so we
			 * catch the exception, log it and keep going.
			 */
			Reporting.logException(exception);
		}
		return list;
	}

	@NonNull
	public static String objectToJson(@NonNull Object object) {
		return Json.objectToJson(object, Json.UseAnnotations.FALSE, Json.ExcludeNulls.TRUE);
	}

	@NonNull
	public static String objectToJson(@NonNull Object object
			, @NonNull Json.UseAnnotations useAnnotations
			, @NonNull Json.ExcludeNulls excludeNulls) {
		final Map map = Json.objectToMap(object, useAnnotations, excludeNulls);
		String json = JSONValue.toJSONString(map);
		if (json == null) {
			throw new IllegalArgumentException("Unable to serialize object to json");
		}
		return json;
	}

	@NonNull
	public static Map<String, Object> objectToMap(@NonNull Object object
			, @NonNull Json.UseAnnotations useAnnotations
			, @NonNull Json.ExcludeNulls excludeNullsProposed) {

		final Map<String, Object> map = new HashMap<String, Object>();

		/*
		 * Order of precedent
		 * 1. object.updateScope: PropertyValue = exclude nulls, Object = include nulls.
		 * 2. excludeNulls parameter: forces exclusion even if updateScope = Object.
		 */
		Boolean excludeNulls = (excludeNullsProposed == Json.ExcludeNulls.TRUE);
		try {
			UpdateScope updateScope = ((ServiceObject) object).updateScope;
			if (updateScope != null && updateScope == UpdateScope.OBJECT) {
				excludeNulls = false;
			}
		}
		catch (Exception ignore) {}

		Class<?> cls = object.getClass();

		try {
			while (true) {
				if (cls == null) return map;
				final Field[] fields = cls.getDeclaredFields();
				for (Field f : fields) {

					f.setAccessible(true); // Ensure trusted access
					/*
					 * We are only mapping public and protected fields.
					 */
					if (!Modifier.isStatic(f.getModifiers())
							&& (Modifier.isPublic(f.getModifiers()) || Modifier.isProtected(f.getModifiers()))) {

						if (useAnnotations == Json.UseAnnotations.TRUE) {
							if (!f.isAnnotationPresent(Expose.class)) {
								continue;
							}
							else {
								Expose annotation = f.getAnnotation(Expose.class);
								if (!annotation.serialize()) {
									continue;
								}
							}
						}

						String key = f.getName();
						/*
						 * Modify the name key if annotations are active and present.
						 */
						if (useAnnotations == Json.UseAnnotations.TRUE) {
							if (f.isAnnotationPresent(SerializedName.class)) {
								SerializedName annotation = f.getAnnotation(SerializedName.class);
								key = annotation.name();
							}
						}

						Object value = f.get(object);
						/*
						 * Only add to map if has value or meets null requirements.
						 */
						if (useAnnotations == Json.UseAnnotations.TRUE) {
							Expose annotation = f.getAnnotation(Expose.class);
							if (!annotation.serializeNull() && value == null) {
								continue;
							}
						}

						if (value != null || !excludeNulls) {
							/*
							 * Handle nested objects and arrays
							 */
							if (value instanceof ServiceObject) {
								Map childMap = Json.objectToMap(value, useAnnotations, excludeNullsProposed);
								map.put(key, childMap);
							}
							else if (value instanceof ArrayList) {
								List<Object> list = new ArrayList<Object>();
								for (Object obj : (ArrayList) value) {
									if (obj != null) {
										if (obj instanceof ServiceObject) {
											Map childMap = Json.objectToMap(obj, useAnnotations, excludeNullsProposed);
											list.add(childMap);
										}
										else {
											list.add(obj);
										}
									}
								}
								map.put(key, list);
							}
							else {
								map.put(key, value);
							}
						}
					}
				}
				cls = cls.getSuperclass();
			}
		}
		catch (IllegalArgumentException e) {
			Reporting.logException(e);
		}
		catch (IllegalAccessException e) {
			Reporting.logException(e);
		}
		return map;
	}

	public static enum UseAnnotations {
		TRUE,
		FALSE
	}

	public static enum ServiceDataWrapper {
		TRUE,
		FALSE
	}

	@SuppressWarnings("ucd")
	public static enum ExcludeNulls {
		TRUE,
		FALSE
	}

	public static enum ObjectType {
		ENTITY,
		SESSION,
		PHOTO,
		LINK,
		RESULT,
		IMAGE_RESULT,
		AIR_LOCATION,
		CATEGORY,
		DOCUMENT,
		NONE,
		SERVICE_ENTRY,
		SHORTCUT,
		INSTALL,
		AIR_MARKER,
		SERVICE_ACTIVITY,
		SERVICE_MESSAGE,
		COUNT,
		OBJECT,
		PHONE
	}
}