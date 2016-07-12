package com.patchr.objects;

import android.support.annotation.NonNull;

import com.patchr.model.RealmEntity;

import java.util.Comparator;

/**
 * Created by jaymassena on 7/10/16.
 */
public class SortBySortDateAscending implements Comparator<RealmEntity> {

	@Override
	public int compare(@NonNull RealmEntity object1, @NonNull RealmEntity object2) {
		if (object1.sortDate == null || object2.sortDate == null)
			return 0;
		else {
			if (object1.sortDate.longValue() > object2.sortDate.longValue())
				return 1;
			else if (object1.sortDate.longValue() == object2.sortDate.longValue())
				return 0;
			return -1;
		}
	}
}
