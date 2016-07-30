package com.patchr.utilities;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.View;

import com.patchr.Constants;
import com.patchr.R;
import com.patchr.components.AnimationManager;
import com.patchr.components.Logger;
import com.patchr.components.StringManager;
import com.patchr.model.RealmEntity;
import com.patchr.objects.enums.TransitionType;

import java.util.concurrent.atomic.AtomicBoolean;

public class Dialogs {

	public static AlertDialog alertDialog(Integer iconResource // $codepro.audit.disable largeNumberOfParameters
			, String titleText
			, String message
			, View customView
			, @NonNull Context context
			, Integer okButtonId
			, Integer cancelButtonId
			, Integer neutralButtonId
			, OnClickListener listenerClick
			, OnCancelListener listenerCancel) {

		final AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.patchr_theme_dialog);

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

	public static void alert(Integer resId, Activity activity) {
		/* Can only be called on the main thread! */
		Dialogs.alert(StringManager.getString(resId), activity, null);
	}

	public static void alert(Integer resId, Activity activity, OnClickListener listener) {
		/* Can only be called on the main thread! */
		Dialogs.alert(StringManager.getString(resId), activity, listener);
	}

	public static void alert(String message, Activity activity) {
		/* Can only be called on the main thread! */
		Dialogs.alert(message, activity, null);
	}

	public static void alert(String message, Activity activity, OnClickListener listener) {
		/* Can only be called on the main thread! */
		final AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.patchr_theme_dialog);
		builder.setMessage(message);
		builder.setPositiveButton(R.string.dialog_ok, listener);
		Dialog dialog = builder.create();
		dialog.show();
	}

	public static void alertDialogSimple(@NonNull final Activity activity, final String titleText, final String message) {
		if (!activity.isFinishing()) {
			activity.runOnUiThread(new Runnable() {

				@Override public void run() {
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

	public static void permissionRationale(@NonNull final Activity activity) {

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
							activity.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
							AnimationManager.doOverridePendingTransition((Activity) activity, TransitionType.FORM_TO);
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

	public static void dismiss(Dialog dialog) {
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

	public static void locked(@NonNull final Activity activity, @NonNull RealmEntity entity) {

		String message = StringManager.getString(R.string.alert_entity_locked, entity.schema);
		Dialogs.alertDialogSimple(activity, null, message);
	}
}