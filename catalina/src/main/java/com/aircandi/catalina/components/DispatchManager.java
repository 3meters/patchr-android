package com.aircandi.catalina.components;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.aircandi.Aircandi;
import com.aircandi.catalina.Constants;
import com.aircandi.catalina.R;
import com.aircandi.catalina.ui.AircandiForm;
import com.aircandi.catalina.ui.SplashForm;
import com.aircandi.catalina.ui.WatcherList;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.StringManager;
import com.aircandi.controllers.IEntityController;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Route;
import com.aircandi.objects.Shortcut;
import com.aircandi.objects.TransitionType;
import com.aircandi.ui.base.BaseActivity;
import com.aircandi.utilities.Dialogs;

public class DispatchManager extends com.aircandi.components.DispatchManager {

	@Override
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

		else if (route == Route.EDIT) {

			if (Aircandi.getInstance().getCurrentUser().isAnonymous()) {
				String message = StringManager.getString(R.string.alert_signin_message_edit, schema);
				Dialogs.signinRequired(activity, message);
				return;
			}

			if (entity == null) {
				throw new IllegalArgumentException("Dispatching edit requires entity");
			}

			IEntityController controller = Aircandi.getInstance().getControllerForSchema(schema);
			if (entity.schema.equals(Constants.SCHEMA_ENTITY_PLACE) && entity.isOwnedBySystem()) {
				if (extras == null) {
					extras = new Bundle();
				}
				extras.putInt(Constants.EXTRA_LAYOUT_RESID, R.layout.place_customize);
			}
			controller.edit(activity, entity, extras, true);
		}

		else if (route == Route.WATCHERS) {

			if (entity == null) {
				throw new IllegalArgumentException("Dispatching watchers requires entity");
			}

			final IntentBuilder intentBuilder = new IntentBuilder(activity, WatcherList.class);
			intentBuilder.setEntityId(entity.id).addExtras(extras);
			activity.startActivity(intentBuilder.create());
			Aircandi.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
		}

		else {
			super.route(activity, route, entity, shortcut, extras);
		}
	}
}
