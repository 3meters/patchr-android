package com.aircandi.catalina.objects;

import java.util.ArrayList;

import android.content.res.Resources;

import com.aircandi.Aircandi;
import com.aircandi.catalina.Constants;
import com.aircandi.catalina.R;
import com.aircandi.objects.Link.Direction;
import com.aircandi.objects.LinkParams;
import com.aircandi.objects.User;
import com.aircandi.utilities.Maps;

/**
 * @author Jayma
 */
public class Links extends com.aircandi.objects.Links {

	private static final long serialVersionUID = 6358655034455139946L;

	@Override
	public com.aircandi.objects.Links build(Integer linkProfile) {

		if (linkProfile == LinkProfile.NO_LINKS)
			return null;
		else {
			User currentUser = Aircandi.getInstance().getCurrentUser();

			com.aircandi.objects.Links links = new Links().setActive(new ArrayList<LinkParams>());
			links.shortcuts = true;

			Resources resources = Aircandi.applicationContext.getResources();
			Number limitProximity = resources.getInteger(R.integer.limit_links_proximity_default);
			Number limitCreate = resources.getInteger(R.integer.limit_links_create_default);
			Number limitWatch = resources.getInteger(R.integer.limit_links_watch_default);
			Number limitContent = resources.getInteger(R.integer.limit_links_content_default);

			if (linkProfile == LinkProfile.LINKS_FOR_PLACE || linkProfile == LinkProfile.LINKS_FOR_BEACONS) {
				/*
				 * These are the same because LINKS_FOR_BEACONS is used to produce places and we want the same link
				 * profile for places regardless of what code path fetches them.
				 */
				links.getActive().add(new LinkParams(Constants.TYPE_LINK_PROXIMITY, Constants.SCHEMA_ENTITY_BEACON, true, true, limitProximity));
				links.getActive().add(new LinkParams(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_MESSAGE, true, true, limitContent));
				links.getActive().add(new LinkParams(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_USER, true, true, 1
						, Maps.asMap("_from", currentUser.id)));
			}
			else if (linkProfile == LinkProfile.LINKS_FOR_MESSAGE) {

				links.getActive().add(new LinkParams(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_PLACE, true, true, 1)
						.setDirection(Direction.out));
				links.getActive().add(new LinkParams(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_MESSAGE, true, true, 1)
						.setDirection(Direction.both));
				links.getActive().add(new LinkParams(Constants.TYPE_LINK_SHARE, Constants.SCHEMA_ENTITY_PLACE, true, true, 1)
						.setDirection(Direction.out));
				links.getActive().add(new LinkParams(Constants.TYPE_LINK_SHARE, Constants.SCHEMA_ENTITY_MESSAGE, true, true, 1)
						.setDirection(Direction.out));
				links.getActive().add(new LinkParams(Constants.TYPE_LINK_SHARE, Constants.SCHEMA_ENTITY_USER, true, true, 5)
						.setDirection(Direction.out));
			}
			else if (linkProfile == LinkProfile.LINKS_FOR_USER_CURRENT) {

				links.getActive().add(new LinkParams(Constants.TYPE_LINK_CREATE, Constants.SCHEMA_ENTITY_MESSAGE, true, true, limitCreate)
						.setDirection(Direction.out));
				links.getActive().add(new LinkParams(Constants.TYPE_LINK_CREATE, Constants.SCHEMA_ENTITY_PLACE, true, true, limitCreate)
						.setDirection(Direction.out));
				links.getActive().add(new LinkParams(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_PLACE, true, true, limitWatch)
						.setDirection(Direction.out));
			}
			else if (linkProfile == LinkProfile.LINKS_FOR_USER) {

				links.getActive().add(new LinkParams(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_USER, true, true, 1
						, Maps.asMap("_from", currentUser.id)));
			}
			else {
				links = super.build(linkProfile);
			}
			return links;
		}
	}
}