package com.patchr.ui.helpers;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.patchr.Constants;
import com.patchr.R;
import com.patchr.components.AnimationManager;
import com.patchr.components.MediaManager;
import com.patchr.components.PermissionUtil;
import com.patchr.components.StringManager;
import com.patchr.objects.TransitionType;
import com.patchr.utilities.Dialogs;

import java.util.ArrayList;
import java.util.List;

@SuppressLint("Registered")
public class PhotoActionPicker extends AppCompatActivity implements OnItemClickListener {

	private TextView    name;
	private ListView    listView;
	private ListAdapter listAdapter;
	private PickerItem  pendingChoice;

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		super.setContentView(R.layout.photo_action_picker);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		initialize(savedInstanceState);
		bind();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

		final PickerItem choice = (PickerItem) view.getTag();

		if (choice.schema.equals(Constants.PHOTO_ACTION_GALLERY)) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
				if (!PermissionUtil.hasSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
					pendingChoice = choice;
					requestPermissions();
					return;
				}
			}
		}
		else if (choice.schema.equals(Constants.PHOTO_ACTION_CAMERA)) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
				if (!PermissionUtil.hasSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
					pendingChoice = choice;
					requestPermissions();
					return;
				}
			}
		}

		pickerAction(choice);
	}

	@Override public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
		switch (requestCode) {
			case Constants.PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
				if (PermissionUtil.verifyPermissions(grantResults)) {
					pickerAction(pendingChoice);
				}
				else {
					cancelAction(false);
				}
			}
		}
	}

	@Override public void onBackPressed() {
		cancelAction(true);
	}

	public void onCancelButtonClick(View view) {
		cancelAction(true);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void initialize(Bundle savedInstanceState) {
		name = (TextView) findViewById(R.id.name);
		listView = (ListView) findViewById(R.id.form_list);
		if (listView != null) {
			listView.setOnItemClickListener(this);
		}
	}

	public void bind() {

		/* Shown as a dialog so doesn't have an action bar */
		final List<Object> listData = new ArrayList<>();

		/* Everyone gets these options */
		listData.add(new PickerItem(R.drawable.ic_action_search_light
				, StringManager.getString(R.string.dialog_photo_action_search), Constants.PHOTO_ACTION_SEARCH));

		listData.add(new PickerItem(R.drawable.ic_action_tiles_large_light
				, StringManager.getString(R.string.dialog_photo_action_gallery), Constants.PHOTO_ACTION_GALLERY));

		/* Only show the camera choice if there is one and there is a place to store the image */
		if (MediaManager.canCaptureWithCamera()) {
			listData.add(new PickerItem(R.drawable.ic_action_camera_light
					, StringManager.getString(R.string.dialog_photo_action_camera), Constants.PHOTO_ACTION_CAMERA));
		}

		name.setText(StringManager.getString(R.string.dialog_photo_action_title));

		listAdapter = new ListAdapter(this, listData);
		listView.setAdapter(listAdapter);
	}

	public void pickerAction(PickerItem choice) {
		final Intent intent = new Intent();
		intent.putExtra(Constants.EXTRA_PHOTO_SOURCE, choice.schema);
		setResult(Activity.RESULT_OK, intent);
		finish();
		AnimationManager.doOverridePendingTransition(this, TransitionType.DIALOG_BACK);
	}

	public void cancelAction(Boolean force) {
		setResult(Activity.RESULT_CANCELED);
		finish();
		AnimationManager.doOverridePendingTransition(this, TransitionType.DIALOG_BACK);
	}

	private void requestPermissions() {

		if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					final AlertDialog dialog = Dialogs.alertDialog(null
							, StringManager.getString(R.string.alert_permission_storage_title)
							, StringManager.getString(R.string.alert_permission_storage_message)
							, null
							, PhotoActionPicker.this
							, R.string.alert_permission_storage_positive
							, R.string.alert_permission_storage_negative
							, null
							, new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int which) {
									if (which == DialogInterface.BUTTON_POSITIVE) {
										if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
											ActivityCompat.requestPermissions(PhotoActionPicker.this
													, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}
													, Constants.PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
										}
										else {
											cancelAction(false);
										}
									}
								}
							}, null);
					dialog.setCanceledOnTouchOutside(false);
				}
			});
		}
		else {
				/*
				 * No explanation needed, we can request the permission.
				 * Parent activity will broadcast an event when permission request is complete.
				 */
			ActivityCompat.requestPermissions(this
					, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}
					, Constants.PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	private class ListAdapter extends ArrayAdapter<Object> {

		private final List<Object> items;

		private ListAdapter(Context context, List<Object> items) {
			super(context, 0, items);
			this.items = items;
		}

		@Override public View getView(int position, View convertView, ViewGroup parent) {

			View view = convertView;
			final PickerItem itemData = (PickerItem) items.get(position);

			if (view == null) {
				view = LayoutInflater.from(PhotoActionPicker.this).inflate(R.layout.temp_listitem_photo_source, null);
			}

			if (itemData != null) {
				((ImageView) view.findViewById(R.id.photo)).setImageResource(itemData.iconResId);
				((TextView) view.findViewById(R.id.name)).setText(itemData.title);
				view.setTag(itemData);
			}
			return view;
		}
	}

	private class PickerItem {

		public Integer iconResId;
		public String  title;
		public String  schema;

		public PickerItem() {}

		public PickerItem(Integer iconResId, String title, String schema) {
			this.iconResId = iconResId;
			this.title = title;
			this.schema = schema;
		}
	}
}