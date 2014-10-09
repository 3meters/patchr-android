package com.aircandi.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ShareCompat;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.ServiceConstants;
import com.aircandi.components.EntityManager;
import com.aircandi.components.ModelResult;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.StringManager;
import com.aircandi.monitors.EntityMonitor;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Link.Direction;
import com.aircandi.objects.Route;
import com.aircandi.queries.WatchersQuery;
import com.aircandi.ui.EntityListFragment.ViewType;
import com.aircandi.ui.base.BaseActivity;
import com.aircandi.ui.base.BaseFragment;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.Integers;
import com.aircandi.utilities.Maps;
import com.aircandi.utilities.UI;

@SuppressWarnings("ucd")
public class WatcherList extends BaseActivity {

	private EntityListFragment mListFragment;

	@Override
	public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mEntityId = extras.getString(Constants.EXTRA_ENTITY_ID);
			mEntity = EntityManager.getCacheEntity(mEntityId);
		}
	}

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mListFragment = new WatcherListFragment();
		EntityMonitor monitor = new EntityMonitor(mEntityId);
		WatchersQuery query = new WatchersQuery();

		query.setEntityId(mEntityId)
		     .setLinkDirection(Direction.in.name())
		     .setLinkType(Constants.TYPE_LINK_WATCH)
		     .setPageSize(Integers.getInteger(R.integer.page_size_messages))
		     .setSchema(Constants.SCHEMA_ENTITY_USER);

		if (!mEntity.isOwnedByCurrentUser()) {
			if (!mEntity.ownerId.equals(ServiceConstants.ADMIN_USER_ID)) {
				query.setLinkWhere(Maps.asMap("enabled", true));
			}
		}

		mListFragment.setQuery(query)
		             .setMonitor(monitor)
		             .setListViewType(ViewType.LIST)
		             .setListLayoutResId(R.layout.watcher_list_fragment)
		             .setListLoadingResId(R.layout.temp_listitem_loading)
		             .setListItemResId(R.layout.temp_listitem_watcher)
		             .setListEmptyMessageResId(R.string.button_list_watchers_share)
		             .setListButtonMessageResId(R.string.button_list_watchers_share)
		             .setSelfBindingEnabled(true)
		             .setTitleResId(R.string.form_title_watchers)
		             .setButtonSpecialClickable(true);

		getFragmentManager().beginTransaction().add(R.id.fragment_holder, mListFragment).commit();
		draw(null);
	}

	@Override
	public void draw(View view) {
		setActivityTitle(StringManager.getString(((BaseFragment) mListFragment).getTitleResId()));
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@SuppressWarnings("ucd")
	public void onMoreButtonClick(View view) {
		mListFragment.onMoreButtonClick(view);
	}

	public void onShareButtonClick(View view) {
		Patchr.dispatch.route(this, Route.SHARE, mEntity, null, null);
	}

	@SuppressWarnings("ucd")
	public void onEnabledClick(View view) {
		Entity fromEntity = (Entity) view.getTag();
		CompoundButton enabled = (CompoundButton) view;
		enableLink(fromEntity, fromEntity.id, mEntity.id, enabled.isChecked());
	}

	@SuppressWarnings("ucd")
	public void onDeleteRequestClick(View view) {

		final Entity entity = (Entity) view.getTag();
		Integer titleResId = entity.enabled
		                     ? R.string.dialog_decline_approved_private_title
		                     : R.string.dialog_decline_requested_private_title;
		Integer messageResId = entity.enabled
		                       ? R.string.dialog_decline_approved_private_message
		                       : R.string.dialog_decline_requested_private_message;
		Integer okResId = entity.enabled
		                  ? R.string.dialog_decline_approved_private_ok
		                  : R.string.dialog_decline_requested_private_ok;
		Integer cancelResId = entity.enabled
		                      ? R.string.dialog_decline_approved_private_cancel
		                      : R.string.dialog_decline_requested_private_cancel;

		/* Confirm a decline since the user won't be able to undo */
		final AlertDialog declineDialog = Dialogs.alertDialog(R.drawable.ic_launcher
				, StringManager.getString(titleResId)
				, StringManager.getString(messageResId)
				, null
				, this
				, okResId
				, cancelResId
				, null
				, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (which == DialogInterface.BUTTON_POSITIVE) {
							/*
							 * Need to change this to delete the link
							 */
					enableLink(entity, entity.id, mEntity.id, false);
					dialog.dismiss();
				}
				else if (which == DialogInterface.BUTTON_NEGATIVE) {
					dialog.dismiss();
				}
			}
		}
				, null);

		declineDialog.setCanceledOnTouchOutside(false);
		declineDialog.show();
	}

	@SuppressWarnings("ucd")
	public void onLeaveRequestClick(View view) {
		Entity entity = (Entity) view.getTag();

		/* Warn when leaving a private place */
		if (mEntity.visibleToCurrentUser() && !mEntity.isOwnedByCurrentUser()) {

			final AlertDialog unwatchDialog = Dialogs.alertDialog(R.drawable.ic_launcher
					, StringManager.getString(R.string.dialog_unwatch_private_title)
					, StringManager.getString(R.string.dialog_unwatch_private_message)
					, null
					, this
					, R.string.dialog_unwatch_private_ok
					, R.string.dialog_unwatch_private_cancel
					, null
					, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (which == DialogInterface.BUTTON_POSITIVE) {
						//watch();
						dialog.dismiss();
					}
					else if (which == DialogInterface.BUTTON_NEGATIVE) {
						dialog.dismiss();
					}
				}
			}
					, null);

			unwatchDialog.setCanceledOnTouchOutside(false);
			unwatchDialog.show();
		}

		UI.showToastNotification(entity.id + " leaving", Toast.LENGTH_SHORT);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void share() {

		ShareCompat.IntentBuilder builder = ShareCompat.IntentBuilder.from(this);

		builder.setSubject(String.format(StringManager.getString(R.string.label_place_share_subject)
				, (mEntity.name != null) ? mEntity.name : "A"));

		builder.setType("text/plain");
		builder.setText(String.format(StringManager.getString(R.string.label_place_share_body), mEntityId));
		builder.setChooserTitle(String.format(StringManager.getString(R.string.label_place_share_title)
				, (mEntity.name != null) ? mEntity.name : StringManager.getString(R.string.container_singular_lowercase)));

		builder.getIntent().putExtra(Constants.EXTRA_SHARE_SOURCE, getPackageName());
		builder.getIntent().putExtra(Constants.EXTRA_SHARE_ID, mEntityId);
		builder.getIntent().putExtra(Constants.EXTRA_SHARE_SCHEMA, Constants.SCHEMA_ENTITY_PLACE);

		builder.startChooser();
	}

	protected void actionBarIcon() {
		if (mActionBar != null) {
			Drawable icon = getResources().getDrawable(R.drawable.img_users_dark);
			mActionBar.setIcon(icon);
		}
	}

	public void enableLink(final Entity entity, final String fromId, final String toId, final Boolean enabled) {

		final String actionEvent = "entity_watch_" + (enabled ? "approved" : "requested");

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncStatusUpdate");
				ModelResult result = Patchr.getInstance().getEntityManager().enabledLink(fromId, toId, Constants.TYPE_LINK_WATCH, enabled, actionEvent);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				if (isFinishing()) return;
				ModelResult result = (ModelResult) response;

				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					UI.showToastNotification("Status for " + entity.id + " set to: " + (enabled ? "approved" : "requested"), Toast.LENGTH_SHORT);
					entity.enabled = enabled;
				}
				else {
					Errors.handleError(WatcherList.this, result.serviceResponse);
				}
			}
		}.execute();
	}

	@Override
	protected int getLayoutId() {
		return R.layout.entity_list;
	}
}