package com.patchr.objects;

import android.support.annotation.NonNull;

import com.patchr.service.Expose;
import com.patchr.utilities.Json;
import com.patchr.utilities.Type;

import java.io.Serializable;
import java.util.Map;

@SuppressWarnings("ucd")
public class PhoneNumber extends ServiceObject implements Cloneable, Serializable {

	private static final long serialVersionUID = 4979315562693226999L;

	@Expose
	public String countryCode;
	@Expose
	public String number;

	public PhoneNumber() {}

	public PhoneNumber(String countryCode, String number) {
		this.countryCode = countryCode;
		this.number = number;
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@NonNull public static PhoneNumber setPropertiesFromMap(@NonNull PhoneNumber phoneNumber, @NonNull Map map, Boolean nameMapping) {

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
		final PhoneNumber other = (PhoneNumber) obj;
		return (Type.equal(this.countryCode + this.number, other.countryCode + other.number));
	}

	public String toJson() {
		String jsonPhone = Json.objectToJson(this);
		return jsonPhone;
	}

	public static PhoneNumber fromJson(@NonNull String json) {
		PhoneNumber phoneNumber = (PhoneNumber) Json.jsonToObject(json, Json.ObjectType.PHONE);
		return phoneNumber;
	}

	@NonNull
	@Override public PhoneNumber clone() {
		try {
			PhoneNumber phoneNumber = (PhoneNumber) super.clone();
			return phoneNumber;
		}
		catch (CloneNotSupportedException e) {
			throw new RuntimeException("PhoneNumber not clonable");
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/
}