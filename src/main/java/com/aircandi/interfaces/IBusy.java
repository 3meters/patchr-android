package com.aircandi.interfaces;

public interface IBusy {

	public void show(BusyAction busyAction);

	public void show(BusyAction busyAction, Object message);

	public void showProgressDialog();

	public void hide(Boolean now);

	public enum BusyAction {
		Refreshing_Empty,
		Refreshing,
		Scanning_Empty,
		Scanning,
		ActionWithMessage,
		Update,
	}
}
