package com.aircandi.objects;

import android.support.annotation.Nullable;

import com.aircandi.Constants;
import com.aircandi.components.LocationManager;
import com.aircandi.service.Expose;

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
	 *--------------------------------------------------------------------------------------------*/    public Boolean test = false;

	public Beacon() {
	}

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
	@Override
	public Float getDistance(Boolean refresh) {

		if (refresh || distance == null) {

			distance = -1f;

			if (signal.intValue() >= -40) {
				distance = 1f;
			}
			else if (signal.intValue() >= -50) {
				distance = 2f;
			}
			else if (signal.intValue() >= -55) {
				distance = 3f;
			}
			else if (signal.intValue() >= -60) {
				distance = 5f;
			}
			else if (signal.intValue() >= -65) {
				distance = 7f;
			}
			else if (signal.intValue() >= -70) {
				distance = 10f;
			}
			else if (signal.intValue() >= -75) {
				distance = 15f;
			}
			else if (signal.intValue() >= -80) {
				distance = 20f;
			}
			else if (signal.intValue() >= -85) {
				distance = 30f;
			}
			else if (signal.intValue() >= -90) {
				distance = 40f;
			}
			else if (signal.intValue() >= -95) {
				distance = 60f;
			}
			else {
				distance = 80f;
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
	@Nullable
	public Beacon clone() {
		final Beacon entity = (Beacon) super.clone();
		return entity;
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/    public static class SortBySignalLevel implements Comparator<Beacon> {

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