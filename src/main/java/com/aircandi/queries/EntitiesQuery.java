package com.aircandi.queries;

import android.support.annotation.NonNull;

import com.aircandi.Patchr;
import com.aircandi.components.ModelResult;
import com.aircandi.components.NetworkManager;
import com.aircandi.interfaces.IEntityController;
import com.aircandi.interfaces.IQuery;
import com.aircandi.objects.Cursor;
import com.aircandi.objects.LinkProfile;
import com.aircandi.objects.Links;
import com.aircandi.objects.ServiceData;
import com.aircandi.utilities.Maps;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EntitiesQuery implements IQuery {

	protected Cursor mCursor;
	protected Boolean mMore        = false;
	protected Boolean mHasExecuted = false;
	protected Integer mPageSize;
	protected Integer mPageCount = 0;
	protected String mSchema;
	protected String mLinkType;
	protected String mLinkDirection;
	protected Map    mLinkWhere = Maps.asMap("enabled", true);
	protected String mEntityId;

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

		Links links = Patchr.getInstance().getDataController().getLinks().build(LinkProfile.NO_LINKS);
		if (mSchema != null) {
			IEntityController controller = Patchr.getInstance().getControllerForSchema(mSchema);
			links = Patchr.getInstance().getDataController().getLinks().build(controller.getLinkProfile());
		}

		/* The only place in the code that calls getEntitiesForEntity */

		ModelResult result = Patchr.getInstance().getDataController().getEntitiesForEntity(mEntityId, links, mCursor, NetworkManager.SERVICE_GROUP_TAG_DEFAULT, null);

		if (result.data != null) {
			mHasExecuted = true;
			mMore = ((ServiceData) result.serviceResponse.data).more;
		}

		return result;
	}

	@NonNull
	@Override
	public Boolean isMore() {
		return mMore;
	}

	@NonNull
	@Override
	public Boolean hasExecuted() {
		return mHasExecuted;
	}

	public String getLinkType() {
		return mLinkType;
	}

	public String getEntityId() {
		return mEntityId;
	}

	@Override
	public Integer getPageSize() {
		return mPageSize;
	}

	public EntitiesQuery setPageSize(Integer pageSize) {
		mPageSize = pageSize;
		return this;
	}

	public EntitiesQuery setSchema(String schema) {
		mSchema = schema;
		return this;
	}

	public EntitiesQuery setLinkType(String linkType) {
		mLinkType = linkType;
		return this;
	}

	public EntitiesQuery setLinkDirection(String linkDirection) {
		mLinkDirection = linkDirection;
		return this;
	}

	public EntitiesQuery setEntityId(String entityId) {
		mEntityId = entityId;
		return this;
	}

	public EntitiesQuery setLinkWhere(Map linkWhere) {
		mLinkWhere = linkWhere;
		return this;
	}
}
