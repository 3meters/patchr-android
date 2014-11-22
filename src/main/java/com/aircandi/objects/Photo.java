package com.aircandi.objects;

import android.content.Context;
import android.net.Uri;
import android.util.TypedValue;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.ServiceConstants;
import com.aircandi.components.StringManager;
import com.aircandi.objects.ImageResult.Thumbnail;
import com.aircandi.service.Expose;
import com.aircandi.utilities.Type;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class Photo extends ServiceObject implements Cloneable, Serializable {
	/*
	 * sourceName: aircandi, foursquare, external
	 */
	private static final long            serialVersionUID = 4979315562693226461L;
	private static final GooglePlusProxy imageResizer     = new GooglePlusProxy();

	@Expose
	public String prefix;
	@Expose
	public String suffix;
	@Expose
	public Number width;
	@Expose
	public Number height;
	@Expose
	public String source;
	@Expose
	public Number createdDate;

	@Expose(serialize = false, deserialize = true)
	public Boolean colorize = false;
	@Expose(serialize = false, deserialize = true)
	public String  colorizeKey;
	@Expose(serialize = false, deserialize = true)
	public Integer color;

	/* Only comes from foursquare */
	@Expose(serialize = false, deserialize = true)
	public Entity user;

	/* client only */
	public String name;
	public String description;
	public Boolean usingDefault = false;
	public Shortcut shortcut;
	public Boolean store = false;

	public Boolean resizerActive = false;
	public Boolean resizerUsed   = false;
	public Number resizerWidth;
	public Number resizerHeight;

	public Photo() {
	}

	public Photo(String prefix, String suffix, Number width, Number height, String source) {
		this.prefix = prefix;
		this.suffix = suffix;
		this.width = width;
		this.height = height;
		this.source = source;
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public static Photo setPropertiesFromMap(Photo photo, Map map, Boolean nameMapping) {

		photo.prefix = (String) map.get("prefix");
		photo.suffix = (String) map.get("suffix");
		photo.width = (Number) map.get("width");
		photo.height = (Number) map.get("height");
		photo.source = (String) map.get("source");
		photo.createdDate = (Number) map.get("createdDate");
		photo.name = (String) map.get("name");
		photo.description = (String) map.get("description");
		photo.color = (Integer) map.get("color");
		photo.colorize = (Boolean) map.get("colorize");
		photo.colorizeKey = (String) map.get("colorizeKey");
		photo.usingDefault = (Boolean) map.get("usingDefault");
		photo.store = (Boolean) map.get("store");
		photo.resizerActive = (Boolean) map.get("resizerActive");
		photo.resizerWidth = (Number) map.get("resizerWidth");
		photo.resizerHeight = (Number) map.get("resizerHeight");

		if (map.get("user") != null) {
			photo.user = User.setPropertiesFromMap(new User(), (HashMap<String, Object>) map.get("user"), nameMapping);
		}

		return photo;
	}

	public Integer getResId() {
		if (!this.source.equals(PhotoSource.resource)) {
			throw new IllegalArgumentException("Photo source must be resource");
		}
		Integer resId = getResourceIdFromResourceName(Patchr.applicationContext, this.prefix);
		return resId;
	}

	public String getUri() {
		return getSizedUri(width, height, false);
	}

	public String getUriWrapped() {
		/* Only called from UI.loadView */
		return getSizedUri(width, height, true);
	}

	private String getSizedUri(Number maxWidth, Number maxHeight, Boolean wrapped) {

		if (source == null) return prefix;

		if (source.equals(PhotoSource.resource)) {
			return ("resource:" + prefix);
		}
		else if (source.equals(PhotoSource.assets_categories)) {
			return (ServiceConstants.URL_PROXIBASE_SERVICE_ASSETS_CATEGORIES + prefix);
		}
		else if (source.equals(PhotoSource.assets_applinks)) {
			return (ServiceConstants.URL_PROXIBASE_SERVICE_ASSETS_APPLINK_ICONS + prefix);
		}
		else if (source.equals(PhotoSource.bing)) {
			return prefix;
		}
		else if (source.equals(PhotoSource.facebook)) {
			return prefix;
		}
		else if (source.equals(PhotoSource.foursquare)) {
			if (wrapped && Type.isTrue(resizerActive) && resizerWidth != null) {
				return (prefix + String.valueOf(resizerWidth.intValue()) + "x" + String.valueOf(resizerHeight.intValue()) + suffix);
			}
			else {
				    /* Sometimes we have height/width info and sometimes we don't */
				Integer width = (maxWidth != null) ? maxWidth.intValue() : 256;
				Integer height = (maxHeight != null) ? maxHeight.intValue() : 256;
				if (prefix != null && suffix != null) {
					return (prefix + String.valueOf(width.intValue()) + "x" + String.valueOf(height.intValue()) + suffix);
				}
			}
			return prefix;
		}
		else if (source.equals(PhotoSource.foursquare_icon)) {
			if (prefix != null && suffix != null) {
				return (prefix + "88" + suffix);
			}
			return prefix;
		}
		else if (source.equals(PhotoSource.google)) {
			int width = (maxWidth != null) ? maxWidth.intValue() : Constants.IMAGE_DIMENSION_MAX;
			if (wrapped && Type.isTrue(resizerActive) && resizerWidth != null) {
				width = resizerWidth.intValue();
			}

			if (prefix.contains("?")) {
				return (prefix + "&maxwidth=" + String.valueOf(width));
			}
			else {
				return (prefix + "?maxwidth=" + String.valueOf(width));
			}
		}
		else {

			StringBuilder builder = new StringBuilder();
			if (source.equals(PhotoSource.aircandi)
					|| source.equals(PhotoSource.aircandi_images)) {
				builder.append(StringManager.getString(R.string.url_media_images) + prefix);
			}
			else if (source.equals(PhotoSource.aircandi_thumbnails)) {
				builder.append(StringManager.getString(R.string.url_media_thumbnails) + prefix);
			}
			else if (source.equals(PhotoSource.aircandi_users)) {
				builder.append(StringManager.getString(R.string.url_media_users) + prefix);
			}
			else if (source.equals(PhotoSource.generic)) {
				builder.append(prefix);
			}
			else if (source.equals(PhotoSource.yelp)) {
				builder.append(prefix);
			}
			else {
				builder.append(prefix);
			}

			if (wrapped && Type.isTrue(resizerActive) && resizerWidth != null) {
				/*
				 * If photo comes with native height/width then use it otherwise
				 * resize based on width.
				 */
				resizerUsed = true;
				if (this.width != null && this.height != null) {

					Float photoAspectRatio = this.width.floatValue() / this.height.floatValue();
					Float targetAspectRatio = this.resizerWidth.floatValue() / this.resizerHeight.floatValue();

					ResizeDimension dimension = (targetAspectRatio >= photoAspectRatio) ? ResizeDimension.WIDTH : ResizeDimension.HEIGHT;

					return (imageResizer.convert(builder.toString()
							, dimension == ResizeDimension.WIDTH ? resizerWidth.intValue() : resizerHeight.intValue()
							, dimension));
				}
				else {
					return (imageResizer.convert(builder.toString(), resizerWidth.intValue(), ResizeDimension.WIDTH));
				}
			}
			return builder.toString();
		}
	}

	public static String getPhotoSourceByPhotoType(PhotoType photoType) {
		if (photoType == PhotoType.GENERAL) {
			return PhotoSource.aircandi_images;
		}
		else if (photoType == PhotoType.USER) {
			return PhotoSource.aircandi_users;
		}
		if (photoType == PhotoType.THUMBNAIL) {
			return PhotoSource.aircandi_thumbnails;
		}
		return PhotoSource.generic;
	}

	public static Boolean isDrawable(String uri) {
		return uri.toLowerCase(Locale.US).startsWith("resource:");
	}

	public static Boolean hasAlpha(Photo photo) {
		/*
		 * We assume alpha unless we see a jpg|jpeg.
		 */
		String uri = photo.getUri();
		return (!(uri.toLowerCase(Locale.US).contains(".jpg") || uri.toLowerCase(Locale.US).contains(".jpeg")));
	}

	public ImageResult getAsImageResult() {

		final ImageResult imageResult = new ImageResult();
		if (width != null) {
			imageResult.setWidth(width.longValue());
		}
		if (height != null) {
			imageResult.setHeight(height.longValue());
		}
		imageResult.setMediaUrl(getUri());
		final Thumbnail thumbnail = new Thumbnail();
		thumbnail.setUrl(getSizedUri(100, 100, true));
		imageResult.setThumbnail(thumbnail);
		return imageResult;
	}

	public static Integer getResourceIdFromResourceName(Context context, String resourceName) {

		final String resolvedResourceName = resolveResourceName(context, resourceName);
		if (resolvedResourceName != null) {
			final int resourceId = Patchr.applicationContext.getResources().getIdentifier(resolvedResourceName
					, "drawable"
					, Patchr.getInstance().getPackageName());
			return resourceId;
		}
		return null;
	}

	public static Integer getResourceIdFromUri(Context context, String uri) {

		final String rawResourceName = uri.substring(uri.indexOf("resource:") + 9);
		final String resolvedResourceName = resolveResourceName(context, rawResourceName);
		if (resolvedResourceName != null) {
			final int resourceId = Patchr.applicationContext.getResources().getIdentifier(resolvedResourceName
					, "drawable"
					, Patchr.getInstance().getPackageName());
			return resourceId;
		}
		return null;
	}

	public static String resolveResourceName(Context context, String rawResourceName) {
		int resourceId = Patchr.applicationContext.getResources().getIdentifier(rawResourceName, "drawable", Patchr.getInstance().getPackageName());
		if (resourceId == 0) {
			resourceId = Patchr.applicationContext.getResources().getIdentifier(rawResourceName, "attr", Patchr.getInstance().getPackageName());
			final TypedValue value = new TypedValue();
			if (context.getTheme().resolveAttribute(resourceId, value, true)) {
				final String redirectedResourceName = (String) value.coerceToString();
				return redirectedResourceName;
			}
			else
				return null;
		}
		else
			return rawResourceName;
	}

	public boolean sameAs(Object obj) {

		if (obj == null) return false;
		if (!((Object) this).getClass().equals(obj.getClass())) return false;
		if (this.getUri() == null) return false;

		final Photo other = (Photo) obj;

		if (other.getUri() == null) return false;
		return (Type.equal(this.getUri(), other.getUri()));
	}

	public static boolean same(Object obj1, Object obj2) {
		if (obj1 == null && obj2 == null) return true;
		if (obj1 != null) {
			return ((Photo) obj1).sameAs(obj2);
		}
		else {
			return false;
		}
	}

	@Override
	public Photo clone() {
		try {
			Photo photo = (Photo) super.clone();
			if (user != null) {
				photo.user = (User) user.clone();
			}
			return photo;
		}
		catch (CloneNotSupportedException e) {
			throw new RuntimeException("Photo not clonable");
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	public Number getCreatedAt() {
		return createdDate;
	}

	public Photo setCreatedAt(Number createdAt) {
		this.createdDate = createdAt;
		return this;
	}

	public Entity getUser() {
		return user;
	}

	public Photo setUser(Entity user) {
		this.user = user;
		return this;
	}

	public String getName() {
		return name;
	}

	public Photo setName(String name) {
		this.name = name;
		return this;
	}

	public Photo setPrefix(String prefix) {
		this.prefix = prefix;
		return this;
	}

	public Photo setPrefix(Uri uri) {
		this.prefix = uri.toString();
		return this;
	}

	public Photo setSource(String source) {
		this.source = source;
		return this;
	}

	public Photo setStore(Boolean store) {
		this.store = store;
		return this;
	}

	public Photo setProxy(Boolean proxyActive) {
		setProxy(proxyActive, null, null);
		return this;
	}

	public Photo setProxy(Boolean proxyActive, Integer height, Integer width) {
		this.resizerActive = proxyActive;
		this.resizerUsed = false;
		if (proxyActive) {
			this.resizerWidth = width;
			this.resizerHeight = height;
		}
		else {
			this.resizerWidth = null;
			this.resizerHeight = null;
		}
		return this;
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/
	/*
	 * Any photo from the device (camera, gallery) is store in s3 and source = aircandi.
	 * Any search photo is not stored in s3 and source = generic.
	 * Any search photo used for user profile ist stored in s3.
	 * Any patch photo from foursquare stays there and photo.source = foursquare.
	 */
	public static class PhotoSource {

		public static String generic = "generic";                           // set in photo picker when using third party photo.
		public static String bing    = "bing";                              // set when thumbnail is coming straight from bing.

		public static String aircandi            = "aircandi";              // legacy that maps to aircandi_images
		public static String aircandi_images     = "aircandi.images";       // set when photo is stored by us and used to construct full uri to image data (s3)
		public static String aircandi_thumbnails = "aircandi.thumbnails";   // set when photo is stored by us and used to construct full uri to image data (s3)
		public static String aircandi_users      = "aircandi.users";        // set when photo is stored by us and used to construct full uri to image data (s3)

		public static String foursquare      = "foursquare";                     // set if photo comes from facebook - used for place photos
		public static String foursquare_icon = "foursquare.icon";           // set if icon comes from facebook - used for place categories
		public static String facebook        = "facebook";                       // set if photo comes from facebook - used for applinks
		public static String twitter         = "twitter";                        // set if photo comes from twitter - used for applinks
		public static String google          = "google";                         // set if photo comes from google - used for applinks
		public static String yelp            = "yelp";                           // set if photo comes from yelp - used for applinks // NO_UCD (unused code)

		/* System sources */

		public static String resource          = "resource";                // set when using default
		public static String file              = "file";                    // set when using a photo from device (camera|gallery)
		public static String assets_applinks   = "assets.applinks";         // used when targeting something like the default applink icons
		public static String assets_categories = "assets.categories";       // ditto to above
	}

	public static enum PhotoType {
		GENERAL,
		THUMBNAIL,
		USER,
	}

	public static enum Orientation {
		LANDSCAPE,
		PORTRAIT
	}

	public static enum ResizeDimension {
		HEIGHT,
		WIDTH
	}

	private static class GooglePlusProxy implements Serializable {
		private static final long   serialVersionUID = 4979315502693226461L;
		/*
		 * Setting refresh to 60 minutes.
		 */
		public static        String baseWidth        = "https://images1-focus-opensocial.googleusercontent.com/gadgets/proxy?url=%s&container=focus&resize_w=%d&no_expand=1&refresh=3600";
		public static        String baseHeight       = "https://images1-focus-opensocial.googleusercontent.com/gadgets/proxy?url=%s&container=focus&resize_h=%d&no_expand=1&refresh=3600";

		public String convert(String uri, Integer size, ResizeDimension dimension) {
			String base = (dimension == ResizeDimension.WIDTH) ? baseWidth : baseHeight;
			String converted = String.format(base, Uri.encode(uri), size);
			return converted;
		}
	}
}