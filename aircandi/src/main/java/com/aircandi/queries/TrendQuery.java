package com.aircandi.queries;

import com.aircandi.Aircandi;
import com.aircandi.components.ProximityManager.ModelResult;

@SuppressWarnings("ucd")
public class TrendQuery implements IQuery {

	protected String toSchema;
	protected String fromSchema;
	protected String trendType;
	protected Integer mPageSize = 50;  // Default service limit if nothing is passed

	@Override
	public ModelResult execute(Integer skip, Integer limit) {
	    /*
		 * Should be called on a background thread. By default returns sorted by rank
		 * in ascending order.
		 */
		ModelResult result = Aircandi.getInstance().getEntityManager().getTrending(toSchema, fromSchema, trendType);
		return result;
	}

	@Override
	public Boolean isMore() {
		return false;
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
