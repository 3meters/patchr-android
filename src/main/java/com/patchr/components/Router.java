package com.patchr.components;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.Fragment;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.interfaces.IEntityController;
import com.patchr.objects.Entity;
import com.patchr.objects.Patch;
import com.patchr.objects.Photo;
import com.patchr.objects.Route;
import com.patchr.objects.TransitionType;
import com.patchr.ui.AboutScreen;
import com.patchr.ui.BaseScreen;
import com.patchr.ui.ListScreen;
import com.patchr.ui.LobbyScreen;
import com.patchr.ui.MainScreen;
import com.patchr.ui.MapScreen;
import com.patchr.ui.PhotoScreen;
import com.patchr.ui.SearchScreen;
import com.patchr.ui.SettingsScreen;
import com.patchr.ui.edit.FeedbackEdit;
import com.patchr.ui.edit.LoginEdit;
import com.patchr.ui.edit.PasswordEdit;
import com.patchr.ui.edit.ProximityEdit;
import com.patchr.ui.edit.RegisterEdit;
import com.patchr.ui.edit.ReportEdit;
import com.patchr.ui.edit.ResetEdit;
import com.patchr.ui.fragments.MapListFragment;
import com.patchr.ui.edit.LocationEdit;
import com.patchr.ui.helpers.PhotoActionPicker;
import com.patchr.ui.helpers.PhotoPicker;
import com.patchr.ui.helpers.PrivacyBuilder;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Json;
import com.patchr.utilities.Type;

public class Router {

	public void intent(Activity activity, Intent intent) {
		activity.startActivity(intent);
		AnimationManager.doOverridePendingTransition(activity, TransitionType.FORM_TO);
	}

	public void route(final Context activity, Integer route, Entity entity, Bundle extras) {

		String schema = null;
		if (extras != null) {
			schema = extras.getString(Constants.EXTRA_ENTITY_SCHEMA);
		}

		if (schema == null && entity != null) {
			schema = entity.schema;
		}

		if (route == Route.HOME) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, MainScreen.class);
			Intent intent = intentBuilder.create();
			intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

			activity.startActivity(intent);
			AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.VIEW_TO);
		}

		else if (route == Route.BROWSE) {

			String entityId = null;
			String parentId = null;
			Boolean synthetic = false;
			if (entity != null) {
				entityId = entity.id;
				parentId = entity.toId;
				synthetic = entity.synthetic;
			}
			else if (extras != null) {
				entityId = extras.getString(Constants.EXTRA_ENTITY_ID);
				parentId = extras.getString(Constants.EXTRA_ENTITY_PARENT_ID);
			}

			if (Type.isFalse(synthetic) && entityId == null) {
				throw new IllegalArgumentException("Dispatching browse requires entity or extras.entityId");
			}

			if (schema != null) {
				IEntityController controller = Patchr.getInstance().getControllerForSchema(schema);
				controller.view(activity, entity, entityId, parentId, null, extras, true);
			}
		}

		else if (route == Route.TUNE) {

			if (entity == null) {
				throw new IllegalArgumentException("Dispatching tune requires entity");
			}
			final IntentBuilder intentBuilder = new IntentBuilder(activity, ProximityEdit.class);
			if (extras == null) {
				extras = new Bundle();
			}
			intentBuilder.setEntityId(entity.id).addExtras(extras);
			activity.startActivity(intentBuilder.create());
			AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.FORM_TO);
		}

		else if (route == Route.MAP) {

			if (entity == null) {
				throw new IllegalArgumentException("Dispatching map requires entity");
			}
			final IntentBuilder intentBuilder = new IntentBuilder(activity, MapScreen.class);
			if (extras == null) {
				extras = new Bundle();
			}

			intentBuilder.setEntity(entity).addExtras(extras);
			activity.startActivity(intentBuilder.create());
			AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.FORM_TO);
		}

		else if (route == Route.SETTINGS) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, SettingsScreen.class);
			((Activity) activity).startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_PREFERENCES);
			AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.FORM_TO);
		}

		else if (route == Route.FEEDBACK) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, FeedbackEdit.class);
			activity.startActivity(intentBuilder.create());
			AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.FORM_TO);
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
			AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.FORM_TO);
		}

		else if (route == Route.ABOUT) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, AboutScreen.class);
			activity.startActivity(intentBuilder.create());
			AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.FORM_TO);
		}

		else if (route == Route.PHOTO) {
		    /*
			 * Single photo to show and it has already been serialized into the extras bundle.
			 */
			final IntentBuilder intentBuilder = new IntentBuilder(activity, PhotoScreen.class);
			intentBuilder.setExtras(extras);
			Intent intent = intentBuilder.create();
			activity.startActivity(intent);
			AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.DRILL_TO);
		}

		else if (route == Route.PHOTO_EDIT) {

			if (extras == null || !extras.containsKey(Constants.EXTRA_PHOTO)) {
				throw new IllegalArgumentException("Valid photo in extras required for selected route");
			}

			final String jsonPhoto = extras.getString(Constants.EXTRA_PHOTO);
			if (jsonPhoto != null) {
				final Photo photo = (Photo) Json.jsonToObject(jsonPhoto, Json.ObjectType.PHOTO);
				final String url = photo.uriDirect();
				Uri uri = Uri.parse(url);

				if (AndroidManager.getInstance().isAviaryInstalled()) {
					Intent intent = new Intent("aviary.intent.action.EDIT");
					intent.setDataAndType(uri, "image/*"); // required
					intent.putExtra("app-id", Patchr.applicationContext.getPackageName()); // required ( it's your app unique package name )
					intent.putExtra("output-format", Bitmap.CompressFormat.JPEG.name());
					intent.putExtra("output-quality", 90);
					intent.putExtra("save-on-no-changes", false);

					((Activity)activity).startActivityForResult(intent, Constants.ACTIVITY_PHOTO_EDIT);
					AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.DRILL_TO);
				}
				else {
					Dialogs.installAviary((Activity) activity);
				}
			}
		}

		else if (route == Route.VIEW_AS_LIST) {

			Fragment fragment = ((MainScreen) activity).getCurrentFragment();
			if (fragment instanceof MapListFragment) {
				String listFragment = ((MapListFragment) fragment).relatedListFragment;
				if (listFragment == null) {
					listFragment = Constants.FRAGMENT_TYPE_NEARBY;
				}
				((MainScreen) activity).switchToFragment(listFragment);
			}
		}

		else if (route == Route.VIEW_AS_MAP) {

			((MainScreen) activity).switchToFragment(Constants.FRAGMENT_TYPE_MAP);
		}

		else if (route == Route.LOGIN) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, LoginEdit.class);
			((Activity)activity).startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_SIGNIN);
			AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.FORM_TO);
		}

		else if (route == Route.SIGNUP) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, RegisterEdit.class);
			((Activity)activity).startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_SIGNIN);
			AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.FORM_TO);
		}

		else if (route == Route.TERMS) {

			final IntentBuilder intentBuilder = new IntentBuilder(android.content.Intent.ACTION_VIEW);
			intentBuilder.setData(Uri.parse(StringManager.getString(R.string.url_terms)));
			activity.startActivity(intentBuilder.create());
			AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.FORM_TO);
		}

		else if (route == Route.PRIVACY) {

			final IntentBuilder intentBuilder = new IntentBuilder(android.content.Intent.ACTION_VIEW);
			intentBuilder.setData(Uri.parse(StringManager.getString(R.string.url_privacy)));
			activity.startActivity(intentBuilder.create());
			AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.FORM_TO);
		}

		else if (route == Route.LEGAL) {

			final IntentBuilder intentBuilder = new IntentBuilder(android.content.Intent.ACTION_VIEW);
			intentBuilder.setData(Uri.parse(StringManager.getString(R.string.url_legal)));
			activity.startActivity(intentBuilder.create());
			AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.FORM_TO);
		}

		else if (route == Route.SETTINGS_LOCATION) {

			activity.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
			AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.FORM_TO);
		}

		else if (route == Route.SETTINGS_WIFI) {

			activity.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
			AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.FORM_TO);
			((Activity)activity).finish();
		}

		else if (route == Route.PRIVACY_EDIT) {

			if (entity == null) {
				throw new IllegalArgumentException("Dispatching privacy edit requires entity");
			}
			final IntentBuilder intentBuilder = new IntentBuilder(activity, PrivacyBuilder.class);
			final Intent intent = intentBuilder.create();
			intent.putExtra(Constants.EXTRA_PRIVACY, ((Patch) entity).privacy);

			((Activity)activity).startActivityForResult(intent, Constants.ACTIVITY_PRIVACY_EDIT);
			AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.BUILDER_TO);
		}

		else if (route == Route.LOCATION_EDIT) {

			if (entity == null) {
				throw new IllegalArgumentException("Dispatching location edit requires entity");
			}
			final IntentBuilder intentBuilder = new IntentBuilder(activity, LocationEdit.class);
			final Intent intent = intentBuilder.create();

			if (((Patch) entity).location != null) {
				final String json = Json.objectToJson(((Patch) entity).location);
				intent.putExtra(Constants.EXTRA_LOCATION, json);
				intent.putExtra(Constants.EXTRA_TITLE, entity.name);
			}

			((Activity)activity).startActivityForResult(intent, Constants.ACTIVITY_LOCATION_EDIT);
			AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.BUILDER_TO);
		}

		else if (route == Route.PASSWORD_CHANGE) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, PasswordEdit.class);
			activity.startActivity(intentBuilder.create());
			AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.FORM_TO);
		}

		else if (route == Route.PASSWORD_RESET) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, ResetEdit.class);
			((Activity)activity).startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_RESET_AND_SIGNIN);
			AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.FORM_TO);
		}

		else if (route == Route.LOBBY) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, LobbyScreen.class);
			final Intent intent = intentBuilder.create();
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
			if (activity instanceof BaseScreen) {
				((BaseScreen) activity).setResult(Activity.RESULT_CANCELED);
			}
			activity.startActivity(intent);
			if (activity instanceof Activity ) {
				((Activity)activity).finish();
				AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.FORM_BACK);
			}
		}

		else if (route == Route.PHOTO_SOURCE) {

			IntentBuilder intentBuilder = new IntentBuilder(activity, PhotoActionPicker.class);
			((Activity)activity).startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_PICTURE_SOURCE_PICK);
			AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.DIALOG_TO);
		}

		else if (route == Route.PHOTO_FROM_CAMERA) {

			IntentBuilder intentBuilder = new IntentBuilder(MediaStore.ACTION_IMAGE_CAPTURE);
			intentBuilder.setExtras(extras);
			((Activity)activity).startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_PHOTO_MAKE);
			AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.FORM_TO);
		}

		else if (route == Route.PHOTO_SEARCH) {

			IntentBuilder intentBuilder = new IntentBuilder(activity, PhotoPicker.class);
			intentBuilder.setExtras(extras);
			((Activity)activity).startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_PHOTO_SEARCH);
			AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.DIALOG_TO);
		}

		else if (route == Route.SEARCH) {

			Integer transitionType = TransitionType.VIEW_TO;
			if (extras != null) {
				transitionType = extras.getInt(Constants.EXTRA_TRANSITION_TYPE, TransitionType.VIEW_TO);
			}
			else {
				extras = new Bundle();
			}
			extras.putInt(Constants.EXTRA_TRANSITION_TYPE, transitionType);

			IntentBuilder intentBuilder = new IntentBuilder(activity, SearchScreen.class);
			intentBuilder.setExtras(extras);
			((Activity)activity).startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_SEARCH);
			AnimationManager.doOverridePendingTransition((Activity) activity, transitionType);
		}

		else if (route == Route.ENTITY_LIST) {

			if (entity == null) {
				throw new IllegalArgumentException("Dispatching user list requires entity");
			}

			Integer transitionType = TransitionType.VIEW_TO;
			if (extras != null) {
				transitionType = extras.getInt(Constants.EXTRA_TRANSITION_TYPE, TransitionType.VIEW_TO);
			}

			final IntentBuilder intentBuilder = new IntentBuilder(activity, ListScreen.class);
			intentBuilder.setEntityId(entity.id).addExtras(extras);
			activity.startActivity(intentBuilder.create());
			AnimationManager.doOverridePendingTransition((Activity) activity, transitionType);
		}

		/* Command routing: deprecated, remove asap */

		else if (route == Route.NEW) {

			if (!MenuManager.canUserAdd(entity)) {
				return;
			}

			if (schema != null) {
				IEntityController controller = Patchr.getInstance().getControllerForSchema(schema);
				controller.insert(activity, extras, true);
			}
		}

		else if (route == Route.EDIT) {

			if (entity == null) {
				throw new IllegalArgumentException("Dispatching edit requires entity");
			}

			IEntityController controller = Patchr.getInstance().getControllerForSchema(schema);
			controller.edit(activity, entity, extras, true);
		}

		else if (route == Route.SUBMIT) {

			((BaseScreen) activity).submitAction();    // Give activity a chance for discard confirmation
		}

		else if (route == Route.DELETE) {

			((BaseScreen) activity).confirmDelete();    // Give activity a chance for discard confirmation
		}

		else if (route == Route.REMOVE) {

			if (extras == null) {
				throw new IllegalArgumentException("Dispatching remove requires extras");
			}
			((BaseScreen) activity).confirmRemove(extras.getString(Constants.EXTRA_ENTITY_PARENT_ID));    // Give activity a chance for remove confirmation
		}

	}

	public Integer routeForMenuId(int itemId) {

		if (itemId == R.id.edit)
			return Route.EDIT;
		else if (itemId == R.id.login)
			return Route.LOGIN;
		else if (itemId == R.id.report)
			return Route.REPORT;
		else if (itemId == R.id.submit)
			return Route.SUBMIT;
		else if (itemId == R.id.refresh)
			return Route.REFRESH;
		else if (itemId == R.id.add)
			return Route.ADD;
		else if (itemId == R.id.invite)
			return Route.SHARE;
		else if (itemId == R.id.share)
			return Route.SHARE;
		else if (itemId == R.id.share_photo)
			return Route.SHARE;
		else if (itemId == R.id.delete)
			return Route.DELETE;
		else if (itemId == R.id.remove)
			return Route.REMOVE;
		else if (itemId == R.id.navigate)
			return Route.NAVIGATE;
		else if (itemId == R.id.search)
			return Route.SEARCH;
		else if (itemId == R.id.map)
			return Route.MAP;
		else if (itemId == R.id.view_as_list)
			return Route.VIEW_AS_LIST;
		else if (itemId == R.id.view_as_map)
			return Route.VIEW_AS_MAP;

		return Route.UNKNOWN;
	}
}
