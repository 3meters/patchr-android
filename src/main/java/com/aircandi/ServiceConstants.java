// $codepro.audit.disable fileComment
package com.aircandi;

import com.aircandi.components.StringManager;

@SuppressWarnings("ucd")
public final class ServiceConstants {

	private static boolean DEV_ENABLED = Patchr.settings.getBoolean(StringManager.getString(R.string.pref_enable_dev), false);
	private static boolean STAGING_ENABLED = Patchr.settings.getBoolean(StringManager.getString(R.string.pref_use_staging_service), false);

	public static final int TIMEOUT_CONNECTION            = 5000;
	public static final int TIMEOUT_SOCKET_READ           = 20000;
	public static final int TIMEOUT_SOCKET_WRITE          = 20000;
	public static final int TIMEOUT_SERVICE_PLACE_SUGGEST = 2000;

	public static final String URI_WALLED_GARDEN           = "http://clients3.google.com/generate_204";
	public static final String URI_PROXIBASE_SEARCH_IMAGES = "https://api.datamarket.azure.com/Bing/Search/v1/Image";
	/*
	 * Used when trying to verify that a network connection is available. The retries
	 * are used to allow for the case where the connecting process is underway.
	 */
	public static final int    CONNECT_TRIES               = 15;
	public static final int    CONNECT_WAIT                = 1000;
	public static final int    DEFAULT_MAX_CONNECTIONS     = 50;

	public static final String ADMIN_USER_ID     = "us.000000.00000.000.000000";
	public static final String ANONYMOUS_USER_ID = "us.000000.00000.000.111111";

	private static final String URL_PROXIBASE_SERVICE         = "https://api.aircandi.com/v1";             // production
	private static final String URL_PROXIBASE_SERVICE_STAGING = "https://api.aircandi.com:8443/v1";        // staging
	private static final String _URL_PROXIBASE_SERVICE        = "http://ariseditions.com:8080/v1";         // local

	public static final String URL_PROXIBASE_SERVICE_REST                 = serviceUrl() + "/data/";
	public static final String URL_PROXIBASE_SERVICE_USER                 = serviceUrl() + "/user/";
	public static final String URL_PROXIBASE_SERVICE_ADMIN                = serviceUrl() + "/admin/";
	public static final String URL_PROXIBASE_SERVICE_METHOD               = serviceUrl() + "/do/";
	public static final String URL_PROXIBASE_SERVICE_STATS                = serviceUrl() + "/stats/";
	public static final String URL_PROXIBASE_SERVICE_PATCHES              = serviceUrl() + "/patches/";
	public static final String URL_PROXIBASE_SERVICE_SUGGEST              = serviceUrl() + "/suggest";
	public static final String URL_PROXIBASE_SERVICE_ACTIONS              = serviceUrl() + "/actions/";
	public static final String URL_PROXIBASE_SERVICE_APPLINKS             = serviceUrl() + "/applinks/";
	public static final String URL_PROXIBASE_SERVICE_AUTH                 = serviceUrl() + "/auth/";
	public static final String URL_PROXIBASE_SERVICE_ASSETS_APPLINK_ICONS = serviceUrl() + "/assets/img/applinks/";
	public static final String URL_PROXIBASE_SERVICE_ASSETS_CATEGORIES    = serviceUrl() + "/assets/img/categories/";

	public static final String  PLACE_SUGGEST_PROVIDER      = "google";
	public static final Integer PLACE_SUGGEST_RADIUS        = 80000; // ~50 miles
	/*
	 * Nearby = 20 minutes walking = 1 mile = 1609 meters.
	 */
	public static final int     PATCH_NEAR_RADIUS           = 10000;
	public static final int     PROXIMITY_BEACON_COVERAGE   = 5;
	public static final int     PROXIMITY_BEACON_UNCOVERAGE = 50;

	public static final float SERVICE_STATUS_CODE_BAD_REQUEST          = 400.0f;
	public static final float SERVICE_STATUS_CODE_MISSING_PARAM        = 400.1f;
	public static final float SERVICE_STATUS_CODE_BAD_PARAM            = 400.11f;
	public static final float SERVICE_STATUS_CODE_BAD_TYPE             = 400.12f;
	public static final float SERVICE_STATUS_CODE_BAD_VALUE            = 400.13f;
	public static final float SERVICE_STATUS_CODE_BAD_JSON             = 400.14f;
	public static final float SERVICE_STATUS_CODE_BAD_USER_AUTH_PARAMS = 400.21f;
	public static final float SERVICE_STATUS_CODE_BAD_VERSION          = 400.4f;
	public static final float SERVICE_STATUS_CODE_BAD_APPLINK          = 400.5f;

	public static final float SERVICE_STATUS_CODE_UNAUTHORIZED                 = 401.0f;
	public static final float SERVICE_STATUS_CODE_UNAUTHORIZED_CREDENTIALS     = 401.1f;
	public static final float SERVICE_STATUS_CODE_UNAUTHORIZED_SESSION_EXPIRED = 401.2f;
	public static final float SERVICE_STATUS_CODE_UNAUTHORIZED_NOT_HUMAN       = 401.3f;
	public static final float SERVICE_STATUS_CODE_UNAUTHORIZED_EMAIL_NOT_FOUND = 401.4f;

	public static final float SERVICE_STATUS_CODE_FORBIDDEN                    = 403.0f;
	public static final float SERVICE_STATUS_CODE_FORBIDDEN_DUPLICATE          = 403.1f;
	public static final float SERVICE_STATUS_CODE_FORBIDDEN_DUPLICATE_LIKELY   = 403.11f;
	public static final float SERVICE_STATUS_CODE_FORBIDDEN_USER_PASSWORD_WEAK = 403.21f;
	public static final float SERVICE_STATUS_CODE_FORBIDDEN_VIA_API_ONLY       = 403.22f;
	public static final float SERVICE_STATUS_CODE_FORBIDDEN_LIMIT_EXCEEDED     = 403.3f;

	public static String serviceUrl() {
		return (DEV_ENABLED && STAGING_ENABLED) ? URL_PROXIBASE_SERVICE_STAGING : URL_PROXIBASE_SERVICE;
	}
}
