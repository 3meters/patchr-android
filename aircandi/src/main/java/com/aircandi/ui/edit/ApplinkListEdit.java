package com.aircandi.ui.edit;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.ServiceConstants;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.StringManager;
import com.aircandi.objects.Applink;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Route;
import com.aircandi.ui.base.BaseEntityListEdit;
import com.aircandi.ui.base.IBusy.BusyAction;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.utilities.Json;
import com.aircandi.utilities.Type;
import com.aircandi.utilities.UI;

public class ApplinkListEdit extends BaseEntityListEdit {

	protected Boolean	mHelpDisplayed	= false;

	@Override
	public void initialize(Bundle savedInstanceState) {
		mListItemResId = R.layout.temp_listitem_applink_edit;
		mListNewMessageResId = R.string.button_list_new_applink;
		mListNewEnabled = true;
		super.initialize(savedInstanceState);
	}

	@Override
	protected ArrayAdapter getAdapter() {
		return new ListAdapter(this, mEntities, mListItemResId);
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@Override
	public void onRefresh() {
		refreshApplinks(mEntities);
	}

	@Override
	public void onHelp() {
		Bundle extras = new Bundle();
		extras.putInt(Constants.EXTRA_HELP_ID, R.layout.applink_list_help);
		Aircandi.dispatch.route(this, Route.HELP, null, null, extras);
	}

	@SuppressWarnings("ucd")
	public void onSearchLinksButtonClick(View view) {
		searchApplinks(mEntities, true, mEntityId, true);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {

		if (resultCode != Activity.RESULT_CANCELED) {
			if (requestCode == Constants.ACTIVITY_ENTITY_EDIT || requestCode == Constants.ACTIVITY_ENTITY_INSERT) {
				if (intent != null && intent.getExtras() != null) {

					final Bundle extras = intent.getExtras();
					List<Entity> entities = new ArrayList<Entity>();

					/* Deserialize */
					final List<String> jsonEntities = extras.getStringArrayList(Constants.EXTRA_ENTITIES);
					if (jsonEntities != null) {
						for (String jsonEntity : jsonEntities) {
							Entity entity = (Applink) Json.jsonToObject(jsonEntity, Json.ObjectType.ENTITY);
							entities.add(entity);
						}
					}

					if (requestCode == Constants.ACTIVITY_ENTITY_EDIT) {

						/* Replace the edited applink */
						for (Entity updated : entities) {
							Boolean replaced = false;
							for (Entity entity : mEntities) {
								if (entity.editing && updated.id.equals(entity.id)) {
									replaced = true;
									entity.editing = false;
									mEntities.set(mEntities.indexOf(entity), updated);
									entities.remove(updated);
									break;
								}
							}
							if (replaced) {
								break;
							}
						}
					}
					else if (requestCode == Constants.ACTIVITY_ENTITY_INSERT) {

						/* Add the inserted entity and any extra friends we don't already have */
						for (Entity inserted : entities) {
							Boolean found = false;
							for (Entity entity : mEntities) {
								if (Type.equal(entity.id, inserted.id)) {
									found = true;
									break;
								}
								if (inserted instanceof Applink) {
									Applink applink = (Applink) entity;
									Applink applinkCandidate = (Applink) inserted;
									if (Type.equal(applink.appUrl, applinkCandidate.appUrl)) {
										found = true;
										break;
									}
								}
							}

							if (!found) {
								inserted.checked = false;
								mEntities.add(inserted);
							}
						}
					}

					mDirty = true;
					rebuildPositions();
					mAdapter.notifyDataSetChanged();
					if (requestCode == Constants.ACTIVITY_ENTITY_INSERT) {
						scrollToBottom();
					}

					/* Did we get back extra entities? */
					if (entities.size() > ((requestCode == Constants.ACTIVITY_ENTITY_INSERT) ? 1 : 0)) {
						UI.showToastNotification(StringManager.getString((entities.size() == 1)
								? R.string.alert_applinks_linked
								: R.string.alert_applinks_linked), Toast.LENGTH_SHORT);
					}
				}
			}
		}
	}

	@Override
	public void onActivityComplete() {
		if (mEntities.size() == 0 && !mHelpDisplayed) {
			mHelpDisplayed = true;
			Aircandi.dispatch.route(this, Route.HELP, null, null, null);
		}
		super.onActivityComplete();
	}

	// --------------------------------------------------------------------------------------------
	// Services
	// --------------------------------------------------------------------------------------------

	private void searchApplinks(final List<Entity> applinks, final Boolean autoInsert, final String placeId, final Boolean userInitiated) {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mBusy.showBusy(BusyAction.ActionWithMessage, R.string.progress_applink_search);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncApplinkSearch");
				ModelResult result = Aircandi.getInstance().getEntityManager().searchApplinks(applinks, placeId, ServiceConstants.TIMEOUT_APPLINK_SEARCH, true);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;
				mBusy.hideBusy(false);
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					final List<Entity> applinks = (List<Entity>) result.data;
					/*
					 * Make sure they have the schema property set
					 */
					for (Entity applink : applinks) {
						if (applink.schema == null) {
							applink.schema = Constants.SCHEMA_ENTITY_APPLINK;
						}
					}

					if (autoInsert) {
						if (applinks.size() > 0) {

							int activeCountOld = mEntities.size();
							int activeCountNew = applinks.size();

							/*
							 * We assume the call to search included our current applinks and those
							 * are included in the results.
							 */
							mEntities.clear();
							mEntities.addAll(applinks);

							for (Entity entity : mEntities) {
								entity.checked = false;
							}

							rebuildPositions();
							mAdapter.notifyDataSetChanged();

							if (activeCountNew == activeCountOld) {
								if (userInitiated) {
									UI.showToastNotification(StringManager.getString(R.string.alert_applinks_no_links), Toast.LENGTH_SHORT);
								}
							}
							else {
								mDirty = true;
								UI.showToastNotification(StringManager.getString((applinks.size() == 1)
										? R.string.alert_applinks_linked
										: R.string.alert_applinks_linked), Toast.LENGTH_SHORT);
							}
						}

					}
				}
			}
		}.execute();
	}

	private void refreshApplinks(final List<Entity> applinks) {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mBusy.showBusy(BusyAction.ActionWithMessage, R.string.progress_applink_refresh);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncApplinkRefresh");
				List<Entity> entities = new ArrayList<Entity>();
				for (Entity applink : applinks) {
					if (applink.id != null) {
						entities.add(applink);
					}
				}
				ModelResult result = Aircandi.getInstance().getEntityManager().refreshApplinks(entities, ServiceConstants.TIMEOUT_APPLINK_REFRESH, true);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;
				mBusy.hideBusy(false);
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					final List<Entity> applinks = (List<Entity>) result.data;

					if (applinks.size() > 0) {
						for (Entity entity : mEntities) {
							for (Entity applink : applinks) {
								if (applink.id != null && entity.id != null && applink.id.equals(entity.id)) {
									mDirty = true;
									entity.name = applink.name;
									entity.description = applink.description;
									if (applink.photo != null) {
										entity.photo = applink.photo.clone();
									}
									entity.data = applink.data;
									((Applink) entity).appId = ((Applink) applink).appId;
									((Applink) entity).appUrl = ((Applink) applink).appUrl;
								}
							}
						}
						mAdapter.notifyDataSetChanged();
						UI.showToastNotification(StringManager.getString(R.string.alert_applinks_refreshed), Toast.LENGTH_SHORT);
					}
				}
			}
		}.execute();
	}

	// --------------------------------------------------------------------------------------------
	// Menus
	// --------------------------------------------------------------------------------------------	

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.applink_list_edit;
	}

	// --------------------------------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------------------------------

	private static class ListAdapter extends EntityListAdapter {

		public ListAdapter(Context context, List<Entity> entities, Integer itemLayoutId) {
			super(context, entities, itemLayoutId);
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
				holder.appId = (TextView) view.findViewById(R.id.app_id);
				holder.appUrl = (TextView) view.findViewById(R.id.app_url);
				holder.type = (TextView) view.findViewById(R.id.type);
				holder.alert = (TextView) view.findViewById(R.id.alert);
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
				final Applink applink = (Applink) entity;

				UI.setVisibility(holder.checked, View.GONE);
				if (holder.checked != null && applink.checked != null) {
					holder.checked.setChecked(applink.checked);
					holder.checked.setTag(applink);
					UI.setVisibility(holder.checked, View.VISIBLE);
				}

				UI.setVisibility(holder.type, View.GONE);
				if (holder.type != null && applink.type != null && applink.type.length() > 0) {
					holder.type.setText(applink.type);
					UI.setVisibility(holder.type, View.VISIBLE);
				}

				UI.setVisibility(holder.name, View.GONE);
				if (holder.name != null && applink.name != null && applink.name.length() > 0) {
					holder.name.setText(applink.name);
					UI.setVisibility(holder.name, View.VISIBLE);
				}

				UI.setVisibility(holder.appId, View.GONE);
				if (holder.appId != null) {
					if (applink.appId != null && applink.appId.length() > 0 && !applink.type.equals(Constants.TYPE_APP_WEBSITE)) {
						holder.appId.setText(applink.appId);
						UI.setVisibility(holder.appId, View.VISIBLE);
					}
				}

				UI.setVisibility(holder.appUrl, View.GONE);
				if (holder.appUrl != null) {
					if (applink.appUrl != null && applink.appUrl.length() > 0) {
						holder.appUrl.setText(applink.appUrl);
						UI.setVisibility(holder.appUrl, View.VISIBLE);
					}
				}

				UI.setVisibility(holder.alert, View.GONE);
				if (holder.alert != null) {
					if (applink.validatedDate != null && applink.validatedDate.longValue() == -1) {
						holder.alert.setText(StringManager.getString(R.string.alert_applink_broken));
						UI.setVisibility(holder.alert, View.VISIBLE);
					}
				}

				if (holder.photoView != null) {
					holder.photoView.setTag(applink.getPhoto());
					UI.drawPhoto(holder.photoView, applink.getPhoto());
				}
			}
			return view;
		}

		private static class ViewHolder {
			private AirImageView	photoView;
			private TextView		name;
			private TextView		appId;
			private TextView		alert;
			private TextView		appUrl;
			private TextView		type;
			private CheckBox		checked;
		}
	}
}