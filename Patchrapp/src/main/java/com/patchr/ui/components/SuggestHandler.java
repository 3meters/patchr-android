package com.patchr.ui.components;

import android.os.Handler;
import android.os.Message;

import java.lang.ref.WeakReference;

/**
 * Created by jaymassena on 7/22/16.
 */
class SuggestHandler extends Handler {
	private final WeakReference<EntitySuggestController> suggestController;

	SuggestHandler(EntitySuggestController suggestController) {
		this.suggestController = new WeakReference<EntitySuggestController>(suggestController);
	}

	@Override public void handleMessage(Message msg) {
		EntitySuggestController suggestController = this.suggestController.get();
		if (suggestController != null) {
			suggestController.suggestAction((String) msg.obj);
		}
	}
}
