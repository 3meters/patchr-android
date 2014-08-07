package com.aircandi.catalina.ui.components;

import android.view.View;
import android.widget.TextView;

import com.aircandi.Constants;
import com.aircandi.catalina.R;
import com.aircandi.components.RenderDelegate;
import com.aircandi.objects.Count;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Link.Direction;
import com.aircandi.objects.User;
import com.aircandi.utilities.UI;

@SuppressWarnings("ucd")
public class UserStats implements RenderDelegate {

	//@SuppressWarnings("unused")
	@Override
	public void draw(Entity entity, View view) {

		User user = (User) entity;
		final TextView stats = (TextView) view.findViewById(R.id.stats);

		UI.setVisibility(stats, View.GONE);
		
		/* Watch stats */

		TextView watchingStats = (TextView) view.findViewById(R.id.watching_stats);
		if (watchingStats != null) {
			Count count = user.getCount(Constants.TYPE_LINK_WATCH, null, null, Direction.in);
			if (count == null) {
				count = new Count(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_PICTURE, null, 0);
			}
			watchingStats.setText(String.valueOf(count.count));
		}

	}
}
