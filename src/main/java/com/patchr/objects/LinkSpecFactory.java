package com.patchr.objects;

import android.content.res.Resources;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.objects.Link.Direction;
import com.patchr.utilities.Maps;

import java.util.ArrayList;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class LinkSpecFactory {

	public static LinkSpec build(Integer linkProfile) {

		if (linkProfile == LinkSpecType.NO_LINKS)
			return null;
		else {
			User currentUser = Patchr.getInstance().getCurrentUser();

			LinkSpec linkSpec = new LinkSpec().setActive(new ArrayList<LinkSpecItem>());
			linkSpec.shortcuts = true;

			Resources resources = Patchr.applicationContext.getResources();
			Number limitProximity = resources.getInteger(R.integer.limit_links_proximity_default);
			Number limitLike = resources.getInteger(R.integer.limit_links_like_default);
			Number limitCreate = resources.getInteger(R.integer.limit_links_create_default);
			Number limitWatch = resources.getInteger(R.integer.limit_links_watch_default);

			if (linkProfile == LinkSpecType.LINKS_FOR_PATCH || linkProfile == LinkSpecType.LINKS_FOR_BEACONS) {
				/*
				 * These are the same because LINKS_FOR_BEACONS is used to produce patches and we want the same link
				 * profile for patches regardless of what code path fetches them.
				 */
				linkSpec.getActive().add(new LinkSpecItem(Constants.TYPE_LINK_PROXIMITY, Constants.SCHEMA_ENTITY_BEACON, true, true, limitProximity)
						.setDirection(Direction.out));
				linkSpec.getActive().add(new LinkSpecItem(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_USER, true, true, 1
						, Maps.asMap("_from", currentUser.id))
						.setDirection(Direction.in));
				linkSpec.getActive().add(new LinkSpecItem(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_MESSAGE, true, true, 1
						, Maps.asMap("_creator", currentUser.id))
						.setDirection(Direction.in));
			}
			else if (linkProfile == LinkSpecType.LINKS_FOR_MESSAGE) {

				linkSpec.getActive().add(new LinkSpecItem(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_PATCH, true, true, 1)
						.setDirection(Direction.out));
				linkSpec.getActive().add(new LinkSpecItem(Constants.TYPE_LINK_SHARE, Constants.SCHEMA_ENTITY_PATCH, true, true, 1)
						.setDirection(Direction.out));
				linkSpec.getActive().add(new LinkSpecItem(Constants.TYPE_LINK_SHARE, Constants.SCHEMA_ENTITY_MESSAGE, true, true, 1)
						.setDirection(Direction.out));
				linkSpec.getActive().add(new LinkSpecItem(Constants.TYPE_LINK_SHARE, Constants.SCHEMA_ENTITY_USER, true, true, 5)
						.setDirection(Direction.out));
				linkSpec.getActive().add(new LinkSpecItem(Constants.TYPE_LINK_LIKE, Constants.SCHEMA_ENTITY_USER, true, true, 1
						, Maps.asMap("_from", currentUser.id))
						.setDirection(Direction.in));
			}
			else if (linkProfile == LinkSpecType.LINKS_FOR_USER_CURRENT) {

				linkSpec.getActive().add(new LinkSpecItem(Constants.TYPE_LINK_LIKE, Constants.SCHEMA_ENTITY_PATCH, true, true, limitLike)
						.setDirection(Direction.out));
				linkSpec.getActive().add(new LinkSpecItem(Constants.TYPE_LINK_CREATE, Constants.SCHEMA_ENTITY_PATCH, true, true, limitCreate)
						.setDirection(Direction.out));
				linkSpec.getActive().add(new LinkSpecItem(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_PATCH, true, true, limitWatch)
						.setDirection(Direction.out));
				linkSpec.getActive().add(new LinkSpecItem(Constants.TYPE_LINK_CREATE, Constants.SCHEMA_ENTITY_MESSAGE, true, true, limitCreate)
						.setDirection(Direction.out));
			}
			else if (linkProfile == LinkSpecType.LINKS_FOR_USER) {

				linkSpec.getActive().add(new LinkSpecItem(Constants.TYPE_LINK_LIKE, Constants.SCHEMA_ENTITY_PATCH, false, true, 0)
						.setDirection(Direction.out));
				linkSpec.getActive().add(new LinkSpecItem(Constants.TYPE_LINK_CREATE, Constants.SCHEMA_ENTITY_PATCH, false, true, 0)
						.setDirection(Direction.out));
				linkSpec.getActive().add(new LinkSpecItem(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_PATCH, false, true, 0)
						.setDirection(Direction.out));
			}
			return linkSpec;
		}
	}
}