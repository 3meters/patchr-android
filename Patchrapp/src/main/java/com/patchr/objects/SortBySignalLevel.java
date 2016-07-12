package com.patchr.objects;

import com.patchr.Constants;
import com.patchr.model.RealmEntity;

import java.util.Comparator;

/**
 * Created by jaymassena on 7/10/16.
 */
public class SortBySignalLevel implements Comparator<RealmEntity> {

	@Override public int compare(RealmEntity object1, RealmEntity object2) {

		if (object1.signal == null && object2.signal == null)
			return 0;
		else if (object1.signal == null)
			return 1;
		else if (object2.signal == null)
			return -1;
		else {
			if ((object1.signal / Constants.RADAR_BEACON_SIGNAL_BUCKET_SIZE)
				> (object2.signal / Constants.RADAR_BEACON_SIGNAL_BUCKET_SIZE))
				return -1;
			else if ((object1.signal / Constants.RADAR_BEACON_SIGNAL_BUCKET_SIZE)
				< (object2.signal / Constants.RADAR_BEACON_SIGNAL_BUCKET_SIZE))
				return 1;
			else
				return 0;
		}
	}
}
