package com.patchr.objects;

import com.patchr.objects.Link.Direction;
import com.patchr.service.Expose;

import java.util.Map;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class LinkSpecItem extends ServiceObject {

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

	public LinkSpecItem() {
	}

	public LinkSpecItem(String type, String schema, Boolean links, Boolean count, Number limit) {
		this(type, schema, links, count, limit, null);
	}

	public LinkSpecItem(String type, String schema, Boolean links, Boolean count, Number limit, Map where) {
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

	public LinkSpecItem setLinks(Boolean links) {
		this.links = links;
		return this;
	}

	public Boolean getCount() {
		return count;
	}

	public LinkSpecItem setCount(Boolean count) {
		this.count = count;
		return this;
	}

	public Object getWhere() {
		return where;
	}

	public LinkSpecItem setWhere(Map where) {
		this.where = where;
		return this;
	}

	public Number getLimit() {
		return limit;
	}

	public LinkSpecItem setLimit(Number limit) {
		this.limit = limit;
		return this;
	}

	public Direction getDirection() {
		return Direction.valueOf(direction);
	}

	public LinkSpecItem setDirection(Direction direction) {
		this.direction = direction.name();
		return this;
	}
}