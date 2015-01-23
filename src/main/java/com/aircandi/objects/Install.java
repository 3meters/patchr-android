package com.aircandi.objects;

import android.support.annotation.NonNull;

import com.aircandi.service.Expose;
import com.aircandi.service.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Jayma
 */

@SuppressWarnings("ucd")
public class Install extends ServiceBase implements Cloneable, Serializable {

	private static final long   serialVersionUID = 694133954499515095L;
	public static final  String collectionId     = "installs";

	@Expose
	@SerializedName(name = "_user")
	public String       userId;
	@Expose
	public String       registrationId;
	@Expose
	public String       installId;
	@Expose
	public Number       clientVersionCode;
	@Expose
	public String       clientVersionName;
	@Expose
	public String       clientPackageName;
	@Expose
	public String       deviceName;
	@Expose
	public List<Beacon> beacons;
	@Expose
	public Number       beaconsDate;
	@Expose
	public AirLocation  location;
	@Expose
	public Number       locationDate;

	public Install() {}

	public Install(@NonNull String userId, @NonNull String registrationId, @NonNull String installId) {
		this.userId = userId;
		this.registrationId = registrationId;
		this.installId = installId;
	}

	public static Install setPropertiesFromMap(Install install, Map map, Boolean nameMapping) {

		install = (Install) ServiceBase.setPropertiesFromMap(install, map, nameMapping);
		install.userId = (String) (nameMapping ? map.get("_user") : map.get("userId"));
		install.registrationId = (String) map.get("registrationId");
		install.installId = (String) map.get("installId");
		install.clientVersionCode = (Number) map.get("clientVersionCode");
		install.clientVersionName = (String) map.get("clientVersionName");
		install.clientPackageName = (String) map.get("clientPackageName");
		install.deviceName = (String) map.get("deviceName");
		install.beaconsDate = (Number) map.get("beaconsDate");
		install.locationDate = (Number) map.get("locationDate");

		if (map.get("beacons") != null) {
			install.beacons = new ArrayList<Beacon>();
			final List<LinkedHashMap<String, Object>> beaconMaps = (List<LinkedHashMap<String, Object>>) map.get("beacons");
			for (Map<String, Object> beaconMap : beaconMaps) {
				install.beacons.add(Beacon.setPropertiesFromMap(new Beacon(), beaconMap, nameMapping));
			}
		}

		if (map.get("location") != null) {
			install.location = AirLocation.setPropertiesFromMap(new AirLocation(), (HashMap<String, Object>) map.get("location"), nameMapping);
		}

		return install;
	}

	@Override
	public String getCollection() {
		return collectionId;
	}
}