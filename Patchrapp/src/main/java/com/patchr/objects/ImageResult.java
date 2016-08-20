package com.patchr.objects;

import com.patchr.model.Photo;

import java.util.Map;

/**
 * The Class IMAGE_RESULT.
 * Used for bing image searches.
 */
public class ImageResult {
	public String name;
	public String contentUrl;
	public Long   contentSize;
	public String encodingFormat;
	public Long   width;
	public Long   height;

	public String thumbnailUrl;
	public Long   thumbnailWidth;
	public Long   thumbnailHeight;

	/* client only */
	public Photo photo;

	public static ImageResult setPropertiesFromMap(ImageResult imageResult, Map map) {

		imageResult.name = (String) map.get("name");
		imageResult.contentUrl = (String) map.get("contentUrl");
		String contentSize = ((String) map.get("contentSize")).replaceAll("[^0-9]", "");
		imageResult.contentSize = Long.parseLong(contentSize);
		imageResult.encodingFormat = (String) map.get("encodingFormat");
		imageResult.width = ((Double) map.get("width")).longValue();
		imageResult.height = ((Double) map.get("height")).longValue();

		imageResult.thumbnailUrl = (String) map.get("thumbnailUrl");
		Map<String, Object> thumbnailMap = (Map<String, Object>) map.get("thumbnail");
		imageResult.thumbnailWidth = ((Double) thumbnailMap.get("width")).longValue();
		imageResult.thumbnailHeight = ((Double) thumbnailMap.get("height")).longValue();

		return imageResult;
	}

	public Photo asPhoto() {
		return new Photo(contentUrl, width.intValue(), height.intValue(), Photo.PhotoSource.generic);
	}

	public Photo thumbnailAsPhoto() {
		return new Photo(thumbnailUrl, thumbnailWidth.intValue(), thumbnailHeight.intValue(), Photo.PhotoSource.generic);
	}
}
