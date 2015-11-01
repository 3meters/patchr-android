package com.aircandi.events;

@SuppressWarnings("ucd")
public class LinkMuteEvent extends DataEventBase {

	public String   linkId;
	public Boolean  mute;
	public String   actionEvent;

	public LinkMuteEvent() {}

	public LinkMuteEvent setLinkId(String linkId) {
		this.linkId = linkId;
		return this;
	}

	public LinkMuteEvent setMute(Boolean mute) {
		this.mute = mute;
		return this;
	}

	public LinkMuteEvent setActionEvent(String actionEvent) {
		this.actionEvent = actionEvent;
		return this;
	}
}
