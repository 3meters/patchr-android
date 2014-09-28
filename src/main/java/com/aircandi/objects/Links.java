package com.aircandi.objects;

import android.content.res.Resources;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.objects.Link.Direction;
import com.aircandi.service.Expose;
import com.aircandi.utilities.Maps;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class Links extends ServiceObject {

	private static final long serialVersionUID = -274203160211174564L;

	@Expose
	public Boolean          shortcuts;
	@Expose
	public List<LinkParams> active;

	public Links() {
	}

	public Links(Boolean shortcuts, List<LinkParams> active) {
		setShortcuts(shortcuts);
		this.active = active;
	}

	public Links build(Integer linkProfile) {

		if (linkProfile == LinkProfile.NO_LINKS)
			return null;
		else {
			User currentUser = Aircandi.getInstance().getCurrentUser();

			Links links = new Links().setActive(new ArrayList<LinkParams>());
			links.shortcuts = true;

			Resources resources = Aircandi.applicationContext.getResources();
			Number limitProximity = resources.getInteger(R.integer.limit_links_proximity_default);
			Number limitCreate = resources.getInteger(R.integer.limit_links_create_default);
			Number limitWatch = resources.getInteger(R.integer.limit_links_watch_default);
			Number limitApplinks = resources.getInteger(R.integer.limit_links_applinks_default);
			Number limitContent = resources.getInteger(R.integer.limit_links_content_default);

			if (linkProfile == LinkProfile.LINKS_FOR_PLACE || linkProfile == LinkProfile.LINKS_FOR_BEACONS) {
				/*
				 * These are the same because LINKS_FOR_BEACONS is used to produce places and we want the same link
				 * profile for places regardless of what code path fetches them.
				 */
				links.getActive().add(new LinkParams(Constants.TYPE_LINK_PROXIMITY, Constants.SCHEMA_ENTITY_BEACON, true, true, limitProximity));
				links.getActive().add(new LinkParams(Constants.TYPE_LINK_CONTENT, com.aircandi.Constants.SCHEMA_ENTITY_MESSAGE, true, true, limitContent));
				links.getActive().add(new LinkParams(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_USER, true, true, 1
						, Maps.asMap("_from", currentUser.id)));
			}
			else if (linkProfile == LinkProfile.LINKS_FOR_PICTURE) {

				links.getActive().add(new LinkParams(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_APPLINK, true, true, limitApplinks));
				links.getActive().add(new LinkParams(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_COMMENT, false, true, 0));
				links.getActive().add(new LinkParams(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_USER, true, true, 1
						, Maps.asMap("_from", currentUser.id)));
			}
			else if (linkProfile == LinkProfile.LINKS_FOR_COMMENT) {

				links.getActive().add(new LinkParams(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_PLACE, true, true, 1)
						.setDirection(Direction.out));
				links.getActive().add(new LinkParams(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_PICTURE, true, true, 1)
						.setDirection(Direction.out));
			}
			else if (linkProfile == LinkProfile.LINKS_FOR_MESSAGE) {

				links.getActive().add(new LinkParams(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_PLACE, true, true, 1)
						.setDirection(Direction.out));
				links.getActive().add(new LinkParams(Constants.TYPE_LINK_CONTENT, com.aircandi.Constants.SCHEMA_ENTITY_MESSAGE, true, true, 1)
						.setDirection(Direction.both));
				links.getActive().add(new LinkParams(Constants.TYPE_LINK_SHARE, Constants.SCHEMA_ENTITY_PLACE, true, true, 1)
						.setDirection(Direction.out));
				links.getActive().add(new LinkParams(Constants.TYPE_LINK_SHARE, com.aircandi.Constants.SCHEMA_ENTITY_MESSAGE, true, true, 1)
						.setDirection(Direction.out));
				links.getActive().add(new LinkParams(Constants.TYPE_LINK_SHARE, Constants.SCHEMA_ENTITY_USER, true, true, 5)
						.setDirection(Direction.out));
			}
			else if (linkProfile == LinkProfile.LINKS_FOR_USER_CURRENT) {

				links.getActive().add(new LinkParams(Constants.TYPE_LINK_CREATE, com.aircandi.Constants.SCHEMA_ENTITY_MESSAGE, true, true, limitCreate)
						.setDirection(Direction.out));
				links.getActive().add(new LinkParams(Constants.TYPE_LINK_CREATE, Constants.SCHEMA_ENTITY_PLACE, true, true, limitCreate)
						.setDirection(Direction.out));
				links.getActive().add(new LinkParams(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_PLACE, true, true, limitWatch)
						.setDirection(Direction.out));
			}
			else if (linkProfile == LinkProfile.LINKS_FOR_USER) {

				links.getActive().add(new LinkParams(Constants.TYPE_LINK_CREATE, Constants.SCHEMA_ENTITY_PLACE, false, true, 0)
						.setDirection(Direction.out));
				links.getActive().add(new LinkParams(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_PLACE, false, true, 0)
						.setDirection(Direction.out));
				links.getActive().add(new LinkParams(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_USER, true, true, 1
						, Maps.asMap("_from", currentUser.id)));
			}
			return links;
		}
	}

	public List<LinkParams> getActive() {
		return active;
	}

	public Links setActive(List<LinkParams> active) {
		this.active = active;
		return this;
	}

	public Boolean getShortcuts() {
		return shortcuts;
	}

	public Links setShortcuts(Boolean shortcuts) {
		this.shortcuts = shortcuts;
		return this;
	}
}