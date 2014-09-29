package com.aircandi.ui.base;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.aircandi.Patch;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.StringManager;
import com.aircandi.interfaces.IBind;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Photo;
import com.aircandi.objects.Route;
import com.aircandi.objects.TransitionType;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Json;
import com.aircandi.utilities.UI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class BaseEntityListEdit extends BaseEdit implements IBind {

	protected ListView mList;
	protected Integer  mListItemResId;
	protected Integer  mListNewMessageResId;
	protected Boolean mListNewEnabled = false;
	protected Entity mEntityEditing;

	/* Inputs */
	protected String mEntityId;
	protected List<Entity> mEntities = new ArrayList<Entity>();
	protected String       mListSchema;
	protected ArrayAdapter mAdapter;

	@Override
	public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			final List<String> jsonEntities = extras.getStringArrayList(Constants.EXTRA_ENTITIES);
			if (jsonEntities != null) {
				for (String jsonEntity : jsonEntities) {
					Entity entity = (Entity) Json.jsonToObject(jsonEntity, Json.ObjectType.ENTITY);
					mEntities.add(entity);
				}
			}
			mEntityId = extras.getString(Constants.EXTRA_ENTITY_ID);
			mListSchema = extras.getString(Constants.EXTRA_LIST_LINK_SCHEMA);
		}
	}

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
		mList = (ListView) findViewById(R.id.list);
		if (mButtonSpecial != null) {
			mButtonSpecial.setText(StringManager.getString(mListNewMessageResId));
		}
	}

	@Override
	public void bind(BindingMode mode) {
		/*
		 * Before entities are customized, they have no position and are
		 * sorted by the modified date on the link. Once entity customization
		 * is saved to the service, the position field has been set on the set of
		 * entities.
		 */
		if (mAdapter == null) {
			Collections.sort(mEntities, new Entity.SortByPositionSortDate());

			if (mEntities.size() != 0) {
				Integer position = 0;
				for (Entity entity : mEntities) {
					entity.checked = false;
					entity.position = position;
					position++;
				}
			}

			mAdapter = getAdapter();
			mList.setAdapter(mAdapter);  // triggers draw
		}
		onActivityComplete();
	}

	protected ArrayAdapter getAdapter() {
		return null;
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/
	public void onActivityComplete() {
		if (mListNewEnabled) {
			showButtonSpecial(mEntities.size() == 0, StringManager.getString(R.string.button_list_new_applink));
		}
	}

	@SuppressWarnings("ucd")
	public void onNewEntityButtonClick(View view) {
		onAdd(new Bundle());
	}

	@Override
	public void onAccept() {
		if (isDirty()) {
			if (validate()) {

				/* Pull all the control values back into the entity object */
				final Intent intent = new Intent();
				final List<String> jsonEntities = new ArrayList<String>();

				for (Entity entity : mEntities) {
					jsonEntities.add(Json.objectToJson(entity, Json.UseAnnotations.FALSE, Json.ExcludeNulls.TRUE));
				}

				intent.putStringArrayListExtra(Constants.EXTRA_ENTITIES, (ArrayList<String>) jsonEntities);
				setResultCode(Activity.RESULT_OK, intent);
				finish();
				Patch.getInstance().getAnimationManager().doOverridePendingTransition(this, TransitionType.FORM_TO_PAGE);
			}
		}
		else {
			finish();
			Patch.getInstance().getAnimationManager().doOverridePendingTransition(BaseEntityListEdit.this, TransitionType.FORM_TO_PAGE);
		}
	}

	@SuppressWarnings("ucd")
	public void onCheckedClick(View view) {
		CheckBox check = (CheckBox) view.findViewById(R.id.checked);
		check.setChecked(!check.isChecked());
		final Entity entity = (Entity) check.getTag();
		entity.checked = check.isChecked();
	}

	@SuppressWarnings("ucd")
	public void onDeleteButtonClick(View view) {
		for (int i = mEntities.size() - 1; i >= 0; i--) {
			if (mEntities.get(i).checked) {
				confirmDelete();
				return;
			}
		}
	}

	@SuppressWarnings("ucd")
	public void onAddButtonClick(View view) {
		onAdd(new Bundle());
	}

	@Override
	public void onAdd(Bundle extras) {
		/*
		 * No permissions check because the user can't get here without the right
		 * permissions.
		 */
		extras.putString(Constants.EXTRA_ENTITY_PARENT_ID, mEntityId);
		extras.putBoolean(Constants.EXTRA_SKIP_SAVE, true);
		extras.putString(Constants.EXTRA_ENTITY_SCHEMA, mListSchema);
		super.onAdd(extras);
	}

	@SuppressWarnings("ucd")
	public void onMoveUpButtonClick(View view) {
		Boolean scrolled = false;
		for (int i = 0; i < mEntities.size(); i++) {
			if (i > 0) {
				if (mEntities.get(i).checked) {
					Entity hold = mEntities.get(i - 1);
					mEntities.set(i - 1, mEntities.get(i));
					mEntities.set(i, hold);
					if (!scrolled && mList.getFirstVisiblePosition() >= (i - 1)) {
						mList.smoothScrollToPosition(i - 1);
						scrolled = true;
					}
				}
			}
		}
		mDirty = true;
		rebuildPositions();
		mAdapter.notifyDataSetChanged();
	}

	@SuppressWarnings("ucd")
	public void onMoveDownButtonClick(View view) {
		Boolean scrolled = false;
		for (int i = mEntities.size() - 1; i >= 0; i--) {
			if (i < (mEntities.size() - 1)) {
				if (mEntities.get(i).checked) {
					Entity hold = mEntities.get(i + 1);
					mEntities.set(i + 1, mEntities.get(i));
					mEntities.set(i, hold);
					if (!scrolled && mList.getLastVisiblePosition() <= (i + 1)) {
						mList.smoothScrollToPosition(i + 1);
						scrolled = true;
					}
				}
			}
		}
		mDirty = true;
		rebuildPositions();
		mAdapter.notifyDataSetChanged();
	}

	@SuppressWarnings("ucd")
	public void onListItemClick(View view) {
		final CheckBox check = (CheckBox) ((View) view.getParent()).findViewById(R.id.checked);
		mEntityEditing = (Entity) check.getTag();
		mEntityEditing.editing = true;
		Bundle extras = new Bundle();
		extras.putBoolean(Constants.EXTRA_SKIP_SAVE, true);
		Patch.dispatch.route(this, Route.EDIT, mEntityEditing, null, extras);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);

		if (resultCode != Activity.RESULT_CANCELED) {
			if (requestCode == Constants.ACTIVITY_ENTITY_EDIT) {
				if (intent != null && intent.getExtras() != null) {

					final Bundle extras = intent.getExtras();
					final String jsonEntity = extras.getString(Constants.EXTRA_ENTITY);

					if (jsonEntity != null) {
						final Entity entityUpdated = (Entity) Json.jsonToObject(jsonEntity, Json.ObjectType.ENTITY);
						if (entityUpdated != null) {
							for (Entity entity : mEntities) {
								if (entity.editing) {
									entity.editing = false;
									mEntities.set(mEntities.indexOf(entity), entityUpdated);
									break;
								}
							}
							mDirty = true;
							mAdapter.notifyDataSetChanged();
						}
					}
				}
			}
			else if (requestCode == Constants.ACTIVITY_ENTITY_INSERT) {
				if (intent != null && intent.getExtras() != null) {

					final Bundle extras = intent.getExtras();
					final String jsonEntity = extras.getString(Constants.EXTRA_ENTITY);

					if (jsonEntity != null) {
						final Entity entityNew = (Entity) Json.jsonToObject(jsonEntity, Json.ObjectType.ENTITY);
						if (entityNew != null) {
							entityNew.checked = false;
							mEntities.add(entityNew);

							mDirty = true;
							rebuildPositions();
							mAdapter.notifyDataSetChanged();
							scrollToBottom();
						}
					}
				}
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/
	@Override
	public void confirmDelete() {

		/* How many are we deleting? */
		Integer deleteCount = 0;
		for (int i = mEntities.size() - 1; i >= 0; i--) {
			if (mEntities.get(i).checked) {
				deleteCount++;
			}
		}

		final LayoutInflater inflater = LayoutInflater.from(this);
		final ViewGroup customView = (ViewGroup) inflater.inflate(R.layout.dialog_applinks_delete, null);
		final TextView message = (TextView) customView.findViewById(R.id.dialog_message);
		final LinearLayout list = (LinearLayout) customView.findViewById(R.id.dialog_list);
		for (Entity entity : mEntities) {
			if (entity.checked) {
				View sourceView = inflater.inflate(R.layout.temp_listitem_applink_delete, null);

				TextView name = (TextView) sourceView.findViewById(R.id.name);
				AirImageView photoView = (AirImageView) sourceView.findViewById(R.id.entity_photo);

				if (entity.name != null) {
					name.setText(entity.name);
				}
				Photo photo = entity.getPhoto();
				photoView.setTag(photo);
				UI.drawPhoto(photoView, photo);

				list.addView(sourceView);
			}
		}

		message.setText((deleteCount == 1)
		                ? R.string.alert_delete_applink_message_single
		                : R.string.alert_delete_applink_message_multiple);

		final AlertDialog dialog = Dialogs.alertDialog(null
				, StringManager.getString(R.string.alert_delete_applink_title)
				, null
				, customView
				, this
				, android.R.string.ok
				, android.R.string.cancel
				, null
				, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (which == DialogInterface.BUTTON_POSITIVE) {
					for (int i = mEntities.size() - 1; i >= 0; i--) {
						if (mEntities.get(i).checked) {
							Entity removed = mEntities.remove(i);
							mAdapter.remove(removed);
						}
					}
					mDirty = true;
					mAdapter.notifyDataSetChanged();
				}
			}
		}
				, null);
		dialog.setCanceledOnTouchOutside(false);
	}

	protected void scrollToBottom() {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				mList.setSelection(mList.getAdapter().getCount() - 1);
			}
		});
	}

	@Override
	protected void insert() {
	}

	@Override
	protected void update() {
	}

	@Override
	protected void delete() {
	}

	protected void rebuildPositions() {
		Integer position = 0;
		for (Entity entity : mEntities) {
			entity.position = position;
			position++;
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/
	@Override
	protected void onResume() {
		super.onResume();
		bind(BindingMode.AUTO);
	}

	/*--------------------------------------------------------------------------------------------
	 * Menus
	 *--------------------------------------------------------------------------------------------*/ 	/*--------------------------------------------------------------------------------------------
	 * Misc
	 *--------------------------------------------------------------------------------------------*/ 	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/    protected abstract static class EntityListAdapter extends ArrayAdapter<Entity> implements Filterable {

		protected       Integer      mItemLayoutId;
		protected final List<Entity> mListItems;
		protected       Context      mContext;

		protected EntityListAdapter(Context context, List<Entity> entities, Integer itemLayoutId) {
			super(context, 0, entities);

			mListItems = entities;
			mContext = context;

			if (itemLayoutId != null) {
				mItemLayoutId = itemLayoutId;
			}
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			final ViewHolder holder;
			final Entity entity = mListItems.get(position);

			if (view == null) {
				view = LayoutInflater.from(mContext).inflate(mItemLayoutId, null);
				holder = new ViewHolder();
				holder.photoView = (AirImageView) view.findViewById(R.id.entity_photo);
				holder.name = (TextView) view.findViewById(R.id.name);
				holder.subtitle = (TextView) view.findViewById(R.id.subtitle);
				holder.description = (TextView) view.findViewById(R.id.description);
				holder.checked = (CheckBox) view.findViewById(R.id.checked);
				if (holder.checked != null) {
					holder.checked.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View view) {
							final CheckBox checkBox = (CheckBox) view;
							final Entity entity = (Entity) checkBox.getTag();
							entity.checked = checkBox.isChecked();
						}
					});
				}
				view.setTag(holder);
			}
			else {
				holder = (ViewHolder) view.getTag();
			}

			if (entity != null) {

				setVisibility(holder.checked, View.GONE);
				if (holder.checked != null && entity.checked != null) {
					holder.checked.setChecked(entity.checked);
					holder.checked.setTag(entity);
					setVisibility(holder.checked, View.VISIBLE);
				}

				setVisibility(holder.name, View.GONE);
				if (holder.name != null && entity.name != null && entity.name.length() > 0) {
					holder.name.setText(entity.name);
					setVisibility(holder.name, View.VISIBLE);
				}

				setVisibility(holder.subtitle, View.GONE);
				if (holder.subtitle != null) {
					if (entity.subtitle != null && entity.subtitle.length() > 0) {
						holder.subtitle.setText(entity.subtitle);
						setVisibility(holder.subtitle, View.VISIBLE);
					}
				}

				setVisibility(holder.description, View.GONE);
				if (holder.description != null) {
					if (entity.description != null && entity.description.length() > 0) {
						holder.description.setText(entity.description);
						setVisibility(holder.description, View.VISIBLE);
					}
				}

				if (holder.photoView != null) {
					holder.photoView.setTag(entity.getPhoto());
					UI.drawPhoto(holder.photoView, entity.getPhoto());
				}
			}
			return view;
		}

		@Override
		public Entity getItem(int position) {
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

		private static class ViewHolder {
			private AirImageView photoView;
			private TextView     name;
			private TextView     subtitle;
			private TextView     description;
			private CheckBox     checked;
		}
	}
}