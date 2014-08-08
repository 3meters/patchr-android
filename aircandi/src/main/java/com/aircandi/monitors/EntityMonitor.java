package com.aircandi.monitors;

import com.aircandi.Aircandi;
import com.aircandi.components.EntityManager;
import com.aircandi.components.Logger;
import com.aircandi.objects.CacheStamp;
import com.aircandi.objects.Entity;
import com.aircandi.objects.User;

public class EntityMonitor extends SimpleMonitor {

	private String mEntityId;

	public EntityMonitor(String entityId) {
		mEntityId = entityId;
	}

	@Override
	public Boolean isChanged() {
		/*
		 * Possible states:
		 * - No entity in the cache, go get it.
		 * - Entity in the cache and difference in cache stamps, go get it.
		 * - Entity in the cache and same cache stamps, call activity check.
		 * - Entity in the cache and no monitor cache stamp, call activity check.
		 */
		this.changed = false;
		Entity entity = EntityManager.getCacheEntity(mEntityId);

		if (mEntityId.equals(Aircandi.getInstance().getCurrentUser().id)) {
			entity = Aircandi.getInstance().getCurrentUser();
		}

		if (entity == null) {
			this.activity = true;
			this.modified = true;
			this.changed = true;
			return true;
		}
		else {
			CacheStamp cacheStamp = entity.getCacheStamp();
			if (mCacheStamp != null) {

				this.changed = (!cacheStamp.equals(mCacheStamp));
				mCacheStamp = cacheStamp;
				Logger.d(this, "changed(): Cache stamp updated from entity to: " + "activity=" + mCacheStamp.activityDate + " modified="
						+ mCacheStamp.modifiedDate);

				/* We know a service refresh is needed so skip service activity check. */
				if (this.changed) {
					this.activity = ((cacheStamp.activityDate == null)
					                 ? mCacheStamp.activityDate == null
					                 : mCacheStamp.activityDate.equals(cacheStamp.activityDate));
					this.modified = ((cacheStamp.modifiedDate == null)
					                 ? mCacheStamp.modifiedDate == null
					                 : mCacheStamp.modifiedDate.equals(cacheStamp.modifiedDate));
					return true;
				}
			}
			else {
				mCacheStamp = cacheStamp;
				Logger.d(this, "changed(): Cache stamp initialized to: " + "activity=" + mCacheStamp.activityDate + " modified=" + mCacheStamp.modifiedDate);
			}
		}
		/*
		 * Haven't found obvious reason to refresh yet so make network call for staleness check.
		 */
		Logger.d(this, "Service activity check from entity monitor");
		CacheStamp cacheStamp = Aircandi.getInstance().getEntityManager().loadCacheStamp(mEntityId, mCacheStamp);

		//		this.activity = (cacheStamp == null || cacheStamp.activity == null) ? true : cacheStamp.activity;
		//		this.modified = (cacheStamp == null || cacheStamp.modified == null) ? true : cacheStamp.modified;

		this.activity = (cacheStamp == null) || ((cacheStamp.activity == null) ? false : cacheStamp.activity);
		this.modified = (cacheStamp == null) || ((cacheStamp.modified == null) ? false : cacheStamp.modified);

		/*
		 * Entities for users have a special dependency because changing the parent entity
		 * (user) can mean updating list items that are showing stale user info.
		 */
		if (entity != null && entity instanceof User && this.modified) {
			this.activity = this.modified;
		}

		this.changed = (cacheStamp == null || !cacheStamp.equals(mCacheStamp));

		if (cacheStamp != null) {
			mCacheStamp = cacheStamp;
			Logger.d(this, "changed(): Cache stamp updated from service to: " + "activity=" + mCacheStamp.activityDate + " modified="
					+ mCacheStamp.modifiedDate);
		}

		return this.changed;
	}

	public void setEntityId(String entityId) {
		mEntityId = entityId;
	}

	public void updateCacheStamp(Entity entity) {
		/*
		 * Called when a fresh version of the entity has been pulled from the service.
		 */
		if (entity != null) {
			CacheStamp cacheStamp = entity.getCacheStamp();
			mCacheStamp = cacheStamp;
			Logger.d(this, "updateCacheStamp(): Cache stamp updated to: " + "activity=" + mCacheStamp.activityDate + " modified=" + mCacheStamp.modifiedDate);
		}
	}
}
