package com.patchr.utilities;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.NetworkManager;
import com.patchr.components.StringManager;
import com.patchr.components.UserManager;
import com.patchr.exceptions.ClientVersionException;
import com.patchr.exceptions.GcmRegistrationIOException;
import com.patchr.exceptions.ServiceException;
import com.patchr.objects.enums.Command;
import com.patchr.objects.ProxibaseError;
import com.patchr.service.ServiceResponse;

import org.apache.http.conn.ConnectTimeoutException;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Locale;

//import org.apache.http.NoHttpResponseException;
//import org.apache.http.client.ClientProtocolException;

public final class Errors {

	public static void handleError(final Activity activity, ServiceResponse response) {
		/* Stub that does nothing. */
	}

	public static void handleError(final Activity activity, ProxibaseError error) {
		handleError(activity, error, ErrorActionType.TOAST, ErrorAction.NONE);
	}

	public static void handleThrowable(final Activity activity, Throwable t) {
		ProxibaseError error = new ProxibaseError();
		error.message = t.getMessage();
		error.description = t.getLocalizedMessage();
		handleError(activity, error, ErrorActionType.TOAST, ErrorAction.NONE);
	}

	public static void handleError(final Activity activity, ProxibaseError error, ErrorActionType errorActionType, ErrorAction errorAction) {

		/* First show any required UI */
		final String alertMessage = error.description != null ? error.description : error.message;

		ErrorAction errAction = errorAction;

		if (errorActionType == ErrorActionType.AUTO || errorActionType == ErrorActionType.TOAST) {
			UI.toast(alertMessage);
			if (error.code.floatValue() == Constants.SERVICE_STATUS_CODE_UNAUTHORIZED_SESSION_EXPIRED
					|| error.code.floatValue() == Constants.SERVICE_STATUS_CODE_UNAUTHORIZED_CREDENTIALS) {
				errAction = ErrorAction.LOGOUT;
			}
		}
		else if (errorActionType == ErrorActionType.ALERT) {
			if (activity != null) {
				Dialogs.alertDialogSimple(activity, null, alertMessage);
			}
			else {
				UI.toast(alertMessage);
			}
		}

		/* Perform any follow-up actions. */
		if (errAction == ErrorAction.LOGOUT) {
			UserManager.shared().setCurrentUser(null, null);
			Patchr.router.route(Patchr.applicationContext, Command.LOBBY, null, null);
		}
		else if (errAction == ErrorAction.LOBBY) {
			/* Mostly because a more current client version is required. */
			if (activity != null && !activity.getClass().getSimpleName().equals("LobbyScreen")) {
				Patchr.router.route(activity, Command.LOBBY, null, null);
			}
		}
	}

	@NonNull
	@SuppressWarnings("ConstantConditions")
	public static ErrorResponse getErrorResponse(Context context, @NonNull ServiceResponse serviceResponse) {

		if (serviceResponse.statusCode != null) {

			if (serviceResponse.statusCode / 100 == HttpURLConnection.HTTP_INTERNAL_ERROR / 100) {  // 5XX Service problem
				/*
				 * Reached the service with a good call but the service failed for an unknown reason. Examples
				 * are service bugs like missing indexes causing mongo queries to throw errors.
				 *
				 * - 500: Something bad and unknown has happened in the service.
				 * - 502: Something bad and unknown has happened with a third party service (via our service)
				 * - 504: Gateway timout.
				 */
				if (Constants.ERROR_LEVEL == Log.VERBOSE) {
					if (serviceResponse.statusCode == HttpURLConnection.HTTP_GATEWAY_TIMEOUT) {
						return new ErrorResponse(ErrorActionType.TOAST, StringManager.getString(R.string.error_service_gateway_timeout));
					}
					return new ErrorResponse(ErrorActionType.TOAST, StringManager.getString(R.string.error_service_unknown_status));
				}
			}

			if (serviceResponse.statusCode / 100 == HttpURLConnection.HTTP_BAD_REQUEST / 100) {  // 4XX Request problem

				/* 400 */

				if (serviceResponse.statusCode == HttpURLConnection.HTTP_BAD_REQUEST) {
					/*
					 * Reached the service with a good call but request was not correct. This is often bad, missing
					 * or incorrect parameters.
					 */
					String description = "Bad service request: "
							+ ((serviceResponse.statusCodeService != null) ? serviceResponse.statusCodeService : "missing service status code");
					if (serviceResponse.statusCodeService != null) {
						if (serviceResponse.statusCodeService == Constants.SERVICE_STATUS_CODE_MISSING_PARAM) {
							description += ": Missing parameter";
						}
						else if (serviceResponse.statusCodeService == Constants.SERVICE_STATUS_CODE_BAD_PARAM) {
							description += ": Bad parameter";
						}
						else if (serviceResponse.statusCodeService == Constants.SERVICE_STATUS_CODE_BAD_TYPE) {
							description += ": Bad type";
						}
						else if (serviceResponse.statusCodeService == Constants.SERVICE_STATUS_CODE_BAD_VALUE) {
							description += ": Bad value";
						}
						else if (serviceResponse.statusCodeService == Constants.SERVICE_STATUS_CODE_BAD_JSON) {
							description += ": Bad json";
						}
					}
					ServiceException exception = new ServiceException(description);
					Reporting.logException(exception);
					return new ErrorResponse(Constants.ERROR_LEVEL == Log.VERBOSE ? ErrorActionType.TOAST : ErrorActionType.SILENT, description);
				}

				/* 401 */

				if (serviceResponse.statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
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

						if (serviceResponse.statusCodeService == Constants.SERVICE_STATUS_CODE_UNAUTHORIZED_SESSION_EXPIRED) {
							ErrorResponse errorResponse = new ErrorResponse(ErrorActionType.ALERT
									, StringManager.getString(R.string.error_session_expired)
									, StringManager.getString(R.string.error_session_expired_title));
							errorResponse.signout = true;
							return errorResponse;
						}

						if (serviceResponse.statusCodeService == Constants.SERVICE_STATUS_CODE_UNAUTHORIZED_CREDENTIALS) {
							if (serviceResponse.activityName != null) {
								if (serviceResponse.activityName.equals("PasswordEdit"))
									return new ErrorResponse(ErrorActionType.ALERT, StringManager.getString(R.string.error_change_password_unauthorized));
								else if (serviceResponse.activityName.equals("SignInEdit"))
									return new ErrorResponse(ErrorActionType.ALERT, StringManager.getString(R.string.error_signin_password_incorrect));
							}
							ErrorResponse errorResponse = new ErrorResponse(ErrorActionType.ALERT, StringManager.getString(R.string.error_session_invalid));
							errorResponse.signout = true;
							return errorResponse;
						}

						if (serviceResponse.statusCodeService == Constants.SERVICE_STATUS_CODE_UNAUTHORIZED_EMAIL_NOT_FOUND) {
							if (serviceResponse.activityName != null) {
								if (serviceResponse.activityName.equals("SignInEdit"))
									return new ErrorResponse(ErrorActionType.ALERT, StringManager.getString(R.string.error_signin_failed));
							}
						}
					}
				}

				/* 403 */

				if (serviceResponse.statusCode == HttpURLConnection.HTTP_FORBIDDEN) {
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
						if (serviceResponse.statusCodeService == Constants.SERVICE_STATUS_CODE_FORBIDDEN_USER_PASSWORD_WEAK)
							return new ErrorResponse(ErrorActionType.ALERT, StringManager.getString(R.string.error_signup_password_weak));
						else if (serviceResponse.statusCodeService == Constants.SERVICE_STATUS_CODE_FORBIDDEN_DUPLICATE)
							return new ErrorResponse(ErrorActionType.ALERT, StringManager.getString(R.string.error_signup_email_taken));
						else if (serviceResponse.statusCodeService == Constants.SERVICE_STATUS_CODE_FORBIDDEN)
							return new ErrorResponse(ErrorActionType.TOAST, StringManager.getString(R.string.error_forbidden));
					}
				}

				/* 404 */

				if (Constants.ERROR_LEVEL == Log.VERBOSE) {
					if (serviceResponse.statusCode == HttpURLConnection.HTTP_NOT_FOUND) {
						return new ErrorResponse(ErrorActionType.TOAST, StringManager.getString(R.string.error_client_request_not_found));
					}
				}

				/* Unhandled 4XX */

				if (Constants.ERROR_LEVEL == Log.VERBOSE) {
					return new ErrorResponse(ErrorActionType.TOAST, StringManager.getString(R.string.error_service_unhandled_request_error));
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
				ErrorResponse errorResponse = new ErrorResponse(ErrorActionType.SILENT, StringManager.getString(R.string.dialog_update_message));
				Patchr.applicationUpdateRequired = true;
				errorResponse.report = true;
				errorResponse.splash = true;
				return errorResponse;
			}

			if (exception instanceof GcmRegistrationIOException) {
				ErrorResponse errorResponse = new ErrorResponse(ErrorActionType.SILENT, StringManager.getString(R.string.error_gcm_registration_failed));
				errorResponse.report = true;
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
						return new ErrorResponse(ErrorActionType.TOAST, StringManager.getString(R.string.error_connection_airplane_mode));
					else
						return new ErrorResponse(ErrorActionType.TOAST, StringManager.getString(R.string.error_connection_none));
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
				 * - ClientProtocolException: malformed request and a bug.
				 * - NotFoundException: Reached service but requested something that isn't there.
				 * - UnauthorizedException: Reached service but user doesn't have needed permissions.
				 * - ForbiddenException: Reached service but request invalid per service policy.
				 * - NoHttpResponseException: target server failed to respond with a valid HTTP response
				 */
				if (Constants.ERROR_LEVEL == Log.VERBOSE) {
					//noinspection deprecation
					if (exception instanceof ConnectTimeoutException)
						return new ErrorResponse(ErrorActionType.TOAST, StringManager.getString(R.string.error_service_unavailable));

					if (exception instanceof SocketTimeoutException ||
							exception instanceof InterruptedIOException)
						return new ErrorResponse(ErrorActionType.TOAST, StringManager.getString(R.string.error_connection_poor));

					//noinspection deprecation
					if (exception instanceof ConnectException)
						return new ErrorResponse(ErrorActionType.TOAST, StringManager.getString(R.string.error_service_unavailable));

					if (exception instanceof SocketException) {
						Integer messageResId = R.string.error_service_unavailable;
						if (exception.getMessage().toLowerCase(Locale.US).contains("econnreset")) {
							messageResId = R.string.error_connection_reset;
						}
						return new ErrorResponse(ErrorActionType.AUTO, StringManager.getString(messageResId));
					}

					if (exception instanceof UnknownHostException)
						return new ErrorResponse(ErrorActionType.TOAST, StringManager.getString(R.string.error_client_unknown_host));

					if (exception instanceof FileNotFoundException)
						return new ErrorResponse(ErrorActionType.TOAST, StringManager.getString(R.string.error_service_file_not_found));

					if (exception instanceof EOFException)
						return new ErrorResponse(ErrorActionType.TOAST, StringManager.getString(R.string.error_client_request_stream_error));
				}
				else {
					//noinspection deprecation
					if (exception instanceof UnknownHostException
							|| exception instanceof ConnectTimeoutException
							|| exception instanceof InterruptedIOException
							|| exception instanceof SocketTimeoutException
							|| exception instanceof SocketException) {
						return new ErrorResponse(ErrorActionType.AUTO, StringManager.getString(R.string.error_connection_poor));
					}
				}

				/* If we have a wifi network connection, check for a walled garden */
				if (NetworkManager.getInstance().isConnected()) {
					if (!NetworkManager.getInstance().isMobileNetwork()) {
						/* This can trigger another call to getErrorResponse and recurse until a stack overflow. */
						if (NetworkManager.getInstance().isInternetReachable()) {
							return new ErrorResponse(ErrorActionType.AUTO, StringManager.getString(R.string.error_connection_walled_garden));
						}
					}
				}
			}

			/* Unhandled exception */
			if (Constants.ERROR_LEVEL == Log.VERBOSE) {
				return new ErrorResponse(ErrorActionType.TOAST, exception.getMessage());
			}
		}

		return new ErrorResponse(ErrorActionType.SILENT, null);
	}

	public static Boolean isNetworkError(@NonNull ServiceResponse serviceResponse) {
		return (serviceResponse.statusCode == null && serviceResponse.exception != null && serviceResponse.exception instanceof IOException);
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	public enum ErrorActionType {
		AUTO,
		ALERT,
		TOAST,
		SILENT
	}

	public enum ErrorAction {
		NONE,
		LOGOUT,
		LOBBY
	}

	@SuppressWarnings("ucd")
	public static class ErrorResponse {
		public String          errorMessage;
		public String          errorTitle;
		public ErrorActionType errorResponseType;

		public Boolean signout    = false;
		public Boolean splash     = false;
		public Boolean report     = false;
		public Boolean clearPhoto = false;

		public ErrorResponse(ErrorActionType responseType) {
			this(responseType, null);
		}

		public ErrorResponse(ErrorActionType responseType, String errorMessage) {
			this(responseType, errorMessage, null);
		}

		public ErrorResponse(ErrorActionType responseType, String errorMessage, String errorTitle) {
			this.errorMessage = errorMessage;
			this.errorTitle = errorTitle;
			this.errorResponseType = responseType;
		}
	}
}