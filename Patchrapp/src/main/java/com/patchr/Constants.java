// $codepro.audit.disable fileComment
package com.patchr;

import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.patchr.utilities.UI;

import java.util.concurrent.Executor;

@SuppressWarnings("ucd")
public class Constants {

	public static final int      LOG_LEVEL = BuildConfig.DEBUG ? Log.VERBOSE : Log.DEBUG;
	public static final Executor EXECUTOR  = AsyncTask.SERIAL_EXECUTOR;

	/* Activity parameters */
	public static final String EXTRA_ENTITY_PARENT_ID   = "com.patchr.EXTRA_PARENT_ENTITY_ID";
	public static final String EXTRA_ENTITY_ID          = "com.patchr.EXTRA_ENTITY_ID";
	public static final String EXTRA_ENTITY_SCHEMA      = "com.patchr.EXTRA_ENTITY_SCHEMA";
	public static final String EXTRA_ENTITY_PARENT_NAME = "com.patchr.EXTRA_PATCH_NAME";
	public static final String EXTRA_NOTIFICATION_ID    = "com.patchr.EXTRA_NOTIFICATION_ID";
	public static final String EXTRA_MESSAGE_TYPE       = "com.patchr.EXTRA_MESSAGE_TYPE";

	public static final String EXTRA_QUERY_NAME            = "com.patchr.EXTRA_QUERY_NAME";
	public static final String EXTRA_LOCATION              = "com.patchr.EXTRA_LOCATION";
	public static final String EXTRA_PRIVACY               = "com.patchr.EXTRA_PRIVACY";
	public static final String EXTRA_SEARCH_SCOPE          = "com.patchr.EXTRA_SEARCH_SCOPE";
	public static final String EXTRA_PHOTO                 = "com.patchr.EXTRA_PHOTO";
	public static final String EXTRA_REFRESH_FROM_SERVICE  = "com.patchr.EXTRA_REFRESH_FORCE";
	public static final String EXTRA_FRAGMENT_TYPE         = "com.patchr.EXTRA_FRAGMENT_TYPE";
	public static final String EXTRA_SHARE_SOURCE          = "com.patchr.EXTRA_SHARE_SOURCE";
	public static final String EXTRA_SHARE_ENTITY_ID       = "com.patchr.EXTRA_SHARE_ID";
	public static final String EXTRA_SHARE_SCHEMA          = "com.patchr.EXTRA_SHARE_SCHEMA";
	public static final String EXTRA_SHOW_REFERRER_WELCOME = "com.patchr.EXTRA_SHOW_REFERRER_WELCOME";
	public static final String EXTRA_REFERRER_NAME         = "com.patchr.EXTRA_REFERRER_NAME";
	public static final String EXTRA_REFERRER_PHOTO_URL    = "com.patchr.EXTRA_REFERRER_PHOTO_URL";
	public static final String EXTRA_EMAIL                 = "com.patchr.EXTRA_EMAIL";
	public static final String EXTRA_PASSWORD              = "com.patchr.EXTRA_PASSWORD";
	public static final String EXTRA_STATE                 = "com.patchr.EXTRA_STATE";
	public static final String EXTRA_RESET_TOKEN           = "com.patchr.EXTRA_RESET_TOKEN";
	public static final String EXTRA_RESET_USER_NAME       = "com.patchr.EXTRA_RESET_USER_NAME";
	public static final String EXTRA_RESET_USER_PHOTO      = "com.patchr.EXTRA_RESET_USER_PHOTO";

	/* Interval helpers */
	public static final int MILLS_PER_SECOND     = 1000;
	public static final int TIME_ONE_SECOND      = MILLS_PER_SECOND;
	public static final int TIME_FIVE_SECONDS    = MILLS_PER_SECOND * 5;
	public static final int TIME_TEN_SECONDS     = MILLS_PER_SECOND * 10;
	public static final int TIME_FIFTEEN_SECONDS = MILLS_PER_SECOND * 15;
	public static final int TIME_TWENTY_SECONDS  = MILLS_PER_SECOND * 20;
	public static final int TIME_THIRTY_SECONDS  = MILLS_PER_SECOND * 30;
	public static final int TIME_ONE_MINUTE      = MILLS_PER_SECOND * 60;
	public static final int TIME_TWO_MINUTES     = MILLS_PER_SECOND * 60 << 1;
	public static final int TIME_THREE_MINUTES   = MILLS_PER_SECOND * 60 * 3;
	public static final int TIME_FIVE_MINUTES    = MILLS_PER_SECOND * 60 * 5;
	public static final int TIME_TEN_MINUTES     = MILLS_PER_SECOND * 60 * 10;
	public static final int TIME_FIFTEEN_MINUTES = MILLS_PER_SECOND * 60 * 15;
	public static final int TIME_THIRTY_MINUTES  = MILLS_PER_SECOND * 60 * 30;
	public static final int TIME_SIXTY_MINUTES   = MILLS_PER_SECOND * 60 * 60;

	/* Distance helpers */
	public static final int DIST_ONE_METER           = 1;
	public static final int DIST_FIVE_METERS         = 5;
	public static final int DIST_TEN_METERS          = 10;
	public static final int DIST_TWENTY_FIVE_METERS  = 25;
	public static final int DIST_THIRTY_METERS       = 30;
	public static final int DIST_FIFTY_METERS        = 50;
	public static final int DIST_SEVENTY_FIVE_METERS = 75;
	public static final int DIST_ONE_HUNDRED_METERS  = 100;
	public static final int DIST_TWO_HUNDRED_METERS  = 200;
	public static final int DIST_FIVE_HUNDRED_METERS = 500;
	public static final int DIST_ONE_KILOMETER       = 1000;
	public static final int DIST_TWO_KILOMETERS      = 2000;
	public static final int DIST_FIVE_KILOMETERS     = 5000;

	public static final int SIZE_KILOBYTES = 1024;
	public static final int SIZE_MEGABYTES = SIZE_KILOBYTES * SIZE_KILOBYTES;

	public static final int    ZOOM_SCALE_BUILDINGS = 17;
	public static final int    ZOOM_SCALE_NEARBY    = 16;
	public static final int    ZOOM_SCALE_CITY      = 11;
	public static final int    ZOOM_SCALE_COUNTY    = 10;
	public static final int    ZOOM_SCALE_STATE     = 6;
	public static final int    ZOOM_SCALE_COUNTRY   = 5;
	public static final int    ZOOM_SCALE_USA       = 3;
	public static final int    ZOOM_SCALE_WORLD     = 1;
	public static final int    ZOOM_DEFAULT         = ZOOM_SCALE_NEARBY;
	public static final LatLng LATLNG_USA           = new LatLng(39.8282, -98.5795);

	/* Ui */
	public static final float DIALOGS_DIM_AMOUNT    = 0.5f;
	public static final int   INTERVAL_BUSY_MINIMUM = 1000;
	public static final int   INTERVAL_BUSY_DELAY   = 0;
	public static final int   PIXEL_SCALE           = UI.getRawPixelsForDisplayPixels(1f);
	public static final int   PAGE_SIZE             = 20;

	/*
	 * Using quality = 70 for jpeg compression reduces image file size by 85% with
	 * an acceptable degradation of image quality. A 1280x960 image went from
	 * 1007K to 152K.
	 */
	public static final int IMAGE_QUALITY_S3         = 70;
	public static final int MAX_WIDTH_LIST           = 480;
	public static final int MAX_WIDTH_FORM           = 480;
	/*
	 * Consistent with 5 megapixel sampled by two.
	 */
	public static final int IMAGE_DIMENSION_MAX      = 1280;
	public static final int IMAGE_DIMENSION_REDUCED  = 640;
	public static final int BING_IMAGE_BYTES_MAX     = 500000;
	public static final int BING_IMAGE_DIMENSION_MAX = 1280;

	public static final String SCHEMA_ENTITY_MESSAGE      = "message";
	public static final String SCHEMA_ENTITY_NOTIFICATION = "notification";
	public static final String SCHEMA_ENTITY_PATCH        = "patch";
	public static final String SCHEMA_ENTITY_PICTURE      = "post";  // Used for sharing a photo
	public static final String SCHEMA_ENTITY_USER         = "user";

	public static final String COLLECTION_ENTITY_MESSAGE       = "messages";
	public static final String COLLECTION_ENTITY_NOTIFICATIONS = "notifications";
	public static final String COLLECTION_ENTITY_PATCHES       = "patches";
	public static final String COLLECTION_ENTITY_USERS         = "users";

	public static final String TYPE_APP_TWITTER     = "twitter";
	public static final String TYPE_APP_YELP        = "yelp";
	public static final String TYPE_APP_FOURSQUARE  = "foursquare";
	public static final String TYPE_APP_TRIPADVISOR = "tripadvisor";

	/* Entity types */

	public static final String TYPE_ENTITY_SHARE = "share";
	public static final String TYPE_ENTITY_TRIP  = "trip";
	public static final String TYPE_ENTITY_GROUP = "group";
	public static final String TYPE_ENTITY_PLACE = "place";
	public static final String TYPE_ENTITY_EVENT = "event";

	public static final String TYPE_LINK_MEMBER  = "watch";
	public static final String TYPE_LINK_LIKE    = "like";
	public static final String TYPE_LINK_CONTENT = "content";
	public static final String TYPE_LINK_CREATE  = "create";
	public static final String TYPE_LINK_SHARE   = "share";

	public static final String PHOTO_ACTION_SEARCH  = "search";
	public static final String PHOTO_ACTION_GALLERY = "gallery";
	public static final String PHOTO_ACTION_CAMERA  = "camera";

	public static final String PRIVACY_PUBLIC  = "public";
	public static final String PRIVACY_PRIVATE = "private";
	public static final String PRIVACY_SECRET  = "secret";

	public static final String LOCATION_PROVIDER_GOOGLE = "fused";
	public static final String LOCATION_PROVIDER_USER   = "user";

	/* Package names */
	public static final String PACKAGE_NAME_TWITTER     = "com.twitter.android";
	public static final String PACKAGE_NAME_FOURSQUARE  = "com.joelapenna.foursquared";
	public static final String PACKAGE_NAME_TRIPADVISOR = "com.tripadvisor.tripadvisor";
	public static final String PACKAGE_NAME_YELP        = "com.yelp.android";

	public static final int ACTIVITY_MARKET            = 200;
	public static final int ACTIVITY_PHOTO_PICK_DEVICE = 300;
	public static final int ACTIVITY_PHOTO_SEARCH      = 305;
	public static final int ACTIVITY_SEARCH            = 325;
	public static final int ACTIVITY_LOGIN             = 400;
	public static final int ACTIVITY_LOGIN_ACCOUNT_KIT = 402;
	public static final int ACTIVITY_SIGNUP            = 405;
	public static final int ACTIVITY_RESET_AND_SIGNIN  = 410;
	public static final int ACTIVITY_PREFERENCES       = 600;
	public static final int ACTIVITY_LOCATION_EDIT     = 820;
	public static final int ACTIVITY_PRIVACY_EDIT      = 830;
	public static final int ACTIVITY_PHOTO_EDIT        = 840;
	public static final int ACTIVITY_ENTITY_EDIT       = 900;
	public static final int ACTIVITY_PHOTO_PICK        = 950;
	public static final int ACTIVITY_ENTITY_INSERT     = 960;
	public static final int ACTIVITY_SHARE             = 970;

	public static final String FRAGMENT_TYPE_NEARBY       = "nearby";
	public static final String FRAGMENT_TYPE_MEMBER_OF    = "watch";
	public static final String FRAGMENT_TYPE_OWNER_OF     = "create";
	public static final String FRAGMENT_TYPE_LIKE         = "like";
	public static final String FRAGMENT_TYPE_TREND_ACTIVE = "trend_active";
	public static final String FRAGMENT_TYPE_SETTINGS     = "settings";
	public static final String FRAGMENT_TYPE_MAP          = "map";

	public static final int RESULT_ENTITY_DELETED = 120;
	public static final int RESULT_ENTITY_REMOVED = 125;
	public static final int RESULT_USER_LOGGED_IN = 400;

	public static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION  = 100;
	public static final int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 200;

	/*--------------------------------------------------------------------------------------------
	 * Service constants
	 *--------------------------------------------------------------------------------------------*/

	public static final int     TIMEOUT_CONNECTION                               = 10000;
	public static final int     TIMEOUT_SOCKET_READ                              = 10000;
	public static final int     TIMEOUT_SOCKET_WRITE                             = 10000;
	public static final int     TIMEOUT_SERVICE_SUGGEST                          = 2000;
	/*
	 * Used when trying to verify that a network connection is available. The retries
	 * are used to allow for the case where the connecting process is underway.
	 */
	public static final int     DEFAULT_MAX_CONNECTIONS                          = 50;
	public static final Integer PLACE_SUGGEST_RADIUS                             = 80000; // ~50 miles
	/*
	 * Nearby = 20 minutes walking = 1 mile = 1609 meters.
	 */
	public static final int     PATCH_NEAR_RADIUS                                = 10000;
	public static final float   SERVICE_STATUS_CODE_BAD_REQUEST                  = 400.0f;
	public static final float   SERVICE_STATUS_CODE_MISSING_PARAM                = 400.1f;
	public static final float   SERVICE_STATUS_CODE_BAD_PARAM                    = 400.11f;
	public static final float   SERVICE_STATUS_CODE_BAD_TYPE                     = 400.12f;
	public static final float   SERVICE_STATUS_CODE_BAD_VALUE                    = 400.13f;
	public static final float   SERVICE_STATUS_CODE_BAD_JSON                     = 400.14f;
	public static final float   SERVICE_STATUS_CODE_BAD_USER_AUTH_PARAMS         = 400.21f;
	public static final float   SERVICE_STATUS_CODE_BAD_VERSION                  = 400.4f;
	public static final float   SERVICE_STATUS_CODE_BAD_APPLINK                  = 400.5f;
	public static final float   SERVICE_STATUS_CODE_UNAUTHORIZED                 = 401.0f;
	public static final float   SERVICE_STATUS_CODE_UNAUTHORIZED_CREDENTIALS     = 401.1f;
	public static final float   SERVICE_STATUS_CODE_UNAUTHORIZED_SESSION_EXPIRED = 401.2f;
	public static final float   SERVICE_STATUS_CODE_UNAUTHORIZED_NOT_HUMAN       = 401.3f;
	public static final float   SERVICE_STATUS_CODE_UNAUTHORIZED_EMAIL_NOT_FOUND = 401.4f;
	public static final float   SERVICE_STATUS_CODE_FORBIDDEN                    = 403.0f;
	public static final float   SERVICE_STATUS_CODE_FORBIDDEN_DUPLICATE          = 403.1f;
	public static final float   SERVICE_STATUS_CODE_FORBIDDEN_DUPLICATE_LIKELY   = 403.11f;
	public static final float   SERVICE_STATUS_CODE_FORBIDDEN_USER_PASSWORD_WEAK = 403.21f;
	public static final float   SERVICE_STATUS_CODE_FORBIDDEN_VIA_API_ONLY       = 403.22f;
	public static final float   SERVICE_STATUS_CODE_FORBIDDEN_LIMIT_EXCEEDED     = 403.3f;

	public static Integer SUGGEST_LIMIT = 10;
}
