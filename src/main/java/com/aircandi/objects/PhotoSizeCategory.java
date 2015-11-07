package com.aircandi.objects;

public enum PhotoSizeCategory {
	/*
	 * Always append new enum items because there is a
	 * dependency on ordering for persistence.
	 */
	NONE,
	STANDARD,
	THUMBNAIL,
	PROFILE
}
