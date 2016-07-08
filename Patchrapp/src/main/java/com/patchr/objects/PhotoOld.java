package com.patchr.objects;

import android.content.Context;
import android.net.Uri;
import android.util.TypedValue;

import com.patchr.Patchr;
import com.patchr.utilities.Type;
import com.patchr.utilities.UI;

import java.io.Serializable;
import java.util.Map;

@SuppressWarnings("ucd")
public class PhotoOld extends ServiceObject implements Cloneable, Serializable {

	private static final long            serialVersionUID = 4979315562693226461L;
	private static final GooglePlusProxy imageResizer     = new GooglePlusProxy();

	public String prefix;
	public String suffix;
	public Number width;
	public Number height;
	public String source;
	public Number createdDate;

	/* client only */

	public Entity user;
	public String name;
	public String description;
	public Boolean store        = false;   // Hint that photo needs to be stored.

	public PhotoOld() {}

	public PhotoOld(String prefix, String suffix, Number width, Number height, String source) {
		this.prefix = prefix;
		this.suffix = suffix;
		this.width = width;
		this.height = height;
		this.source = source;
	}

	public PhotoOld(String prefix, String source) {
		this.prefix = prefix;
		this.source = source;
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public static PhotoOld setPropertiesFromMap(PhotoOld photo, Map map) {

		if (!map.containsKey("prefix")) {
			throw new RuntimeException("Photo object is missing required prefix property");
		}
		if (!map.containsKey("source")) {
			throw new RuntimeException("Photo object is missing required source property");
		}
		photo.source = (String) map.get("source");
		photo.prefix = (String) map.get("prefix");
		photo.suffix = (String) map.get("suffix");
		photo.width = (Number) map.get("width");
		photo.height = (Number) map.get("height");

		photo.createdDate = (Number) map.get("createdDate");
		photo.name = (String) map.get("name");
		photo.description = (String) map.get("description");

		photo.store = (Boolean) map.get("store");

		if (map.get("user") != null) {
			photo.user = User.setPropertiesFromMap(new User(), (Map<String, Object>) map.get("user"));
		}

		return photo;
	}

	public String uri(PhotoCategory category) {
		return UI.uri(this.prefix, this.source, category);
	}

	public String uriNative() {
		return UI.uri(this.prefix, this.source, PhotoCategory.NONE);
	}

	public boolean isFile() {
		return this.source.equals(PhotoOld.PhotoSource.file);
	}

	public boolean isResource() {
		return this.source.equals(PhotoOld.PhotoSource.resource);
	}

	public boolean isUri() {
		return !(this.source.equals(PhotoOld.PhotoSource.resource) || this.source.equals(PhotoOld.PhotoSource.file));
	}

	public Integer getResId() {
		return getResourceIdFromResourceName(Patchr.applicationContext, this.prefix);
	}

	public static Integer getResourceIdFromResourceName(Context context, String resourceName) {

		final String resolvedResourceName = resolveResourceName(context, resourceName);
		if (resolvedResourceName != null) {
			return Patchr.applicationContext.getResources().getIdentifier(resolvedResourceName
					, "drawable"
					, Patchr.getInstance().getPackageName());
		}
		return null;
	}

	public static String resolveResourceName(Context context, String rawResourceName) {
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
		final PhotoOld other = (PhotoOld) obj;
		return (Type.equal(this.uriNative(), other.uriNative()));
	}

	public static boolean same(Object obj1, Object obj2) {
		return (obj1 == null && obj2 == null)
				|| (obj1 != null && ((PhotoOld) obj1).sameAs(obj2));
	}

	@Override public PhotoOld clone() {
		try {
			PhotoOld photo = (PhotoOld) super.clone();
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

	public PhotoOld setCreatedAt(Number createdAt) {
		this.createdDate = createdAt;
		return this;
	}

	public String getName() {
		return name;
	}

	public PhotoOld setName(String name) {
		this.name = name;
		return this;
	}

	public PhotoOld setPrefix(String prefix) {
		this.prefix = prefix;
		return this;
	}

	public PhotoOld setPrefix(Uri uri) {
		this.prefix = uri.toString();
		return this;
	}

	public PhotoOld setSource(String source) {
		this.source = source;
		return this;
	}

	public PhotoOld setStore(Boolean store) {
		this.store = store;
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

		/* Service supported */
		public static String aircandi_images = "aircandi.images";           // set when photo is stored by us and used to construct full uri to image data (s3)
		public static String google          = "google";                    // set if photo comes from google - used for place photos

		/* Client only */
		public static String generic  = "generic";                   // set in photo picker when using third party photo.
		public static String resource = "resource";                         // set when using embedded resource
		public static String bing     = "bing";                             // set when thumbnail is coming straight from bing.
		public static String file     = "file";                                 // set when using a photo from device (camera|gallery)
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
		public static        String baseWidth        = "https://images1-focus-opensocial.googleusercontent.com/gadgets/proxy?url=%s&container=focus&resize_w=%d&no_expand=1&refresh=3600";
		public static        String baseHeight       = "https://images1-focus-opensocial.googleusercontent.com/gadgets/proxy?url=%s&container=focus&resize_h=%d&no_expand=1&refresh=3600";

		public String convert(String uri, Integer size, ResizeDimension dimension) {
			String base = (dimension == ResizeDimension.WIDTH) ? baseWidth : baseHeight;
			return String.format(base, Uri.encode(uri), size);
		}
	}
}