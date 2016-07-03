package com.patchr.model;

import com.patchr.utilities.Json;
import com.patchr.utilities.Type;

import java.util.Map;

import io.realm.RealmObject;

@SuppressWarnings("ucd")
public class RealmPhoneNumber extends RealmObject {

	public String countryCode;
	public String number;

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public static RealmPhoneNumber setPropertiesFromMap(RealmPhoneNumber phoneNumber, Map map) {

		if (!map.containsKey("countryCode")) {
			throw new RuntimeException("PhoneNumber object is missing required countryCode property");
		}

		if (!map.containsKey("number")) {
			throw new RuntimeException("PhoneNumber object is missing required number property");
		}

		phoneNumber.countryCode = (String) map.get("countryCode");
		phoneNumber.number = (String) map.get("number");

		return phoneNumber;
	}

	public String displayNumber() {
		return "+" + this.countryCode + this.number;
	}

	public boolean sameAs(Object obj) {
		if (obj == null) return false;
		if (!((Object) this).getClass().equals(obj.getClass())) return false;
		final RealmPhoneNumber other = (RealmPhoneNumber) obj;
		return (Type.equal(this.countryCode + this.number, other.countryCode + other.number));
	}

	public String toJson() {
		String jsonPhone = Json.objectToJson(this);
		return jsonPhone;
	}

	public static RealmPhoneNumber fromJson(String json) {
		RealmPhoneNumber phoneNumber = (RealmPhoneNumber) Json.jsonToObject(json, Json.ObjectType.PHONE);
		return phoneNumber;
	}
}