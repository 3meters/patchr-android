package com.aircandi.interfaces;

public interface IBind {

	public void bind(BindingMode mode);

	public void onRefresh();

	public enum BindingMode {
		AUTO,
		MANUAL,
	}
}
