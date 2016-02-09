package com.patchr.utilities;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.objects.Photo;
import com.patchr.objects.PhotoSizeCategory;

public class PhotoUtils {

	public static String url(String prefix, String source, PhotoSizeCategory category) {

		Float density = Patchr.applicationContext.getResources().getDisplayMetrics().density;

		String path = "";
		Integer quality = 75;
		if (density >= 3) {
			quality = 25;
		}
		else if (density >= 2) {
			quality = 50;
		}

		if (source == Photo.PhotoSource.aircandi_images) {
			Integer width = (category == PhotoSizeCategory.STANDARD) ? 400 : 100;
			if (category == PhotoSizeCategory.PROFILE) {
				path = String.format("https://3meters-images.imgix.net/%1$s?w=%2$s&dpr=%3$s&q=%4$s&h=%5$s&fit=min&trim=auto", prefix, width, density, quality, width);
			}
			else {
				path = String.format("https://3meters-images.imgix.net/%1$s?w=%2$s&dpr=%3$s&q=%4$s", prefix, width, density, quality);
			}
		}
		else if (source == Photo.PhotoSource.google) {
			Float width = Constants.IMAGE_DIMENSION_MAX * density;
			if (prefix.contains("?")) {
				path = String.format("%1$s&maxwidth=%2$s", prefix, width);
			}
			else {
				path = String.format("%1$s?maxwidth=%2$s", prefix, width);
			}
		}

		return path;
	}
}