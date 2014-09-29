package com.aircandi.objects;

import com.aircandi.objects.Link.Direction;
import com.aircandi.service.Expose;

import java.util.Map;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class LinkParams extends ServiceObject {

	private static final long serialVersionUID = 4371355790668325686L;

	@Expose
	public String type;
	@Expose
	public String schema;
	@Expose
	public Boolean links = false;
	@Expose
	public Boolean count = true;
	@Expose
	public Map    where;
	@Expose
	public Number limit;
	@Expose
	public String direction = "both";

	public LinkParams() {
	}

	public LinkParams(String type, String schema, Boolean links, Boolean count, Number limit) {
		this(type, schema, links, count, limit, null);
	}

	public LinkParams(String type, String schema, Boolean links, Boolean count, Number limit, Map where) {
		this.type = type;
		this.schema = schema;
		this.links = links;
		this.count = count;
		this.where = where;
		this.limit = limit;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Boolean getLinks() {
		return links;
	}

	public LinkParams setLinks(Boolean links) {
		this.links = links;
		return this;
	}

	public Boolean getCount() {
		return count;
	}

	public LinkParams setCount(Boolean count) {
		this.count = count;
		return this;
	}

	public Object getWhere() {
		return where;
	}

	public LinkParams setWhere(Map where) {
		this.where = where;
		return this;
	}

	public Number getLimit() {
		return limit;
	}

	public LinkParams setLimit(Number limit) {
		this.limit = limit;
		return this;
	}

	public Direction getDirection() {
		return Direction.valueOf(direction);
	}

	public LinkParams setDirection(Direction direction) {
		this.direction = direction.name();
		return this;
	}
}