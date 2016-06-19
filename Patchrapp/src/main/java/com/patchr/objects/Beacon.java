package com.patchr.objects;

import android.support.annotation.NonNull;

import com.patchr.Constants;
import com.patchr.components.LocationManager;
import com.patchr.service.Expose;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;

/**
 * @author Jayma
 */

@SuppressWarnings("ucd")
public class Beacon extends Entity implements Cloneable, Serializable {

	private static final long   serialVersionUID = 694133954499515095L;
	public static final  String collectionId     = "beacons";
	public static final  String schemaName       = "beacon";
	public static final  String schemaId         = "be";

	/*--------------------------------------------------------------------------------------------
	 * service fields
	 *--------------------------------------------------------------------------------------------*/
	@Expose
	public String ssid;
	@Expose
	public String bssid;
	@Expose
	public Number signal;                                    // Used to evaluate location accuracy

	/*--------------------------------------------------------------------------------------------
	 * client fields (NONE are transferred)
	 *--------------------------------------------------------------------------------------------*/

	public Boolean test = false;

	public Beacon() {}

	public Beacon(String bssid, String ssid, String label, int levelDb, Boolean test) { // $codepro.audit.disable largeNumberOfParameters
		id = "be." + bssid;
		this.ssid = ssid;
		this.bssid = bssid;
		this.name = label;
		this.signal = levelDb;
		this.test = test;
	}

	/*--------------------------------------------------------------------------------------------
	 * Set and get
	 *--------------------------------------------------------------------------------------------*/

	@NonNull
	@Override
	public Float getDistance(Boolean refresh) {

		if (refresh || this.distance == null) {

			this.distance = -1f;

			if (this.signal.intValue() >= -40) {
				this.distance = 1f;
			}
			else if (this.signal.intValue() >= -50) {
				this.distance = 2f;
			}
			else if (this.signal.intValue() >= -55) {
				this.distance = 3f;
			}
			else if (this.signal.intValue() >= -60) {
				this.distance = 5f;
			}
			else if (this.signal.intValue() >= -65) {
				this.distance = 7f;
			}
			else if (this.signal.intValue() >= -70) {
				this.distance = 10f;
			}
			else if (this.signal.intValue() >= -75) {
				this.distance = 15f;
			}
			else if (this.signal.intValue() >= -80) {
				this.distance = 20f;
			}
			else if (this.signal.intValue() >= -85) {
				this.distance = 30f;
			}
			else if (this.signal.intValue() >= -90) {
				this.distance = 40f;
			}
			else if (this.signal.intValue() >= -95) {
				this.distance = 60f;
			}
			else {
				this.distance = 80f;
			}
		}

		return distance * LocationManager.FeetToMetersConversion;
	}

	@Override
	public String getCollection() {
		return collectionId;
	}

	/*--------------------------------------------------------------------------------------------
	 * Copy and serialization
	 *--------------------------------------------------------------------------------------------*/

	public static Beacon setPropertiesFromMap(Beacon entity, Map map, Boolean nameMapping) {

		synchronized (entity) {
			entity = (Beacon) Entity.setPropertiesFromMap(entity, map, nameMapping);
			entity.ssid = (String) map.get("ssid");
			entity.bssid = (String) map.get("bssid");
			entity.signal = (Number) map.get("signal");
		}
		return entity;
	}

	@Override
	public Beacon clone() {
		final Beacon entity = (Beacon) super.clone();
		return entity;
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	public static class SortBySignalLevel implements Comparator<Beacon> {

		@Override
		public int compare(Beacon object1, Beacon object2) {
			if ((object1.signal.intValue() / Constants.RADAR_BEACON_SIGNAL_BUCKET_SIZE)
					> (object2.signal.intValue() / Constants.RADAR_BEACON_SIGNAL_BUCKET_SIZE))
				return -1;
			else if ((object1.signal.intValue() / Constants.RADAR_BEACON_SIGNAL_BUCKET_SIZE)
					< (object2.signal.intValue() / Constants.RADAR_BEACON_SIGNAL_BUCKET_SIZE))
				return 1;
			else
				return 0;
		}
	}
}