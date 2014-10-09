package com.aircandi.queries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.aircandi.Patchr;
import com.aircandi.components.ModelResult;
import com.aircandi.interfaces.IEntityController;
import com.aircandi.interfaces.IQuery;
import com.aircandi.objects.Cursor;
import com.aircandi.objects.LinkProfile;
import com.aircandi.objects.Links;
import com.aircandi.objects.ServiceData;
import com.aircandi.utilities.Maps;

public class WatchersQuery implements IQuery {

	protected Cursor mCursor;
	protected Boolean mMore = false;
	protected Integer mPageSize;
	protected String  mSchema;
	protected String  mLinkType;
	protected String  mLinkDirection;
	protected Map     mLinkWhere;
	protected String  mEntityId;

	@Override
	public ModelResult execute(Integer skip, Integer limit) {
		/*
		 * - Should be called on a background thread.
		 * - Sorting is applied to links not the entities on the service side.
		 */
		mCursor = new Cursor()
				.setLimit((limit == null) ? mPageSize : limit)
				.setSort(Maps.asMap("modifiedDate", -1))
				.setSkip(skip);

		if (mLinkWhere != null) {
			mCursor.setWhere(mLinkWhere);
		}

		if (mSchema != null) {
			List<String> toSchemas = new ArrayList<String>();
			toSchemas.add(mSchema);
			mCursor.setToSchemas(toSchemas);
		}

		if (mLinkType != null) {
			List<String> linkTypes = new ArrayList<String>();
			linkTypes.add(mLinkType);
			mCursor.setLinkTypes(linkTypes);
		}

		if (mLinkDirection != null) {
			mCursor.setDirection(mLinkDirection);
		}

		Links links = Patchr.getInstance().getEntityManager().getLinks().build(LinkProfile.NO_LINKS);
		if (mSchema != null) {
			IEntityController controller = Patchr.getInstance().getControllerForSchema(mSchema);
			links = Patchr.getInstance().getEntityManager().getLinks().build(controller.getLinkProfile());
		}

		ModelResult result = Patchr.getInstance().getEntityManager().loadEntitiesForEntity(mEntityId, links, mCursor, null);

		if (result.data != null) {
			mMore = ((ServiceData) result.serviceResponse.data).more;
		}

		return result;
	}

	@Override
	public Boolean isMore() {
		return mMore;
	}

	public String getLinkType() {
		return mLinkType;
	}

	public String getEntityId() {
		return mEntityId;
	}

	public WatchersQuery setPageSize(Integer pageSize) {
		mPageSize = pageSize;
		return this;
	}

	public WatchersQuery setSchema(String schema) {
		mSchema = schema;
		return this;
	}

	public WatchersQuery setLinkType(String linkType) {
		mLinkType = linkType;
		return this;
	}

	public WatchersQuery setLinkDirection(String linkDirection) {
		mLinkDirection = linkDirection;
		return this;
	}

	public WatchersQuery setEntityId(String entityId) {
		mEntityId = entityId;
		return this;
	}

	public WatchersQuery setLinkWhere(Map linkWhere) {
		mLinkWhere = linkWhere;
		return this;
	}

	@Override
	public Integer getPageSize() {
		return mPageSize;
	}
}
