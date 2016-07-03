package com.patchr.objects;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("ucd")
public class LinkSpec {

	public String              to;
	public String              from;
	public String              type;
	public Boolean             enabled;
	public Integer             limit;
	public Boolean             count;
	public Map<String, Object> filter;
	public String              fields;
	public String              linkFields;
	public LinkSpec            linked;
	public Map<String, Object> linkedFilter;
	public LinkSpec            links;
	public LinkSpec            linkCount;
	public Map<String, Object> refs;

	public LinkSpec() {}

	public SimpleMap asMap() {
		SimpleMap map = new SimpleMap();
		if (to != null) {
			map.put("to", to);
		}
		if (from != null) {
			map.put("from", from);
		}
		if (type != null) {
			map.put("type", type);
		}
		if (enabled != null) {
			map.put("enabled", enabled);
		}
		if (limit != null) {
			map.put("limit", limit);
		}
		if (count != null) {
			map.put("count", count);
		}
		if (filter != null) {
			map.put("filter", filter);
		}
		if (fields != null) {
			map.put("fields", fields);
		}
		if (linkFields != null) {
			map.put("linkFields", linkFields);
		}
		if (linked != null) {
			map.put("linked", linked);
		}
		if (linkedFilter != null) {
			map.put("linkedFilter", linkedFilter);
		}
		if (links != null) {
			map.put("links", links);
		}
		if (linkCount != null) {
			map.put("linkCount", linkCount);
		}
		if (refs != null) {
			map.put("refs", refs);
		}
		return map;
	}

	public String getTo() {
		return to;
	}

	public LinkSpec setTo(String to) {
		this.to = to;
		return this;
	}

	public String getFrom() {
		return from;
	}

	public LinkSpec setFrom(String from) {
		this.from = from;
		return this;
	}

	public String getType() {
		return type;
	}

	public LinkSpec setType(String type) {
		this.type = type;
		return this;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public LinkSpec setEnabled(Boolean enabled) {
		this.enabled = enabled;
		return this;
	}

	public Number getLimit() {
		return limit;
	}

	public LinkSpec setLimit(Integer limit) {
		this.limit = limit;
		return this;
	}

	public Boolean getCount() {
		return count;
	}

	public LinkSpec setCount(Boolean count) {
		this.count = count;
		return this;
	}

	public Map<String, Object> getFilter() {
		return filter;
	}

	public LinkSpec setFilter(Map<String, Object> filter) {
		this.filter = filter;
		return this;
	}

	public String getFields() {
		return fields;
	}

	public LinkSpec setFields(String fields) {
		this.fields = fields;
		return this;
	}

	public String getLinkFields() {
		return linkFields;
	}

	public LinkSpec setLinkFields(String linkFields) {
		this.linkFields = linkFields;
		return this;
	}

	public LinkSpec getLinked() {
		return linked;
	}

	public LinkSpec setLinked(LinkSpec linked) {
		this.linked = linked;
		return this;
	}

	public Map<String, Object> getLinkedFilter() {
		return linkedFilter;
	}

	public LinkSpec setLinkedFilter(Map<String, Object> linkedFilter) {
		this.linkedFilter = linkedFilter;
		return this;
	}

	public LinkSpec getLinks() {
		return links;
	}

	public LinkSpec setLinks(LinkSpec links) {
		this.links = links;
		return this;
	}

	public LinkSpec getLinkCount() {
		return linkCount;
	}

	public LinkSpec setLinkCount(LinkSpec linkCount) {
		this.linkCount = linkCount;
		return this;
	}

	public Map<String, Object> getRefs() {
		return refs;
	}

	public LinkSpec setRefs(Map<String, Object> refs) {
		this.refs = refs;
		return this;
	}
}
