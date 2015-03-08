package com.aircandi.queries;

import com.aircandi.Patchr;
import com.aircandi.components.ModelResult;
import com.aircandi.components.NetworkManager;
import com.aircandi.interfaces.IQuery;

@SuppressWarnings("ucd")
public class TrendQuery implements IQuery {

	protected String toSchema;
	protected String fromSchema;
	protected String trendType;
	protected Integer mPageSize    = 50;  // Default service limit if nothing is passed
	protected Integer mPageCount   = 0;
	protected Boolean mHasExecuted = false;

	@Override
	public ModelResult execute(Integer skip, Integer limit) {
	    /*
		 * Should be called on a background thread. By default returns sorted by rank
		 * in ascending order.
		 */
		ModelResult result = Patchr.getInstance().getDataController().getTrending(toSchema, fromSchema, trendType, NetworkManager.SERVICE_GROUP_TAG_DEFAULT);
		mHasExecuted = true;
		return result;
	}

	@Override
	public Boolean isMore() {
		return false;
	}

	@Override
	public Boolean hasExecuted() {
		return mHasExecuted;
	}

	public String getToSchema() {
		return toSchema;
	}

	public TrendQuery setToSchema(String toSchema) {
		this.toSchema = toSchema;
		return this;
	}

	public String getFromSchema() {
		return fromSchema;
	}

	public TrendQuery setFromSchema(String fromSchema) {
		this.fromSchema = fromSchema;
		return this;
	}

	public String getTrendType() {
		return trendType;
	}

	public TrendQuery setTrendType(String trendType) {
		this.trendType = trendType;
		return this;
	}

	@Override
	public Integer getPageSize() {
		return mPageSize;
	}

	@Override
	public String getEntityId() {
		return null;
	}
}
