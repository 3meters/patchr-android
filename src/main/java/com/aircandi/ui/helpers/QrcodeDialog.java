package com.aircandi.ui.helpers;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.Patchr.ThemeTone;
import com.aircandi.R;
import com.aircandi.components.MediaManager;
import com.aircandi.components.StringManager;
import com.aircandi.interfaces.IBind.BindingMode;
import com.aircandi.objects.TransitionType;
import com.aircandi.ui.base.BasePicker;

import java.util.ArrayList;
import java.util.List;

@SuppressLint("Registered")
public class QrcodeDialog extends BasePicker {

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected int getLayoutId() {
		return R.layout.qrcode_dialog;
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/
}