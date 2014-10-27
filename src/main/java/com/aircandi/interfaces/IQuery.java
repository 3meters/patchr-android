package com.aircandi.interfaces;

import com.aircandi.components.ModelResult;

public interface IQuery {

	public ModelResult execute(Integer skip, Integer limit);

	public Integer getPageSize();

	public Boolean isMore();

	public Boolean hasExecuted();

	public String getEntityId();

}
