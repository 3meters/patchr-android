package com.aircandi.interfaces;

import com.aircandi.ui.widgets.SmoothProgressBar;

public interface IBusy {

	public void showBusy(BusyAction busyAction);

	public void showBusy(BusyAction busyAction, Object message);

	public void showProgress();

	public void hideBusy(Boolean now);

	public enum BusyAction {
		Loading,
		Refreshing,
		ActionWithMessage,
		Scanning,
		Update
	}
}
