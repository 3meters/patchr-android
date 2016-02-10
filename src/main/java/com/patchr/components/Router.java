package com.patchr.components;

import android.app.Activity;
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
import com.patchr.ui.AboutForm;
import com.patchr.ui.AircandiForm;
import com.patchr.ui.MapForm;
import com.patchr.ui.MapListFragment;
import com.patchr.ui.PatchList;
import com.patchr.ui.PhotoForm;
import com.patchr.ui.SearchForm;
import com.patchr.ui.SettingsForm;
import com.patchr.ui.LobbyForm;
import com.patchr.ui.UserList;
import com.patchr.ui.base.BaseActivity;
import com.patchr.ui.edit.FeedbackEdit;
import com.patchr.ui.edit.PasswordEdit;
import com.patchr.ui.edit.ProximityEdit;
import com.patchr.ui.edit.RegisterEdit;
import com.patchr.ui.edit.ReportEdit;
import com.patchr.ui.edit.ResetEdit;
import com.patchr.ui.edit.LoginEdit;
import com.patchr.ui.helpers.LocationPicker;
import com.patchr.ui.helpers.PhotoActionPicker;
import com.patchr.ui.helpers.PhotoPicker;
import com.patchr.ui.helpers.PrivacyBuilder;
import com.patchr.ui.helpers.QrcodeDialog;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Json;
import com.patchr.utilities.Type;

public class Router {

	public void intent(Activity activity, Intent intent) {
		activity.startActivity(intent);
		AnimationManager.doOverridePendingTransition(activity, TransitionType.FORM_TO);
	}

	public void route(final Activity activity, Integer route, Entity entity, Bundle extras) {

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
			AnimationManager.doOverridePendingTransition(activity, TransitionType.VIEW_TO);
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

		else if (route == Route.EDIT) {

			if (Patchr.getInstance().getCurrentUser().isAnonymous()) {
				String message = StringManager.getString(R.string.alert_signin_message_edit, schema);
				Dialogs.signinRequired(activity, message);
				return;
			}

			if (entity == null) {
				throw new IllegalArgumentException("Dispatching edit requires entity");
			}

			IEntityController controller = Patchr.getInstance().getControllerForSchema(schema);
			controller.edit(activity, entity, extras, true);
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
			AnimationManager.doOverridePendingTransition(activity, TransitionType.FORM_TO);
		}

		else if (route == Route.ADD) {

			((BaseActivity) activity).onAdd(new Bundle());
		}

		else if (route == Route.NEW_PLACE) {

			if (extras == null) {
				extras = new Bundle();
			}

			extras.putString(Constants.EXTRA_ENTITY_SCHEMA, Constants.SCHEMA_ENTITY_PATCH);

			((BaseActivity) activity).onAdd(extras);
		}

		else if (route == Route.SHARE) {

			((BaseActivity) activity).share();
		}

		else if (route == Route.NEW) {

			if (Patchr.getInstance().getCurrentUser().isAnonymous()) {
				if (schema == null) {
					throw new IllegalArgumentException("Handling anonymous new requires schema");
				}
				String message = StringManager.getString(R.string.alert_signin_message_add, schema);
				if (schema.equals(Constants.SCHEMA_ENTITY_PATCH)) {
					message = StringManager.getString(R.string.alert_signin_message_patch_new, schema);
				}
				Dialogs.signinRequired(activity, message);
				return;
			}

			if (!MenuManager.canUserAdd(entity)) {
				return;
			}

			if (schema != null) {
				IEntityController controller = Patchr.getInstance().getControllerForSchema(schema);
				controller.insert(activity, extras, true);
			}
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

			intentBuilder.setEntity(entity).addExtras(extras);
			activity.startActivity(intentBuilder.create());
			AnimationManager.doOverridePendingTransition(activity, TransitionType.FORM_TO);
		}

		else if (route == Route.SETTINGS) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, SettingsForm.class);
			activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_PREFERENCES);
			AnimationManager.doOverridePendingTransition(activity, TransitionType.FORM_TO);
		}

		else if (route == Route.FEEDBACK) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, FeedbackEdit.class);
			activity.startActivity(intentBuilder.create());
			AnimationManager.doOverridePendingTransition(activity, TransitionType.FORM_TO);
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
			AnimationManager.doOverridePendingTransition(activity, TransitionType.FORM_TO);
		}

		else if (route == Route.ABOUT) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, AboutForm.class);
			activity.startActivity(intentBuilder.create());
			AnimationManager.doOverridePendingTransition(activity, TransitionType.FORM_TO);
		}

		else if (route == Route.PHOTOS) {

			if (entity == null) {
				throw new IllegalArgumentException("Valid entity required for selected route");
			}
			if (entity.photo == null) {
				throw new IllegalArgumentException("Routing to photo form requires photo object");
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
			AnimationManager.doOverridePendingTransition(activity, TransitionType.DRILL_TO);
		}

		else if (route == Route.PHOTO) {
		    /*
			 * Single photo to show and it has already been serialized into the extras bundle.
			 */
			final IntentBuilder intentBuilder = new IntentBuilder(activity, PhotoForm.class);
			intentBuilder.setExtras(extras);
			Intent intent = intentBuilder.create();
			activity.startActivity(intent);
			AnimationManager.doOverridePendingTransition(activity, TransitionType.DRILL_TO);
		}

		else if (route == Route.PHOTO_EDIT) {

			if (extras == null || !extras.containsKey(Constants.EXTRA_PHOTO)) {
				throw new IllegalArgumentException("Valid photo in extras required for selected route");
			}

			final String jsonPhoto = extras.getString(Constants.EXTRA_PHOTO);
			if (jsonPhoto != null) {
				final Photo photo = (Photo) Json.jsonToObject(jsonPhoto, Json.ObjectType.PHOTO);
				final String url = photo.getDirectUri();
				Uri uri = Uri.parse(url);

				if (AndroidManager.getInstance().isAviaryInstalled()) {
					Intent intent = new Intent("aviary.intent.action.EDIT");
					intent.setDataAndType(uri, "image/*"); // required
					intent.putExtra("app-id", Patchr.applicationContext.getPackageName()); // required ( it's your app unique package name )
					intent.putExtra("output-format", Bitmap.CompressFormat.JPEG.name());
					intent.putExtra("output-quality", 90);
					intent.putExtra("save-on-no-changes", false);

					activity.startActivityForResult(intent, Constants.ACTIVITY_PHOTO_EDIT);
					AnimationManager.doOverridePendingTransition(activity, TransitionType.DRILL_TO);
				}
				else {
					Dialogs.installAviary(activity);
				}
			}
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

		else if (route == Route.VIEW_AS_LIST) {

			Fragment fragment = ((AircandiForm) activity).getCurrentFragment();
			if (fragment instanceof MapListFragment) {
				String listFragment = ((MapListFragment) fragment).getRelatedListFragment();
				if (listFragment == null) {
					listFragment = Constants.FRAGMENT_TYPE_NEARBY;
				}
				((AircandiForm) activity).setCurrentFragment(listFragment);
			}
		}

		else if (route == Route.VIEW_AS_MAP) {

			((AircandiForm) activity).setCurrentFragment(Constants.FRAGMENT_TYPE_MAP);
		}

		else if (route == Route.SIGNOUT) {

			((BaseActivity) activity).signout();
		}

		else if (route == Route.SIGNIN) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, LoginEdit.class);
			activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_SIGNIN);
			AnimationManager.doOverridePendingTransition(activity, TransitionType.FORM_TO);
		}

		else if (route == Route.REGISTER) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, RegisterEdit.class);
			activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_SIGNIN);
			AnimationManager.doOverridePendingTransition(activity, TransitionType.FORM_TO);
		}

		else if (route == Route.TERMS) {

			final IntentBuilder intentBuilder = new IntentBuilder(android.content.Intent.ACTION_VIEW);
			intentBuilder.setData(Uri.parse(StringManager.getString(R.string.url_terms)));
			activity.startActivity(intentBuilder.create());
			AnimationManager.doOverridePendingTransition(activity, TransitionType.FORM_TO);
		}

		else if (route == Route.PRIVACY) {

			final IntentBuilder intentBuilder = new IntentBuilder(android.content.Intent.ACTION_VIEW);
			intentBuilder.setData(Uri.parse(StringManager.getString(R.string.url_privacy)));
			activity.startActivity(intentBuilder.create());
			AnimationManager.doOverridePendingTransition(activity, TransitionType.FORM_TO);
		}

		else if (route == Route.LEGAL) {

			final IntentBuilder intentBuilder = new IntentBuilder(android.content.Intent.ACTION_VIEW);
			intentBuilder.setData(Uri.parse(StringManager.getString(R.string.url_legal)));
			activity.startActivity(intentBuilder.create());
			AnimationManager.doOverridePendingTransition(activity, TransitionType.FORM_TO);
		}

		else if (route == Route.SETTINGS_LOCATION) {

			activity.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
			AnimationManager.doOverridePendingTransition(activity, TransitionType.FORM_TO);
		}

		else if (route == Route.SETTINGS_WIFI) {

			activity.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
			AnimationManager.doOverridePendingTransition(activity, TransitionType.FORM_TO);
			activity.finish();
		}

		else if (route == Route.PRIVACY_EDIT) {

			if (entity == null) {
				throw new IllegalArgumentException("Dispatching privacy edit requires entity");
			}
			final IntentBuilder intentBuilder = new IntentBuilder(activity, PrivacyBuilder.class);
			final Intent intent = intentBuilder.create();
			intent.putExtra(Constants.EXTRA_PRIVACY, ((Patch) entity).privacy);

			activity.startActivityForResult(intent, Constants.ACTIVITY_PRIVACY_EDIT);
			AnimationManager.doOverridePendingTransition(activity, TransitionType.BUILDER_TO);
		}

		else if (route == Route.LOCATION_EDIT) {

			if (entity == null) {
				throw new IllegalArgumentException("Dispatching location edit requires entity");
			}
			final IntentBuilder intentBuilder = new IntentBuilder(activity, LocationPicker.class);
			final Intent intent = intentBuilder.create();

			if (((Patch) entity).location != null) {
				final String json = Json.objectToJson(((Patch) entity).location);
				intent.putExtra(Constants.EXTRA_LOCATION, json);
				intent.putExtra(Constants.EXTRA_TITLE, entity.name);
			}

			activity.startActivityForResult(intent, Constants.ACTIVITY_LOCATION_EDIT);
			AnimationManager.doOverridePendingTransition(activity, TransitionType.BUILDER_TO);
		}

		else if (route == Route.PASSWORD_CHANGE) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, PasswordEdit.class);
			activity.startActivity(intentBuilder.create());
			AnimationManager.doOverridePendingTransition(activity, TransitionType.FORM_TO);
		}

		else if (route == Route.PASSWORD_RESET) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, ResetEdit.class);
			activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_RESET_AND_SIGNIN);
			AnimationManager.doOverridePendingTransition(activity, TransitionType.FORM_TO);
		}

		else if (route == Route.SPLASH) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, LobbyForm.class);
			final Intent intent = intentBuilder.create();
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
			if (activity instanceof BaseActivity) {
				((BaseActivity) activity).setResultCode(Activity.RESULT_CANCELED);
			}
			activity.startActivity(intent);
			activity.finish();
			AnimationManager.doOverridePendingTransition(activity, TransitionType.FORM_BACK);
		}

		else if (route == Route.PHOTO_SOURCE) {

			IntentBuilder intentBuilder = new IntentBuilder(activity, PhotoActionPicker.class);
			activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_PICTURE_SOURCE_PICK);
			AnimationManager.doOverridePendingTransition(activity, TransitionType.DIALOG_TO);
		}

		else if (route == Route.QRCODE) {

			IntentBuilder intentBuilder = new IntentBuilder(activity, QrcodeDialog.class);
			activity.startActivity(intentBuilder.create());
			AnimationManager.doOverridePendingTransition(activity, TransitionType.DIALOG_TO);
		}

		else if (route == Route.PHOTO_FROM_CAMERA) {

			IntentBuilder intentBuilder = new IntentBuilder(MediaStore.ACTION_IMAGE_CAPTURE);
			intentBuilder.setExtras(extras);
			activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_PHOTO_MAKE);
			AnimationManager.doOverridePendingTransition(activity, TransitionType.FORM_TO);
		}

		else if (route == Route.PHOTO_SEARCH) {

			IntentBuilder intentBuilder = new IntentBuilder(activity, PhotoPicker.class);
			intentBuilder.setExtras(extras);
			activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_PHOTO_SEARCH);
			AnimationManager.doOverridePendingTransition(activity, TransitionType.DIALOG_TO);
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

			IntentBuilder intentBuilder = new IntentBuilder(activity, SearchForm.class);
			intentBuilder.setExtras(extras);
			activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_SEARCH);
			AnimationManager.doOverridePendingTransition(activity, transitionType);
		}

		else if (route == Route.USER_LIST) {

			if (entity == null) {
				throw new IllegalArgumentException("Dispatching user list requires entity");
			}

			Integer transitionType = TransitionType.VIEW_TO;
			if (extras != null) {
				transitionType = extras.getInt(Constants.EXTRA_TRANSITION_TYPE, TransitionType.VIEW_TO);
			}

			final IntentBuilder intentBuilder = new IntentBuilder(activity, UserList.class);
			intentBuilder.setEntityId(entity.id).addExtras(extras);
			activity.startActivity(intentBuilder.create());
			AnimationManager.doOverridePendingTransition(activity, transitionType);
		}

		else if (route == Route.PATCH_LIST) {

			if (entity == null) {
				throw new IllegalArgumentException("Dispatching watchers requires entity");
			}

			Integer transitionType = TransitionType.VIEW_TO;
			if (extras != null) {
				transitionType = extras.getInt(Constants.EXTRA_TRANSITION_TYPE, TransitionType.VIEW_TO);
			}

			final IntentBuilder intentBuilder = new IntentBuilder(activity, PatchList.class);
			intentBuilder.setEntityId(entity.id).addExtras(extras);
			activity.startActivity(intentBuilder.create());
			AnimationManager.doOverridePendingTransition(activity, transitionType);
		}
	}

	public Integer routeForMenuId(int itemId) {

		if (itemId == R.id.edit)
			return Route.EDIT;
		else if (itemId == android.R.id.home)
			return Route.CANCEL;
		else if (itemId == R.id.signout)
			return Route.SIGNOUT;
		else if (itemId == R.id.signin)
			return Route.SIGNIN;
		else if (itemId == R.id.report)
			return Route.REPORT;
		else if (itemId == R.id.qrcode)
			return Route.QRCODE;
		else if (itemId == R.id.accept)
			return Route.ACCEPT;
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
