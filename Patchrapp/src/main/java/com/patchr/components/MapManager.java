package com.patchr.components;

import com.google.android.gms.maps.model.LatLng;

public class MapManager {
	public static final int    ZOOM_SCALE_BUILDINGS = 17;
	public static final int    ZOOM_SCALE_NEARBY    = 16;
	public static final int    ZOOM_SCALE_CITY      = 11;
	public static final int    ZOOM_SCALE_COUNTY    = 10;
	public static final int    ZOOM_SCALE_STATE     = 6;
	public static final int    ZOOM_SCALE_COUNTRY   = 5;
	public static final int    ZOOM_SCALE_USA       = 3;
	public static final int    ZOOM_SCALE_WORLD     = 1;
	public static final int    ZOOM_DEFAULT         = ZOOM_SCALE_NEARBY;
	public static final LatLng LATLNG_USA           = new LatLng(39.8282, -98.5795);
}
