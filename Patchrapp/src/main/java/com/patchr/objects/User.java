package com.patchr.objects;

import com.patchr.Constants;
import com.patchr.utilities.DateTime;

import java.util.List;
import java.util.Map;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class User extends Entity {

	private static final long serialVersionUID = 127428776257201065L;

	/* Persisted fields */

	public String      area;
	public Boolean     developer;
	public String      email;
	public String      password;
	public PhoneNumber phone;
	public String      role;

	/* Calculated fields */

	public Number patchesOwned  = 0;
	public Number patchesMember = 0;

	/* Client convenience fields */

	public String  authType;
	public Session session;

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@SuppressWarnings("unchecked")
	public static User setPropertiesFromMap(User user, Map map) {

		synchronized (user) {

			user = (User) Entity.setPropertiesFromMap(user, map);

			user.area = (String) map.get("area");
			user.email = (String) map.get("email");
			user.role = (String) map.get("role");
			user.developer = (Boolean) map.get("developer");
			user.authType = (String) map.get("authType");

			if (map.get("session") != null) {
				user.session = Session.setPropertiesFromMap(new Session(), (Map<String, Object>) map.get("session"));
			}

			if (map.get("phone") != null) {
				user.phone = PhoneNumber.setPropertiesFromMap(new PhoneNumber(), (Map<String, Object>) map.get("phone"));
			}

			user.patchesMember = 0;
			user.patchesOwned = 0;

			if (map.get("linkCounts") instanceof List) {
				List<Map<String, Object>> linkCounts = (List<Map<String, Object>>) map.get("linkCounts");
				for (Map<String, Object> linkMap : linkCounts) {
					LinkCount linkCount = LinkCount.setPropertiesFromMap(new LinkCount(), linkMap);
					if (linkCount.to.equals(LinkDestination.Patches) && linkCount.type.equals(LinkType.Create)) {
						user.patchesOwned = linkCount.count;
					}
					if (linkCount.to.equals(LinkDestination.Patches) && linkCount.type.equals(LinkType.Watch)) {
						user.patchesMember = linkCount.count;
					}
				}
			}

			return user;
		}
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