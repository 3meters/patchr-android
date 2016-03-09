package com.patchr.ui.helpers;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.patchr.Constants;
import com.patchr.R;
import com.patchr.components.AnimationManager;
import com.patchr.components.MediaManager;
import com.patchr.components.PermissionUtil;
import com.patchr.components.StringManager;
import com.patchr.interfaces.IBind.BindingMode;
import com.patchr.objects.TransitionType;
import com.patchr.ui.base.BasePicker;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.UI;

import java.util.ArrayList;
import java.util.List;

@SuppressLint("Registered")
public class PhotoActionPicker extends BasePicker implements OnItemClickListener {

	private TextView    mName;
	private ListView    mListView;
	private ListAdapter mListAdapter;

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mName = (TextView) findViewById(R.id.name);
		mListView = (ListView) findViewById(R.id.form_list);
		mListView.setOnItemClickListener(this);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

		final PickerItem choice = (PickerItem) view.getTag();

		if (choice.schema.equals(Constants.PHOTO_ACTION_GALLERY)) {
			if (!PermissionUtil.hasSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
				UI.showToastNotification("Using a gallery photo requires permission to access storage", Toast.LENGTH_SHORT);
				ensurePermissions();
				return;
			}
		}
		else if (choice.schema.equals(Constants.PHOTO_ACTION_CAMERA)) {
			if (!PermissionUtil.hasSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
				UI.showToastNotification("Using the camera requires permission to access storage", Toast.LENGTH_SHORT);
				ensurePermissions();
				return;
			}
		}

		final Intent intent = new Intent();
		intent.putExtra(Constants.EXTRA_PHOTO_SOURCE, choice.schema);
		setResultCode(Activity.RESULT_OK, intent);
		finish();
		AnimationManager.doOverridePendingTransition(this, TransitionType.DIALOG_BACK);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void bind(BindingMode mode) {

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

		mName.setText(StringManager.getString(R.string.dialog_photo_action_title));

		mListAdapter = new ListAdapter(this, listData);
		mListView.setAdapter(mListAdapter);
	}

	private void ensurePermissions() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			if (!PermissionUtil.hasSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {

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
		}
	}

	@Override
	protected int getLayoutId() {
		return R.layout.photo_source_picker;
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

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			final PickerItem itemData = (PickerItem) items.get(position);

			if (view == null) {
				view = LayoutInflater.from(PhotoActionPicker.this).inflate(R.layout.temp_listitem_photo_source, null);
			}

			if (itemData != null) {
				((ImageView) view.findViewById(R.id.photo_view)).setImageResource(itemData.iconResId);
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