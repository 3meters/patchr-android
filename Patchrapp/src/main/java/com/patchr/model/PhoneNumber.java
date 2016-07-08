package com.patchr.model;

import com.patchr.objects.ServiceObject;
import com.patchr.utilities.Type;

import java.io.Serializable;
import java.util.Map;

@SuppressWarnings("ucd")
public class PhoneNumber extends ServiceObject implements Cloneable, Serializable {

	private static final long serialVersionUID = 4979315562693226999L;

	public String countryCode;
	public String number;

	public PhoneNumber() {}

	public PhoneNumber(String countryCode, String number) {
		this.countryCode = countryCode;
		this.number = number;
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public static PhoneNumber setPropertiesFromMap(PhoneNumber phoneNumber, Map map) {

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
}