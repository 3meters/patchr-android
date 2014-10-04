package com.aircandi.components;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.Nullable;

import com.aircandi.Constants;
import com.aircandi.Patch;
import com.aircandi.R;
import com.aircandi.interfaces.IEntityController;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Link.Direction;
import com.aircandi.objects.Photo;
import com.aircandi.objects.Place;
import com.aircandi.objects.Route;
import com.aircandi.objects.Shortcut;
import com.aircandi.objects.TransitionType;
import com.aircandi.ui.AboutForm;
import com.aircandi.ui.AircandiForm;
import com.aircandi.ui.HelpForm;
import com.aircandi.ui.MapForm;
import com.aircandi.ui.PhotoForm;
import com.aircandi.ui.PlaceList;
import com.aircandi.ui.Preferences;
import com.aircandi.ui.SplashForm;
import com.aircandi.ui.WatcherList;
import com.aircandi.ui.base.BaseActivity;
import com.aircandi.ui.edit.FeedbackEdit;
import com.aircandi.ui.edit.ReportEdit;
import com.aircandi.ui.edit.TuningEdit;
import com.aircandi.ui.helpers.AddressBuilder;
import com.aircandi.ui.helpers.CategoryBuilder;
import com.aircandi.ui.helpers.LocationPicker;
import com.aircandi.ui.helpers.PhotoPicker;
import com.aircandi.ui.helpers.PhotoSourcePicker;
import com.aircandi.ui.helpers.PlacePicker;
import com.aircandi.ui.user.PasswordEdit;
import com.aircandi.ui.user.RegisterEdit;
import com.aircandi.ui.user.ResetEdit;
import com.aircandi.ui.user.SignInEdit;
import com.aircandi.utilities.Debug;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Json;

public class DispatchManager {

	public void intent(Activity activity, Intent intent) {
		activity.startActivity(intent);
		Patch.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
	}

	public void route(final Activity activity, Integer route, @Nullable Entity entity, Shortcut shortcut, Bundle extras) {

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
			Patch.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_HELP);
		}

		else if (route == Route.BROWSE) {

			if (entity == null) {
				throw new IllegalArgumentException("Dispatching browse requires entity");
			}

			IEntityController controller = Patch.getInstance().getControllerForSchema(schema);
			controller.view(activity, entity, entity.id, entity.toId, null, extras, true);
		}

		else if (route == Route.EDIT) {

			if (Patch.getInstance().getCurrentUser().isAnonymous()) {
				String message = StringManager.getString(R.string.alert_signin_message_edit, schema);
				Dialogs.signinRequired(activity, message);
				return;
			}

			if (entity == null) {
				throw new IllegalArgumentException("Dispatching edit requires entity");
			}

			IEntityController controller = Patch.getInstance().getControllerForSchema(schema);
			if (entity.schema.equals(Constants.SCHEMA_ENTITY_PLACE) && entity.isOwnedBySystem()) {
				if (extras == null) {
					extras = new Bundle();
				}
				extras.putInt(Constants.EXTRA_LAYOUT_RESID, R.layout.place_customize);
			}
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

			if (Patch.getInstance().getCurrentUser().isAnonymous()) {
				if (schema == null) {
					throw new IllegalArgumentException("Handling anonymous new requires schema");
				}
				String message = StringManager.getString(R.string.alert_signin_message_add, schema);
				if (schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
					message = StringManager.getString(R.string.alert_signin_message_place_new, schema);
				}
				Dialogs.signinRequired(activity, message);
				return;
			}
			else {
				if (!Patch.getInstance().getMenuManager().canUserAdd(entity)) {
					return;
				}
			}

			IEntityController controller = Patch.getInstance().getControllerForSchema(schema);
			controller.insert(activity, extras, true);
		}

		else if (route == Route.REFRESH) {

			((BaseActivity) activity).onRefresh();
		}

		else if (route == Route.MAP) {

			if (entity == null) {
				throw new IllegalArgumentException("Dispatching map requires entity");
			}
			final IntentBuilder intentBuilder = new IntentBuilder(activity, MapForm.class);
			if (extras == null) {
				extras = new Bundle();
			}
			intentBuilder.setEntityId(entity.id).addExtras(extras);
			activity.startActivity(intentBuilder.create());
			Patch.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
		}

		else if (route == Route.SETTINGS) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, Preferences.class);
			activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_PREFERENCES);
			Patch.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
		}

		else if (route == Route.FEEDBACK) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, FeedbackEdit.class);
			activity.startActivity(intentBuilder.create());
			Patch.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
		}

		else if (route == Route.REPORT) {

			if (entity == null) {
				throw new IllegalArgumentException("Dispatching report requires entity");
			}
			final IntentBuilder intentBuilder = new IntentBuilder(activity, ReportEdit.class);
			if (extras == null) {
				extras = new Bundle();
			}
			extras.putString(Constants.EXTRA_ENTITY_SCHEMA, entity.schema);
			intentBuilder.setEntityId(entity.id).addExtras(extras);
			activity.startActivity(intentBuilder.create());
			Patch.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
		}

		else if (route == Route.ABOUT) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, AboutForm.class);
			activity.startActivity(intentBuilder.create());
			Patch.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
		}

		else if (route == Route.HELP) {

			if (extras == null) {
				((BaseActivity) activity).onHelp();
			}
			else {
				IntentBuilder intentBuilder = new IntentBuilder(activity, HelpForm.class);
				intentBuilder.setExtras(extras);
				activity.startActivity(intentBuilder.create());
				Patch.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_HELP);
			}
		}

		else if (route == Route.PHOTOS) {

			if (entity == null) {
				throw new IllegalArgumentException("Valid entity required for selected route");
			}
			if (extras == null) {
				throw new IllegalArgumentException("Dispatching photos requires extras");
			}

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
			Patch.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_PAGE);
		}

		else if (route == Route.PHOTO) {
		    /*
			 * Single photo to show and it has already been serialized into the extras bundle.
			 */
			final IntentBuilder intentBuilder = new IntentBuilder(activity, PhotoForm.class);
			intentBuilder.setExtras(extras);
			Intent intent = intentBuilder.create();
			activity.startActivity(intent);
			Patch.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_PAGE);
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

			if (extras == null) {
				throw new IllegalArgumentException("Dispatching remove requires extras");
			}
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

		else if (route == Route.SIGNIN) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, SignInEdit.class);
			activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_SIGNIN);
			Patch.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
		}

		else if (route == Route.REGISTER) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, RegisterEdit.class);
			activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_SIGNIN);
			Patch.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
		}

		else if (route == Route.TERMS) {

			final IntentBuilder intentBuilder = new IntentBuilder(android.content.Intent.ACTION_VIEW);
			intentBuilder.setData(Uri.parse(StringManager.getString(R.string.url_terms)));
			activity.startActivity(intentBuilder.create());
			Patch.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
		}

		else if (route == Route.PRIVACY) {

			final IntentBuilder intentBuilder = new IntentBuilder(android.content.Intent.ACTION_VIEW);
			intentBuilder.setData(Uri.parse(StringManager.getString(R.string.url_privacy)));
			activity.startActivity(intentBuilder.create());
			Patch.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
		}

		else if (route == Route.LEGAL) {

			final IntentBuilder intentBuilder = new IntentBuilder(android.content.Intent.ACTION_VIEW);
			intentBuilder.setData(Uri.parse(StringManager.getString(R.string.url_legal)));
			activity.startActivity(intentBuilder.create());
			Patch.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
		}

		else if (route == Route.SETTINGS_LOCATION) {

			activity.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
			Patch.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
		}

		else if (route == Route.SETTINGS_WIFI) {

			activity.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
			Patch.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
			activity.finish();
		}

		else if (route == Route.ADDRESS_EDIT) {

			IntentBuilder intentBuilder = new IntentBuilder(activity, AddressBuilder.class);
			intentBuilder.setEntity(entity);
			activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_ADDRESS_EDIT);
			Patch.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
		}

		else if (route == Route.CATEGORY_EDIT) {

			if (entity == null) {
				throw new IllegalArgumentException("Dispatching category edit requires entity");
			}
			final IntentBuilder intentBuilder = new IntentBuilder(activity, CategoryBuilder.class);
			final Intent intent = intentBuilder.create();

			if (((Place) entity).category != null) {
				final String json = Json.objectToJson(((Place) entity).category);
				intent.putExtra(Constants.EXTRA_CATEGORY, json);
			}

			activity.startActivityForResult(intent, Constants.ACTIVITY_CATEGORY_EDIT);
			Patch.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.FORM_TO_BUILDER);
		}

		else if (route == Route.LOCATION_EDIT) {

			if (entity == null) {
				throw new IllegalArgumentException("Dispatching location edit requires entity");
			}
			final IntentBuilder intentBuilder = new IntentBuilder(activity, LocationPicker.class);
			final Intent intent = intentBuilder.create();

			if (((Place) entity).location != null) {
				final String json = Json.objectToJson(((Place) entity).location);
				intent.putExtra(Constants.EXTRA_LOCATION, json);
			}

			activity.startActivityForResult(intent, Constants.ACTIVITY_CATEGORY_EDIT);
			Patch.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.FORM_TO_BUILDER);
		}

		else if (route == Route.PASSWORD_CHANGE) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, PasswordEdit.class);
			activity.startActivity(intentBuilder.create());
			Patch.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
		}

		else if (route == Route.PASSWORD_RESET) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, ResetEdit.class);
			activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_RESET_AND_SIGNIN);
			Patch.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
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
			if (Patch.getInstance().getAnimationManager() != null) {
				Patch.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.FORM_TO_PAGE);
			}
		}

		else if (route == Route.PHOTO_SOURCE) {

			IntentBuilder intentBuilder = new IntentBuilder(activity, PhotoSourcePicker.class);
			intentBuilder.setEntity(entity);
			activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_PICTURE_SOURCE_PICK);
			Patch.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
		}

		else if (route == Route.PHOTO_FROM_CAMERA) {

			IntentBuilder intentBuilder = new IntentBuilder(MediaStore.ACTION_IMAGE_CAPTURE);
			intentBuilder.setExtras(extras);
			activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_PHOTO_MAKE);
			Patch.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
		}

		else if (route == Route.PHOTO_SEARCH) {

			IntentBuilder intentBuilder = new IntentBuilder(activity, PhotoPicker.class);
			intentBuilder.setExtras(extras);
			activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_PHOTO_SEARCH);
			Patch.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
		}

		else if (route == Route.PHOTO_PLACE_SEARCH) {

			if (entity == null)
				throw new IllegalArgumentException("valid entity required for selected route");
			IntentBuilder intentBuilder = new IntentBuilder(activity, PhotoPicker.class);
			intentBuilder.setEntityId(entity.id);
			activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_PHOTO_PICK_PLACE);
			Patch.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
		}

		else if (route == Route.PLACE_SEARCH) {

			IntentBuilder intentBuilder = new IntentBuilder(activity, PlacePicker.class);
			intentBuilder.setExtras(extras);
			activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_PLACE_SEARCH);
			Patch.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
		}

		else if (route == Route.TUNE) {

			if (entity == null)
				throw new IllegalArgumentException("valid entity required for selected route");
			IntentBuilder intentBuilder = new IntentBuilder(activity, TuningEdit.class);
			intentBuilder.setEntity(entity);
			activity.startActivity(intentBuilder.create());
			Patch.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
		}

		else if (route == Route.SAVE_BEACON) {

			Debug.insertBeacon();
		}

		else if (route == Route.WATCHERS) {

			if (entity == null) {
				throw new IllegalArgumentException("Dispatching watchers requires entity");
			}

			final IntentBuilder intentBuilder = new IntentBuilder(activity, WatcherList.class);
			intentBuilder.setEntityId(entity.id).addExtras(extras);
			activity.startActivity(intentBuilder.create());
			Patch.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
		}

		else if (route == Route.PLACE_LIST) {

			if (entity == null) {
				throw new IllegalArgumentException("Dispatching watchers requires entity");
			}

			final IntentBuilder intentBuilder = new IntentBuilder(activity, PlaceList.class);
			intentBuilder.setEntityId(entity.id).addExtras(extras);
			activity.startActivity(intentBuilder.create());
			Patch.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
		}
	}

	public void shortcut(final Activity activity, Shortcut shortcut, Entity entity, Direction direction, Bundle extras) {

		if (shortcut.isContent()) {
			IEntityController controller = Patch.getInstance().getControllerForSchema(shortcut.app);
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

	public Integer routeForMenuId(int itemId) {

		if (itemId == R.id.edit)
			return Route.EDIT;
		else if (itemId == R.id.help)
			return Route.HELP;
		else if (itemId == R.id.settings)
			return Route.SETTINGS;
		else if (itemId == android.R.id.home)
			return Route.CANCEL;
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

		return Route.UNKNOWN;
	}
}
