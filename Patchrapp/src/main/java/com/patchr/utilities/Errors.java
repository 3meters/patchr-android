package com.patchr.utilities;

import android.app.Activity;
import android.content.Context;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.components.Logger;
import com.patchr.components.UserManager;
import com.patchr.exceptions.ClientVersionException;
import com.patchr.exceptions.NoNetworkException;
import com.patchr.exceptions.ServiceException;
import com.patchr.objects.enums.ErrorAction;
import com.patchr.objects.enums.ErrorActionType;

public final class Errors {

	public static void handleError(final Context context, final Throwable throwable) {
		handleError(context, throwable, ErrorActionType.TOAST, ErrorAction.NONE);
	}

	public static void handleError(Context context, final Throwable throwable, ErrorActionType errorActionType, ErrorAction errorAction) {

		/* First show any required UI */

		String alertMessage = throwable.getMessage();
		ErrorAction errAction = errorAction;

		if (throwable instanceof NoNetworkException) {
			return;
		}

		if (errorActionType == ErrorActionType.AUTO || errorActionType == ErrorActionType.TOAST) {
			UI.toast(alertMessage);
		}
		else if (errorActionType == ErrorActionType.ALERT) {
			if (context != null && context instanceof Activity) {
				Dialogs.alertDialogSimple((Activity) context, null, alertMessage);
			}
			else {
				UI.toast(alertMessage);
			}
		}

		if (throwable instanceof ServiceException) {
			float code = ((ServiceException) throwable).code.floatValue();
			if (code == Constants.SERVICE_STATUS_CODE_UNAUTHORIZED_SESSION_EXPIRED
				|| code == Constants.SERVICE_STATUS_CODE_UNAUTHORIZED_CREDENTIALS) {
				errAction = ErrorAction.LOGOUT;
			}
		}
		else if (throwable instanceof ClientVersionException) {
			/* Post calls to browse are the primary check */
			Patchr.applicationUpdateRequired = true;
			errAction = ErrorAction.LOGOUT;
		}

		/* Perform any follow-up actions. */
		if (errAction == ErrorAction.LOGOUT) {
			UserManager.shared().setCurrentUser(null, null);
			UI.routeLobby(Patchr.applicationContext);
		}
		else if (errAction == ErrorAction.LOBBY) {
			/* Mostly because a more current client version is required. */
			if (context != null && !context.getClass().getSimpleName().equals("LobbyScreen")) {
				UI.routeLobby(context);
			}
		}

		Logger.w(context, "Network Error Summary");
		Logger.w(context, throwable.getMessage());
	}
}