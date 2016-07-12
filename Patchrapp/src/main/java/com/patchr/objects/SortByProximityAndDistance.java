package com.patchr.objects;

import com.patchr.model.RealmEntity;

import java.util.Comparator;

/**
 * Created by jaymassena on 7/10/16.
 */
public class SortByProximityAndDistance implements Comparator<RealmEntity> {

	@Override public int compare(RealmEntity object1, RealmEntity object2) {
		/*
		 * Ordering
		 * 1. has distance
		 * 2. distance is null
		 */
		if (object1.distance == null && object2.distance == null)
			return 0;
		else if (object1.distance == null)
			return 1;
		else if (object2.distance == null)
			return -1;
		else {
			if (object1.distance.intValue() < object2.distance.intValue())
				return -1;
			else if (object1.distance.intValue() > object2.distance.intValue())
				return 1;
			else
				return 0;
		}
	}
}
