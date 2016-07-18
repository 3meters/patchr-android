package com.patchr.components;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;

import com.patchr.Constants;
import com.patchr.R;
import com.patchr.model.RealmEntity;
import com.patchr.objects.enums.Command;
import com.patchr.objects.enums.TransitionType;
import com.patchr.ui.BaseScreen;
import com.patchr.ui.MainScreen;
import com.patchr.ui.MapScreen;
import com.patchr.ui.SettingsScreen;
import com.patchr.ui.collections.BaseListScreen;
import com.patchr.ui.collections.MemberListScreen;
import com.patchr.ui.collections.PhotoSearchScreen;
import com.patchr.ui.collections.SearchScreen;
import com.patchr.ui.edit.LoginEdit;
import com.patchr.ui.fragments.MapListFragment;

public class Router {

	public void route(final Context activity, Integer route, RealmEntity entity, Bundle extras) {

		if (route == Command.MAP) {

			if (entity == null) {
				throw new IllegalArgumentException("Dispatching map requires entity");
			}
			final IntentBuilder intentBuilder = new IntentBuilder(activity, MapScreen.class);
			if (extras == null) {
				extras = new Bundle();
			}

			intentBuilder.setEntityId(entity.id).addExtras(extras);
			activity.startActivity(intentBuilder.build());
			AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.FORM_TO);
		}

		else if (route == Command.SETTINGS) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, SettingsScreen.class);
			((Activity) activity).startActivityForResult(intentBuilder.build(), Constants.ACTIVITY_PREFERENCES);
			AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.FORM_TO);
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
			Intent intent = intentBuilder.build();
			((Activity) activity).startActivityForResult(intent, Constants.ACTIVITY_LOGIN);
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

		else if (route == Command.PHOTO_SEARCH) {

			IntentBuilder intentBuilder = new IntentBuilder(activity, PhotoSearchScreen.class);
			intentBuilder.setExtras(extras);
			((Activity) activity).startActivityForResult(intentBuilder.build(), Constants.ACTIVITY_PHOTO_SEARCH);
			AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.DIALOG_TO);
		}

		else if (route == Command.SEARCH) {

			IntentBuilder intentBuilder = new IntentBuilder(activity, SearchScreen.class);
			intentBuilder.addExtras(extras);
			((Activity) activity).startActivityForResult(intentBuilder.build(), Constants.ACTIVITY_SEARCH);
			AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.VIEW_TO);
		}

		else if (route == Command.ENTITY_LIST) {

			if (entity == null) {
				throw new IllegalArgumentException("Dispatching entity list requires entity");
			}

			final IntentBuilder intentBuilder = new IntentBuilder(activity, BaseListScreen.class);
			intentBuilder.setEntityId(entity.id).addExtras(extras);
			activity.startActivity(intentBuilder.build());
			AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.FORM_TO);
		}

		else if (route == Command.MEMBER_LIST) {

			if (entity == null) {
				throw new IllegalArgumentException("Dispatching member list requires entity");
			}

			final IntentBuilder intentBuilder = new IntentBuilder(activity, MemberListScreen.class);
			intentBuilder.setEntityId(entity.id).addExtras(extras);
			activity.startActivity(intentBuilder.build());
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

		if (itemId == R.id.delete)
			return Command.DELETE;
		else if (itemId == R.id.edit)
			return Command.EDIT;
		else if (itemId == R.id.invite)
			return Command.SHARE;
		else if (itemId == R.id.login)
			return Command.LOGIN;
		else if (itemId == R.id.map)
			return Command.MAP;
		else if (itemId == R.id.navigate)
			return Command.NAVIGATE;
		else if (itemId == R.id.remove)
			return Command.REMOVE;
		else if (itemId == R.id.report)
			return Command.REPORT;
		else if (itemId == R.id.search)
			return Command.SEARCH;
		else if (itemId == R.id.share)
			return Command.SHARE;
		else if (itemId == R.id.share_photo)
			return Command.SHARE;
		else if (itemId == R.id.submit)
			return Command.SUBMIT;
		else if (itemId == R.id.view_as_list)
			return Command.VIEW_AS_LIST;
		else if (itemId == R.id.view_as_map)
			return Command.VIEW_AS_MAP;

		return Command.UNKNOWN;
	}
}
