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
		final Intent intent = new Intent();
		intent.putExtra(Constants.EXTRA_PHOTO_SOURCE, choice.schema);
		setResultCode(Activity.RESULT_OK, intent);
		finish();
		Patchr.getInstance().getAnimationManager().doOverridePendingTransition(this, TransitionType.DIALOG_BACK);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void bind(BindingMode mode) {

		/* Shown as a dialog so doesn't have an action bar */
		final List<Object> listData = new ArrayList<Object>();

		/* Everyone gets these options */
		listData.add(new PickerItem(Patchr.themeTone.equals(ThemeTone.LIGHT)
		                            ? R.drawable.ic_action_search_light : R.drawable.ic_action_search_dark
				, StringManager.getString(R.string.dialog_photo_action_search), Constants.PHOTO_ACTION_SEARCH));

		listData.add(new PickerItem(Patchr.themeTone.equals(ThemeTone.LIGHT)
		                            ? R.drawable.ic_action_tiles_large_light
		                            : R.drawable.ic_action_tiles_large_dark
				, StringManager.getString(R.string.dialog_photo_action_gallery), Constants.PHOTO_ACTION_GALLERY));

		/* Only show the camera choice if there is one and there is a place to store the image */
		if (MediaManager.canCaptureWithCamera()) {
			listData.add(new PickerItem(Patchr.themeTone.equals(ThemeTone.LIGHT)
			                            ? R.drawable.ic_action_camera_light
			                            : R.drawable.ic_action_camera_dark
					, StringManager.getString(R.string.dialog_photo_action_camera), Constants.PHOTO_ACTION_CAMERA));
		}

		/* Only show if a photo has been set */
		if (mEntity.photo != null) {
			listData.add(new PickerItem(Patchr.themeTone.equals(ThemeTone.LIGHT)
			                            ? R.drawable.ic_action_edit_light
			                            : R.drawable.ic_action_edit_dark
					, StringManager.getString(R.string.dialog_photo_action_edit), Constants.PHOTO_ACTION_EDIT));
		}

		/* Everyone gets the default option */
		if (mEntity.type == null) {
			listData.add(new PickerItem(Patchr.themeTone.equals(ThemeTone.LIGHT)
			                            ? R.drawable.ic_action_picture_light
			                            : R.drawable.ic_action_picture_dark
					, StringManager.getString(R.string.dialog_photo_action_default), Constants.PHOTO_ACTION_DEFAULT));
		}

		mName.setText(StringManager.getString(R.string.dialog_photo_action_title));

		mListAdapter = new ListAdapter(this, listData);
		mListView.setAdapter(mListAdapter);
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