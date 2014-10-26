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

import com.aircandi.Patchr;
import com.aircandi.Patchr.ThemeTone;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.AirApplication;
import com.aircandi.components.MediaManager;
import com.aircandi.components.StringManager;
import com.aircandi.objects.Place;
import com.aircandi.ui.base.BasePicker;
import com.aircandi.interfaces.IBind.BindingMode;

import java.util.ArrayList;
import java.util.List;

@SuppressLint("Registered")
public class PhotoSourcePicker extends BasePicker implements OnItemClickListener {

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

	@Override
	public void bind(BindingMode mode) {

		/* Shown as a dialog so doesn't have an action bar */
		final List<Object> listData = new ArrayList<Object>();

		/* Everyone gets these options */
		listData.add(new AirApplication(Patchr.themeTone.equals(ThemeTone.LIGHT)
		                                ? R.drawable.ic_action_search_light : R.drawable.ic_action_search_dark
				, StringManager.getString(R.string.dialog_photo_source_search), null, Constants.PHOTO_SOURCE_SEARCH));

		listData.add(new AirApplication(Patchr.themeTone.equals(ThemeTone.LIGHT)
		                                ? R.drawable.ic_action_tiles_large_light
		                                : R.drawable.ic_action_tiles_large_dark
				, StringManager.getString(R.string.dialog_photo_source_gallery), null, Constants.PHOTO_SOURCE_GALLERY));

		/* Only show the camera choice if there is one and there is a place to store the image */
		if (MediaManager.canCaptureWithCamera()) {
			listData.add(new AirApplication(Patchr.themeTone.equals(ThemeTone.LIGHT)
			                                ? R.drawable.ic_action_camera_light
			                                : R.drawable.ic_action_camera_dark
					, StringManager.getString(R.string.dialog_photo_source_camera), null, Constants.PHOTO_SOURCE_CAMERA));
		}

		/* Add place photo option if this is a place entity */
		if (mEntity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
			Place place = (Place) mEntity;
			if (place.getProvider().type != null && place.getProvider().type.equals(Constants.TYPE_PROVIDER_FOURSQUARE)) {
				listData.add(new AirApplication(Patchr.themeTone.equals(ThemeTone.LIGHT)
				                                ? R.drawable.ic_action_location_light
				                                : R.drawable.ic_action_location_dark
						, StringManager.getString(R.string.dialog_photo_source_place), null, Constants.PHOTO_SOURCE_PLACE));
			}
		}

		/* Everyone gets the default option */
		if (mEntity.type == null) {
			listData.add(new AirApplication(Patchr.themeTone.equals(ThemeTone.LIGHT)
			                                ? R.drawable.ic_action_picture_light
			                                : R.drawable.ic_action_picture_dark
					, StringManager.getString(R.string.dialog_photo_source_default), null, Constants.PHOTO_SOURCE_DEFAULT));
		}

		mName.setText(StringManager.getString(R.string.dialog_photo_source_title));

		mListAdapter = new ListAdapter(this, listData);
		mListView.setAdapter(mListAdapter);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		final AirApplication choice = (AirApplication) view.getTag();
		final Intent intent = new Intent();
		intent.putExtra(Constants.EXTRA_PHOTO_SOURCE, choice.schema);
		setResultCode(Activity.RESULT_OK, intent);
		finish();
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
			final AirApplication itemData = (AirApplication) items.get(position);

			if (view == null) {
				view = LayoutInflater.from(PhotoSourcePicker.this).inflate(R.layout.temp_listitem_photo_source, null);
			}

			if (itemData != null) {
				((ImageView) view.findViewById(R.id.photo)).setImageResource(itemData.iconResId);
				((TextView) view.findViewById(R.id.name)).setText(itemData.title);
				view.setTag(itemData);
			}
			return view;
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Misc
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected int getLayoutId() {
		return R.layout.photo_source_picker;
	}
}