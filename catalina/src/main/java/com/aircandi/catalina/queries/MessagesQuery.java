package com.aircandi.catalina.queries;

import java.util.ArrayList;
import java.util.List;

import com.aircandi.Aircandi;
import com.aircandi.catalina.Constants;
import com.aircandi.catalina.components.EntityManager;
import com.aircandi.catalina.objects.EventType;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.controllers.IEntityController;
import com.aircandi.objects.Cursor;
import com.aircandi.objects.LinkProfile;
import com.aircandi.objects.Links;
import com.aircandi.objects.ServiceData;
import com.aircandi.queries.IQuery;
import com.aircandi.utilities.Maps;

public class MessagesQuery implements IQuery {

	protected Cursor	mCursor;
	protected Boolean	mMore		= false;
	protected Integer	mPageSize	= 30;
	protected String	mSchema;
	protected String	mEntityId;

	@Override
	public ModelResult execute(Integer skip, Integer limit) {
		/*
		 * - Should be called on a background thread.
		 * - Sorting is applied to links not the entities on the service side.
		 */
		List<String> toSchemas = new ArrayList<String>();
		toSchemas.add(Constants.SCHEMA_ENTITY_PLACE);
		toSchemas.add(Constants.SCHEMA_ENTITY_MESSAGE);
		toSchemas.add(Constants.SCHEMA_ENTITY_USER);

		List<String> linkTypes = new ArrayList<String>();
		linkTypes.add(Constants.TYPE_LINK_CREATE);
		linkTypes.add(Constants.TYPE_LINK_WATCH);

		mCursor = new Cursor()
				.setLimit((limit == null) ? mPageSize : limit)
				.setSort(Maps.asMap("modifiedDate", -1))
				.setSkip(skip)
				.setToSchemas(toSchemas)
				.setLinkTypes(linkTypes);

		List<String> events = new ArrayList<String>();
		events.add(EventType.INSERT_MESSAGE_TO_PLACE);
		events.add(EventType.INSERT_MESSAGE_TO_MESSAGE);

		Links links = Aircandi.getInstance().getEntityManager().getLinks().build(LinkProfile.NO_LINKS);
		if (mSchema != null) {
			IEntityController controller = Aircandi.getInstance().getControllerForSchema(mSchema);
			links = Aircandi.getInstance().getEntityManager().getLinks().build(controller.getLinkProfile());
		}

		ModelResult result = ((EntityManager) Aircandi.getInstance().getEntityManager()).loadMessages(mEntityId, links, mCursor, events);

		if (result.data != null) {
			mMore = ((ServiceData) result.serviceResponse.data).more;
		}

		return result;
	}

	@Override
	public Boolean isMore() {
		return mMore;
	}

	public MessagesQuery setPageSize(Integer pageSize) {
		mPageSize = pageSize;
		return this;
	}

	public MessagesQuery setSchema(String schema) {
		mSchema = schema;
		return this;
	}

	public MessagesQuery setEntityId(String entityId) {
		mEntityId = entityId;
		return this;
	}

	@Override
	public Integer getPageSize() {
		return mPageSize;
	}
}
