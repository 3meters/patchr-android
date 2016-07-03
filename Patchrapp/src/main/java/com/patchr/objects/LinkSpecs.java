package com.patchr.objects;

import com.patchr.service.Expose;

import java.util.List;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class LinkSpecs extends ServiceObject {

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

	public LinkSpecs setActive(List<LinkSpecItem> active) {
		this.active = active;
		return this;
	}

	public LinkSpecs setShortcuts(Boolean shortcuts) {
		this.shortcuts = shortcuts;
		return this;
	}
}