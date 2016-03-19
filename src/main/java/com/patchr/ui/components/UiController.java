package com.patchr.ui.components;

public class UiController {

	protected BusyController  mBusyController;
	protected EmptyController mMessageController;

	public UiController() {}

	public UiController setBusyController(BusyController busyController) {
		mBusyController = busyController;
		return this;
	}

	public BusyController getBusyController() {
		return mBusyController;
	}

	public UiController setMessageController(EmptyController messageController) {
		mMessageController = messageController;
		if (mMessageController != null) {
			mMessageController.showEmptyMessage(false);
		}
		return this;
	}

	public EmptyController getMessageController() {
		return mMessageController;
	}

	public void onPause() {
		if (mBusyController != null) {
			mBusyController.onPause();
		}
	}

	public void onResume() {
		if (mBusyController != null) {
			mBusyController.onResume();
		}
	}
}
