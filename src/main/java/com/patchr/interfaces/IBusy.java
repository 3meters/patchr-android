package com.patchr.interfaces;

import android.content.Context;

public interface IBusy {

	public void show(BusyAction busyAction);

	public void show(BusyAction busyAction, Object message, Context context);

	public void showProgressDialog(Context context);

	public void hide(Boolean now);

	public void onPause();

	public void onResume();

	public enum BusyAction {
		Refreshing_Empty,
		Refreshing,
		Scanning_Empty,
		Scanning,
		ActionWithMessage,
		Update,
	}
}
