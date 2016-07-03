package com.patchr.objects;

import com.patchr.Constants;
import com.patchr.components.UserManager;
import com.patchr.objects.Link.Direction;
import com.patchr.utilities.Maps;

import java.util.ArrayList;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class LinkSpecFactory {

	public static LinkSpecs build(Integer linkProfile) {

		if (linkProfile == LinkSpecType.NO_LINKS)
			return null;
		else {

			LinkSpecs linkSpec = new LinkSpecs().setActive(new ArrayList<LinkSpecItem>());
			linkSpec.shortcuts = true;

			if (linkProfile == LinkSpecType.LINKS_FOR_PATCH || linkProfile == LinkSpecType.LINKS_FOR_BEACONS) {
				/*
				 * These are the same because LINKS_FOR_BEACONS is used to produce patches and we want the same link
				 * profile for patches regardless of what code path fetches them.
				 */
				linkSpec.getActive().add(new LinkSpecItem(Constants.TYPE_LINK_PROXIMITY, Constants.SCHEMA_ENTITY_BEACON, true, true, 10)
						.setDirection(Direction.out));
				linkSpec.getActive().add(new LinkSpecItem(Constants.TYPE_LINK_MEMBER, Constants.SCHEMA_ENTITY_USER, true, true, 1
						, UserManager.shared().authenticated() ? Maps.asMap("_from", UserManager.currentRealmUser.id) : null)
						.setDirection(Direction.in));
				linkSpec.getActive().add(new LinkSpecItem(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_MESSAGE, true, true, 1
						, UserManager.shared().authenticated() ? Maps.asMap("_creator", UserManager.currentRealmUser.id) : null)
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
						, UserManager.shared().authenticated() ? Maps.asMap("_from", UserManager.currentRealmUser.id) : null)
						.setDirection(Direction.in));
			}
			else if (linkProfile == LinkSpecType.LINKS_FOR_USER_CURRENT) {

				linkSpec.getActive().add(new LinkSpecItem(Constants.TYPE_LINK_LIKE, Constants.SCHEMA_ENTITY_PATCH, true, true, 0)
						.setDirection(Direction.out));
				linkSpec.getActive().add(new LinkSpecItem(Constants.TYPE_LINK_CREATE, Constants.SCHEMA_ENTITY_PATCH, true, true, 0)
						.setDirection(Direction.out));
				linkSpec.getActive().add(new LinkSpecItem(Constants.TYPE_LINK_MEMBER, Constants.SCHEMA_ENTITY_PATCH, true, true, 0)
						.setDirection(Direction.out));
				linkSpec.getActive().add(new LinkSpecItem(Constants.TYPE_LINK_CREATE, Constants.SCHEMA_ENTITY_MESSAGE, true, true, 0)
						.setDirection(Direction.out));
			}
			else if (linkProfile == LinkSpecType.LINKS_FOR_USER) {

				linkSpec.getActive().add(new LinkSpecItem(Constants.TYPE_LINK_LIKE, Constants.SCHEMA_ENTITY_PATCH, false, true, 0)
						.setDirection(Direction.out));
				linkSpec.getActive().add(new LinkSpecItem(Constants.TYPE_LINK_CREATE, Constants.SCHEMA_ENTITY_PATCH, false, true, 0)
						.setDirection(Direction.out));
				linkSpec.getActive().add(new LinkSpecItem(Constants.TYPE_LINK_MEMBER, Constants.SCHEMA_ENTITY_PATCH, false, true, 0)
						.setDirection(Direction.out));
			}
			return linkSpec;
		}
	}
}