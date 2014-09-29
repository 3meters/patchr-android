package com.aircandi.queries;

import com.aircandi.Patch;
import com.aircandi.components.ModelResult;
import com.aircandi.interfaces.IQuery;
import com.aircandi.objects.LinkProfile;
import com.aircandi.objects.Links;

public class ShortcutsQuery implements IQuery {

	protected String  mEntityId;
	protected Integer mPageSize;

	@Override
	public ModelResult execute(Integer skip, Integer limit) {
		mPageSize = limit;
		Links options = Patch.getInstance().getEntityManager().getLinks().build(LinkProfile.LINKS_FOR_USER_CURRENT);
		ModelResult result = Patch.getInstance().getEntityManager().getEntity(mEntityId, true, options);
		return result;
	}

	public ShortcutsQuery setEntityId(String entityId) {
		mEntityId = entityId;
		return this;
	}

	@Override
	public Boolean isMore() {
		return false;
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