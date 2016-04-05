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
import com.patchr.objects.Command;
import com.patchr.objects.Entity;
import com.patchr.objects.Patch;
import com.patchr.objects.Photo;
import com.patchr.objects.TransitionType;
import com.patchr.ui.AboutScreen;
import com.patchr.ui.BaseScreen;
import com.patchr.ui.ListScreen;
import com.patchr.ui.LobbyScreen;
import com.patchr.ui.MainScreen;
import com.patchr.ui.MapScreen;
import com.patchr.ui.MemberListScreen;
import com.patchr.ui.MessageScreen;
import com.patchr.ui.PatchScreen;
import com.patchr.ui.PhotoScreen;
import com.patchr.ui.PhotoSearchScreen;
import com.patchr.ui.PhotoSwitchboardScreen;
import com.patchr.ui.ProfileScreen;
import com.patchr.ui.SearchScreen;
import com.patchr.ui.SettingsScreen;
import com.patchr.ui.edit.FeedbackEdit;
import com.patchr.ui.edit.LocationEdit;
import com.patchr.ui.edit.LoginEdit;
import com.patchr.ui.edit.PasswordEdit;
import com.patchr.ui.edit.PatchEdit;
import com.patchr.ui.edit.PostEdit;
import com.patchr.ui.edit.PrivacyEdit;
import com.patchr.ui.edit.ProfileEdit;
import com.patchr.ui.edit.ProximityEdit;
import com.patchr.ui.edit.ReportEdit;
import com.patchr.ui.edit.ResetEdit;
import com.patchr.ui.edit.ShareEdit;
import com.patchr.ui.fragments.MapListFragment;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Json;

public class Router {

	public Intent add(Context context, String schema, Bundle extras, Boolean start) {
		/*
		 * Not used to route when creating an invite or share.
		 */
		Class<?> newClass = PostEdit.class;
		if (Constants.SCHEMA_ENTITY_PATCH.equals(schema)) {
			newClass = PatchEdit.class;
		}
		else if (Constants.SCHEMA_ENTITY_USER.equals(schema)) {
			newClass = ProfileEdit.class;
		}

		IntentBuilder intentBuilder = new IntentBuilder(context, newClass);
		intentBuilder.setEntitySchema(schema).addExtras(extras);
		Intent intent = intentBuilder.create();

		if (start) {
			((Activity) context).startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_ENTITY_INSERT);
			AnimationManager.doOverridePendingTransition((Activity) context, TransitionType.FORM_TO);
		}

		return intent;
	}

	public Intent browse(Context context, String entityId, Bundle extras, Boolean start) {

		String schema = Entity.getSchemaForId(entityId);
		Class<?> browseClass = MessageScreen.class;
		if (Constants.SCHEMA_ENTITY_PATCH.equals(schema)) {
			browseClass = PatchScreen.class;
		}
		else if (Constants.SCHEMA_ENTITY_USER.equals(schema)) {
			browseClass = ProfileScreen.class;
		}

		IntentBuilder intentBuilder = new IntentBuilder(context, browseClass);
		intentBuilder.setEntityId(entityId).addExtras(extras);
		Intent intent = intentBuilder.create();

		if (start) {
			context.startActivity(intent);
			AnimationManager.doOverridePendingTransition((Activity) context, TransitionType.FORM_TO);
		}

		return intent;
	}

	public Intent edit(Context context, Entity entity, Bundle extras, Boolean start) {

		Class<?> editClass = PostEdit.class;
		if (Constants.SCHEMA_ENTITY_PATCH.equals(entity.schema)) {
			editClass = PatchEdit.class;
		}
		else if (Constants.SCHEMA_ENTITY_USER.equals(entity.schema)) {
			editClass = ProfileEdit.class;
		}
		else if (Constants.SCHEMA_ENTITY_MESSAGE.equals(entity.schema) && entity.type.equals("share")) {
			editClass = ShareEdit.class;
		}

		IntentBuilder intentBuilder = new IntentBuilder(context, editClass);
		intentBuilder.setEntity(entity).addExtras(extras);
		Intent intent = intentBuilder.create();

		if (start) {
			((Activity) context).startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_ENTITY_EDIT);
			AnimationManager.doOverridePendingTransition((Activity) context, TransitionType.FORM_TO);
		}

		return intent;
	}

	public void route(final Context activity, Integer route, Entity entity, Bundle extras) {

		if (route == Command.HOME) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, MainScreen.class);
			Intent intent = intentBuilder.create();
			intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

			activity.startActivity(intent);
			AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.VIEW_TO);
		}

		else if (route == Command.TUNE) {

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

		else if (route == Command.MAP) {

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

		else if (route == Command.SETTINGS) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, SettingsScreen.class);
			((Activity) activity).startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_PREFERENCES);
			AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.FORM_TO);
		}

		else if (route == Command.FEEDBACK) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, FeedbackEdit.class);
			activity.startActivity(intentBuilder.create());
			AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.FORM_TO);
		}

		else if (route == Command.REPORT) {

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

		else if (route == Command.ABOUT) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, AboutScreen.class);
			activity.startActivity(intentBuilder.create());
			AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.FORM_TO);
		}

		else if (route == Command.PHOTO) {
		    /*
			 * Single photo to show and it has already been serialized into the extras bundle.
			 */
			final IntentBuilder intentBuilder = new IntentBuilder(activity, PhotoScreen.class);
			intentBuilder.setExtras(extras);
			Intent intent = intentBuilder.create();
			activity.startActivity(intent);
			AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.FORM_TO);
		}

		else if (route == Command.PHOTO_EDIT) {

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

					((Activity) activity).startActivityForResult(intent, Constants.ACTIVITY_PHOTO_EDIT);
					AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.FORM_TO);
				}
				else {
					Dialogs.installAviary((Activity) activity);
				}
			}
		}

		else if (route == Command.VIEW_AS_LIST) {

			Fragment fragment = ((MainScreen) activity).getCurrentFragment();
			if (fragment instanceof MapListFragment) {
				String listFragment = ((MapListFragment) fragment).relatedListFragment;
				if (listFragment == null) {
					listFragment = Constants.FRAGMENT_TYPE_NEARBY;
				}
				((MainScreen) activity).switchToFragment(listFragment);
			}
		}

		else if (route == Command.VIEW_AS_MAP) {

			((MainScreen) activity).switchToFragment(Constants.FRAGMENT_TYPE_MAP);
		}

		else if (route == Command.LOGIN) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, LoginEdit.class);
			intentBuilder.addExtras(extras);
			Intent intent = intentBuilder.create();
			((Activity) activity).startActivityForResult(intent, Constants.ACTIVITY_LOGIN);
			AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.FORM_TO);
		}

		else if (route == Command.SIGNUP) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, ProfileEdit.class);
			intentBuilder.addExtras(extras);
			Intent intent = intentBuilder.create();
			((Activity) activity).startActivityForResult(intent, Constants.ACTIVITY_SIGNUP);
			AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.FORM_TO);
		}

		else if (route == Command.TERMS) {

			final IntentBuilder intentBuilder = new IntentBuilder(android.content.Intent.ACTION_VIEW);
			intentBuilder.setData(Uri.parse(StringManager.getString(R.string.url_terms)));
			activity.startActivity(intentBuilder.create());
			AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.FORM_TO);
		}

		else if (route == Command.PRIVACY) {

			final IntentBuilder intentBuilder = new IntentBuilder(android.content.Intent.ACTION_VIEW);
			intentBuilder.setData(Uri.parse(StringManager.getString(R.string.url_privacy)));
			activity.startActivity(intentBuilder.create());
			AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.FORM_TO);
		}

		else if (route == Command.LEGAL) {

			final IntentBuilder intentBuilder = new IntentBuilder(android.content.Intent.ACTION_VIEW);
			intentBuilder.setData(Uri.parse(StringManager.getString(R.string.url_legal)));
			activity.startActivity(intentBuilder.create());
			AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.FORM_TO);
		}

		else if (route == Command.SETTINGS_LOCATION) {

			activity.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
			AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.FORM_TO);
		}

		else if (route == Command.SETTINGS_WIFI) {

			activity.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
			AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.FORM_TO);
			((Activity) activity).finish();
		}

		else if (route == Command.PRIVACY_EDIT) {

			if (entity == null) {
				throw new IllegalArgumentException("Dispatching privacy edit requires entity");
			}
			final IntentBuilder intentBuilder = new IntentBuilder(activity, PrivacyEdit.class);
			final Intent intent = intentBuilder.create();
			intent.putExtra(Constants.EXTRA_PRIVACY, ((Patch) entity).privacy);

			((Activity) activity).startActivityForResult(intent, Constants.ACTIVITY_PRIVACY_EDIT);
			AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.BUILDER_TO);
		}

		else if (route == Command.LOCATION_EDIT) {

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

			((Activity) activity).startActivityForResult(intent, Constants.ACTIVITY_LOCATION_EDIT);
			AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.BUILDER_TO);
		}

		else if (route == Command.PASSWORD_CHANGE) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, PasswordEdit.class);
			activity.startActivity(intentBuilder.create());
			AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.FORM_TO);
		}

		else if (route == Command.PASSWORD_RESET) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, ResetEdit.class);
			((Activity) activity).startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_RESET_AND_SIGNIN);
			AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.FORM_TO);
		}

		else if (route == Command.LOBBY) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, LobbyScreen.class);
			final Intent intent = intentBuilder.create();
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
			if (activity instanceof BaseScreen) {
				((BaseScreen) activity).setResult(Activity.RESULT_CANCELED);
			}
			activity.startActivity(intent);
			if (activity instanceof Activity) {
				((Activity) activity).finish();
				AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.FORM_BACK);
			}
		}

		else if (route == Command.PHOTO_PICK) {

			IntentBuilder intentBuilder = new IntentBuilder(activity, PhotoSwitchboardScreen.class);
			((Activity) activity).startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_PHOTO_PICK);
			AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.DIALOG_TO);
		}

		else if (route == Command.PHOTO_FROM_CAMERA) {

			IntentBuilder intentBuilder = new IntentBuilder(MediaStore.ACTION_IMAGE_CAPTURE);
			intentBuilder.addExtras(extras);
			((Activity) activity).startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_PHOTO_MAKE);
			AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.FORM_TO);
		}

		else if (route == Command.PHOTO_SEARCH) {

			IntentBuilder intentBuilder = new IntentBuilder(activity, PhotoSearchScreen.class);
			intentBuilder.setExtras(extras);
			((Activity) activity).startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_PHOTO_SEARCH);
			AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.DIALOG_TO);
		}

		else if (route == Command.SEARCH) {

			IntentBuilder intentBuilder = new IntentBuilder(activity, SearchScreen.class);
			intentBuilder.addExtras(extras);
			((Activity) activity).startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_SEARCH);
			AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.VIEW_TO);
		}

		else if (route == Command.ENTITY_LIST) {

			if (entity == null) {
				throw new IllegalArgumentException("Dispatching entity list requires entity");
			}

			final IntentBuilder intentBuilder = new IntentBuilder(activity, ListScreen.class);
			intentBuilder.setEntityId(entity.id).addExtras(extras);
			activity.startActivity(intentBuilder.create());
			AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.FORM_TO);
		}

		else if (route == Command.MEMBER_LIST) {

			if (entity == null) {
				throw new IllegalArgumentException("Dispatching member list requires entity");
			}

			final IntentBuilder intentBuilder = new IntentBuilder(activity, MemberListScreen.class);
			intentBuilder.setEntityId(entity.id).addExtras(extras);
			activity.startActivity(intentBuilder.create());
			AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.FORM_TO);
		}

		/* Command routing: deprecated, remove asap */

		else if (route == Command.SUBMIT) {

			((BaseScreen) activity).submitAction();    // Give activity a chance for discard confirmation
		}

		else if (route == Command.DELETE) {

			((BaseScreen) activity).confirmDelete();    // Give activity a chance for discard confirmation
		}

		else if (route == Command.REMOVE) {

			if (extras == null) {
				throw new IllegalArgumentException("Dispatching remove requires extras");
			}
			((BaseScreen) activity).confirmRemove(extras.getString(Constants.EXTRA_ENTITY_PARENT_ID));    // Give activity a chance for remove confirmation
		}
	}

	public Integer routeForMenuId(int itemId) {

		if (itemId == R.id.edit)
			return Command.EDIT;
		else if (itemId == R.id.login)
			return Command.LOGIN;
		else if (itemId == R.id.report)
			return Command.REPORT;
		else if (itemId == R.id.submit)
			return Command.SUBMIT;
		else if (itemId == R.id.invite)
			return Command.SHARE;
		else if (itemId == R.id.share)
			return Command.SHARE;
		else if (itemId == R.id.share_photo)
			return Command.SHARE;
		else if (itemId == R.id.delete)
			return Command.DELETE;
		else if (itemId == R.id.remove)
			return Command.REMOVE;
		else if (itemId == R.id.navigate)
			return Command.NAVIGATE;
		else if (itemId == R.id.search)
			return Command.SEARCH;
		else if (itemId == R.id.map)
			return Command.MAP;
		else if (itemId == R.id.view_as_list)
			return Command.VIEW_AS_LIST;
		else if (itemId == R.id.view_as_map)
			return Command.VIEW_AS_MAP;

		return Command.UNKNOWN;
	}
}
