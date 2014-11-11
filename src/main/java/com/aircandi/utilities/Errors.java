package com.aircandi.utilities;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.ServiceConstants;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.StringManager;
import com.aircandi.exceptions.ClientVersionException;
import com.aircandi.exceptions.GcmRegistrationIOException;
import com.aircandi.exceptions.ServiceException;
import com.aircandi.objects.Route;
import com.aircandi.service.ServiceResponse;
import com.aircandi.ui.base.BaseActivity;

import org.apache.http.HttpStatus;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.conn.ConnectTimeoutException;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Locale;

public final class Errors {

	public static void handleError(final Activity activity, ServiceResponse serviceResponse) {

		ErrorResponse errorResponse = serviceResponse.errorResponse;
		if (errorResponse == null || errorResponse.errorResponseType == null) {
			errorResponse = new ErrorResponse(ResponseType.TOAST, "Unhandled status error: " + serviceResponse.statusCode);
		}
		/*
		 * First show any required UI
		 */
		if (errorResponse.errorResponseType == ResponseType.AUTO) {
			if (activity != null) {
				final String errorMessage = errorResponse.errorMessage;
				Patchr.mainThreadHandler.post(new Runnable() {

					@Override
					public void run() {
						Dialogs.alertDialog(R.drawable.ic_launcher
								, null
								, errorMessage
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
			else {
				UI.showToastNotification(errorResponse.errorMessage, Toast.LENGTH_SHORT);
			}
		}
		else if (errorResponse.errorResponseType == ResponseType.TOAST) {
			UI.showToastNotification(errorResponse.errorMessage, Toast.LENGTH_SHORT);
		}
		/*
		 * Perform any follow-up actions.
		 */
		if (errorResponse.track) {
			Patchr.tracker.sendException(serviceResponse.exception);
		}

		if (errorResponse.signout) {
			BaseActivity.signout(activity, false);
		}
		else if (errorResponse.splash) {
			/*
			 * Mostly because a more current client version is required.
			 */
			if (activity != null && !activity.getClass().getSimpleName().equals("SplashForm")) {
				Patchr.dispatch.route(activity, Route.SPLASH, null, null, null);
			}
		}
	}

	public static final ErrorResponse getErrorResponse(Context context, ServiceResponse serviceResponse) {

		if (serviceResponse.statusCode != null) {

			if (serviceResponse.statusCode / 100 == HttpStatus.SC_INTERNAL_SERVER_ERROR / 100) {  // 5XX Service problem
				/*
				 * Reached the service with a good call but the service failed for an unknown reason. Examples
				 * are service bugs like missing indexes causing mongo queries to throw errors.
				 *
				 * - 500: Something bad and unknown has happened in the service.
				 * - 502: Something bad and unknown has happened with a third party service (via our service)
				 * - 504: Gateway timout.
				 */
				if (Constants.ERROR_LEVEL == Log.VERBOSE) {
					if (serviceResponse.statusCode == HttpStatus.SC_GATEWAY_TIMEOUT) {
						return new ErrorResponse(ResponseType.TOAST, StringManager.getString(R.string.error_service_gateway_timeout));
					}
					return new ErrorResponse(ResponseType.TOAST, StringManager.getString(R.string.error_service_unknown_status));
				}
			}

			if (serviceResponse.statusCode / 100 == HttpStatus.SC_BAD_REQUEST / 100) {  // 4XX Request problem

				/* 400 */

				if (serviceResponse.statusCode == HttpStatus.SC_BAD_REQUEST) {
					/*
					 * Reached the service with a good call but request was not correct. This is often bad, missing
					 * or incorrect parameters.
					 */
					String description = "Bad service request: "
							+ ((serviceResponse.statusCodeService != null) ? serviceResponse.statusCodeService : "missing service status code");
					ServiceException exception = new ServiceException(description);
					Reporting.logException(exception);
					return new ErrorResponse(Constants.ERROR_LEVEL == Log.VERBOSE ? ResponseType.TOAST : ResponseType.NONE, description);
				}

				/* 401 */

				if (serviceResponse.statusCode == HttpStatus.SC_UNAUTHORIZED) {
					/*
					 * Reached the service with a good call but failed for a well known reason.
					 *
					 * This could have been caused by any problem while inserting/updating.
					 * We look first for ones that are known responses from the service.
					 *
					 * - 401.1: invalid or missing session
					 * - 401.2: expired session
					 */
					if (serviceResponse.statusCodeService != null) {

						if (serviceResponse.statusCodeService == ServiceConstants.SERVICE_STATUS_CODE_UNAUTHORIZED_SESSION_EXPIRED) {
							ErrorResponse errorResponse = new ErrorResponse(ResponseType.DIALOG
									, StringManager.getString(R.string.error_session_expired)
									, StringManager.getString(R.string.error_session_expired_title));
							errorResponse.splash = true;
							return errorResponse;
						}

						if (serviceResponse.statusCodeService == ServiceConstants.SERVICE_STATUS_CODE_UNAUTHORIZED_CREDENTIALS) {
							if (serviceResponse.activityName != null) {
								if (serviceResponse.activityName.equals("PasswordEdit"))
									return new ErrorResponse(ResponseType.DIALOG, StringManager.getString(R.string.error_change_password_unauthorized));
								else if (serviceResponse.activityName.equals("SignInEdit"))
									return new ErrorResponse(ResponseType.DIALOG, StringManager.getString(R.string.error_signin_invalid_signin));
							}
							ErrorResponse errorResponse = new ErrorResponse(ResponseType.DIALOG, StringManager.getString(R.string.error_session_invalid));
							errorResponse.splash = true;
							return errorResponse;
						}
					}
				}

				/* 403 */

				if (serviceResponse.statusCode == HttpStatus.SC_FORBIDDEN) {
					/*
					 * Reached the service with a good call but failed for a well known reason.
					 *
					 * This could have been caused by any problem while inserting/updating.
					 * We look first for ones that are known responses from the service.
					 *
					 * - 403: Forbidden
					 * - 403.1: Duplicate value not allowed (email not unique for the clients purposes)
					 * - 403.11: Duplicate found
					 * - 403.21: Password not strong enough
					 */
					if (serviceResponse.statusCodeService != null) {
						if (serviceResponse.statusCodeService == ServiceConstants.SERVICE_STATUS_CODE_FORBIDDEN_USER_PASSWORD_WEAK)
							return new ErrorResponse(ResponseType.DIALOG, StringManager.getString(R.string.error_signup_password_weak));
						else if (serviceResponse.statusCodeService == ServiceConstants.SERVICE_STATUS_CODE_FORBIDDEN_DUPLICATE)
							return new ErrorResponse(ResponseType.DIALOG, StringManager.getString(R.string.error_signup_email_taken));
					}
				}

				/* 404 */

				if (Constants.ERROR_LEVEL == Log.VERBOSE) {
					if (serviceResponse.statusCode == HttpStatus.SC_NOT_FOUND) {
						return new ErrorResponse(ResponseType.TOAST, StringManager.getString(R.string.error_client_request_not_found));
					}
				}

				/* Unhandled 4XX */

				if (Constants.ERROR_LEVEL == Log.VERBOSE) {
					return new ErrorResponse(ResponseType.TOAST, StringManager.getString(R.string.error_service_unhandled_request_error));
				}
			}
		}

		if (serviceResponse.exception != null) {
			/*
			 * Exception based error
			 */
			Exception exception = serviceResponse.exception;
			if (exception instanceof ClientVersionException) {
				/*
				 * The current client version is not allowed to access the service api.
				 */
				ErrorResponse errorResponse = new ErrorResponse(ResponseType.NONE, StringManager.getString(R.string.dialog_update_message));
				Patchr.applicationUpdateRequired = true;
				errorResponse.track = true;
				errorResponse.splash = true;
				return errorResponse;
			}

			if (exception instanceof GcmRegistrationIOException) {
				ErrorResponse errorResponse = new ErrorResponse(ResponseType.NONE, StringManager.getString(R.string.error_gcm_registration_failed));
				errorResponse.track = true;
				return errorResponse;
			}

			if (exception instanceof IOException) {
				/*
				 * We get an UnknownHostException when mobile data and wifi are is disabled. Also fails fast
				 * instead of waiting for socket/connection timeout.
				 *
				 * In airplane mode, we spin until a socket/connection timeout drops us into here.
				 */
				if (!NetworkManager.getInstance().isConnected()) {
					if (NetworkManager.isAirplaneMode(context))
						return new ErrorResponse(ResponseType.TOAST, StringManager.getString(R.string.error_connection_airplane_mode));
					else
						return new ErrorResponse(ResponseType.TOAST, StringManager.getString(R.string.error_connection_none));
				}

				/*
				 * Network request failed for some reason:
				 *
				 * - UnknownHostException: The ip address of the host could not be resolved or the dns server couldn't be reached.
				 * - ConnectException: Couldn't connect to the service host.
				 * - ConnectTimeoutException: Timeout trying to establish connection to service host.
				 *
				 * - SocketException: thrown during socket creation or setting options, we don't have a connection
				 * - SocketTimeoutException: Timeout trying to send/receive data to the service (might not be up).
				 *
				 * - WalledGardenException: have a connection but user was taken to a different host than requested
				 * - ClientProtocolException: malformed request and a bug.
				 * - NotFoundException: Reached service but requested something that isn't there.
				 * - UnauthorizedException: Reached service but user doesn't have needed permissions.
				 * - ForbiddenException: Reached service but request invalid per service policy.
				 * - NoHttpResponseException: target server failed to respond with a valid HTTP response
				 */
				if (Constants.ERROR_LEVEL == Log.VERBOSE) {
					if (exception instanceof ConnectTimeoutException)
						return new ErrorResponse(ResponseType.TOAST, StringManager.getString(R.string.error_service_unavailable)).setTrack(true);

					if (exception instanceof SocketTimeoutException)
						return new ErrorResponse(ResponseType.TOAST, StringManager.getString(R.string.error_connection_poor)).setTrack(true);

					if (exception instanceof ConnectException
							|| exception instanceof NoHttpResponseException)
						return new ErrorResponse(ResponseType.TOAST, StringManager.getString(R.string.error_service_unavailable));

					if (exception instanceof SocketException) {
						Integer messageResId = R.string.error_service_unavailable;
						if (exception.getMessage().toLowerCase(Locale.US).contains("econnreset")) {
							messageResId = R.string.error_connection_reset;
						}
						return new ErrorResponse(ResponseType.AUTO, StringManager.getString(messageResId));
					}

					if (exception instanceof UnknownHostException)
						return new ErrorResponse(ResponseType.TOAST, StringManager.getString(R.string.error_client_unknown_host));

					if (exception instanceof FileNotFoundException)
						return new ErrorResponse(ResponseType.TOAST, StringManager.getString(R.string.error_service_file_not_found));

					if (exception instanceof ClientProtocolException)
						return new ErrorResponse(ResponseType.TOAST, StringManager.getString(R.string.error_client_request_error));

					if (exception instanceof EOFException)
						return new ErrorResponse(ResponseType.TOAST, StringManager.getString(R.string.error_client_request_stream_error));
				}
				else {
					if (exception instanceof UnknownHostException
							|| exception instanceof ConnectTimeoutException
							|| exception instanceof SocketTimeoutException
							|| exception instanceof ConnectException
							|| exception instanceof SocketException) {
						return new ErrorResponse(ResponseType.AUTO, StringManager.getString(R.string.error_connection_poor));
					}
				}

				/* If we have a wifi network connection, check for a walled garden */
				if (NetworkManager.getInstance().isConnected()) {
					if (!NetworkManager.getInstance().isMobileNetwork()) {
						/* This can trigger another call to getErrorResponse and recurse until a stack overflow. */
						if (NetworkManager.getInstance().isWalledGardenConnection()) {
							return new ErrorResponse(ResponseType.AUTO, StringManager.getString(R.string.error_connection_walled_garden));
						}
					}
				}
			}

			/* Unhandled exception */
			if (Constants.ERROR_LEVEL == Log.VERBOSE) {
				return new ErrorResponse(ResponseType.TOAST, exception.getMessage());
			}
		}

		ErrorResponse errorResponse = new ErrorResponse(ResponseType.NONE, null);
		return errorResponse;
	}

	public static Boolean isNetworkError(ServiceResponse serviceResponse) {
		return (serviceResponse.statusCode == null && serviceResponse.exception != null && serviceResponse.exception instanceof IOException);
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	public static enum ResponseType {
		AUTO,
		DIALOG,
		TOAST,
		NONE
	}

	@SuppressWarnings("ucd")
	public static class ErrorResponse {
		public String       errorMessage;
		public String       errorTitle;
		public ResponseType errorResponseType;
		public Boolean signout = false;
		public Boolean splash  = false;
		public Boolean track   = false;

		public ErrorResponse(ResponseType responseType) {
			this(responseType, null);
		}

		public ErrorResponse(ResponseType responseType, String errorMessage) {
			this(responseType, errorMessage, null);
		}

		public ErrorResponse(ResponseType responseType, String errorMessage, String errorTitle) {
			this.errorMessage = errorMessage;
			this.errorTitle = errorTitle;
			this.errorResponseType = responseType;
		}

		public Boolean getTrack() {
			return track;
		}

		public ErrorResponse setTrack(Boolean track) {
			this.track = track;
			return this;
		}
	}
}