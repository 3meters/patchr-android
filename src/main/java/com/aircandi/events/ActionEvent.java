package com.aircandi.events;

import android.view.View;

import com.aircandi.components.DataController.ActionType;

@SuppressWarnings("ucd")
public class ActionEvent {

	public ActionType actionType;
	public View       view;          // Optional view associated with action
	public Object     tag;          // Optional payload associated with action

	public ActionEvent() {}

	public ActionEvent setActionType(ActionType actionType) {
		this.actionType = actionType;
		return this;
	}

	public ActionEvent setView(View view) {
		this.view = view;
		return this;
	}

	public ActionEvent setTag(Object tag) {
		this.tag = tag;
		return this;
	}
}
