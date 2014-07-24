package com.aircandi.ui.components;

import android.text.Html;
import android.view.View;
import android.widget.TextView;

import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.RenderDelegate;
import com.aircandi.objects.Count;
import com.aircandi.objects.Entity;
import com.aircandi.objects.EventType;
import com.aircandi.objects.Link.Direction;
import com.aircandi.objects.User;
import com.aircandi.utilities.UI;

public class UserStats implements RenderDelegate {

	@SuppressWarnings("unused")
	@Override
	public void draw(Entity entity, View view) {

		User user = (User) entity;
		final TextView stats = (TextView) view.findViewById(R.id.stats);

		UI.setVisibility(stats, View.GONE);
		final StringBuilder statString = new StringBuilder(500);

		/* Watch stats */
		
		TextView watchingStats = (TextView) view.findViewById(R.id.watching_stats);
		if (watchingStats != null) {
			Count count = user.getCount(Constants.TYPE_LINK_WATCH, null, null, Direction.in);
			if (count == null) {
				count = new Count(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_PICTURE, null, 0);
			}
			watchingStats.setText(String.valueOf(count.count));
		}

		/* Other stats */

		if (stats != null && user.stats != null && user.stats.size() > 0) {

			int tuneCount = 0;

			int insertCount = 0;
			int insertPictureCount = 0;
			int insertPlaceCount = 0;
			int insertCommentCount = 0;

			int updateCount = 0;
			int updatePictureCount = 0;
			int updatePlaceCount = 0;

			for (Count stat : user.stats) {

				/* Tune */
				if (stat.type.startsWith(EventType.LINK_PROXIMITY)) {
					tuneCount += stat.count.intValue();
				}
				else if (stat.type.equals(EventType.ENTITY_PROXIMITY)) {
					tuneCount += stat.count.intValue();
				}

				/* Insert */
				else if (stat.type.equals(EventType.INSERT_PLACE)) {
					insertPlaceCount += stat.count.intValue();
					insertCount += stat.count.intValue();
				}
				else if (stat.type.equals(EventType.INSERT_PICTURE_TO_PLACE)) {
					insertPictureCount += stat.count.intValue();
					insertCount += stat.count.intValue();
				}
				else if (stat.type.equals(EventType.INSERT_COMMENT_TO_PLACE)
						|| stat.type.equals(EventType.INSERT_COMMENT_TO_PICTURE)) {
					insertCommentCount += stat.count.intValue();
					insertCount += stat.count.intValue();
				}

				/* Update */
				else if (stat.type.equals(EventType.UPDATE_PLACE)) {
					updatePlaceCount += stat.count.intValue();
					updateCount += stat.count.intValue();
				}
				else if (stat.type.equals(EventType.UPDATE_PICTURE)) {
					updatePictureCount += stat.count.intValue();
					updateCount += stat.count.intValue();
				}
			}

			if (insertPlaceCount > 0) {
				statString.append("Places: " + String.valueOf(insertPlaceCount) + "<br/>");
			}

			if (insertPictureCount > 0) {
				statString.append("Pictures: " + String.valueOf(insertPictureCount) + "<br/>");
			}

			if (insertCommentCount > 0) {
				statString.append("Comments: " + String.valueOf(insertCommentCount) + "<br/>");
			}

			if (updatePlaceCount > 0) {
				statString.append("Places edited: " + String.valueOf(updatePlaceCount) + "<br/>");
			}

			if (updatePictureCount > 0) {
				statString.append("Pictures edited: " + String.valueOf(updatePictureCount) + "<br/>");
			}

			if (tuneCount > 0) {
				statString.append("Places tuned: " + String.valueOf(tuneCount) + "<br/>");
			}

			stats.setText(Html.fromHtml(statString.toString()));
			UI.setVisibility(stats, View.VISIBLE);
		}
	}
}
