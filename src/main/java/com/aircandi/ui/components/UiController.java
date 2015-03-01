package com.aircandi.ui.components;

public class UiController {

	private BusyController    mBusyController;
	private MessageController mMessageController;

	public UiController() {}

	public UiController setBusyController(BusyController busyController) {
		mBusyController = busyController;
		return this;
	}

	public BusyController getBusyController() {
		return mBusyController;
	}

	public UiController setMessageController(MessageController messageController) {
		mMessageController = messageController;
		if (mMessageController != null) {
			mMessageController.showMessage(false);
		}
		return this;
	}

	public MessageController getMessageController() {
		return mMessageController;
	}

	public void pause() {
		if (mBusyController != null) {
			mBusyController.pause();
		}
	}

	public void resume() {
		if (mBusyController != null) {
			mBusyController.resume();
		}
	}
}
