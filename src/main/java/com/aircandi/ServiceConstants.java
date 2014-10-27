// $codepro.audit.disable fileComment
package com.aircandi;

@SuppressWarnings("ucd")
public final class ServiceConstants {

	public static final int TIMEOUT_CONNECTION   = 20000;
	public static final int TIMEOUT_SOCKET_READ  = 30000;
	public static final int TIMEOUT_SOCKET_WRITE = 30000;

	public static final int TIMEOUT_APPLINK_SEARCH  = 10000;
	public static final int TIMEOUT_APPLINK_REFRESH = 10000;
	public static final int TIMEOUT_PLACE_QUERIES   = 20000;
	public static final int TIMEOUT_PLACE_SUGGEST   = 2000;

	public static final String WALLED_GARDEN_URI               = "http://clients3.google.com/generate_204";
	public static final int    WALLED_GARDEN_SOCKET_TIMEOUT_MS = 5000;

	/*
	 * Used when trying to verify that a network connection is available. The retries
	 * are used to allow for the case where the connecting process is underway.
	 */
	public static final int CONNECT_TRIES           = 15;
	public static final int CONNECT_WAIT            = 1000;
	public static final int DEFAULT_MAX_CONNECTIONS = 50;

	public static final String ADMIN_USER_ID     = "us.000000.00000.000.000000";
	public static final String ANONYMOUS_USER_ID = "us.000000.00000.000.111111";

	public static final String URL_PROXIBASE_SERVICE = "http://ariseditions.com:8080/v1";           // local
	public static final String __URL_PROXIBASE_SERVICE   = "https://api.aircandi.com/v1";               // production
	public static final String _URL_PROXIBASE_SERVICE  = "https://api.aircandi.com:444/v1";           // staging

	public static final String PATH_PROXIBASE_SERVICE_ASSETS_APPLINK_ICONS = "/assets/img/applinks/";
	public static final String PATH_PROXIBASE_SERVICE_ASSETS_CATEGORIES    = "/assets/img/categories/";

	public static final String URL_PROXIBASE_SERVICE_REST                 = URL_PROXIBASE_SERVICE + "/data/";
	public static final String URL_PROXIBASE_SERVICE_USER                 = URL_PROXIBASE_SERVICE + "/user/";
	public static final String URL_PROXIBASE_SERVICE_ADMIN                = URL_PROXIBASE_SERVICE + "/admin/";
	public static final String URL_PROXIBASE_SERVICE_METHOD               = URL_PROXIBASE_SERVICE + "/do/";
	public static final String URL_PROXIBASE_SERVICE_STATS                = URL_PROXIBASE_SERVICE + "/stats/";
	public static final String URL_PROXIBASE_SERVICE_PLACES               = URL_PROXIBASE_SERVICE + "/places/";
	public static final String URL_PROXIBASE_SERVICE_SUGGEST              = URL_PROXIBASE_SERVICE + "/suggest";
	public static final String URL_PROXIBASE_SERVICE_ACTIONS              = URL_PROXIBASE_SERVICE + "/actions/";
	public static final String URL_PROXIBASE_SERVICE_APPLINKS             = URL_PROXIBASE_SERVICE + "/applinks/";
	public static final String URL_PROXIBASE_SERVICE_AUTH                 = URL_PROXIBASE_SERVICE + "/auth/";
	public static final String URL_PROXIBASE_SERVICE_ASSETS_APPLINK_ICONS = URL_PROXIBASE_SERVICE + PATH_PROXIBASE_SERVICE_ASSETS_APPLINK_ICONS;
	public static final String URL_PROXIBASE_SERVICE_ASSETS_CATEGORIES    = URL_PROXIBASE_SERVICE + PATH_PROXIBASE_SERVICE_ASSETS_CATEGORIES;

	public static final String URL_PROXIBASE_SEARCH_IMAGES = "https://api.datamarket.azure.com/Bing/Search/v1/Image";

	public static final String  PLACE_SUGGEST_PROVIDER = "google";
	public static final Integer PLACE_SUGGEST_RADIUS   = 20000000;

	public static final String PLACE_NEAR_PROVIDERS        = "foursquare|google";
	/*
	 * Nearby = 20 minutes walking = 1 mile = 1609 meters.
	 */
	public static final int    PLACE_NEAR_RADIUS           = 10000;
	public static final int    PROXIMITY_BEACON_COVERAGE   = 5;
	public static final int    PROXIMITY_BEACON_UNCOVERAGE = 50;

	public static final float SERVICE_STATUS_CODE_BAD_REQUEST          = 400.0f;
	public static final float SERVICE_STATUS_CODE_MISSING_PARAM        = 400.1f;
	public static final float SERVICE_STATUS_CODE_BAD_PARAM            = 400.11f;
	public static final float SERVICE_STATUS_CODE_BAD_TYPE             = 400.12f;
	public static final float SERVICE_STATUS_CODE_BAD_VALUE            = 400.13f;
	public static final float SERVICE_STATUS_CODE_BAD_JSON             = 400.14f;
	public static final float SERVICE_STATUS_CODE_BAD_USER_AUTH_PARAMS = 400.21f;
	public static final float SERVICE_STATUS_CODE_BAD_VERSION          = 400.4f;
	public static final float SERVICE_STATUS_CODE_BAD_APPLINK          = 400.5f;

	public static final float SERVICE_STATUS_CODE_UNAUTHORIZED                     = 401.0f;
	public static final float SERVICE_STATUS_CODE_UNAUTHORIZED_CREDENTIALS         = 401.1f;
	public static final float SERVICE_STATUS_CODE_UNAUTHORIZED_SESSION_EXPIRED     = 401.2f;                                                                
	public static final float SERVICE_STATUS_CODE_UNAUTHORIZED_NOT_HUMAN           = 401.3f;                                                                
	public static final float SERVICE_STATUS_CODE_UNAUTHORIZED_EMAIL               = 401.4f;                                                                
	public static final float SERVICE_STATUS_CODE_UNAUTHORIZED_EMAIL_NOT_VALIDATED = 401.5f;                                                                

	public static final float SERVICE_STATUS_CODE_FORBIDDEN                    = 403.0f;                                                                
	public static final float SERVICE_STATUS_CODE_FORBIDDEN_DUPLICATE          = 403.1f;                                                                
	public static final float SERVICE_STATUS_CODE_FORBIDDEN_DUPLICATE_LIKELY   = 403.11f;                                                                
	public static final float SERVICE_STATUS_CODE_FORBIDDEN_USER_PASSWORD_WEAK = 403.21f;                                                                
	public static final float SERVICE_STATUS_CODE_FORBIDDEN_VIA_API_ONLY       = 403.22f;                                                                
	public static final float SERVICE_STATUS_CODE_FORBIDDEN_LIMIT_EXCEEDED     = 403.3f;                                                                

	private ServiceConstants() {}
}
