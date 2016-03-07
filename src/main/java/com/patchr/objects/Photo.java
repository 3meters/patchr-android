package com.patchr.objects;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.TypedValue;

import com.patchr.Patchr;
import com.patchr.service.Expose;
import com.patchr.utilities.Type;
import com.patchr.utilities.UI;

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

	/* Only comes from foursquare */

	@Expose(serialize = false, deserialize = true)
	public Entity user;

	/* client only */

	public String name;

	public String description;

	@NonNull
	public Boolean usingDefault  = false;
	@NonNull
	public Boolean store         = false;   // Hint that photo needs to be stored.

	public Photo() {}

	public Photo(@NonNull String prefix, String suffix, Number width, Number height, @NonNull String source) {
		this.prefix = prefix;
		this.suffix = suffix;
		this.width = width;
		this.height = height;
		this.source = source;
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@NonNull
	public static Photo setPropertiesFromMap(@NonNull Photo photo, @NonNull Map map, Boolean nameMapping) {

		if (!map.containsKey("prefix")) {
			throw new RuntimeException("Photo object is missing required prefix property");
		}
		photo.prefix = (String) map.get("prefix");
		photo.suffix = (String) map.get("suffix");
		photo.width = (Number) map.get("width");
		photo.height = (Number) map.get("height");
		if (!map.containsKey("source")) {
			throw new RuntimeException("Photo object is missing required source property");
		}
		photo.source = (String) map.get("source");

		photo.createdDate = (Number) map.get("createdDate");
		photo.name = (String) map.get("name");
		photo.description = (String) map.get("description");

		photo.store = (Boolean) map.get("store");

		if (map.get("user") != null) {
			photo.user = User.setPropertiesFromMap(new User(), (HashMap<String, Object>) map.get("user"), nameMapping);
		}

		return photo;
	}

	public Integer getResId() {
		return getResourceIdFromResourceName(Patchr.applicationContext, this.prefix);
	}

	@NonNull
	public String getUri(PhotoSizeCategory category) {
		return UI.url(this.prefix, this.source, category);
	}

	@NonNull
	public String getDirectUri() {
		return UI.url(this.prefix, this.source, PhotoSizeCategory.NONE);
	}

	@NonNull
	public static String getPhotoSourceByPhotoType(PhotoType photoType) {
		return PhotoSource.aircandi_images;
	}

	public static Boolean isDrawable(@NonNull String uri) {
		return uri.toLowerCase(Locale.US).startsWith("resource:");
	}

	@NonNull
	public static Boolean hasAlpha(@NonNull Photo photo) {
		/*
		 * We assume alpha unless we see a jpg|jpeg.
		 */
		String uri = photo.getDirectUri();
		return (!(uri.toLowerCase(Locale.US).contains(".jpg") || uri.toLowerCase(Locale.US).contains(".jpeg")));
	}

	public static Integer getResourceIdFromResourceName(@NonNull Context context, String resourceName) {

		final String resolvedResourceName = resolveResourceName(context, resourceName);
		if (resolvedResourceName != null) {
			return Patchr.applicationContext.getResources().getIdentifier(resolvedResourceName
					, "drawable"
					, Patchr.getInstance().getPackageName());
		}
		return null;
	}

	public static Integer getResourceIdFromUri(@NonNull Context context, @NonNull String uri) {

		final String rawResourceName = uri.substring(uri.indexOf("resource:") + 9);
		final String resolvedResourceName = resolveResourceName(context, rawResourceName);
		if (resolvedResourceName != null) {
			return Patchr.applicationContext.getResources().getIdentifier(resolvedResourceName
					, "drawable"
					, Patchr.getInstance().getPackageName());
		}
		return null;
	}

	public static String resolveResourceName(@NonNull Context context, String rawResourceName) {
		int resourceId = Patchr.applicationContext.getResources().getIdentifier(rawResourceName, "drawable", Patchr.getInstance().getPackageName());
		if (resourceId == 0) {
			resourceId = Patchr.applicationContext.getResources().getIdentifier(rawResourceName, "attr", Patchr.getInstance().getPackageName());
			final TypedValue value = new TypedValue();
			if (context.getTheme().resolveAttribute(resourceId, value, true)) {
				return (String) value.coerceToString();
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
		final Photo other = (Photo) obj;
		return (Type.equal(this.getDirectUri(), other.getDirectUri()));
	}

	public static boolean same(Object obj1, Object obj2) {
		return obj1 == null
				&& obj2 == null
				|| (obj1 != null && ((Photo) obj1).sameAs(obj2));
	}

	@NonNull
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

	@NonNull
	public Photo setCreatedAt(Number createdAt) {
		this.createdDate = createdAt;
		return this;
	}

	public Entity getUser() {
		return user;
	}

	@NonNull
	public Photo setUser(Entity user) {
		this.user = user;
		return this;
	}

	public String getName() {
		return name;
	}

	@NonNull
	public Photo setName(String name) {
		this.name = name;
		return this;
	}

	@NonNull
	public Photo setPrefix(@NonNull String prefix) {
		this.prefix = prefix;
		return this;
	}

	@NonNull
	public Photo setPrefix(@NonNull Uri uri) {
		this.prefix = uri.toString();
		return this;
	}

	@NonNull
	public Photo setSource(@NonNull String source) {
		this.source = source;
		return this;
	}

	@NonNull
	public Photo setStore(@NonNull Boolean store) {
		this.store = store;
		return this;
	}

	//	@NonNull
	//	public Photo setProxy(Boolean proxyActive, Integer height, Integer width) {
	//		this.resizerActive = proxyActive;
	//		if (proxyActive) {
	//			this.resizerWidth = width;
	//			this.resizerHeight = height;
	//		}
	//		else {
	//			this.resizerWidth = null;
	//			this.resizerHeight = null;
	//		}
	//		return this;
	//	}

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

		@NonNull
		public static String generic         = "generic";                   // set in photo picker when using third party photo.
		@NonNull
		public static String aircandi_images = "aircandi.images";           // set when photo is stored by us and used to construct full uri to image data (s3)
		@NonNull
		public static String aircandi_users  = "aircandi.users";            // set when photo is stored by us and used to construct full uri to image data (s3)
		@NonNull
		public static String foursquare      = "foursquare";                // set if photo comes from foursquare - used for place photos
		@NonNull
		public static String google          = "google";                    // set if photo comes from google - used for place photos
		@NonNull
		public static String yelp            = "yelp";                      // set if photo comes from yelp - used for place photos
		@NonNull
		public static String gravatar        = "gravatar";                  // set if photo comes from gravatar - used for default user photos

		/* System sources */

		@NonNull
		public static String resource          = "resource";                // set when using embedded resource
		@NonNull
		public static String assets_categories = "assets.categories";       // set when using service category photo asset

		/* Client only */

		@NonNull
		public static String none = "none";                                 // initialization value
		@NonNull
		public static String bing = "bing";                                 // set when thumbnail is coming straight from bing.
		@NonNull
		public static String file = "file";                                 // set when using a photo from device (camera|gallery)

		/* Deprecated */

		@NonNull
		public static String aircandi            = "aircandi";              // legacy that maps to aircandi_images
		@NonNull
		public static String aircandi_thumbnails = "aircandi.thumbnails";   // set when photo is stored by us and used to construct full uri to image data (s3)
		@NonNull
		public static String foursquare_icon     = "foursquare.icon";       // set if icon comes from foursquare - used for place categories
	}

	public enum PhotoType {
		GENERAL,
		THUMBNAIL,
		USER,
	}

	public enum Orientation {
		LANDSCAPE,
		PORTRAIT
	}

	public enum ResizeDimension {
		HEIGHT,
		WIDTH
	}

	private static class GooglePlusProxy implements Serializable {
		private static final long   serialVersionUID = 4979315502693226461L;
		/*
		 * Setting refresh to 60 minutes.
		 */
		@NonNull
		public static        String baseWidth        = "https://images1-focus-opensocial.googleusercontent.com/gadgets/proxy?url=%s&container=focus&resize_w=%d&no_expand=1&refresh=3600";
		@NonNull
		public static        String baseHeight       = "https://images1-focus-opensocial.googleusercontent.com/gadgets/proxy?url=%s&container=focus&resize_h=%d&no_expand=1&refresh=3600";

		public String convert(String uri, Integer size, ResizeDimension dimension) {
			String base = (dimension == ResizeDimension.WIDTH) ? baseWidth : baseHeight;
			return String.format(base, Uri.encode(uri), size);
		}
	}
}