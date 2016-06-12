package com.patchr.objects;

import com.patchr.Constants;
import com.patchr.service.Expose;

import java.util.List;
import java.util.Map;

public class Cursor extends ServiceObject {

	private static final long serialVersionUID = -8424707925181657940L;

	@Expose public Map sort;
	@Expose public Number skip  = 0;
	@Expose public Number limit = Constants.PAGE_SIZE;
	@Expose public List<String> linkTypes;
	@Expose public List<String> schemas;
	@Expose public String       direction;
	@Expose public Map          where;

	public Cursor() {}

	public Map getSort() {
		return sort;
	}

	public Cursor setSort(Map sort) {
		this.sort = sort;
		return this;
	}

	public Cursor setSkip(Number skip) {
		this.skip = skip;
		return this;
	}

	public Cursor setLinkTypes(List<String> linkTypes) {
		this.linkTypes = linkTypes;
		return this;
	}

	public Cursor setToSchemas(List<String> schemas) {
		this.schemas = schemas;
		return this;
	}

	public Cursor setDirection(String direction) {
		this.direction = direction;
		return this;
	}

	public Cursor setWhere(Map where) {
		this.where = where;
		return this;
	}
}