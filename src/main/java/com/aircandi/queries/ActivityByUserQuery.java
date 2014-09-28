package com.aircandi.queries;

import com.aircandi.Aircandi;
import com.aircandi.components.ModelResult;
import com.aircandi.objects.Cursor;
import com.aircandi.objects.EventType;
import com.aircandi.objects.ServiceData;
import com.aircandi.utilities.Maps;

import java.util.ArrayList;
import java.util.List;

public class ActivityByUserQuery implements IQuery {

	protected Boolean mMore     = false;
	protected Integer mPageSize = 30;
	protected String mEntityId;

	@Override
	public ModelResult execute(Integer skip, Integer limit) {
		/*
		 * - Should be called on a background thread.
		 * - Sorting is applied to links not the entities on the service side.
		 */
		Cursor cursor = new Cursor()
				.setLimit((limit == null) ? mPageSize : limit)
				.setSort(Maps.asMap("modifiedDate", -1))
				.setSkip(skip);

		List<String> events = new ArrayList<String>();
		events.add(EventType.INSERT_PLACE);
		events.add(EventType.INSERT_PICTURE_TO_PLACE);
		events.add(EventType.INSERT_COMMENT_TO_PLACE);
		events.add(EventType.INSERT_COMMENT_TO_PICTURE);

		events.add(EventType.UPDATE_PLACE);
		events.add(EventType.UPDATE_PICTURE);
		events.add(EventType.UPDATE_USER);
		events.add(EventType.UPDATE_APPLINKS);

		events.add(EventType.DELETE_PLACE);
		events.add(EventType.DELETE_PICTURE);

		events.add(EventType.WATCH_PLACE);
		events.add(EventType.WATCH_PICTURE);
		events.add(EventType.WATCH_USER);

		events.add(EventType.UNWATCH_PLACE);
		events.add(EventType.UNWATCH_PICTURE);
		events.add(EventType.UNWATCH_USER);

		ModelResult result = Aircandi.getInstance().getEntityManager().loadActivities(mEntityId, cursor, events);

		if (result.data != null) {
			mMore = ((ServiceData) result.serviceResponse.data).more;
		}
		return result;
	}

	@Override
	public Boolean isMore() {
		return mMore;
	}

	public ActivityByUserQuery setPageSize(Integer pageSize) {
		mPageSize = pageSize;
		return this;
	}

	public ActivityByUserQuery setEntityId(String entityId) {
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
