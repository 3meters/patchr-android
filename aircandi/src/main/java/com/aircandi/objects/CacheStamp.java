package com.aircandi.objects;

import java.util.Map;

import com.aircandi.service.Expose;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class CacheStamp extends ServiceObject {

	private static final long	serialVersionUID	= 706592482666845156L;
	@Expose
	public Number				activityDate;
	@Expose
	public Number				modifiedDate;
	@Expose
	public Boolean				activity			= false;
	@Expose
	public Boolean				modified			= false;
	@Expose
	public Boolean				override			= false;
	@Expose
	public String				source;

	public CacheStamp() {}

	public CacheStamp(Number activityDate, Number modifiedDate) {
		this.activityDate = activityDate;
		this.modifiedDate = modifiedDate;
	}

	public static CacheStamp setPropertiesFromMap(CacheStamp cacheStamp, Map map, Boolean nameMapping) {
		cacheStamp.activityDate = (Number) map.get("activityDate");
		cacheStamp.modifiedDate = (Number) map.get("modifiedDate");
		cacheStamp.activity = (Boolean) map.get("activity");
		cacheStamp.modified = (Boolean) map.get("modified");
		return cacheStamp;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) return false;
		if (o == this) return true;
		if (!(o instanceof CacheStamp)) return false;
		CacheStamp other = (CacheStamp) o;
		if (other.override || this.override) return false;
		Boolean activityEqual = ((this.activityDate == null) ? other.activityDate == null : this.activityDate.equals(other.activityDate));
		Boolean modifiedEqual = ((this.modifiedDate == null) ? other.modifiedDate == null : this.modifiedDate.equals(other.modifiedDate));
		return (activityEqual && modifiedEqual);
	}

	@Override
	public int hashCode() {
		int result = 17;
		long activity = (this.activityDate != null) ? activityDate.longValue() : 0;
		long modified = (this.modifiedDate != null) ? modifiedDate.longValue() : 0;
		result = 37 * result + (int) (activity ^ (activity >>> 32));
		result = 37 * result + (int) (modified ^ (modified >>> 32));
		return result;
	}

	@Override
	public CacheStamp clone() {
		CacheStamp cacheStamp = null;
		try {
			cacheStamp = (CacheStamp) super.clone();
		}
		catch (CloneNotSupportedException exception) {
			exception.printStackTrace();
			throw new AssertionError();
		}
		return cacheStamp;
	}

	public static enum StampSource {
		ENTITY,
		SERVICE,
		ENTITY_MANAGER
	}

}