// $codepro.audit.disable fileComment
package com.aircandi;

import android.os.Build;
import android.util.Log;

@SuppressWarnings("ucd")
public class Constants {

	public static final int LOG_LEVEL   = Patchr.debug ? Log.VERBOSE : Log.DEBUG;
	public static final int ERROR_LEVEL = Log.VERBOSE;

	/* Activity parameters */
	public static final String EXTRA_ENTITY_PARENT_ID = "com.aircandi.EXTRA_PARENT_ENTITY_ID";
	public static final String EXTRA_ENTITY_CHILD_ID  = "com.aircandi.EXTRA_CHILD_ENTITY_ID";
	public static final String EXTRA_ENTITY_FOR_ID    = "com.aircandi.EXTRA_ENTITY_FOR_ID";
	public static final String EXTRA_ENTITY_ID        = "com.aircandi.EXTRA_ENTITY_ID";
	public static final String EXTRA_ENTITY_SCHEMA    = "com.aircandi.EXTRA_ENTITY_SCHEMA";
	public static final String EXTRA_ENTITY_TYPE      = "com.aircandi.EXTRA_ENTITY_TYPE";
	public static final String EXTRA_ENTITIES         = "com.aircandi.EXTRA_ENTITIES";
	public static final String EXTRA_ENTITY           = "com.aircandi.EXTRA_ENTITY";
	public static final String EXTRA_ENTITY_PARENT    = "com.aircandi.EXTRA_ENTITY_PARENT";

	public static final String EXTRA_NOTIFICATION_ID = "com.aircandi.EXTRA_NOTIFICATION_ID";
	public static final String EXTRA_PATCH           = "com.aircandi.EXTRA_PATCH";
	public static final String EXTRA_PATCH_ID        = "com.aircandi.EXTRA_PATCH_ID";

	public static final String EXTRA_URI = "com.aircandi.EXTRA_URI";

	public static final String EXTRA_MESSAGE_TYPE          = "com.aircandi.EXTRA_MESSAGE_TYPE";
	public static final String EXTRA_MESSAGE_ROOT_ID       = "com.aircandi.EXTRA_MESSAGE_ROOT_ID";
	public static final String EXTRA_MESSAGE_REPLY_TO_ID   = "com.aircandi.EXTRA_MESSAGE_REPLY_TO_ID";
	public static final String EXTRA_MESSAGE_REPLY_TO_NAME = "com.aircandi.EXTRA_MESSAGE_REPLY_TO_NAME";

	public static final String EXTRA_LAYOUT_RESID                = "com.aircandi.EXTRA_LAYOUT_RESID";
	public static final String EXTRA_MESSAGE                     = "com.aircandi.EXTRA_MESSAGE";
	public static final String EXTRA_CATEGORY                    = "com.aircandi.EXTRA_CATEGORY";
	public static final String EXTRA_LOCATION                    = "com.aircandi.EXTRA_LOCATION";
	public static final String EXTRA_PRIVACY                     = "com.aircandi.EXTRA_PRIVACY";
	public static final String EXTRA_SEARCH_PHRASE               = "com.aircandi.EXTRA_SEARCH_PHRASE";
	public static final String EXTRA_SEARCH_SCOPE                = "com.aircandi.EXTRA_SEARCH_SCOPE";
	public static final String EXTRA_SEARCH_RETURN_ENTITY        = "com.aircandi.EXTRA_SEARCH_RETURN_ENTITY";
	public static final String EXTRA_SEARCH_CLEAR_BUTTON         = "com.aircandi.EXTRA_SEARCH_CLEAR_BUTTON";
	public static final String EXTRA_SEARCH_CLEAR_BUTTON_MESSAGE = "com.aircandi.EXTRA_SEARCH_CLEAR_BUTTON_MESSAGE";
	public static final String EXTRA_PHOTO_SOURCE                = "com.aircandi.EXTRA_PHOTO_SOURCE";
	public static final String EXTRA_PHOTO                       = "com.aircandi.EXTRA_PHOTO";
	public static final String EXTRA_REFRESH_FROM_SERVICE        = "com.aircandi.EXTRA_REFRESH_FORCE";
	public static final String EXTRA_TITLE                       = "com.aircandi.EXTRA_TITLE";
	public static final String EXTRA_FRAGMENT_TYPE               = "com.aircandi.EXTRA_FRAGMENT_TYPE";
	public static final String EXTRA_TO_MODE                     = "com.aircandi.EXTRA_TO_MODE";
	public static final String EXTRA_TO_EDITABLE                 = "com.aircandi.EXTRA_TO_EDITABLE";
	public static final String EXTRA_SHARE_SOURCE                = "com.aircandi.EXTRA_SHARE_SOURCE";
	public static final String EXTRA_SHARE_ID                    = "com.aircandi.EXTRA_SHARE_ID";
	public static final String EXTRA_SHARE_SCHEMA                = "com.aircandi.EXTRA_SHARE_SCHEMA";
	public static final String EXTRA_SHARE_PATCH                 = "com.aircandi.EXTRA_SHARE_PATCH";
	public static final String EXTRA_PRE_APPROVED                = "com.aircandi.EXTRA_PRE_APPROVED";
	public static final String EXTRA_TRANSITION_TYPE             = "com.aircandi.EXTRA_TRANSITION_TYPE";

	/* Activity parameters: lists */
	public static final String EXTRA_LIST_LINK_TYPE         = "com.aircandi.EXTRA_LIST_LINK_TYPE";
	public static final String EXTRA_LIST_LINK_SCHEMA       = "com.aircandi.EXTRA_LIST_SCHEMA";
	public static final String EXTRA_LIST_LINK_DIRECTION    = "com.aircandi.EXTRA_LIST_DIRECTION";
	public static final String EXTRA_LIST_NEW_ENABLED       = "com.aircandi.EXTRA_LIST_NEW_ENABLED";
	public static final String EXTRA_LIST_TITLE             = "com.aircandi.EXTRA_LIST_TITLE";
	public static final String EXTRA_LIST_TITLE_RESID       = "com.aircandi.EXTRA_LIST_TITLE_RESID";
	public static final String EXTRA_LIST_EMPTY_RESID       = "com.aircandi.EXTRA_LIST_EMPTY_RESID";
	public static final String EXTRA_LIST_PAGE_SIZE         = "com.aircandi.EXTRA_LIST_PAGE_SIZE";
	public static final String EXTRA_LIST_VIEW_TYPE         = "com.aircandi.EXTRA_LIST_VIEW_TYPE";
	public static final String EXTRA_LIST_ITEM_RESID        = "com.aircandi.EXTRA_LIST_ITEM_RESID";
	public static final String EXTRA_LIST_LOADING_RESID     = "com.aircandi.EXTRA_LIST_LOADING_RESID";
	public static final String EXTRA_LIST_NEW_MESSAGE_RESID = "com.aircandi.EXTRA_LIST_NEW_MESSAGE_RESID";

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

	/* Wifi scanning */
	public static final int INTERVAL_REFRESH      = TIME_TEN_MINUTES;
	public static final int INTERVAL_TETHER_ALERT = TIME_SIXTY_MINUTES * 12;
	public static final int DISTANCE_REFRESH      = DIST_TWO_HUNDRED_METERS;

	/* Ui */
	public static final int   MAX_Y_OVERSCROLL_DISTANCE = 0;
	public static final float DIALOGS_DIM_AMOUNT        = 0.5f;
	public static final int   INTERVAL_BUSY_MINIMUM     = 1000;
	public static final int   INTERVAL_BUSY_DELAY       = 0;

	public static final int RADAR_BEACON_SIGNAL_BUCKET_SIZE = 1;
	/*
	 * Using quality = 70 for jpeg compression reduces image file size by 85% with
	 * an acceptable degradation of image quality. A 1280x960 image went from
	 * 1007K to 152K.
	 */
	public static final int IMAGE_QUALITY_S3                = 70;
	/*
	 * Consistent with 5 megapixel sampled by two.
	 */
	public static final int IMAGE_DIMENSION_MAX             = 1280;
	public static final int IMAGE_DIMENSION_REDUCED         = 640;
	public static final int BING_IMAGE_BYTES_MAX            = 500000;
	public static final int BING_IMAGE_DIMENSION_MAX        = 1280;

	public static final String SCHEMA_ANY                 = "any";
	public static final String SCHEMA_ENTITY_BEACON       = "beacon";
	public static final String SCHEMA_ENTITY_MESSAGE      = "message";
	public static final String SCHEMA_ENTITY_NOTIFICATION = "notification";
	public static final String SCHEMA_ENTITY_PATCH        = "patch";
	public static final String SCHEMA_ENTITY_PLACE        = "place";
	public static final String SCHEMA_ENTITY_PICTURE      = "post";  // Used for sharing a photo
	public static final String SCHEMA_ENTITY_USER         = "user";
	public static final String SCHEMA_LINK                = "link";
	public static final String SCHEMA_INTENT              = "intent";

	public static final String ACTION_VIEW = "view";

	public static final String TYPE_ANY             = "any";
	public static final String TYPE_APP_FACEBOOK    = "facebook";
	public static final String TYPE_APP_TWITTER     = "twitter";
	public static final String TYPE_APP_WEBSITE     = "website";
	public static final String TYPE_APP_EMAIL       = "email";
	public static final String TYPE_APP_YELP        = "yelp";
	public static final String TYPE_APP_FOURSQUARE  = "foursquare";
	public static final String TYPE_APP_OPENTABLE   = "opentable";
	public static final String TYPE_APP_URBANSPOON  = "urbanspoon";
	public static final String TYPE_APP_CITYSEARCH  = "citysearch";
	public static final String TYPE_APP_CITYGRID    = "citygrid";
	public static final String TYPE_APP_YAHOOLOCAL  = "yahoolocal";
	public static final String TYPE_APP_OPENMENU    = "openmenu";
	public static final String TYPE_APP_ZAGAT       = "zagat";
	public static final String TYPE_APP_TRIPADVISOR = "tripadvisor";
	public static final String TYPE_APP_GOOGLEPLACE = "googleplace";
	public static final String TYPE_APP_GOOGLEPLUS  = "googleplus";
	public static final String TYPE_APP_INSTAGRAM   = "instagram";
	public static final String TYPE_APP_MAP         = "map";

	/* Entity types */

	public static final String TYPE_LINK_PROXIMITY = "proximity";
	public static final String TYPE_LINK_WATCH     = "watch";
	public static final String TYPE_LINK_LIKE      = "like";
	public static final String TYPE_LINK_CONTENT   = "content";
	public static final String TYPE_LINK_CREATE    = "create";
	public static final String TYPE_LINK_SHARE     = "share";

	public static final String TYPE_COUNT_LINK_PROXIMITY       = "link_proximity";
	public static final String TYPE_COUNT_LINK_PROXIMITY_MINUS = "link_proximity_minus";

	public static final String TYPE_BEACON_FIXED     = "fixed";
	public static final String TYPE_BEACON_MOBILE    = "mobile";
	public static final String TYPE_BEACON_TEMPORARY = "temporary";

	public static final String TYPE_PROVIDER_FOURSQUARE = "foursquare";
	public static final String TYPE_PROVIDER_GOOGLE     = "google";
	public static final String TYPE_PROVIDER_FACTUAL    = "factual";
	public static final String TYPE_PROVIDER_AIRCANDI   = "aircandi";
	public static final String TYPE_PROVIDER_YELP       = "yelp";
	public static final String TYPE_PROVIDER_USER       = "user";

	public static final String PHOTO_ACTION_DEFAULT           = "default";
	public static final String PHOTO_ACTION_SEARCH            = "search";
	public static final String PHOTO_ACTION_GALLERY           = "gallery";
	public static final String PHOTO_ACTION_CAMERA            = "camera";
	public static final String PHOTO_ACTION_EDIT              = "edit";
	public static final String PHOTO_ACTION_WEBSITE_THUMBNAIL = "website_thumbnail";

	public static final String PRIVACY_PUBLIC  = "public";
	public static final String PRIVACY_PRIVATE = "private";
	public static final String PRIVACY_SECRET  = "secret";

	public static final String LOCATION_PROVIDER_GOOGLE = "fused";
	public static final String LOCATION_PROVIDER_USER   = "user";
	public static final String LOCATION_PROVIDER_PLACE  = "place";

	/* Package names */
	public static final String PACKAGE_NAME_FACEBOOK    = "com.facebook.katana";
	public static final String PACKAGE_NAME_TWITTER     = "com.twitter.android";
	public static final String PACKAGE_NAME_FOURSQUARE  = "com.joelapenna.foursquared";
	public static final String PACKAGE_NAME_TRIPADVISOR = "com.tripadvisor.tripadvisor";
	public static final String PACKAGE_NAME_YELP        = "com.yelp.android";

	public static final int ACTIVITY_NONE                = 0;
	public static final int ACTIVITY_MARKET              = 200;
	public static final int ACTIVITY_PHOTO_PICK_DEVICE   = 300;
	public static final int ACTIVITY_PHOTO_SEARCH        = 305;
	public static final int ACTIVITY_PHOTO_MAKE          = 310;
	public static final int ACTIVITY_SEARCH              = 325;
	public static final int ACTIVITY_SIGNIN              = 400;
	public static final int ACTIVITY_RESET_AND_SIGNIN    = 410;
	public static final int ACTIVITY_COMMENT             = 430;
	public static final int ACTIVITY_APPLINKS_EDIT       = 535;
	public static final int ACTIVITY_APPLINK_EDIT        = 540;
	public static final int ACTIVITY_APPLINK_NEW         = 545;
	public static final int ACTIVITY_APPLICATION_PICK    = 560;
	public static final int ACTIVITY_PREFERENCES         = 600;
	public static final int ACTIVITY_ADDRESS_EDIT        = 800;
	public static final int ACTIVITY_CATEGORY_EDIT       = 810;
	public static final int ACTIVITY_LOCATION_EDIT       = 820;
	public static final int ACTIVITY_PRIVACY_EDIT        = 830;
	public static final int ACTIVITY_PHOTO_EDIT          = 840;
	public static final int ACTIVITY_ENTITY_EDIT         = 900;
	public static final int ACTIVITY_PICTURE_SOURCE_PICK = 950;
	public static final int ACTIVITY_ENTITY_INSERT       = 960;

	public static final String FRAGMENT_TYPE_NEARBY        = "nearby";
	public static final String FRAGMENT_TYPE_WATCH         = "watch";
	public static final String FRAGMENT_TYPE_CREATE        = "create";
	public static final String FRAGMENT_TYPE_TREND_POPULAR = "trend_popular";
	public static final String FRAGMENT_TYPE_TREND_ACTIVE  = "trend_active";
	public static final String FRAGMENT_TYPE_SETTINGS      = "settings";
	public static final String FRAGMENT_TYPE_FEEDBACK      = "feedback";
	public static final String FRAGMENT_TYPE_PROFILE       = "profile";
	public static final String FRAGMENT_TYPE_HISTORY       = "history";
	public static final String FRAGMENT_TYPE_MAP           = "map";
	public static final String FRAGMENT_TYPE_SENT          = "sent";
	public static final String FRAGMENT_TYPE_MESSAGES      = "messages";
	public static final String FRAGMENT_TYPE_PATCHES       = "patches";

	public static final int RESULT_ENTITY_INSERTED        = 100;
	public static final int RESULT_ENTITY_UPDATED         = 110;
	public static final int RESULT_ENTITY_UPDATED_REFRESH = 115;
	public static final int RESULT_ENTITY_DELETED         = 120;
	public static final int RESULT_ENTITY_REMOVED         = 125;
	public static final int RESULT_ENTITY_EDITED          = 130;
	public static final int RESULT_COMMENT_INSERTED       = 200;
	public static final int RESULT_PROFILE_UPDATED        = 310;
	public static final int RESULT_USER_SIGNED_IN         = 400;

	public static final boolean SUPPORTS_ICE_CREAM_SANDWICH = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;
	public static final boolean SUPPORTS_JELLY_BEAN         = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN;
	public static final boolean SUPPORTS_JELLY_BEAN_MR1     = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
	public static final boolean SUPPORTS_KIT_KAT            = android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
	public static final boolean SUPPORTS_LOLLIPOP           = android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;

	/* Install id */
	public static final String INSTALL_TYPE_RANDOM     = "random_uuid";
	public static final String INSTALL_TYPE_ANDROID_ID = "android_id";
	public static final String INSTALL_TYPE_SERIAL     = "serial_num";
}
