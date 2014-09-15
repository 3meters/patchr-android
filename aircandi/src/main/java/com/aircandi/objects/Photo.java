package com.aircandi.objects;

import android.content.Context;
import android.net.Uri;
import android.util.TypedValue;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.ServiceConstants;
import com.aircandi.components.StringManager;
import com.aircandi.objects.ImageResult.Thumbnail;
import com.aircandi.service.Expose;
import com.aircandi.ui.widgets.AirImageView.SizeType;
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
	private static final long serialVersionUID = 4979315562693226461L;

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

	public Number  sizeType    = SizeType.FULLSIZE.ordinal();
	public Boolean proxyActive = false;
	public Number proxyWidth;
	public Number proxyHeight;

	private GooglePlusProxy mImageProxy = new GooglePlusProxy();

	public Photo() {
	}

	public Photo(String prefix, String suffix, Number width, Number height, String sourceName) {
		this.prefix = prefix;
		this.suffix = suffix;
		this.width = width;
		this.height = height;
		this.source = sourceName;
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
		photo.proxyActive = (Boolean) map.get("proxyActive");
		photo.proxyWidth = (Number) map.get("proxyWidth");
		photo.proxyHeight = (Number) map.get("proxyHeight");
		photo.sizeType = (Number) map.get("sizeType");

		if (map.get("user") != null) {
			photo.user = User.setPropertiesFromMap(new User(), (HashMap<String, Object>) map.get("user"), nameMapping);
		}

		return photo;
	}

	public String getUri() {
		return getSizedUri(width, height, false);
	}

	public String getUriWrapped() {
		return getSizedUri(width, height, true);
	}

	private String getSizedUri(Number maxWidth, Number maxHeight, Boolean wrapped) {

		String photoUri = prefix;

		if (source != null) {
			if (source.equals(PhotoSource.resource)) {

				photoUri = "resource:" + prefix;
			}
			else if (source.equals(PhotoSource.assets_categories)) {

				photoUri = ServiceConstants.URL_PROXIBASE_SERVICE_ASSETS_CATEGORIES + prefix + String.valueOf(88) + suffix;
			}
			else if (source.equals(PhotoSource.assets_applinks)) {

				photoUri = ServiceConstants.URL_PROXIBASE_SERVICE_ASSETS_APPLINK_ICONS + prefix;
			}
			else if (source.equals(PhotoSource.bing)) {

				photoUri = prefix;
			}
			else if (source.equals(PhotoSource.facebook)) {

				photoUri = prefix;
			}
			else if (source.equals(PhotoSource.foursquare)) {

				photoUri = prefix;
				if (wrapped && Type.isTrue(proxyActive) && proxyWidth != null) {
					photoUri = prefix + String.valueOf(proxyWidth.intValue()) + "x" + String.valueOf(proxyHeight.intValue()) + suffix;
				}
				else {
		            /* Sometimes we have height/width info and sometimes we don't */
					Integer width = (maxWidth != null) ? maxWidth.intValue() : 256;
					Integer height = (maxHeight != null) ? maxHeight.intValue() : 256;
					if (prefix != null && suffix != null) {
						photoUri = prefix + String.valueOf(width.intValue()) + "x" + String.valueOf(height.intValue()) + suffix;
					}
				}
			}
			else if (source.equals(PhotoSource.google)) {

				Integer width = (maxWidth != null) ? maxWidth.intValue() : Constants.IMAGE_DIMENSION_MAX;
				if (wrapped && Type.isTrue(proxyActive) && proxyWidth != null) {
					width = proxyWidth.intValue();
				}
				if (prefix.contains("?")) {
					photoUri = prefix + "&maxwidth=" + String.valueOf(width);
				}
				else {
					photoUri = prefix + "?maxwidth=" + String.valueOf(width);
				}
			}
			else {

				if (source.equals(PhotoSource.aircandi)
						|| source.equals(PhotoSource.aircandi_images)) {
					photoUri = StringManager.getString(R.string.url_media_images) + prefix;
				}
				else if (source.equals(PhotoSource.aircandi_thumbnails)) {
					photoUri = StringManager.getString(R.string.url_media_thumbnails) + prefix;
				}
				else if (source.equals(PhotoSource.aircandi_users)) {
					photoUri = StringManager.getString(R.string.url_media_users) + prefix;
				}
				else if (source.equals(PhotoSource.generic)) {
					photoUri = prefix;
				}
				if (wrapped && Type.isTrue(proxyActive) && proxyWidth != null) {
					if (this.width != null && this.height != null) {
						Orientation orientation = (this.width.intValue() >= this.height.intValue())
						                          ? Orientation.LANDSCAPE
						                          : Orientation.PORTRAIT;
						photoUri = mImageProxy.convert(photoUri, proxyWidth.intValue(), orientation);
					}
					else {
						photoUri = mImageProxy.convert(photoUri, proxyWidth.intValue(), Orientation.LANDSCAPE);
					}
				}
			}
		}
		return photoUri;
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

	public static Integer getResourceIdFromUri(Context context, String uri) {

		final String rawResourceName = uri.substring(uri.indexOf("resource:") + 9);
		final String resolvedResourceName = resolveResourceName(context, rawResourceName);
		if (resolvedResourceName != null) {
			final int resourceId = Aircandi.applicationContext.getResources().getIdentifier(resolvedResourceName
					, "drawable"
					, Aircandi.getInstance().getPackageName());
			return resourceId;
		}
		return null;
	}

	public static String resolveResourceName(Context context, String rawResourceName) {
		int resourceId = Aircandi.applicationContext.getResources().getIdentifier(rawResourceName, "drawable", Aircandi.getInstance().getPackageName());
		if (resourceId == 0) {
			resourceId = Aircandi.applicationContext.getResources().getIdentifier(rawResourceName, "attr", Aircandi.getInstance().getPackageName());
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
			final Photo photo = (Photo) super.clone();

			if (user != null) {
				photo.user = user.clone();
			}

			return photo;
		}
		catch (final CloneNotSupportedException ex) {
			throw new AssertionError();
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

	public String getPrefix() {
		return prefix;
	}

	public Photo setPrefix(String prefix) {
		this.prefix = prefix;
		return this;
	}

	public Photo setPrefix(Uri uri) {
		this.prefix = uri.toString();
		return this;
	}

	public Number getWidth() {
		return width;
	}

	public Photo setWidth(Number width) {
		this.width = width;
		return this;
	}

	public Number getHeight() {
		return height;
	}

	public Photo setHeight(Number height) {
		this.height = height;
		return this;
	}

	public String getSource() {
		return source;
	}

	public Photo setSource(String source) {
		this.source = source;
		return this;
	}

	public Boolean getStore() {
		return store;
	}

	public Photo setStore(Boolean store) {
		this.store = store;
		return this;
	}

	public Boolean getProxyActive() {
		return proxyActive;
	}

	public SizeType getSizeType() {
		return SizeType.values()[sizeType.intValue()];
	}

	public Photo setProxy(Boolean proxyActive) {
		setProxy(proxyActive, null);
		return this;
	}

	public Photo setProxy(Boolean proxyActive, SizeType sizeType) {
		this.proxyActive = proxyActive;
		if (proxyActive) {
			this.sizeType = sizeType.ordinal();
			if (sizeType == SizeType.FULLSIZE) {
				this.proxyWidth = Aircandi.applicationContext.getResources().getDimensionPixelSize(R.dimen.image_category_fullsize);
				this.proxyHeight = Aircandi.applicationContext.getResources().getDimensionPixelSize(R.dimen.image_category_fullsize);
			}
			else if (sizeType == SizeType.PREVIEW) {
				this.proxyWidth = Aircandi.applicationContext.getResources().getDimensionPixelSize(R.dimen.image_category_preview);
				this.proxyHeight = Aircandi.applicationContext.getResources().getDimensionPixelSize(R.dimen.image_category_preview);
			}
			else if (sizeType == SizeType.PREVIEW_LARGE) {
				this.proxyWidth = Aircandi.applicationContext.getResources().getDimensionPixelSize(R.dimen.image_category_preview_large);
				this.proxyHeight = Aircandi.applicationContext.getResources().getDimensionPixelSize(R.dimen.image_category_preview_large);
			}
			else if (sizeType == SizeType.THUMBNAIL) {
				this.proxyWidth = Aircandi.applicationContext.getResources().getDimensionPixelSize(R.dimen.image_category_thumbnail);
				this.proxyHeight = Aircandi.applicationContext.getResources().getDimensionPixelSize(R.dimen.image_category_thumbnail);
			}
			else if (sizeType == SizeType.FULLSIZE_CAPPED) {
				this.proxyWidth = Aircandi.applicationContext.getResources().getDimensionPixelSize(R.dimen.image_category_fullsize_capped);
				this.proxyHeight = Aircandi.applicationContext.getResources().getDimensionPixelSize(R.dimen.image_category_fullsize_capped);
			}
		}
		else {
			this.sizeType = SizeType.FULLSIZE.ordinal();
			this.proxyWidth = null;
			this.proxyHeight = null;
		}
		return this;
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/ 	/*
     * Any photo from the device (camera, gallery) is store in s3 and source = aircandi
	 * Any search photo is not stored in s3 and source = generic
	 * Any place photo from foursquare stays there and photo.source = foursquare
	 */

	public static class PhotoSource {

		public static String generic = "generic";                // set in photo picker when using third party photo.
		public static String bing    = "bing";                    // set when thumbnail is coming straight from bing.

		public static String aircandi            = "aircandi";                // legacy that maps to aircandi_images
		public static String aircandi_images     = "aircandi.images";        // set when photo is stored by us and used to construct full uri to image data (s3)
		public static String aircandi_thumbnails = "aircandi.thumbnails";    // set when photo is stored by us and used to construct full uri to image data (s3)
		public static String aircandi_users      = "aircandi.users";        // set when photo is stored by us and used to construct full uri to image data (s3)

		public static String foursquare = "foursquare";            // set when photo is stored by us and used to construct full uri to image data (s3)
		public static String facebook   = "facebook";                // set if photo comes from facebook - used for applinks
		public static String twitter    = "twitter";                // set if photo comes from twitter - used for applinks
		public static String google     = "google";                // set if photo comes from google - used for applinks
		public static String yelp       = "yelp";                    // set if photo comes from yelp - used for applinks // NO_UCD (unused code)

		/* System sources */

		public static String resource          = "resource";                // set as default for shortcut and applink photos
		public static String assets_applinks   = "assets.applinks";        // used when targeting something like the default applink icons
		public static String assets_categories = "assets.categories";        // ditto to above

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

	private class GooglePlusProxy implements Serializable {
		private static final long   serialVersionUID = 4979315502693226461L;
		/*
		 * Setting refresh to 60 minutes.
		 */
		public               String baseWidth        = "https://images1-focus-opensocial.googleusercontent.com/gadgets/proxy?url=%s&container=focus&resize_w=%d&refresh=3600";
		public               String baseHeight       = "https://images1-focus-opensocial.googleusercontent.com/gadgets/proxy?url=%s&container=focus&resize_h=%d&refresh=3600";

		public String convert(String uri, Integer size, Orientation orientation) {
			String base = (orientation == Orientation.LANDSCAPE) ? baseWidth : baseHeight;
			String converted = String.format(base, Uri.encode(uri), size);
			return converted;
		}
	}
}