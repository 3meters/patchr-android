package com.aircandi.utilities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Toast;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.components.Logger;
import com.aircandi.components.StringManager;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Route;
import com.aircandi.objects.TransitionType;

import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

public class Dialogs {

	@NonNull
	public static AlertDialog alertDialog(@Nullable Integer iconResource // $codepro.audit.disable largeNumberOfParameters
			, @Nullable String titleText
			, String message
			, @Nullable View customView
			, @NonNull Context context
			, @Nullable Integer okButtonId
			, @Nullable Integer cancelButtonId
			, @Nullable Integer neutralButtonId
			, OnClickListener listenerClick
			, @Nullable OnCancelListener listenerCancel) {

		final AlertDialog.Builder builder = new AlertDialog.Builder(context);

		if (iconResource != null) {
			builder.setIcon(iconResource);
		}

		if (titleText != null) {
			builder.setTitle(titleText);
		}

		if (customView == null) {
			builder.setMessage(message);
		}
		else {
			builder.setView(customView);
		}

		if (okButtonId != null) {
			builder.setPositiveButton(okButtonId, listenerClick);
		}

		if (cancelButtonId != null) {
			builder.setNegativeButton(cancelButtonId, listenerClick);
		}

		if (neutralButtonId != null) {
			builder.setNeutralButton(neutralButtonId, listenerClick);
		}

		if (listenerCancel != null) {
			builder.setOnCancelListener(listenerCancel);
		}

		final AlertDialog alert = builder.create();
		alert.show();

		/* Prevent dimming the background */
		alert.getWindow().setDimAmount(Constants.DIALOGS_DIM_AMOUNT);

		return alert;
	}

	public static void alertDialogSimple(@NonNull final Activity activity, final String titleText, final String message) {
		if (!activity.isFinishing()) {
			activity.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					alertDialog(R.drawable.ic_launcher
							, titleText
							, message
							, null
							, activity
							, android.R.string.ok
							, null
							, null
							, null
							, null);
				}
			});
		}
	}

	public static void signinRequired(final Activity activity, @Nullable final Integer messageResId) {
		String message = StringManager.getString((messageResId == null) ? R.string.alert_signin_message : messageResId);
		signinRequired(activity, message);
	}

	public static void signinRequired(final Activity activity, final String message) {
		UI.showToastNotification(message, Toast.LENGTH_SHORT);
	}

	public static void updateApp(@NonNull final Activity activity) {

		final AlertDialog updateDialog = alertDialog(R.drawable.ic_launcher
				, StringManager.getString(R.string.dialog_update_title)
				, StringManager.getString(R.string.dialog_update_message)
				, null
				, activity
				, R.string.dialog_update_ok
				, R.string.dialog_update_cancel
				, null
				, new DialogInterface.OnClickListener() {

			@SuppressWarnings("deprecation")
			@Override
			public void onClick(@NonNull DialogInterface dialog, int which) {
				if (which == DialogInterface.BUTTON_POSITIVE) {
					try {
						Reporting.sendEvent(Reporting.TrackerCategory.UX, "aircandi_update_button_click", "com.aircandi", 0);
						Logger.d(this, "Update: navigating to market install/update page");
						final Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(StringManager.getString(R.string.uri_app_update)));
						intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
						intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
						activity.startActivity(intent);
					}
					catch (Exception e) {
								/*
								 * In case the market app isn't installed on the phone
								 */
						Logger.d(this, "Install: navigating to play website install page");
						final Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(StringManager.getString(R.string.uri_app_update_web)));
						intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
						activity.startActivityForResult(intent, Constants.ACTIVITY_MARKET);
					}
					dialog.dismiss();
					Patchr.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.EXTERNAL_TO);
				}
				else if (which == DialogInterface.BUTTON_NEGATIVE) {
					dialog.dismiss();
					activity.finish();
				}
			}
		}
				, new DialogInterface.OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
						/* Back button can trigger this */
				activity.finish();
			}
		});
		updateDialog.setCanceledOnTouchOutside(false);
		updateDialog.show();
	}

	public static void installAviary(@NonNull final Activity activity) {

		final AlertDialog updateDialog = alertDialog(R.drawable.ic_launcher
				, StringManager.getString(R.string.dialog_aviary_title)
				, StringManager.getString(R.string.dialog_aviary_message)
				, null
				, activity
				, R.string.dialog_aviary_ok
				, R.string.dialog_aviary_cancel
				, null
				, new DialogInterface.OnClickListener() {

			@SuppressWarnings("deprecation")
			@Override
			public void onClick(@NonNull DialogInterface dialog, int which) {
				if (which == DialogInterface.BUTTON_POSITIVE) {
					try {
						Reporting.sendEvent(Reporting.TrackerCategory.UX, "aviary_install_button_click", "com.aircandi", 0);
						Logger.d(this, "Update: navigating to aviary install/update page");
						final Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(StringManager.getString(R.string.uri_aviary_install)));
						intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
						activity.startActivity(intent);
					}
					catch (Exception e) {
						/*
						 * In case the market app isn't installed on the phone
						 */
						Logger.d(this, "Install: navigating to play website install page");
						final Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(StringManager.getString(R.string.uri_app_update_web)));
						intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
						activity.startActivityForResult(intent, Constants.ACTIVITY_MARKET);
					}
					dialog.dismiss();
					Patchr.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.EXTERNAL_TO);
				}
				else if (which == DialogInterface.BUTTON_NEGATIVE) {
					dialog.dismiss();
				}
			}
		}
				, new DialogInterface.OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				/* Back button can trigger this */
			}
		});
		updateDialog.setCanceledOnTouchOutside(false);
		updateDialog.show();
	}

	public static void locationServicesDisabled(@NonNull final Activity activity, @NonNull final AtomicBoolean shot) {

		final AlertDialog updateDialog = alertDialog(null
				, StringManager.getString(R.string.dialog_location_services_disabled_title)
				, StringManager.getString(R.string.dialog_location_services_disabled_message)
				, null
				, activity
				, R.string.dialog_location_services_disabled_ok
				, R.string.dialog_location_services_disabled_cancel
				, null
				, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(@NonNull DialogInterface dialog, int which) {
				if (which == DialogInterface.BUTTON_POSITIVE) {
					Reporting.sendEvent(Reporting.TrackerCategory.UX, "aircandi_location_settings_button_click", "com.aircandi", 0);
					Patchr.dispatch.route(activity, Route.SETTINGS_LOCATION, null, null);
					dialog.dismiss();
				}
				else if (which == DialogInterface.BUTTON_NEGATIVE) {
					shot.set(true);
					dialog.dismiss();
				}
			}
		}, null);
		updateDialog.setCanceledOnTouchOutside(false);
		updateDialog.show();
	}

	public static void dismiss(@Nullable Dialog dialog) {
		if (dialog != null
				&& dialog.isShowing()
				&& dialog.getWindow().getWindowManager() != null) {
			try {
				dialog.dismiss();
			}
			catch (Exception e) {
				/*
				 * Sometime we get a harmless exception that the view is not attached to window manager.
				 * It could be that the activity is getting destroyed before the dismiss can happen.
				 * It's bad form to eat all exceptions but we hold our nose, catch it and move on.
				 */
				Logger.v(dialog.getContext(), e.getMessage());
			}
		}
	}

	public static void locked(@NonNull final Activity activity, @NonNull Entity entity) {

		String message = StringManager.getString(R.string.alert_entity_locked, entity.schema);
		if (message != null) {
			Dialogs.alertDialogSimple(activity, null, message);
		}
	}
}