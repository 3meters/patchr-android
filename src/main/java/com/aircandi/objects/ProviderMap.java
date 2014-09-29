package com.aircandi.objects;

import com.aircandi.service.Expose;

import java.io.Serializable;
import java.util.Map;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class ProviderMap extends ServiceObject implements Cloneable, Serializable {

	private static final long serialVersionUID = 4362288672245719998L;

	@Expose
	public String aircandi;
	@Expose
	public String foursquare;
	@Expose
	public String google;
	@Expose
	public String yelp;
	@Expose
	public String googleReference;
	@Expose
	public String factual;

	@Override
	public ProviderMap clone() {
		try {
			final ProviderMap provider = (ProviderMap) super.clone();
			return provider;
		}
		catch (final CloneNotSupportedException ex) {
			throw new AssertionError();
		}
	}

	public static ProviderMap setPropertiesFromMap(ProviderMap provider, Map map, Boolean nameMapping) {
		provider.aircandi = (String) map.get("aircandi");
		provider.foursquare = (String) map.get("foursquare");
		provider.yelp = (String) map.get("yelp");
		provider.google = (String) map.get("google");
		provider.googleReference = (String) map.get("googleReference");
		provider.factual = (String) map.get("factual");
		return provider;
	}
}