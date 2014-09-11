// $codepro.audit.disable fileComment
package com.aircandi;

import android.graphics.Bitmap.Config;
import android.os.Build;
import android.util.Log;

@SuppressWarnings("ucd")
public class Constants {

	public static final int     LOG_LEVEL           = Aircandi.DEBUG ? Log.VERBOSE : Log.DEBUG;
	public static final boolean ERROR_LEVEL_VERBOSE = false;

	/* Activity parameters */
	public static final String EXTRA_ENTITY_PARENT_ID = "com.aircandi.EXTRA_PARENT_ENTITY_ID";                                                    //$NON-NLS-1$
	public static final String EXTRA_ENTITY_CHILD_ID  = "com.aircandi.EXTRA_CHILD_ENTITY_ID";                                                    //$NON-NLS-1$
	public static final String EXTRA_ENTITY_FOR_ID    = "com.aircandi.EXTRA_ENTITY_FOR_ID";                                                    //$NON-NLS-1$
	public static final String EXTRA_ENTITY_ID        = "com.aircandi.EXTRA_ENTITY_ID";                                                            //$NON-NLS-1$
	public static final String EXTRA_ENTITY_SCHEMA    = "com.aircandi.EXTRA_ENTITY_SCHEMA";                                                        //$NON-NLS-1$
	public static final String EXTRA_ENTITY_TYPE      = "com.aircandi.EXTRA_ENTITY_TYPE";                                                        //$NON-NLS-1$
	public static final String EXTRA_ENTITIES         = "com.aircandi.EXTRA_ENTITIES";                                                            //$NON-NLS-1$
	public static final String EXTRA_ENTITY           = "com.aircandi.EXTRA_ENTITY";                                                                //$NON-NLS-1$

	public static final String EXTRA_PLACE    = "com.aircandi.EXTRA_PLACE";                                                                //$NON-NLS-1$
	public static final String EXTRA_PLACE_ID = "com.aircandi.EXTRA_PLACE_ID";                                                            //$NON-NLS-1$

	public static final String EXTRA_URI = "com.aircandi.EXTRA_URI";                                                                //$NON-NLS-1$

	public static final String EXTRA_LAYOUT_RESID              = "com.aircandi.EXTRA_LAYOUT_RESID";                                                        //$NON-NLS-1$
	public static final String EXTRA_MESSAGE                   = "com.aircandi.EXTRA_MESSAGE";                                                             //$NON-NLS-1$
	public static final String EXTRA_CATEGORY                  = "com.aircandi.EXTRA_CATEGORY";                                                            //$NON-NLS-1$
	public static final String EXTRA_VERIFY_URI                = "com.aircandi.EXTRA_VERIFY_URI";                                                          //$NON-NLS-1$
	public static final String EXTRA_SEARCH_PHRASE             = "com.aircandi.EXTRA_SEARCH_PHRASE";                                                       //$NON-NLS-1$
	public static final String EXTRA_PHOTO_SOURCE              = "com.aircandi.EXTRA_PHOTO_SOURCE";                                                        //$NON-NLS-1$
	public static final String EXTRA_UPSIZE_SYNTHETIC          = "com.aircandi.EXTRA_UPSIZE_SYNTHETIC";                                                    //$NON-NLS-1$
	public static final String EXTRA_PAGING_ENABLED            = "com.aircandi.EXTRA_PAGING_ENABLED";                                                      //$NON-NLS-1$
	public static final String EXTRA_PHOTO                     = "com.aircandi.EXTRA_PHOTO";                                                               //$NON-NLS-1$
	public static final String EXTRA_REFRESH_FROM_SERVICE      = "com.aircandi.EXTRA_REFRESH_FORCE";                                                       //$NON-NLS-1$
	public static final String EXTRA_HELP_ID                   = "com.aircandi.EXTRA_HELP_ID";                                                             //$NON-NLS-1$
	public static final String EXTRA_SHORTCUTS                 = "com.aircandi.EXTRA_SHORTCUTS";                                                           //$NON-NLS-1$
	public static final String EXTRA_MARKERS                   = "com.aircandi.EXTRA_MARKERS";                                                             //$NON-NLS-1$
	public static final String EXTRA_SKIP_SAVE                 = "com.aircandi.EXTRA_EDIT_ONLY";                                                           //$NON-NLS-1$
	public static final String EXTRA_TAB_POSITION              = "com.aircandi.EXTRA_TAB_POSITION";                                                        //$NON-NLS-1$
	public static final String EXTRA_SHORTCUT_TYPE             = "com.aircandi.EXTRA_SHORTCUT_TYPE";                                                       //$NON-NLS-1$
	public static final String EXTRA_TITLE                     = "com.aircandi.EXTRA_TITLE";                                                               //$NON-NLS-1$
	public static final String EXTRA_FRAGMENT_TYPE             = "com.aircandi.EXTRA_FRAGMENT_TYPE";                                                       //$NON-NLS-1$
	public static final String EXTRA_NOTIFICATIONS_CLEAR_COUNT = "com.aircandi.EXTRA_NOTIFICATIONS_CLEAR_COUNT";                                           //$NON-NLS-1$
	public static final String EXTRA_TO_MODE                   = "com.aircandi.EXTRA_TO_MODE";                                                     //$NON-NLS-1$
	public static final String EXTRA_TO_EDITABLE               = "com.aircandi.EXTRA_TO_EDITABLE";                                                       //$NON-NLS-1$
	public static final String EXTRA_SUGGEST_SCOPE             = "com.aircandi.EXTRA_SUGGEST_SCOPE";                                                       //$NON-NLS-1$
	public static final String EXTRA_SHARE_SOURCE              = "com.aircandi.EXTRA_SHARE_SOURCE";                                                       //$NON-NLS-1$
	public static final String EXTRA_SHARE_ID                  = "com.aircandi.EXTRA_SHARE_ID";                                                       //$NON-NLS-1$
	public static final String EXTRA_SHARE_SCHEMA              = "com.aircandi.EXTRA_SHARE_SCHEMA";                                                       //$NON-NLS-1$
	public static final String EXTRA_AUTO_WATCH                = "com.aircandi.EXTRA_AUTO_WATCH";                                                    //$NON-NLS-1$

	/* Activity parameters: lists */
	public static final String EXTRA_LIST_LINK_TYPE            = "com.aircandi.EXTRA_LIST_LINK_TYPE";                                                        //$NON-NLS-1$
	public static final String EXTRA_LIST_LINK_SCHEMA          = "com.aircandi.EXTRA_LIST_SCHEMA";                                                        //$NON-NLS-1$
	public static final String EXTRA_LIST_LINK_DIRECTION       = "com.aircandi.EXTRA_LIST_DIRECTION";                                                        //$NON-NLS-1$
	public static final String EXTRA_LIST_NEW_ENABLED          = "com.aircandi.EXTRA_LIST_NEW_ENABLED";                                                    //$NON-NLS-1$
	public static final String EXTRA_LIST_TITLE                = "com.aircandi.EXTRA_LIST_TITLE";                                                            //$NON-NLS-1$
	public static final String EXTRA_LIST_PAGE_SIZE            = "com.aircandi.EXTRA_LIST_PAGE_SIZE";                                                        //$NON-NLS-1$
	public static final String EXTRA_LIST_VIEW_TYPE            = "com.aircandi.EXTRA_LIST_VIEW_TYPE";                                                        //$NON-NLS-1$
	public static final String EXTRA_LIST_ITEM_RESID           = "com.aircandi.EXTRA_LIST_ITEM_RESID";                                                    //$NON-NLS-1$
	public static final String EXTRA_LIST_LOADING_RESID        = "com.aircandi.EXTRA_LIST_LOADING_RESID";                                                    //$NON-NLS-1$
	public static final String EXTRA_LIST_NEW_MESSAGE_RESID    = "com.aircandi.EXTRA_LIST_NEW_MESSAGE_RESID";                                                //$NON-NLS-1$
	public static final String EXTRA_LIST_PAGING_ENABLED       = "com.aircandi.EXTRA_LIST_PAGING_ENABLED";                                                //$NON-NLS-1$
	public static final String EXTRA_LIST_ENTITY_CACHE_ENABLED = "com.aircandi.EXTRA_LIST_ENTITY_CACHE_ENABLED";                                            //$NON-NLS-1$
	public static final String EXTRA_LIST_PARALLAX_HEADER      = "com.aircandi.EXTRA_LIST_PARALLAX_HEADER";                                                //$NON-NLS-1$

	/* Interval helpers */
	public static final int MILLS_PER_SECOND     = 1000;
	public static final int TIME_ONE_SECOND      = MILLS_PER_SECOND;
	public static final int TIME_FIVE_SECONDS    = MILLS_PER_SECOND * 5;
	public static final int TIME_TEN_SECONDS     = MILLS_PER_SECOND * 10;
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

	/* Wifi scanning */
	public static final int INTERVAL_SCAN_WIFI           = TIME_ONE_MINUTE;
	public static final int INTERVAL_CATEGORIES_DOWNLOAD = TIME_ONE_SECOND;
	public static final int INTERVAL_UPDATE_CHECK        = TIME_SIXTY_MINUTES;
	public static final int INTERVAL_REFRESH             = TIME_TEN_MINUTES;
	public static final int INTERVAL_TETHER_ALERT        = TIME_SIXTY_MINUTES * 12;
	public static final int DISTANCE_REFRESH             = DIST_TWO_HUNDRED_METERS;

	/* Ui */
	public static final int   MAX_Y_OVERSCROLL_DISTANCE      = 0;
	public static final float DIALOGS_DIM_AMOUNT             = 0.5f;
	public static final float POPUP_DIM_AMOUNT               = 0.0f;
	public static final int   INTERVAL_BUSY_MINIMUM          = 1000;
	public static final int   INTERVAL_BUSY_DELAY            = 0;
	public static final int   INTERVAL_PROGRESS_CANCEL_DELAY = 5000;

	public static final int TABS_PRIMARY_ID     = 1;
	public static final int TABS_USER_FORM_ID   = 2;
	public static final int TABS_USER_EDIT_ID   = 3;
	public static final int TABS_ENTITY_FORM_ID = 4;

	public static final long TOOLTIPS_PATCH_LIST_ID   = 100;
	public static final long TOOLTIPS_PLACE_EDIT_ID   = 101;
	public static final long TOOLTIPS_PLACE_BROWSE_ID = 102;

	public static final int    RADAR_BEACON_SIGNAL_BUCKET_SIZE = 1;
	/*
	 * Using quality = 70 for jpeg compression reduces image file size by 85% with
	 * an acceptable degradation of image quality. A 1280x960 image went from
	 * 1007K to 152K.
	 */
	public static final int    IMAGE_QUALITY_S3                = 70;                                                                                        // $codepro.audit.disable constantNamingConvention
	/*
	 * Will handle a typical 5 megapixel 2560x1920 image that has been sampled by two to 1280x960
	 * Sampling by 4 produces 640x480. Assumes four channel ARGB including alpha.
	 */
	public static final int    IMAGE_MEMORY_BYTES_MAX          = 4915200;                                                                                    // 4 megapixels
	/*
	 * We can choke processing super large images before we ever get a chance to downsample plus they
	 * take forever and eat the users data allowance.
	 */
	public static final int    IMAGE_DOWNLOAD_BYTES_MAX        = 4915200;                                                                                    // 4 megapixels
	public static final int    IMAGE_DOWNLOAD_BYTES_MIN        = 128;                                                                                        // 4 megapixels
	public static final Config IMAGE_CONFIG_DEFAULT            = Config.ARGB_8888;
	/*
	 * Consistent with 5 megapixel sampled by two.
	 */
	public static final int    IMAGE_DIMENSION_MAX             = 1280;
	public static final int    IMAGE_DIMENSION_REDUCED         = 640;
	public static final int    BING_IMAGE_BYTES_MAX            = 500000;
	public static final int    BING_IMAGE_DIMENSION_MAX        = 1280;

	public static final String SCHEMA_ANY            = "any";                                                                                    //$NON-NLS-1$
	public static final String SCHEMA_ENTITY_PICTURE = "post";                                                                                    //$NON-NLS-1$
	public static final String SCHEMA_ENTITY_PLACE   = "place";                                                                                    //$NON-NLS-1$
	public static final String SCHEMA_ENTITY_COMMENT = "comment";                                                                                //$NON-NLS-1$
	public static final String SCHEMA_ENTITY_APPLINK = "applink";                                                                                //$NON-NLS-1$
	public static final String SCHEMA_ENTITY_BEACON  = "beacon";                                                                                //$NON-NLS-1$
	public static final String SCHEMA_ENTITY_USER    = "user";                                                                                    //$NON-NLS-1$
	public static final String SCHEMA_LINK           = "link";                                                                                    //$NON-NLS-1$
	public static final String SCHEMA_INTENT         = "intent";                                                                                //$NON-NLS-1$
	public static final String SCHEMA_REMAP_PICTURE  = "picture";                                                                                //$NON-NLS-1$

	public static final String ACTION_VIEW      = "view";
	public static final String ACTION_VIEW_FOR  = "view_for";
	public static final String ACTION_VIEW_AUTO = "view_auto";
	public static final String ACTION_INSERT    = "insert";

	public static final String TYPE_ANY             = "any";                                                                                    //$NON-NLS-1$
	public static final String TYPE_APP_FACEBOOK    = "facebook";                                                                                //$NON-NLS-1$
	public static final String TYPE_APP_TWITTER     = "twitter";                                                                                //$NON-NLS-1$
	public static final String TYPE_APP_WEBSITE     = "website";                                                                                //$NON-NLS-1$
	public static final String TYPE_APP_EMAIL       = "email";                                                                                    //$NON-NLS-1$
	public static final String TYPE_APP_YELP        = "yelp";                                                                                    //$NON-NLS-1$
	public static final String TYPE_APP_FOURSQUARE  = "foursquare";                                                                            //$NON-NLS-1$
	public static final String TYPE_APP_OPENTABLE   = "opentable";                                                                                //$NON-NLS-1$
	public static final String TYPE_APP_URBANSPOON  = "urbanspoon";                                                                            //$NON-NLS-1$
	public static final String TYPE_APP_CITYSEARCH  = "citysearch";                                                                            //$NON-NLS-1$
	public static final String TYPE_APP_CITYGRID    = "citygrid";                                                                                //$NON-NLS-1$
	public static final String TYPE_APP_YAHOOLOCAL  = "yahoolocal";                                                                            //$NON-NLS-1$
	public static final String TYPE_APP_OPENMENU    = "openmenu";                                                                                //$NON-NLS-1$
	public static final String TYPE_APP_ZAGAT       = "zagat";                                                                                    //$NON-NLS-1$
	public static final String TYPE_APP_TRIPADVISOR = "tripadvisor";                                                                            //$NON-NLS-1$
	public static final String TYPE_APP_GOOGLEPLACE = "googleplace";                                                                            //$NON-NLS-1$
	public static final String TYPE_APP_GOOGLEPLUS  = "googleplus";                                                                            //$NON-NLS-1$
	public static final String TYPE_APP_INSTAGRAM   = "instagram";                                                                                //$NON-NLS-1$

	/* Local app */
	public static final String TYPE_APP_MAP    = "map";                                                                                    //$NON-NLS-1$
	public static final String TYPE_APP_INTENT = "intent";                                                                                //$NON-NLS-1$

	/* Verb types */
	public static final String TYPE_APP_WATCH = "watch";                                                                                    //$NON-NLS-1$

	/* Entity types */
	public static final String TYPE_APP_COMMENT = "comment";                                                                                //$NON-NLS-1$
	public static final String TYPE_APP_PICTURE = "picture";                                                                                //$NON-NLS-1$
	public static final String TYPE_APP_POST    = "post";                                                                                    //$NON-NLS-1$
	public static final String TYPE_APP_PLACE   = "place";                                                                                    //$NON-NLS-1$
	public static final String TYPE_APP_USER    = "user";                                                                                    //$NON-NLS-1$

	public static final String TYPE_LINK_PROXIMITY = "proximity";                                                                                //$NON-NLS-1$
	public static final String TYPE_LINK_WATCH     = "watch";                                                                                    //$NON-NLS-1$
	public static final String TYPE_LINK_CONTENT   = "content";                                                                                //$NON-NLS-1$
	public static final String TYPE_LINK_CREATE    = "create";                                                                                //$NON-NLS-1$
	public static final String TYPE_LINK_SHARE     = "share";                                                                                //$NON-NLS-1$

	public static final String TYPE_COUNT_LINK_PROXIMITY       = "link_proximity";                                                                        //$NON-NLS-1$
	public static final String TYPE_COUNT_LINK_PROXIMITY_MINUS = "link_proximity_minus";                                                                    //$NON-NLS-1$

	public static final String TYPE_BEACON_FIXED     = "fixed";                                                                                    //$NON-NLS-1$
	public static final String TYPE_BEACON_MOBILE    = "mobile";                                                                                //$NON-NLS-1$
	public static final String TYPE_BEACON_TEMPORARY = "temporary";                                                                                //$NON-NLS-1$

	public static final String TYPE_PROVIDER_FOURSQUARE = "foursquare";                                                                            //$NON-NLS-1$
	public static final String TYPE_PROVIDER_GOOGLE     = "google";                                                                                //$NON-NLS-1$
	public static final String TYPE_PROVIDER_FACTUAL    = "factual";                                                                                //$NON-NLS-1$
	public static final String TYPE_PROVIDER_AIRCANDI   = "aircandi";                                                                                //$NON-NLS-1$
	public static final String TYPE_PROVIDER_YELP       = "yelp";                                                                                    //$NON-NLS-1$
	public static final String TYPE_PROVIDER_USER       = "user";                                                                                    //$NON-NLS-1$

	public static final String PHOTO_SOURCE_DEFAULT           = "default";                                                                                //$NON-NLS-1$
	public static final String PHOTO_SOURCE_SEARCH            = "search";                                                                                //$NON-NLS-1$
	public static final String PHOTO_SOURCE_GALLERY           = "gallery";                                                                                //$NON-NLS-1$
	public static final String PHOTO_SOURCE_CAMERA            = "camera";                                                                                //$NON-NLS-1$
	public static final String PHOTO_SOURCE_PLACE             = "place";                                                                                    //$NON-NLS-1$
	public static final String PHOTO_SOURCE_FACEBOOK          = "facebook";                                                                                //$NON-NLS-1$
	public static final String PHOTO_SOURCE_TWITTER           = "twitter";                                                                                //$NON-NLS-1$
	public static final String PHOTO_SOURCE_WEBSITE_THUMBNAIL = "website_thumbnail";                                                                        //$NON-NLS-1$

	public static final String VISIBILITY_PUBLIC  = "public";                                                                                //$NON-NLS-1$
	public static final String VISIBILITY_PRIVATE = "private";                                                                                //$NON-NLS-1$

	/* Package names */
	public static final String PACKAGE_NAME_FACEBOOK    = "com.facebook.katana";                                                                    //$NON-NLS-1$
	public static final String PACKAGE_NAME_TWITTER     = "com.twitter.android";                                                                    //$NON-NLS-1$
	public static final String PACKAGE_NAME_FOURSQUARE  = "com.joelapenna.foursquared";                                                            //$NON-NLS-1$
	public static final String PACKAGE_NAME_TRIPADVISOR = "com.tripadvisor.tripadvisor";                                                            //$NON-NLS-1$
	public static final String PACKAGE_NAME_YELP        = "com.yelp.android";                                                                        //$NON-NLS-1$

	public static final int ACTIVITY_NONE                = 0;
	public static final int ACTIVITY_MARKET              = 200;
	public static final int ACTIVITY_PHOTO_PICK_DEVICE   = 300;
	public static final int ACTIVITY_PHOTO_SEARCH        = 305;
	public static final int ACTIVITY_PHOTO_MAKE          = 310;
	public static final int ACTIVITY_PHOTO_PICK_PLACE    = 315;
	public static final int ACTIVITY_PLACE_SEARCH        = 320;
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
	public static final int ACTIVITY_ENTITY_EDIT         = 900;
	public static final int ACTIVITY_PICTURE_SOURCE_PICK = 950;
	public static final int ACTIVITY_ENTITY_INSERT       = 960;

	public static final String FRAGMENT_TYPE_FEED          = "feed";
	public static final String FRAGMENT_TYPE_NEARBY        = "nearby";
	public static final String FRAGMENT_TYPE_WATCH         = "watch";
	public static final String FRAGMENT_TYPE_CREATE        = "create";
	public static final String FRAGMENT_TYPE_TREND_POPULAR = "trend_popular";
	public static final String FRAGMENT_TYPE_TREND_ACTIVE  = "trend_active";
	public static final String FRAGMENT_TYPE_PROFILE       = "profile";
	public static final String FRAGMENT_TYPE_HISTORY       = "history";

	public static final String NAVIGATION_CATEGORY_PLACES   = "places";
	public static final String NAVIGATION_CATEGORY_TRENDS   = "trends";
	public static final String NAVIGATION_CATEGORY_MESSAGES = "messages";
	public static final String NAVIGATION_CATEGORY_NONE     = "none";

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

	/* Install id */
	public static final String INSTALL_TYPE_RANDOM     = "random_uuid";
	public static final String INSTALL_TYPE_ANDROID_ID = "android_id";
	public static final String INSTALL_TYPE_SERIAL     = "serial_num";

	/*
	 * Update criteria for active and passive location updates.
	 *
	 * We use aggresive criteria for passive updates because they are free
	 * and we aren't doing any processing in response to them.
	 */
	public static final long MAXIMUM_AGE           = Constants.TIME_THIRTY_MINUTES;
	public static final long MAXIMUM_AGE_PREFERRED = Constants.TIME_TWO_MINUTES;
	public static final long BUSY_TIMEOUT          = Constants.TIME_THIRTY_SECONDS;

	public static final int     MIN_DISTANCE_UPDATES                  = DIST_FIFTY_METERS;
	public static final Integer MINIMUM_ACCURACY                      = DIST_ONE_KILOMETER;
	public static final Integer MINIMUM_ACCURACY_FOR_DISTANCE_DISPLAY = DIST_FIVE_HUNDRED_METERS;
	public static final Integer DESIRED_ACCURACY_GPS                  = DIST_THIRTY_METERS;
	public static final Integer DESIRED_ACCURACY_NETWORK              = DIST_THIRTY_METERS;
	public static final Integer DESIRED_ACCURACY                      = DIST_THIRTY_METERS;

	/* Used to filter for notification that active location update provider has been disabled */
	public static final String ACTIVE_LOCATION_UPDATE_PROVIDER_DISABLED = "com.aircandi.location.active_location_update_provider_disabled";                        //$NON-NLS-1$
}
