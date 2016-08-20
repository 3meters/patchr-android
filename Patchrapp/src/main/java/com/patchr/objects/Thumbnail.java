package com.patchr.objects;

import com.patchr.model.Photo;

import java.util.Map;

public class Thumbnail {

	public String mediaUrl;
	public String contentType;
	public Long   width;
	public Long   height;
	public Long   fileSize;
	public Long   runTime;

	public static Thumbnail setPropertiesFromMap(Thumbnail thumbnail, Map map) {

		thumbnail.mediaUrl = (String) map.get("MediaUrl");
		thumbnail.width = Long.parseLong((String) map.get("Width"));
		thumbnail.height = Long.parseLong((String) map.get("Height"));
		thumbnail.fileSize = Long.parseLong((String) map.get("FileSize"));
		thumbnail.contentType = (String) map.get("ContentType");
		if (map.get("RunTime") != null) {
			thumbnail.runTime = Long.parseLong((String) map.get("RunTime"));
		}

		return thumbnail;
	}

	public Photo asPhoto() {
		return new Photo(mediaUrl, width.intValue(), height.intValue(), Photo.PhotoSource.generic);
	}
}
