package com.aircandi.catalina.ui;

import com.aircandi.Aircandi;
import com.aircandi.catalina.Constants;
import com.aircandi.catalina.components.ActivityDecorator;
import com.aircandi.catalina.components.EntityManager;
import com.aircandi.catalina.components.MenuManager;
import com.aircandi.catalina.controllers.Messages;
import com.aircandi.catalina.controllers.Places;
import com.aircandi.catalina.controllers.Users;
import com.aircandi.catalina.objects.Links;
import com.aircandi.components.ActivityRecognitionManager;
import com.aircandi.components.AnimationManager;
import com.aircandi.components.LocationManager;
import com.aircandi.components.Logger;
import com.aircandi.components.MediaManager;
import com.aircandi.components.ShortcutManager;
import com.aircandi.controllers.Applinks;
import com.aircandi.controllers.Beacons;
import com.aircandi.controllers.Comments;
import com.aircandi.controllers.Maps;
import com.aircandi.controllers.Pictures;

public class SplashForm extends com.aircandi.ui.SplashForm {


	@Override
	protected void configure() {
		/*
		 * Only called when app is first started
		 */		
		Aircandi.getInstance()
				.setMenuManager(new MenuManager())
				.setActivityDecorator(new ActivityDecorator())
				.setShortcutManager(new ShortcutManager())
				.setEntityManager(new EntityManager().setLinks(new Links()))
				.setMediaManager(new MediaManager().initSoundPool())
				.setAnimationManager(new AnimationManager());

		Aircandi.controllerMap.put(Constants.SCHEMA_ENTITY_APPLINK, new Applinks());
		Aircandi.controllerMap.put(Constants.SCHEMA_ENTITY_BEACON, new Beacons());
		Aircandi.controllerMap.put(Constants.SCHEMA_ENTITY_COMMENT, new Comments());
		Aircandi.controllerMap.put(Constants.SCHEMA_ENTITY_PICTURE, new Pictures());
		Aircandi.controllerMap.put(Constants.SCHEMA_ENTITY_PLACE, new Places());
		Aircandi.controllerMap.put(Constants.SCHEMA_ENTITY_USER, new Users());
		Aircandi.controllerMap.put(Constants.TYPE_APP_MAP, new Maps());
		Aircandi.controllerMap.put(Constants.SCHEMA_ENTITY_MESSAGE, new Messages());		
		
		/* Start out with anonymous user then upgrade to signed in user if possible */
		Aircandi.getInstance().initializeUser();
		
		/* Stash last known location but doesn't start location updates */
		LocationManager.getInstance().initialize(getApplicationContext());		

		/* Starts activity recognition */
		ActivityRecognitionManager.getInstance().initialize(getApplicationContext());
		
		Logger.i(this, "First run configuration completed");
	}

}