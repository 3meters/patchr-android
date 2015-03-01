package com.aircandi.ui.components;

public class ListController extends UiController {

	private FloatingActionController mFloatingActionController;

	public ListController() {}

	public FloatingActionController getFloatingActionController() {
		return mFloatingActionController;
	}

	public ListController setFloatingActionController(FloatingActionController floatingActionController) {
		mFloatingActionController = floatingActionController;
		return this;
	}
}
