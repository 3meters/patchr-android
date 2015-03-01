package com.aircandi.interfaces;

import android.content.Context;

public interface IBusy {

	public void show(BusyAction busyAction);

	public void show(BusyAction busyAction, Object message, Context context);

	public void showProgressDialog(Context context);

	public void hide(Boolean now);

	public void pause();

	public void resume();

	public enum BusyAction {
		Refreshing_Empty,
		Refreshing,
		Scanning_Empty,
		Scanning,
		ActionWithMessage,
		Update,
	}
}
