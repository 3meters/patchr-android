package com.aircandi.queries;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.components.ModelResult;
import com.aircandi.objects.Cursor;
import com.aircandi.objects.EventType;
import com.aircandi.objects.ServiceData;
import com.aircandi.utilities.Maps;

import java.util.ArrayList;
import java.util.List;

public class ActivityByAffinityQuery implements IQuery {

	protected Boolean mMore     = false;
	protected Integer mPageSize = 30;
	protected String mEntityId;

	@Override
	public ModelResult execute(Integer skip, Integer limit) {
	    /*
		 * - Should be called on a background thread.
		 * - Sorting is applied to links not the entities on the service side.
		 */

		List<String> schemas = new ArrayList<String>();
		schemas.add(Constants.SCHEMA_ENTITY_PLACE);
		schemas.add(Constants.SCHEMA_ENTITY_PICTURE);
		schemas.add(Constants.SCHEMA_ENTITY_USER);

		List<String> linkTypes = new ArrayList<String>();
		linkTypes.add(Constants.TYPE_LINK_CREATE);
		linkTypes.add(Constants.TYPE_LINK_WATCH);

		Cursor cursor = new Cursor()
				.setLimit((limit == null) ? mPageSize : limit)
				.setSort(Maps.asMap("modifiedDate", -1))
				.setSkip(skip)
				.setToSchemas(schemas)
				.setLinkTypes(linkTypes);

		List<String> events = new ArrayList<String>();
		events.add(EventType.INSERT_PLACE);
		events.add(EventType.INSERT_PICTURE_TO_PLACE);
		events.add(EventType.INSERT_COMMENT_TO_PLACE);
		events.add(EventType.INSERT_COMMENT_TO_PICTURE);

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

	public ActivityByAffinityQuery setPageSize(Integer pageSize) {
		mPageSize = pageSize;
		return this;
	}

	public ActivityByAffinityQuery setEntityId(String entityId) {
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
