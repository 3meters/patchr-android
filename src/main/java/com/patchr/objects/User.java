package com.patchr.objects;

import com.patchr.Constants;
import com.patchr.service.Expose;
import com.patchr.utilities.DateTime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class User extends Entity {

	private static final long   serialVersionUID = 127428776257201065L;
	public static final  String collectionId     = "users";
	public static final  String schemaName       = "user";
	public static final  String schemaId         = "us";

	/*--------------------------------------------------------------------------------------------
	 * service fields
	 *--------------------------------------------------------------------------------------------*/
	@Expose
	public String  email;                                        // Required
	@Expose
	public String  role;
	@Expose
	public String  area;
	@Expose
	public Boolean developer;
	@Expose
	public String  password;

	@Expose(serialize = false, deserialize = true)
	public Number lastSignedInDate;
	@Expose(serialize = false, deserialize = true)
	public Number validationDate;
	@Expose(serialize = false, deserialize = true)
	public Number validationNotifyDate;

	/*--------------------------------------------------------------------------------------------
	 * client fields
	 *--------------------------------------------------------------------------------------------*/

	public List<Count> stats;
	public Session     session;

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public String getCollection() {
		return collectionId;
	}

	/*--------------------------------------------------------------------------------------------
	 * Copy and serialization
	 *--------------------------------------------------------------------------------------------*/

	@SuppressWarnings("unchecked")
	public static User setPropertiesFromMap(User entity, Map map, Boolean nameMapping) {

		synchronized (entity) {
			entity = (User) Entity.setPropertiesFromMap(entity, map, nameMapping);

			entity.area = (String) map.get("area");
			entity.email = (String) map.get("email");
			entity.role = (String) map.get("role");
			entity.developer = (Boolean) map.get("developer");
			entity.password = (String) map.get("password");
			entity.lastSignedInDate = (Number) map.get("lastSignedInDate");
			entity.validationDate = (Number) map.get("validationDate");
			entity.validationNotifyDate = (Number) map.get("validationNotifyDate");

			if (map.get("session") != null) {
				entity.session = Session.setPropertiesFromMap(new Session(), (HashMap<String, Object>) map.get("session"), nameMapping);
			}

			/* For local serialization */
			if (map.get("stats") != null) {
				entity.stats = new ArrayList<>();
				final List<LinkedHashMap<String, Object>> statMaps = (List<LinkedHashMap<String, Object>>) map.get("stats");
				for (Map<String, Object> statMap : statMaps) {
					entity.stats.add(Count.setPropertiesFromMap(new Count(), statMap, nameMapping));
				}
			}
		}

		return entity;
	}

	public static Entity build() {
		User entity = new User();
		entity.schema = Constants.SCHEMA_ENTITY_USER;
		entity.id = "temp:" + DateTime.nowString(DateTime.DATE_NOW_FORMAT_FILENAME); // Temporary
		return entity;
	}

	@SuppressWarnings("unchecked")
	@Override public User clone() {
		final User user = (User) super.clone();
		if (user != null && stats != null) {
			user.stats = (List<Count>) ((ArrayList) stats).clone();
		}
		return user;
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	public static class Role {
		public static String OWNER  = "owner";
		public static String MEMBER = "member";
	}
}