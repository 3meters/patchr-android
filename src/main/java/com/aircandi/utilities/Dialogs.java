package com.aircandi.utilities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.Toast;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.AndroidManager;
import com.aircandi.components.Logger;
import com.aircandi.components.StringManager;
import com.aircandi.components.TrackerBase.TrackerCategory;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Route;
import com.aircandi.objects.Shortcut;
import com.aircandi.objects.Shortcut.InstallStatus;
import com.aircandi.objects.ShortcutMeta;
import com.aircandi.objects.TransitionType;
import com.aircandi.ui.base.BaseActivity;

import java.util.concurrent.atomic.AtomicBoolean;

public class Dialogs {

	public static AlertDialog alertDialog(Integer iconResource // $codepro.audit.disable largeNumberOfParameters
			, String titleText
			, String message
			, View customView
			, Context context
			, Integer okButtonId
			, Integer cancelButtonId
			, Integer neutralButtonId
			, OnClickListener listenerClick
			, OnCancelListener listenerCancel) {

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

	public static void alertDialogSimple(final Activity activity, final String titleText, final String message) {
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

	public static void signinRequired(final Activity activity, final Integer messageResId) {
		String message = StringManager.getString((messageResId == null) ? R.string.alert_signin_message : messageResId);
		signinRequired(activity, message);
	}

	public static void signinRequired(final Activity activity, final String message) {
		UI.showToastNotification(message, Toast.LENGTH_SHORT);
	}

	public static void updateApp(final Activity activity) {

		final AlertDialog updateDialog = alertDialog(R.drawable.ic_launcher
				, StringManager.getString(R.string.dialog_update_title)
				, StringManager.getString(R.string.dialog_update_message)
				, null
				, activity
				, R.string.dialog_update_ok
				, R.string.dialog_update_cancel
				, null
				, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (which == DialogInterface.BUTTON_POSITIVE) {
					try {
						Aircandi.tracker.sendEvent(TrackerCategory.UX, "aircandi_update_button_click", "com.aircandi", 0);
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
					Aircandi.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
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

	public static void locationServicesDisabled(final Activity activity, final AtomicBoolean shot) {

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
			public void onClick(DialogInterface dialog, int which) {
				if (which == DialogInterface.BUTTON_POSITIVE) {
					Aircandi.tracker.sendEvent(TrackerCategory.UX, "aircandi_location_settings_button_click", "com.aircandi", 0);
					Aircandi.dispatch.route(activity, Route.SETTINGS_LOCATION, null, null, null);
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

	public static void installApp(final Activity activity, final Shortcut shortcut, final Entity entity) {

		final AlertDialog installDialog = Dialogs.alertDialog(null
				, StringManager.getString(R.string.dialog_install_title)
				, StringManager.getString(R.string.dialog_install_message)
				, null
				, activity
				, R.string.dialog_install_ok
				, R.string.dialog_install_cancel
				, R.string.dialog_install_later
				, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (which == DialogInterface.BUTTON_POSITIVE) {
					String packageName = AndroidManager.getPackageNameByAppName(shortcut.app);
					try {
						Aircandi.tracker.sendEvent(TrackerCategory.UX, "app_install_button_click", packageName, 0);
						Logger.d(this, "Install: navigating to market install page");

						final Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(StringManager.getString("market://details?id="
								+ packageName
								+ "&referrer=utm_source%3D[@string/id_referrer]")));

						intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
						intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
						activity.startActivity(intent);
					}
					catch (Exception e) {
								/*
								 * In case the market app isn't installed on the phone
								 */
						Logger.d(this, "Install: navigating to play website install page");

						final Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(StringManager
								.getString("http://play.google.com/store/apps/details?id="
										+ packageName
										+ "&referrer=utm_source%3D[@string/id_referrer]")));

						intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
						activity.startActivityForResult(intent, Constants.ACTIVITY_MARKET);
					}
					dialog.dismiss();
					Aircandi.getInstance().getAnimationManager().doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
				}
				else if (which == DialogInterface.BUTTON_NEGATIVE) {
					final ShortcutMeta meta = Shortcut.shortcutMeta.get(shortcut.app);
					if (meta != null) {
						meta.installStatus = InstallStatus.DECLINED;
					}
					else {
						ShortcutMeta shortcutMeta = new ShortcutMeta(InstallStatus.DECLINED);
						Shortcut.shortcutMeta.put(shortcut.app, shortcutMeta);
					}
					((BaseActivity) activity).setInvalidated(true); // forces redraw
					Aircandi.dispatch.route(activity, Route.SHORTCUT, entity, shortcut, null);
					dialog.dismiss();
				}
				else if (which == DialogInterface.BUTTON_NEUTRAL) {
					final ShortcutMeta meta = Shortcut.shortcutMeta.get(shortcut.app);
					if (meta != null) {
						meta.installStatus = InstallStatus.LATER;
					}
					else {
						ShortcutMeta shortcutMeta = new ShortcutMeta(InstallStatus.LATER);
						Shortcut.shortcutMeta.put(shortcut.app, shortcutMeta);
					}
					Aircandi.dispatch.route(activity, Route.SHORTCUT, entity, shortcut, null);
					dialog.dismiss();
				}
			}
		}
				, null);

		installDialog.setCanceledOnTouchOutside(false);
		installDialog.show();
	}

	public static void locked(final Activity activity, Entity entity) {

		String message = StringManager.getString(R.string.alert_entity_locked, entity.schema);
		if (message != null) {
			Dialogs.alertDialogSimple(activity, null, message);
		}
	}
}