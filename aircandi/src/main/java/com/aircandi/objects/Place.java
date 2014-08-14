package com.aircandi.objects;

import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;

import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.service.Expose;
import com.aircandi.utilities.Colors;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class Place extends Entity implements Cloneable, Serializable {

	private static final long   serialVersionUID = -3599862145425838670L;
	public static final  String collectionId     = "places";
	public static final  String schemaName       = "place";
	public static final  String schemaId         = "pl";

	/*--------------------------------------------------------------------------------------------
	 * service fields
	 *--------------------------------------------------------------------------------------------*/
	@Expose
	public String address;
	@Expose
	public String      city;
	@Expose
	public String      region;
	@Expose
	public String      country;
	@Expose
	public String      postalCode;
	@Expose
	public String      phone;
	@Expose
	public ProviderMap provider;
	@Expose
	public Category    category;
	@Expose(serialize = false, deserialize = true)
	public Number      applinkDate;

	/*--------------------------------------------------------------------------------------------
	 * client fields (NONE are transferred)
	 *--------------------------------------------------------------------------------------------*/ 	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/
	public static Place upsizeFromSynthetic(Place synthetic) {
	    /*
		 * Sythetic entity created from foursquare data
		 * 
		 * We make a copy so these changes don't effect the synthetic entity
		 * in the entity model in case we keep it because of a failure.
		 */
		final Place entity = synthetic.clone();
		entity.locked = false;
		if (synthetic.category != null) {
			entity.subtitle = synthetic.category.name;
		}
		return entity;
	}

	@Override
	public Photo getPhoto() {
		Photo photo = this.photo;
		if (photo == null) {
			if (category != null && category.photo != null) {
				photo = category.photo.clone();
			}
			else {
				photo = getDefaultPhoto();
			}
		}
		return photo;
	}

	public String getBeaconId() {
		final Beacon beacon = getActiveBeacon(Constants.TYPE_LINK_PROXIMITY, true);
		if (beacon != null) return beacon.id;
		return null;
	}

	public Provider getProvider() {
		if (provider.aircandi != null)
			return new Provider(provider.aircandi, Constants.TYPE_PROVIDER_AIRCANDI);
		else if (provider.foursquare != null)
			return new Provider(provider.foursquare, Constants.TYPE_PROVIDER_FOURSQUARE);
		else if (provider.yelp != null)
			return new Provider(provider.yelp, Constants.TYPE_PROVIDER_YELP);
		else if (provider.google != null)
			return new Provider(provider.google, Constants.TYPE_PROVIDER_GOOGLE);
		else if (provider.factual != null)
			return new Provider(provider.factual, Constants.TYPE_PROVIDER_FACTUAL);
		return null;
	}

	public String getAddressBlock() {
		String addressBlock = "";
		if (!TextUtils.isEmpty(address)) {
			addressBlock = address + "<br/>";
		}

		if (!TextUtils.isEmpty(city) && !TextUtils.isEmpty(region)) {
			addressBlock += city + ", " + region;
		}
		else if (!TextUtils.isEmpty(city)) {
			addressBlock += city;
		}
		else if (!TextUtils.isEmpty(region)) {
			addressBlock += region;
		}

		if (!TextUtils.isEmpty(postalCode)) {
			addressBlock += " " + postalCode;
		}
		return addressBlock;
	}

	public String getAddressString(Boolean includePostalCode) {
		String addressString = "";
		if (!TextUtils.isEmpty(address)) {
			addressString = address;
		}

		if (!TextUtils.isEmpty(city) && !TextUtils.isEmpty(region)) {
			addressString += (!TextUtils.isEmpty(addressString) ? ", " : "") + city + ", " + region;
		}
		else if (!TextUtils.isEmpty(city)) {
			addressString += (!TextUtils.isEmpty(addressString) ? ", " : "") + city;
		}
		else if (!TextUtils.isEmpty(region)) {
			addressString += (!TextUtils.isEmpty(addressString) ? ", " : "") + region;
		}

		if (includePostalCode && !TextUtils.isEmpty(postalCode)) {
			addressString += " " + postalCode;
		}
		return addressString;
	}

	public String getFormattedPhone() {
		return PhoneNumberUtils.formatNumber(phone);
	}

	public static Integer getCategoryColor(String categoryName) {
		int colorResId = getCategoryColorResId(categoryName);
		return Colors.getColor(colorResId);
	}

	public static Integer getCategoryColorResId(String categoryName) {
		int colorResId = R.color.gray_50_pcnt;
		if (categoryName != null && !categoryName.toLowerCase(Locale.US).equals("generic")) {

			final Random rand = new Random(categoryName.hashCode());
			final int colorIndex = rand.nextInt(5 - 1 + 1) + 1;
			if (colorIndex == 1) {
				colorResId = R.color.holo_blue_dark;
			}
			else if (colorIndex == 2) {
				colorResId = R.color.holo_orange_dark;
			}
			else if (colorIndex == 3) {
				colorResId = R.color.holo_green_dark;
			}
			else if (colorIndex == 4) {
				colorResId = R.color.holo_purple_dark;
			}
			else if (colorIndex == 5) {
				colorResId = R.color.holo_red_dark;
			}
		}
		return colorResId;
	}

	@Override
	public String getCollection() {
		return collectionId;
	}

	/*--------------------------------------------------------------------------------------------
	 * Copy and serialization
	 *--------------------------------------------------------------------------------------------*/
	public static Place setPropertiesFromMap(Place entity, Map map, Boolean nameMapping) {
		/*
		 * Properties involved with editing are copied from one entity to another.
		 */
		entity = (Place) Entity.setPropertiesFromMap(entity, map, nameMapping);

		entity.address = (String) map.get("address");
		entity.city = (String) map.get("city");
		entity.region = (String) map.get("region");
		entity.country = (String) map.get("country");
		entity.postalCode = (String) map.get("postalCode");
		entity.phone = (String) map.get("phone");
		entity.applinkDate = (Number) map.get("applinkDate");

		if (map.get("provider") != null) {
			entity.provider = ProviderMap.setPropertiesFromMap(new ProviderMap(), (HashMap<String, Object>) map.get("provider"), nameMapping);
		}

		if (map.get("category") != null) {
			entity.category = Category.setPropertiesFromMap(new Category(), (HashMap<String, Object>) map.get("category"), nameMapping);
		}

		return entity;
	}

	@Override
	public Place clone() {
		final Place place = (Place) super.clone();
		if (location != null) {
			place.location = location.clone();
		}
		if (provider != null) {
			place.provider = provider.clone();
		}
		if (category != null) {
			place.category = category.clone();
		}
		return place;
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/    public static class SortByProximityAndDistance implements Comparator<Entity> {

		@Override
		public int compare(Entity object1, Entity object2) {

			if (object1.hasActiveProximity() && !object2.hasActiveProximity())
				return -1;
			else if (object2.hasActiveProximity() && !object1.hasActiveProximity())
				return 1;
			else {
				/*
				 * Ordering
				 * 1. has distance and not fuzzy
				 * 2. fuzzy 
				 * 3. distance is null
				 */
				if (object1.distance == null && object2.distance == null)
					return 0;
				else if (object1.distance == null && object2.distance != null)
					return 1;
				else if (object2.distance == null && object1.distance != null)
					return -1;
				else if (object1.fuzzy && object2.fuzzy)
					return 0;
				else if (object1.fuzzy && !object2.fuzzy)
					return 1;
				else if (object2.fuzzy && !object1.fuzzy)
					return -1;
				else if (object1.distance < object2.distance.intValue())
					return -1;
				else if (object1.distance.intValue() > object2.distance.intValue())
					return 1;
				else
					return 0;
			}
		}
	}

	@SuppressWarnings("ucd")
	public static class ReasonType {
		public static String WATCH    = "watch";
		public static String LOCATION = "location";
		public static String RECENT   = "recent";
		public static String OTHER    = "other";
	}
}