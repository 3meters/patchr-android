package com.aircandi.controllers;

import java.util.Map;

import com.aircandi.Constants;
import com.aircandi.objects.Beacon;
import com.aircandi.objects.Entity;
import com.aircandi.utilities.DateTime;

public class Beacons extends EntityControllerBase {

	public Beacons() {
		mSchema = Constants.SCHEMA_ENTITY_BEACON;
	}

	@Override
	public Entity makeNew() {
		Entity entity = new Beacon();
		entity.schema = mSchema;
		entity.id = "temp:" + DateTime.nowString(DateTime.DATE_NOW_FORMAT_FILENAME); // Temporary
		entity.signalFence = -100.0f;
		return entity;
	}

	@Override
	public Entity makeFromMap(Map<String, Object> map, Boolean nameMapping) {
		return Beacon.setPropertiesFromMap(new Beacon(), map, nameMapping);
	}
}
