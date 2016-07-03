package com.patchr.model;

import android.content.Context;
import android.net.Uri;
import android.util.TypedValue;

import com.patchr.Patchr;
import com.patchr.objects.Photo;
import com.patchr.objects.PhotoCategory;
import com.patchr.utilities.Type;
import com.patchr.utilities.UI;

import java.io.Serializable;
import java.util.Map;

import io.realm.RealmObject;
import io.realm.annotations.Ignore;

@SuppressWarnings("ucd")
public class RealmPhoto extends RealmObject {

	private static final GooglePlusProxy imageResizer = new GooglePlusProxy();

	public String  prefix;
	public Integer width;
	public Integer height;
	public String  source;

	/* Local client */

	@Ignore public Boolean store = false;   // Hint that photo needs to be stored.

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public static RealmPhoto setPropertiesFromMap(RealmPhoto photo, Map map) {

		if (!map.containsKey("prefix")) {
			throw new RuntimeException("Photo object is missing required prefix property");
		}

		if (!map.containsKey("source")) {
			throw new RuntimeException("Photo object is missing required source property");
		}

		photo.prefix = (String) map.get("prefix");
		photo.source = (String) map.get("source");
		photo.width = map.get("width") != null ? ((Double) map.get("width")).intValue() : null;
		photo.height = map.get("height") != null ? ((Double) map.get("height")).intValue() : null;

		return photo;
	}

	public Photo asPhoto() {
		Photo photo = new Photo(prefix, source);
		return photo;
	}

	public String uri(PhotoCategory category) {
		return UI.uri(this.prefix, this.source, category);
	}

	public String uriNative() {
		return UI.uri(this.prefix, this.source, PhotoCategory.NONE);
	}

	public boolean isFile() {
		return this.source.equals(RealmPhoto.PhotoSource.file);
	}

	public boolean isResource() {
		return this.source.equals(RealmPhoto.PhotoSource.resource);
	}

	public boolean isUri() {
		return !(this.source.equals(RealmPhoto.PhotoSource.resource) || this.source.equals(RealmPhoto.PhotoSource.file));
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
		final RealmPhoto other = (RealmPhoto) obj;
		return (Type.equal(this.uriNative(), other.uriNative()));
	}

	public static boolean same(Object obj1, Object obj2) {
		return (obj1 == null && obj2 == null)
			|| (obj1 != null && ((RealmPhoto) obj1).sameAs(obj2));
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