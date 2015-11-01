package com.aircandi.objects;

/**
 * Created by jaymassena on 11/1/15.
 */
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
