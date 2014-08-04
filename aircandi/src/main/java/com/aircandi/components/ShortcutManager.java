package com.aircandi.components;

import java.util.List;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Link;
import com.aircandi.objects.Link.Direction;
import com.aircandi.objects.Shortcut;
import com.aircandi.utilities.Colors;

public class ShortcutManager {

	public void getClientShortcuts(List<Shortcut> shortcuts, Entity entity) {

		if (entity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {

			/* Picture */
			
			Shortcut shortcut = Shortcut.builder(entity
					, Constants.SCHEMA_ENTITY_APPLINK
					, Constants.TYPE_APP_POST
					, Constants.ACTION_VIEW_AUTO
					, StringManager.getString(R.string.label_link_pictures)
                    , null
					, "img_picture_temp"
					, 10
					, false
					, true);
			Link link = entity.getLink(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_PICTURE, null, Direction.in);
			if (link != null) {
				shortcut.photo = link.shortcut.getPhoto();
				shortcut.appId = link.fromId;
			}
			else {
				shortcut.photo.colorize = true;
				shortcut.photo.color = Colors.getColor(Aircandi.getInstance().getControllerForSchema(Constants.TYPE_APP_POST).getColorPrimary());
			}
			shortcut.linkType = Constants.TYPE_LINK_CONTENT;
			shortcuts.add(shortcut);

			/* Maps: Map is evaluated in shortcut.isActive() at draw time to determine if it gets shown */
			
			shortcut = Shortcut.builder(entity
					, Constants.SCHEMA_ENTITY_APPLINK
					, Constants.TYPE_APP_MAP
					, Constants.ACTION_VIEW
					, StringManager.getString(R.string.label_link_map)
                    , null
					, "img_map_temp"
					, 30
					, false
					, true);
			shortcut.photo.colorize = true;
			shortcut.photo.color = Colors.getColor(Aircandi.getInstance().getControllerForSchema(Constants.TYPE_APP_MAP).getColorPrimary());
			shortcuts.add(shortcut);

			/* Comments */
			
			shortcut = Shortcut.builder(entity
					, Constants.SCHEMA_ENTITY_APPLINK
					, Constants.TYPE_APP_COMMENT
					, Constants.ACTION_VIEW_FOR
					, StringManager.getString(R.string.label_link_comments)
                    , null
					, "img_comment_temp"
					, 20
					, false
					, true);
			shortcut.photo.colorize = true;
			shortcut.photo.color = Colors.getColor(Aircandi.getInstance().getControllerForSchema(Constants.TYPE_APP_COMMENT).getColorPrimary());
			shortcut.linkType = Constants.TYPE_LINK_CONTENT;
			shortcuts.add(shortcut);
		}		
		else if (entity.schema.equals(Constants.SCHEMA_ENTITY_PICTURE)) {

			/* Comments */
			
			Shortcut shortcut = Shortcut.builder(entity
					, Constants.SCHEMA_ENTITY_APPLINK
					, Constants.TYPE_APP_COMMENT
					, Constants.ACTION_VIEW_FOR
					, StringManager.getString(R.string.label_link_comments)
                    , null
					, "img_comment_temp"
					, 20
					, false
					, true);
			shortcut.photo.colorize = true;
			shortcut.photo.color = Colors.getColor(Aircandi.getInstance().getControllerForSchema(Constants.TYPE_APP_COMMENT).getColorPrimary());
			shortcut.linkType = Constants.TYPE_LINK_CONTENT;
			shortcuts.add(shortcut);
		}		
	}
}
