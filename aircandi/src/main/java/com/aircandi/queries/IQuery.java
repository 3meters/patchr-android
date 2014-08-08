package com.aircandi.queries;

import com.aircandi.components.ProximityManager.ModelResult;

public interface IQuery {

	public ModelResult execute(Integer skip, Integer limit);

	public Integer getPageSize();

	public Boolean isMore();

	public String getEntityId();

}
