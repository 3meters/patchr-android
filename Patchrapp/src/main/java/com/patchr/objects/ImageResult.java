package com.patchr.objects;

import com.patchr.model.Photo;

import java.util.Map;

/**
 * The Class IMAGE_RESULT.
 * Used for bing image searches.
 */
public class ImageResult {
	public String    title;
	public String    mediaUrl;
	public String    url;
	public String    displayUrl;
	public Long      width;
	public Long      height;
	public Long      fileSize;
	public String    contentType;
	public Thumbnail thumbnail;

	/* client only */
	public Photo photo;

	public static ImageResult setPropertiesFromMap(ImageResult imageResult, Map map) {

		imageResult.title = (String) map.get("Title");
		imageResult.mediaUrl = (String) map.get("MediaUrl");
		imageResult.url = (String) map.get("Url");
		imageResult.displayUrl = (String) map.get("DisplayUrl");
		imageResult.width = Long.parseLong((String) map.get("Width"));
		imageResult.height = Long.parseLong((String) map.get("Height"));
		imageResult.fileSize = Long.parseLong((String) map.get("FileSize"));
		imageResult.contentType = (String) map.get("ContentType");

		if (map.get("Thumbnail") != null) {
			imageResult.thumbnail = Thumbnail.setPropertiesFromMap(new Thumbnail(), (Map<String, Object>) map.get("Thumbnail"));
		}

		return imageResult;
	}

	public Photo asPhoto() {
		return new Photo(mediaUrl, width.intValue(), height.intValue(), Photo.PhotoSource.generic);
	}
}
