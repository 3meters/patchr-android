package com.aircandi.ui.helpers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.TextView;

import com.aircandi.Patch;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.DownloadManager;
import com.aircandi.interfaces.IEntityController;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Photo;
import com.aircandi.objects.Shortcut;
import com.aircandi.ui.base.BaseActivity;
import com.aircandi.ui.helpers.ShortcutPicker.ShortcutListAdapter.ViewHolder;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.utilities.Json;
import com.aircandi.utilities.UI;
import com.squareup.picasso.Picasso.LoadedFrom;
import com.squareup.picasso.Target;

import java.util.ArrayList;
import java.util.List;

public class ShortcutPicker extends BaseActivity {

	private ListView mList;
	private final List<Shortcut> mShortcuts = new ArrayList<Shortcut>();

	@Override
	public void unpackIntent() {
		super.unpackIntent();
		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			final List<String> jsonShortcuts = extras.getStringArrayList(Constants.EXTRA_SHORTCUTS);
			if (jsonShortcuts != null) {
				for (String jsonShortcut : jsonShortcuts) {
					Shortcut shortcut = (Shortcut) Json.jsonToObject(jsonShortcut, Json.ObjectType.SHORTCUT);
					mShortcuts.add(shortcut);
				}
			}
			final String jsonEntity = extras.getString(Constants.EXTRA_ENTITY);
			if (jsonEntity != null) {
				mEntity = (Entity) Json.jsonToObject(jsonEntity, Json.ObjectType.ENTITY);
			}
		}
	}

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
		mList = (ListView) findViewById(R.id.list);
	}

	@Override
	public void bind(BindingMode mode) {

		if (mShortcuts.size() > 0) {
			Shortcut shortcut = mShortcuts.get(0);

			/* Show default photo based on the type of the shortcut set */
			IEntityController controller = Patch.getInstance().getControllerForSchema(shortcut.schema);
			Photo photo = controller.getDefaultPhoto(shortcut.app);

			Integer width = Patch.applicationContext.getResources().getDimensionPixelSize(R.dimen.image_category_thumbnail);
			//noinspection SuspiciousNameCombination
			DownloadManager.with(Patch.applicationContext)
			               .load(photo.getUri())
			               .centerCrop()
			               .placeholder(null)
			               .resize(width, width)
			               .into(new Target() {

				               @Override
				               public void onBitmapFailed(Drawable arg0) {
				               }

				               @Override
				               public void onBitmapLoaded(Bitmap bitmap, LoadedFrom loadedFrom) {
					               DownloadManager.checkDebug(bitmap, loadedFrom);
					               mActionBar.setIcon(new BitmapDrawable(Patch.applicationContext.getResources(), bitmap));
				               }

				               @Override
				               public void onPrepareLoad(Drawable arg0) {
				               }
			               });
			draw(null);
		}

		final ShortcutListAdapter adapter = new ShortcutListAdapter(this, mShortcuts, R.layout.temp_listitem_shortcut_picker);
		mList.setAdapter(adapter);
	}

	public void draw(View view) {
		setActivityTitle(mShortcuts.get(0).app);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@SuppressWarnings("ucd")
	public void onListItemClick(View view) {
		Shortcut shortcut = (Shortcut) ((ViewHolder) view.getTag()).data;
		Patch.dispatch.shortcut(this, shortcut, mEntity, null, null);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected int getLayoutId() {
		return R.layout.link_picker;
	}

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected void onResume() {
		super.onResume();
		if (!isFinishing()) {
			bind(BindingMode.AUTO);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	static public class ShortcutListAdapter extends ArrayAdapter<Shortcut>
			implements Filterable {

		private final LayoutInflater mInflater;
		private       Integer        mItemLayoutId;
		private final List<Shortcut> mListItems;

		public ShortcutListAdapter(Context context, List<Shortcut> shortcuts, Integer itemLayoutId) {
			super(context, 0, shortcuts);

			mListItems = shortcuts;
			mInflater = LayoutInflater.from(context);

			if (itemLayoutId != null) {
				mItemLayoutId = itemLayoutId;
			}
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			final ViewHolder holder;
			final Shortcut itemData = mListItems.get(position);

			if (view == null) {
				view = mInflater.inflate(mItemLayoutId, null);
				holder = new ViewHolder();
				holder.photoView = (AirImageView) view.findViewById(R.id.entity_photo);
				holder.name = (TextView) view.findViewById(R.id.name);
				holder.appId = (TextView) view.findViewById(R.id.app_id);
				holder.appUrl = (TextView) view.findViewById(R.id.app_url);
				view.setTag(holder);
			}
			else {
				holder = (ViewHolder) view.getTag();
			}

			if (itemData != null) {
				final Shortcut shortcut = itemData;
				holder.data = shortcut;

				UI.setVisibility(holder.name, View.GONE);
				if (holder.name != null && shortcut.name != null && shortcut.name.length() > 0) {
					holder.name.setText(shortcut.name);
					setVisibility(holder.name, View.VISIBLE);
				}

				UI.setVisibility(holder.appId, View.GONE);
				if (holder.appId != null) {
					if (shortcut.appId != null && shortcut.appId.length() > 0 && !shortcut.app.equals(Constants.TYPE_APP_WEBSITE)) {
						holder.appId.setText(shortcut.appId);
						UI.setVisibility(holder.appId, View.VISIBLE);
					}
				}

				UI.setVisibility(holder.appUrl, View.GONE);
				if (holder.appUrl != null) {
					if (shortcut.appUrl != null && shortcut.appUrl.length() > 0) {
						holder.appUrl.setText(shortcut.appUrl);
						UI.setVisibility(holder.appUrl, View.VISIBLE);
					}
				}

				if (holder.photoView != null) {
					Photo photo = shortcut.getPhoto();
					if (holder.photoView.getPhoto() == null || !photo.getUri().equals(holder.photoView.getPhoto().getUri())) {
						holder.photoView.setTag(shortcut.getPhoto());
						UI.drawPhoto(holder.photoView, photo);
					}
				}
			}
			return view;
		}

		@Override
		public Shortcut getItem(int position) {
			return mListItems.get(position);
		}

		@Override
		public int getCount() {
			return mListItems.size();
		}

		@Override
		public boolean areAllItemsEnabled() {
			return false;
		}

		@Override
		public boolean isEnabled(int position) {
			return true;
		}

		private static void setVisibility(View view, Integer visibility) {
			if (view != null) {
				view.setVisibility(visibility);
			}
		}

		static class ViewHolder {
			private AirImageView photoView;
			private TextView     name;
			private TextView     appId;
			private TextView     appUrl;
			private Object       data;
		}
	}
}