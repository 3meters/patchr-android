package com.aircandi.interfaces;

import android.os.Bundle;
import android.view.View;

public interface IForm {

	public void onAdd(Bundle extras);

	public void onError();

	public void onHelp();

	public void share();

	public void unpackIntent();

	public void initialize(Bundle savedInstanceState);

	public void draw(View view);
}
