package com.aircandi.utilities;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import com.aircandi.Patchr;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.ServiceConstants;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.StringManager;
import com.aircandi.objects.Route;
import com.aircandi.exceptions.ClientVersionException;
import com.aircandi.exceptions.GcmRegistrationIOException;
import com.aircandi.exceptions.ImageSizeException;
import com.aircandi.exceptions.ImageUnusableException;
import com.aircandi.exceptions.ServiceException;
import com.aircandi.service.ServiceResponse;
import com.aircandi.ui.base.BaseActivity;

import org.apache.http.HttpStatus;
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
		if (errorResponse.errorResponseType == ResponseType.DIALOG) {
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

		//noinspection PointlessBooleanExpression,ConstantConditions
		if (!Constants.ERROR_LEVEL_VERBOSE) {

			if (serviceResponse.statusCode != null) {
				/*
				 * Any bad service request: 4xx
				 */
				if (serviceResponse.statusCode / 100 == HttpStatus.SC_BAD_REQUEST / 100) {

					if (serviceResponse.statusCode == HttpStatus.SC_BAD_REQUEST) {
						/*
						 * Reached the service with a good call but request was not correct. This is often bad, missing
						 * or incorrect parameters.
						 */
						String description = "Bad service request: "
								+ ((serviceResponse.statusCodeService != null) ? serviceResponse.statusCodeService : "missing service status code");
						ServiceException exception = new ServiceException(description);
						Reporting.logException(exception);
						ErrorResponse errorResponse = new ErrorResponse(ResponseType.NONE, null);
						return errorResponse;
					}
					else if (serviceResponse.statusCode == HttpStatus.SC_UNAUTHORIZED) {
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
							else if (serviceResponse.statusCodeService == ServiceConstants.SERVICE_STATUS_CODE_UNAUTHORIZED_CREDENTIALS) {
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
							else if (serviceResponse.statusCodeService == ServiceConstants.SERVICE_STATUS_CODE_UNAUTHORIZED_EMAIL)
								return new ErrorResponse(ResponseType.DIALOG, StringManager.getString(R.string.error_whitelist_unauthorized));
							else if (serviceResponse.statusCodeService == ServiceConstants.SERVICE_STATUS_CODE_UNAUTHORIZED_EMAIL_NOT_VALIDATED)
								return new ErrorResponse(ResponseType.DIALOG, StringManager.getString(R.string.error_unverified_unauthorized));
						}
					}
					else //noinspection StatementWithEmptyBody
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
						else {
						/*
						 * Something in the 4xx range not handled earlier
						 */
						}
				}
				ErrorResponse errorResponse = new ErrorResponse(ResponseType.NONE, null);
				return errorResponse;
			}
			else {
				/*
				 * Exception based error
				 */
				Exception exception = serviceResponse.exception;
				if (exception instanceof ClientVersionException) {
					/*
					 * This gets returned by any network call to service where this aircandi version
					 * is not allowed to access the service api.
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
				else if (exception instanceof IOException) {
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
					 * - ConnectException: Couldn't connect to the service host.
					 * - ConnectTimeoutException: Timeout trying to establish connection to service host.
					 * - SocketException: thrown during socket creation or setting options, we don't have a connection
					 * - SocketTimeoutException: Timeout trying to send/receive data to the service (might not be up).
					 * - WalledGardenException: have a connection but user was taken to a different host than requested
					 * - UnknownHostException: The ip address of the host could not be determined.
					 * - ClientProtocolException: malformed request and a bug.
					 * - NotFoundException: Reached service but requested something that isn't there.
					 * - UnauthorizedException: Reached service but user doesn't have needed permissions.
					 * - ForbiddenException: Reached service but request invalid per service policy.
					 * - GatewayTimeoutException: ?
					 * 
					 * Still left
					 * - NoHttpResponseException: target server failed to respond with a valid HTTP response
					 * - UnknownHostException: hostname didn't exist in the dns system
					 */
					/* If we have a wifi network connection, check for a walled garden */
					if (NetworkManager.getInstance().isConnected()) {
						if (!NetworkManager.getInstance().isMobileNetwork()) {
							/* This can trigger another call to getErrorResponse and recurse until a stack overflow. */
							if (NetworkManager.getInstance().isWalledGardenConnection())
								return new ErrorResponse(ResponseType.DIALOG, StringManager.getString(R.string.error_connection_walled_garden));
						}
					}

					/* Show unfriendly message if user is developer and dev stuff is enabled */
					if (Patchr.settings.getBoolean(StringManager.getString(R.string.pref_enable_dev), false)
							&& Type.isTrue(Patchr.getInstance().getCurrentUser().developer))
						return new ErrorResponse(ResponseType.TOAST, StringManager.getString(R.string.error_service_unknown_exception) + ": "
								+ exception.getClass().getSimpleName());

				}
				ErrorResponse errorResponse = new ErrorResponse(ResponseType.NONE, null);
				return errorResponse;
			}
		}
		else {
			if (serviceResponse.statusCode != null) {
				/*
				 * Status code based error
				 */
				if (serviceResponse.statusCode / 100 == HttpStatus.SC_INTERNAL_SERVER_ERROR / 100) {
					/*
					 * Reached the service with a good call but the service failed for an unknown reason. Examples
					 * are service bugs like missing indexes causing mongo queries to throw errors.
					 * 
					 * - 500: Something bad and unknown has happened in the service.
					 * - 502: Something bad and unknown has happened with a third party service (via our service)
					 */
					if (Patchr.settings.getBoolean(StringManager.getString(R.string.pref_enable_dev), false)
							&& Type.isTrue(Patchr.getInstance().getCurrentUser().developer))
						return new ErrorResponse(ResponseType.TOAST, StringManager.getString(R.string.error_service_unknown_status));
					return new ErrorResponse(ResponseType.TOAST, StringManager.getString(R.string.error_service_unknown));
				}
				else if (serviceResponse.statusCode == HttpStatus.SC_NOT_FOUND)
					return new ErrorResponse(ResponseType.TOAST, StringManager.getString(R.string.error_client_request_not_found));
				else if (serviceResponse.statusCode == HttpStatus.SC_FORBIDDEN) {
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
				else if (serviceResponse.statusCode == HttpStatus.SC_GATEWAY_TIMEOUT)
					return new ErrorResponse(ResponseType.TOAST, StringManager.getString(R.string.error_service_gateway_timeout));
				else if (serviceResponse.statusCode == HttpStatus.SC_UNAUTHORIZED) {
					/*
					 * Reached the service with a good call but failed for a well known reason.
					 * 
					 * This could have been caused by any problem while inserting/updating.
					 * We look first for ones that are known responses from the service.
					 * 
					 * - 403.x: password not strong enough
					 * - 403.x: email not unique
					 * - 401.2: expired session
					 * - 401.1: invalid or missing session
					 */
					if (serviceResponse.statusCodeService != null) {
						if (serviceResponse.statusCodeService == ServiceConstants.SERVICE_STATUS_CODE_UNAUTHORIZED_SESSION_EXPIRED) {
							ErrorResponse errorResponse = new ErrorResponse(ResponseType.DIALOG
									, StringManager.getString(R.string.error_session_expired)
									, StringManager.getString(R.string.error_session_expired_title));
							errorResponse.splash = true;
							return errorResponse;
						}
						else if (serviceResponse.statusCodeService == ServiceConstants.SERVICE_STATUS_CODE_UNAUTHORIZED_CREDENTIALS) {
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
						else if (serviceResponse.statusCodeService == ServiceConstants.SERVICE_STATUS_CODE_UNAUTHORIZED_EMAIL)
							return new ErrorResponse(ResponseType.DIALOG, StringManager.getString(R.string.error_whitelist_unauthorized));
						else if (serviceResponse.statusCodeService == ServiceConstants.SERVICE_STATUS_CODE_UNAUTHORIZED_EMAIL_NOT_VALIDATED)
							return new ErrorResponse(ResponseType.DIALOG, StringManager.getString(R.string.error_unverified_unauthorized));
					}
				}
				return new ErrorResponse(ResponseType.TOAST, "Unhandled status error: " + serviceResponse.statusCode);
			}
			else {
				/*
				 * Exception based error
				 */
				Exception exception = serviceResponse.exception;
				if (exception instanceof ClientVersionException) {
					/*
					 * This gets returned by any network call to service where this aircandi version
					 * is not allowed to access the service api.
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
				else if (exception instanceof ImageSizeException)
					return new ErrorResponse(ResponseType.TOAST, StringManager.getString(R.string.error_image_too_large)).setTrack(true);
				else if (exception instanceof ImageUnusableException)
					return new ErrorResponse(ResponseType.TOAST, StringManager.getString(R.string.error_image_unusable)).setTrack(true);
				else if (exception instanceof IOException) {
					/*
					 * We get an UnknownHostException when mobile data and wifi are is disabled. Also fails fast
					 * instead of waiting for socket/connection timeout.
					 * 
					 * In airplane mode, we spin until a socket/connection timeout drops us into here.
					 */
					if (!NetworkManager.getInstance().isConnected()) {
						if (NetworkManager.isAirplaneMode(context))
							return new ErrorResponse(ResponseType.DIALOG, StringManager.getString(R.string.error_connection_airplane_mode));
						else
							return new ErrorResponse(ResponseType.DIALOG, StringManager.getString(R.string.error_connection_none));
					}

					/*
					 * Network request failed for some reason:
					 * 
					 * - ConnectException: Couldn't connect to the service host.
					 * - ConnectTimeoutException: Timeout trying to establish connection to service host.
					 * - SocketException: thrown during socket creation or setting options, we don't have a connection
					 * - SocketTimeoutException: Timeout trying to send/receive data to the service (might not be up).
					 * - WalledGardenException: have a connection but user was taken to a different host than requested
					 * - UnknownHostException: The ip address of the host could not be determined.
					 * - ClientProtocolException: malformed request and a bug.
					 * - NotFoundException: Reached service but requested something that isn't there.
					 * - UnauthorizedException: Reached service but user doesn't have needed permissions.
					 * - ForbiddenException: Reached service but request invalid per service policy.
					 * - GatewayTimeoutException: ?
					 * 
					 * Still left
					 * - NoHttpResponseException: target server failed to respond with a valid HTTP response
					 * - UnknownHostException: hostname didn't exist in the dns system
					 */
					if (exception instanceof ConnectTimeoutException)
						return new ErrorResponse(ResponseType.TOAST, StringManager.getString(R.string.error_service_unavailable)).setTrack(true);
					else if (exception instanceof SocketTimeoutException)
						return new ErrorResponse(ResponseType.TOAST, StringManager.getString(R.string.error_connection_poor)).setTrack(true);
					if (exception instanceof ConnectException)
						return new ErrorResponse(ResponseType.TOAST, StringManager.getString(R.string.error_service_unavailable));
					else if (exception instanceof SocketException) {
						Integer messageResId = R.string.error_service_unavailable;
						if (exception.getMessage().toLowerCase(Locale.US).contains("econnreset")) {
							messageResId = R.string.error_connection_reset;
						}
						return new ErrorResponse(ResponseType.DIALOG, StringManager.getString(messageResId));
					}
					else if (exception instanceof UnknownHostException)
						return new ErrorResponse(ResponseType.DIALOG, StringManager.getString(R.string.error_client_unknown_host));
					else if (exception instanceof FileNotFoundException)
						return new ErrorResponse(ResponseType.DIALOG, StringManager.getString(R.string.error_service_file_not_found));
					else if (exception instanceof ClientProtocolException)
						return new ErrorResponse(ResponseType.TOAST, StringManager.getString(R.string.error_client_request_error));
					else if (exception instanceof EOFException)
						return new ErrorResponse(ResponseType.TOAST, StringManager.getString(R.string.error_client_request_stream_error));
					else {
						/* If we have a wifi network connection, check for a walled garden */
						if (NetworkManager.getInstance().isConnected()) {
							if (!NetworkManager.getInstance().isMobileNetwork()) {
								/* This can trigger another call to getErrorResponse and recurse until a stack overflow. */
								if (NetworkManager.getInstance().isWalledGardenConnection())
									return new ErrorResponse(ResponseType.DIALOG, StringManager.getString(R.string.error_connection_walled_garden));
							}
						}

						/* Show unfriendly message if user is developer and dev stuff is enabled */
						if (Patchr.settings.getBoolean(StringManager.getString(R.string.pref_enable_dev), false)
								&& Type.isTrue(Patchr.getInstance().getCurrentUser().developer))
							return new ErrorResponse(ResponseType.TOAST, StringManager.getString(R.string.error_service_unknown_exception) + ": "
									+ exception.getClass().getSimpleName());

						return new ErrorResponse(ResponseType.TOAST, StringManager.getString(R.string.error_service_unknown));
					}
				}
				return new ErrorResponse(ResponseType.TOAST, exception.getMessage());
			}
		}
	}

	public static Boolean isNetworkError(ServiceResponse serviceResponse) {
		return (serviceResponse.statusCode == null && serviceResponse.exception != null && serviceResponse.exception instanceof IOException);
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	public static enum ResponseType {
		TOAST,
		DIALOG,
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