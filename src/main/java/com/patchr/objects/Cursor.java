package com.patchr.objects;

import com.patchr.service.Expose;

import java.util.List;
import java.util.Map;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class Cursor extends ServiceObject {

	private static final long serialVersionUID = -8424707925181657940L;

	@Expose
	public Map sort;
	@Expose
	public Number skip = 0;
	@Expose
	public Number       limit;
	@Expose
	public List<String> linkTypes;
	@Expose
	public List<String> schemas;
	@Expose
	public String       direction;
	@Expose
	public Map          where;

	public Cursor() {}

	public Map getSort() {
		return sort;
	}

	public Cursor setSort(Map sort) {
		this.sort = sort;
		return this;
	}

	public Number getSkip() {
		return skip;
	}

	public Cursor setSkip(Number skip) {
		this.skip = skip;
		return this;
	}

	public Number getLimit() {
		return limit;
	}

	public Cursor setLimit(Number limit) {
		this.limit = limit;
		return this;
	}

	public List<String> getLinkTypes() {
		return linkTypes;
	}

	public Cursor setLinkTypes(List<String> linkTypes) {
		this.linkTypes = linkTypes;
		return this;
	}

	public List<String> getToSchemas() {
		return schemas;
	}

	public Cursor setToSchemas(List<String> schemas) {
		this.schemas = schemas;
		return this;
	}

	public String getDirection() {
		return direction;
	}

	public Cursor setDirection(String direction) {
		this.direction = direction;
		return this;
	}

	public Map getWhere() {
		return where;
	}

	public Cursor setWhere(Map where) {
		this.where = where;
		return this;
	}
}