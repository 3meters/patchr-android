package com.aircandi.queries;

import com.aircandi.Patchr;
import com.aircandi.components.EntityManager;
import com.aircandi.components.ModelResult;
import com.aircandi.interfaces.IQuery;
import com.aircandi.objects.Cursor;
import com.aircandi.objects.ServiceData;
import com.aircandi.utilities.Maps;

public class NotificationsQuery implements IQuery {

	protected Cursor mCursor;
	protected Boolean mMore      = false;
	protected Integer mPageSize  = 30;
	protected String mSchema;
	protected String mEntityId;
	protected Boolean mHasExecuted = false;

	@Override
	public ModelResult execute(Integer skip, Integer limit) {
		/*
		 * - Should be called on a background thread.
		 * - Sorting is applied to links not the entities on the service side.
		 *
		 * We need to round up to the nearest page size.
		 */
		Integer skipCount = ((int) Math.ceil((double) skip / mPageSize) * mPageSize);
		mCursor = new Cursor()
				.setLimit((limit == null) ? mPageSize : limit)
				.setSort(Maps.asMap("modifiedDate", -1))
				.setSkip(skipCount);

		ModelResult result = ((EntityManager) Patchr.getInstance().getEntityManager()).loadNotifications(mEntityId, mCursor);

		if (result.data != null) {
			mHasExecuted = true;
			mMore = ((ServiceData) result.serviceResponse.data).more;
		}

		return result;
	}

	@Override
	public Boolean isMore() {
		return mMore;
	}

	@Override
	public Boolean hasExecuted() {
		return mHasExecuted;
	}

	public NotificationsQuery setPageSize(Integer pageSize) {
		mPageSize = pageSize;
		return this;
	}

	public NotificationsQuery setSchema(String schema) {
		mSchema = schema;
		return this;
	}

	public NotificationsQuery setEntityId(String entityId) {
		mEntityId = entityId;
		return this;
	}

	@Override
	public Integer getPageSize() {
		return mPageSize;
	}

	@Override
	public String getEntityId() {
		return mEntityId;
	}
}
