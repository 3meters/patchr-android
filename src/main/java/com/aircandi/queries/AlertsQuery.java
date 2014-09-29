package com.aircandi.queries;

import com.aircandi.Patch;
import com.aircandi.Constants;
import com.aircandi.components.EntityManager;
import com.aircandi.components.ModelResult;
import com.aircandi.interfaces.IQuery;
import com.aircandi.objects.Cursor;
import com.aircandi.objects.ServiceData;
import com.aircandi.utilities.Maps;

import java.util.ArrayList;
import java.util.List;

public class AlertsQuery implements IQuery {

	protected Cursor mCursor;
	protected Boolean mMore     = false;
	protected Integer mPageSize = 30;
	protected String mSchema;
	protected String mEntityId;

	@Override
	public ModelResult execute(Integer skip, Integer limit) {
		/*
		 * - Should be called on a background thread.
		 * - Sorting is applied to links not the entities on the service side.
		 */
		List<String> toSchemas = new ArrayList<String>();
		toSchemas.add(Constants.SCHEMA_ENTITY_PLACE);

		List<String> linkTypes = new ArrayList<String>();
		linkTypes.add(Constants.TYPE_LINK_CREATE);

		mCursor = new Cursor()
				.setLimit((limit == null) ? mPageSize : limit)
				.setSort(Maps.asMap("modifiedDate", -1))
				.setSkip(skip)
				.setToSchemas(toSchemas)
				.setLinkTypes(linkTypes);

		ModelResult result = ((EntityManager) Patch.getInstance().getEntityManager()).loadAlerts(mEntityId, mCursor);

		if (result.data != null) {
			mMore = ((ServiceData) result.serviceResponse.data).more;
		}

		return result;
	}

	@Override
	public Boolean isMore() {
		return mMore;
	}

	public AlertsQuery setPageSize(Integer pageSize) {
		mPageSize = pageSize;
		return this;
	}

	public AlertsQuery setSchema(String schema) {
		mSchema = schema;
		return this;
	}

	public AlertsQuery setEntityId(String entityId) {
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
