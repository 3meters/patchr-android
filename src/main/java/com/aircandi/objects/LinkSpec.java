package com.aircandi.objects;

import com.aircandi.service.Expose;

import java.util.List;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class LinkSpec extends ServiceObject {

	private static final long serialVersionUID = -274203160211174564L;

	@Expose
	public Boolean            shortcuts;
	@Expose
	public List<LinkSpecItem> active;

	public List<LinkSpecItem> getActive() {
		return active;
	}

	public Boolean getShortcuts() {
		return shortcuts;
	}

	public LinkSpec setActive(List<LinkSpecItem> active) {
		this.active = active;
		return this;
	}

	public LinkSpec setShortcuts(Boolean shortcuts) {
		this.shortcuts = shortcuts;
		return this;
	}
}