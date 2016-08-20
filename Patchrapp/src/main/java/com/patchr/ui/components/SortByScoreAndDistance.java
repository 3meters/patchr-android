package com.patchr.ui.components;

import android.support.annotation.NonNull;

import com.patchr.model.RealmEntity;

import java.util.Comparator;

public class SortByScoreAndDistance implements Comparator<RealmEntity> {

	@Override public int compare(@NonNull RealmEntity object1, @NonNull RealmEntity object2) {

		if (object1.score > object2.score)
			return -1;
		else if (object2.score < object1.score)
			return 1;
		else {
			if (object1.distance == null || object2.distance == null)
				return 0;
			else if (object1.distance < object2.distance.intValue())
				return -1;
			else if (object1.distance.intValue() > object2.distance.intValue())
				return 1;
			else
				return 0;
		}
	}
}
