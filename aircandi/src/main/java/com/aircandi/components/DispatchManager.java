package com.aircandi.components;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.TrackerBase.TrackerCategory;
import com.aircandi.controllers.IEntityController;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Link.Direction;
import com.aircandi.objects.Photo;
import com.aircandi.objects.Place;
import com.aircandi.objects.Route;
import com.aircandi.objects.Shortcut;
import com.aircandi.objects.Shortcut.InstallStatus;
import com.aircandi.objects.ShortcutMeta;
import com.aircandi.objects.TransitionType;
import com.aircandi.ui.AboutForm;
import com.aircandi.ui.AircandiForm;
import com.aircandi.ui.HelpForm;
import com.aircandi.ui.PhotoForm;
import com.aircandi.ui.Preferences;
import com.aircandi.ui.SplashForm;
import com.aircandi.ui.base.BaseActivity;
import com.aircandi.ui.edit.ApplinkEdit;
import com.aircandi.ui.edit.ApplinkListEdit;
import com.aircandi.ui.edit.FeedbackEdit;
import com.aircandi.ui.edit.ReportEdit;
import com.aircandi.ui.edit.TuningEdit;
import com.aircandi.ui.helpers.AddressBuilder;
import com.aircandi.ui.helpers.ApplicationPicker;
import com.aircandi.ui.helpers.CategoryBuilder;
import com.aircandi.ui.helpers.PhotoPicker;
import com.aircandi.ui.helpers.PhotoSourcePicker;
import com.aircandi.ui.helpers.PlacePicker;
import com.aircandi.ui.helpers.ShortcutPicker;
import com.aircandi.ui.user.PasswordEdit;
import com.aircandi.ui.user.RegisterEdit;
import com.aircandi.ui.user.ResetEdit;
import com.aircandi.ui.user.SignInEdit;
import com.aircandi.utilities.Debug;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Json;

import java.util.ArrayList;
import java.util.List;

public class DispatchManager {

	public void intent(Activity activity, Intent intent) {
		activity.startActivity(intent);
		Aircandi.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
	}

	public void route(final Activity activity, Integer route, Entity entity, Shortcut shortcut, Bundle extras) {

		String schema = null;
		if (extras != null) {
			schema = extras.getString(Constants.EXTRA_ENTITY_SCHEMA);
		}

		if (schema == null && entity != null) {
			schema = entity.schema;
		}

		if (route == Route.HOME) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, AircandiForm.class);
			Intent intent = intentBuilder.create();
			intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

			activity.startActivity(intent);
			Aircandi.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_HELP);
		}

		else if (route == Route.SHORTCUT) {

			if (shortcut == null)
				throw new IllegalArgumentException("valid shortcut required for selected route");

			final ShortcutMeta meta = Shortcut.shortcutMeta.get(shortcut.app);
			Boolean installCheck = (meta == null || (meta.installStatus != InstallStatus.LATER && meta.installStatus != InstallStatus.DECLINED));

			if (meta != null && meta.installStatus == InstallStatus.LATER) {
				meta.installStatus = InstallStatus.NONE;
			}

			if (installCheck) {
				if (AndroidManager.hasIntentSupport(shortcut.app)
						&& AndroidManager.appExists(shortcut.app)
						&& !AndroidManager.isAppInstalled(shortcut.app)) {
					Dialogs.installApp(activity, shortcut, entity);
					return;
				}
			}

			if (shortcut.group != null && shortcut.group.size() > 1) {
				IntentBuilder intentBuilder = new IntentBuilder(activity, ShortcutPicker.class);
				intentBuilder.setEntity(entity);
				final Intent intent = intentBuilder.create();
				final List<String> shortcutStrings = new ArrayList<String>();
				for (Shortcut item : shortcut.group) {
					Shortcut clone = item.clone();
					clone.group = null;
					shortcutStrings.add(Json.objectToJson(clone, Json.UseAnnotations.FALSE, Json.ExcludeNulls.TRUE));
				}
				intent.putStringArrayListExtra(Constants.EXTRA_SHORTCUTS, (ArrayList<String>) shortcutStrings);
				activity.startActivity(intent);
				Aircandi.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
			}
			else {
				shortcut(activity, shortcut, entity, null, null);
			}

		}

		else if (route == Route.BROWSE) {

			IEntityController controller = Aircandi.getInstance().getControllerForSchema(schema);
			controller.view(activity, entity, entity.id, entity.toId, null, extras, true);
		}

		else if (route == Route.EDIT) {

			if (Aircandi.getInstance().getCurrentUser().isAnonymous()) {
				String message = StringManager.getString(R.string.alert_signin_message_edit, schema);
				Dialogs.signinRequired(activity, message);
				return;
			}

			IEntityController controller = Aircandi.getInstance().getControllerForSchema(schema);
			controller.edit(activity, entity, extras, true);
		}

		else if (route == Route.ADD) {

			((BaseActivity) activity).onAdd(new Bundle());
		}

		else if (route == Route.NEW_PLACE) {

			if (extras == null) {
				extras = new Bundle();
			}

			extras.putString(Constants.EXTRA_ENTITY_SCHEMA, Constants.SCHEMA_ENTITY_PLACE);

			((BaseActivity) activity).onAdd(extras);
		}

		else if (route == Route.SHARE) {

			((BaseActivity) activity).share();
		}

		else if (route == Route.NEW) {

			if (Aircandi.getInstance().getCurrentUser().isAnonymous()) {
				String message = StringManager.getString(R.string.alert_signin_message_add, schema);
				if (schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
					message = StringManager.getString(R.string.alert_signin_message_place_new, schema);
				}
				Dialogs.signinRequired(activity, message);
				return;
			}
			else {
				if (!Aircandi.getInstance().getMenuManager().canUserAdd(entity)) {
					if (entity.locked) {
						Dialogs.locked(activity, entity);
					}
					return;
				}
			}

			IEntityController controller = Aircandi.getInstance().getControllerForSchema(schema);
			controller.insert(activity, extras, true);
		}

		else if (route == Route.NEW_PICKER) {

			/* Launches application picker */

			if (Aircandi.getInstance().getCurrentUser().isAnonymous()) {
				String message = StringManager.getString(R.string.alert_signin_message_add_to, schema);
				Dialogs.signinRequired(activity, message);
				return;
			}
			else {
				if (!Aircandi.getInstance().getMenuManager().canUserAdd(entity)) {
					if (entity.locked) {
						Dialogs.locked(activity, entity);
					}
					return;
				}
			}

			IntentBuilder intentBuilder = new IntentBuilder(activity, ApplicationPicker.class);
			intentBuilder.setEntity(entity);
			activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_APPLICATION_PICK);
			Aircandi.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
		}

		else if (route == Route.REFRESH) {

			((BaseActivity) activity).onRefresh();
		}

		else if (route == Route.SETTINGS) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, Preferences.class);
			activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_PREFERENCES);
			Aircandi.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
		}

		else if (route == Route.FEEDBACK) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, FeedbackEdit.class);
			activity.startActivity(intentBuilder.create());
			Aircandi.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
		}

		else if (route == Route.REPORT) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, ReportEdit.class);
			if (extras == null) {
				extras = new Bundle();
			}
			extras.putString(Constants.EXTRA_ENTITY_SCHEMA, entity.getSchemaMapped());
			intentBuilder.setEntityId(entity.id).addExtras(extras);
			activity.startActivity(intentBuilder.create());
			Aircandi.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
		}

		else if (route == Route.ABOUT) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, AboutForm.class);
			activity.startActivity(intentBuilder.create());
			Aircandi.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
		}

		else if (route == Route.HELP) {

			if (extras == null) {
				((BaseActivity) activity).onHelp();
			}
			else {
				IntentBuilder intentBuilder = new IntentBuilder(activity, HelpForm.class);
				intentBuilder.setExtras(extras);
				activity.startActivity(intentBuilder.create());
				Aircandi.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_HELP);
			}
		}

		else if (route == Route.PHOTOS) {

			if (entity == null)
				throw new IllegalArgumentException("valid entity required for selected route");

			final Photo photo = entity.photo;
			photo.setCreatedAt(entity.modifiedDate.longValue());
			photo.setName(entity.name);
			photo.setUser(entity.creator);
			final String jsonPhoto = Json.objectToJson(photo);
			extras.putString(Constants.EXTRA_PHOTO, jsonPhoto);

			final IntentBuilder intentBuilder = new IntentBuilder(activity, PhotoForm.class);

			if (entity.toId != null) {
				intentBuilder.setEntityParentId(entity.toId);
			}

			intentBuilder.addExtras(extras);

			Intent intent = intentBuilder.create();
			activity.startActivity(intent);
			Aircandi.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_PAGE);
		}

		else if (route == Route.PHOTO) {
		    /*
			 * Single photo to show and it has already been serialized into the extras bundle.
			 */
			final IntentBuilder intentBuilder = new IntentBuilder(activity, PhotoForm.class);
			intentBuilder.setExtras(extras);
			Intent intent = intentBuilder.create();
			activity.startActivity(intent);
			Aircandi.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_PAGE);
		}

		else if (route == Route.ACCEPT) {

			((BaseActivity) activity).onAccept();    // Give activity a chance for discard confirmation
		}

		else if (route == Route.CANCEL) {

			((BaseActivity) activity).onCancel(false);    // Give activity a chance for discard confirmation
		}

		else if (route == Route.CANCEL_FORCE) {

			((BaseActivity) activity).onCancel(true);    // Give activity a chance for discard confirmation
		}

		else if (route == Route.DELETE) {

			((BaseActivity) activity).confirmDelete();    // Give activity a chance for discard confirmation
		}

		else if (route == Route.REMOVE) {

			((BaseActivity) activity).confirmRemove(extras.getString(Constants.EXTRA_ENTITY_PARENT_ID));    // Give activity a chance for remove confirmation
		}

		else if (route == Route.ZOOM_IN) {

			((PhotoForm) activity).onZoomIn();
		}

		else if (route == Route.ZOOM_OUT) {

			((PhotoForm) activity).onZoomOut();
		}

		else if (route == Route.SIGNOUT) {

			BaseActivity.signout(activity, false);
		}

		else if (route == Route.TEST) {
			((ApplinkEdit) activity).onTestButtonClick();
		}

		else if (route == Route.SIGNIN) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, SignInEdit.class);
			activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_SIGNIN);
			Aircandi.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
		}

		else if (route == Route.REGISTER) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, RegisterEdit.class);
			activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_SIGNIN);
			Aircandi.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
		}

		else if (route == Route.TERMS) {

			final IntentBuilder intentBuilder = new IntentBuilder(android.content.Intent.ACTION_VIEW);
			intentBuilder.setData(Uri.parse(StringManager.getString(R.string.url_terms)));
			activity.startActivity(intentBuilder.create());
			Aircandi.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
		}

		else if (route == Route.PRIVACY) {

			final IntentBuilder intentBuilder = new IntentBuilder(android.content.Intent.ACTION_VIEW);
			intentBuilder.setData(Uri.parse(StringManager.getString(R.string.url_privacy)));
			activity.startActivity(intentBuilder.create());
			Aircandi.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
		}

		else if (route == Route.LEGAL) {

			final IntentBuilder intentBuilder = new IntentBuilder(android.content.Intent.ACTION_VIEW);
			intentBuilder.setData(Uri.parse(StringManager.getString(R.string.url_legal)));
			activity.startActivity(intentBuilder.create());
			Aircandi.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
		}

		else if (route == Route.SETTINGS_LOCATION) {

			activity.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
			Aircandi.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
			activity.finish();
		}

		else if (route == Route.SETTINGS_WIFI) {

			activity.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
			Aircandi.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
			activity.finish();
		}

		else if (route == Route.ADDRESS_EDIT) {

			IntentBuilder intentBuilder = new IntentBuilder(activity, AddressBuilder.class);
			intentBuilder.setEntity(entity);
			activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_ADDRESS_EDIT);
			Aircandi.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
		}

		else if (route == Route.CATEGORY_EDIT) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, CategoryBuilder.class);
			final Intent intent = intentBuilder.create();

			if (((Place) entity).category != null) {
				final String jsonCategory = Json.objectToJson(((Place) entity).category);
				intent.putExtra(Constants.EXTRA_CATEGORY, jsonCategory);
			}

			activity.startActivityForResult(intent, Constants.ACTIVITY_CATEGORY_EDIT);
			Aircandi.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
		}

		else if (route == Route.PASSWORD_CHANGE) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, PasswordEdit.class);
			activity.startActivity(intentBuilder.create());
			Aircandi.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
		}

		else if (route == Route.PASSWORD_RESET) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, ResetEdit.class);
			activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_RESET_AND_SIGNIN);
			Aircandi.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
		}

		else if (route == Route.SPLASH) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, SplashForm.class);
			final Intent intent = intentBuilder.create();
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
			if (activity instanceof BaseActivity) {
				((BaseActivity) activity).setResultCode(Activity.RESULT_CANCELED);
			}
			activity.startActivity(intent);
			activity.finish();
			if (Aircandi.getInstance().getAnimationManager() != null) {
				Aircandi.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.FORM_TO_PAGE);
			}
		}

		else if (route == Route.PHOTO_SOURCE) {

			IntentBuilder intentBuilder = new IntentBuilder(activity, PhotoSourcePicker.class);
			intentBuilder.setEntity(entity);
			activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_PICTURE_SOURCE_PICK);
			Aircandi.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
		}

		else if (route == Route.APPLINKS_EDIT) {

			if (entity == null)
				throw new IllegalArgumentException("valid entity required for selected route");
			IntentBuilder intentBuilder = new IntentBuilder(activity, ApplinkListEdit.class);
			intentBuilder.setEntityId(entity.id).addExtras(extras);
			activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_APPLINKS_EDIT);
			Aircandi.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
		}

		else if (route == Route.PHOTO_FROM_CAMERA) {

			IntentBuilder intentBuilder = new IntentBuilder(MediaStore.ACTION_IMAGE_CAPTURE);
			intentBuilder.setExtras(extras);
			activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_PHOTO_MAKE);
			Aircandi.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
		}

		else if (route == Route.PHOTO_SEARCH) {

			IntentBuilder intentBuilder = new IntentBuilder(activity, PhotoPicker.class);
			intentBuilder.setExtras(extras);
			activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_PHOTO_SEARCH);
			Aircandi.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
		}

		else if (route == Route.PHOTO_PLACE_SEARCH) {

			if (entity == null)
				throw new IllegalArgumentException("valid entity required for selected route");
			IntentBuilder intentBuilder = new IntentBuilder(activity, PhotoPicker.class);
			intentBuilder.setEntityId(entity.id);
			activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_PHOTO_PICK_PLACE);
			Aircandi.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
		}

		else if (route == Route.PLACE_SEARCH) {

			IntentBuilder intentBuilder = new IntentBuilder(activity, PlacePicker.class);
			intentBuilder.setExtras(extras);
			activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_PLACE_SEARCH);
			Aircandi.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
		}

		else if (route == Route.TUNE) {

			if (entity == null)
				throw new IllegalArgumentException("valid entity required for selected route");
			IntentBuilder intentBuilder = new IntentBuilder(activity, TuningEdit.class);
			intentBuilder.setEntity(entity);
			activity.startActivity(intentBuilder.create());
			Aircandi.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
		}
		else if (route == Route.SAVE_BEACON) {

			Debug.insertBeacon();
		}

	}

	public void shortcut(final Activity activity, Shortcut shortcut, Entity entity, Direction direction, Bundle extras) {

		if (shortcut.schema.equals(Constants.SCHEMA_ENTITY_APPLINK)) {
			Aircandi.tracker.sendEvent(TrackerCategory.UX, "applink_click", shortcut.app, 0);

			IEntityController controller = Aircandi.getInstance().getControllerForSchema(shortcut.app);
			if (controller != null) {
				if (shortcut.getAction().equals(Constants.ACTION_VIEW)) {
					controller.view(activity, entity, shortcut.appId, entity.id, shortcut.linkType, extras, true);
				}
				else if (shortcut.getAction().equals(Constants.ACTION_VIEW_FOR)) {
					controller.viewFor(activity, null, entity.id, shortcut.linkType, direction, null, true, true);
				}
				else if (shortcut.getAction().equals(Constants.ACTION_VIEW_AUTO)) {
					controller.viewFor(activity, null, entity.id, shortcut.linkType, direction, null, true, true);
				}
			}
			else if (shortcut.app.equals(Constants.TYPE_APP_TWITTER)) {
				AndroidManager.getInstance().callTwitterActivity(activity, (shortcut.appId != null) ? shortcut.appId : shortcut.appUrl);
			}
			else if (shortcut.app.equals(Constants.TYPE_APP_FOURSQUARE)) {
				//AndroidManager.getInstance().callFoursquareActivity(activity, (shortcut.appId != null) ? shortcut.appId : shortcut.appUrl);
				AndroidManager.getInstance().callFoursquareActivity(activity, shortcut.appId, shortcut.appUrl);
			}
			else if (shortcut.app.equals(Constants.TYPE_APP_FACEBOOK)) {
				AndroidManager.getInstance().callFacebookActivity(activity, (shortcut.appId != null) ? shortcut.appId : shortcut.appUrl);
			}
			else if (shortcut.app.equals(Constants.TYPE_APP_YELP)) {
				AndroidManager.getInstance().callYelpActivity(activity, shortcut.appId, shortcut.appUrl);
			}
			else if (shortcut.app.equals(Constants.TYPE_APP_OPENTABLE)) {
				AndroidManager.getInstance().callOpentableActivity(activity, shortcut.appId, shortcut.appUrl);
			}
			else if (shortcut.app.equals(Constants.TYPE_APP_WEBSITE)) {
				AndroidManager.getInstance().callBrowserActivity(activity, (shortcut.appUrl != null) ? shortcut.appUrl : shortcut.appId);
			}
			else if (shortcut.app.equals(Constants.TYPE_APP_EMAIL)) {
				AndroidManager.getInstance().callSendToActivity(activity, shortcut.name, shortcut.appId, null, null);
			}
			else {
				AndroidManager.getInstance().callGenericActivity(activity, (shortcut.appUrl != null) ? shortcut.appUrl : shortcut.appId);
			}
		}
		else {
			if (shortcut.isContent()) {
				IEntityController controller = Aircandi.getInstance().getControllerForSchema(shortcut.app);
				if (controller != null) {
					if (shortcut.getAction().equals(Constants.ACTION_VIEW)) {
						controller.view(activity, entity, shortcut.getId(), (entity != null) ? entity.toId : null, shortcut.linkType, extras, true);
					}
				}
			}
			else if (shortcut.intent != null) {
				intent(activity, shortcut.intent);
			}
		}
	}

	public Integer routeForMenuId(int itemId) {

		if (itemId == R.id.edit)
			return Route.EDIT;
		else if (itemId == R.id.help)
			return Route.HELP;
		else if (itemId == R.id.settings)
			return Route.SETTINGS;
		else if (itemId == android.R.id.home)
			return Route.CANCEL;
		else if (itemId == R.id.home)
			return Route.HOME;
		else if (itemId == R.id.signout)
			return Route.SIGNOUT;
		else if (itemId == R.id.signin)
			return Route.SIGNIN;
		else if (itemId == R.id.feedback)
			return Route.FEEDBACK;
		else if (itemId == R.id.report)
			return Route.REPORT;
		else if (itemId == R.id.cancel)
			return Route.CANCEL;
		else if (itemId == R.id.accept)
			return Route.ACCEPT;
		else if (itemId == R.id.refresh)
			return Route.REFRESH;
		else if (itemId == R.id.add)
			return Route.ADD;
		else if (itemId == R.id.new_place)
			return Route.NEW_PLACE;
		else if (itemId == R.id.share)
			return Route.SHARE;
		else if (itemId == R.id.delete)
			return Route.DELETE;
		else if (itemId == R.id.remove)
			return Route.REMOVE;
		else if (itemId == R.id.navigate)
			return Route.NAVIGATE;
		else if (itemId == R.id.save_beacon)
			return Route.SAVE_BEACON;
		else if (itemId == R.id.zoom_in)
			return Route.ZOOM_IN;
		else if (itemId == R.id.zoom_out)
			return Route.ZOOM_OUT;
		else if (itemId == R.id.watchers)
			return Route.WATCHERS;

		return Route.UNKNOWN;
	}
}
