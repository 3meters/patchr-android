package com.aircandi.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ShareCompat;
import android.view.View;
import android.widget.CompoundButton;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.ServiceConstants;
import com.aircandi.components.EntityManager;
import com.aircandi.components.ModelResult;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.StringManager;
import com.aircandi.events.ProcessingFinishedEvent;
import com.aircandi.monitors.EntityMonitor;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Link.Direction;
import com.aircandi.objects.Route;
import com.aircandi.objects.Shortcut;
import com.aircandi.queries.WatchersQuery;
import com.aircandi.ui.EntityListFragment.ViewType;
import com.aircandi.ui.base.BaseActivity;
import com.aircandi.ui.base.BaseFragment;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.Integers;
import com.aircandi.utilities.Maps;
import com.squareup.otto.Subscribe;

@SuppressWarnings("ucd")
public class WatcherList extends BaseActivity {

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

		mBubbleButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				onShareButtonClick(view);
			}
		});

		mCurrentFragment = new WatcherListFragment();
		EntityMonitor monitor = new EntityMonitor(mEntityId);
		WatchersQuery query = new WatchersQuery();

		query.setEntityId(mEntityId)
		     .setLinkDirection(Direction.in.name())
		     .setLinkType(Constants.TYPE_LINK_WATCH)
		     .setPageSize(Integers.getInteger(R.integer.page_size_messages))
		     .setSchema(Constants.SCHEMA_ENTITY_USER);

		if (!mEntity.isOwnedByCurrentUser()
				&& !mEntity.ownerId.equals(ServiceConstants.ADMIN_USER_ID)) {
			query.setLinkWhere(Maps.asMap("enabled", true));
		}

		((EntityListFragment) mCurrentFragment).setQuery(query)
		                                       .setMonitor(monitor)
		                                       .setListViewType(ViewType.LIST)
		                                       .setListLayoutResId(R.layout.watcher_list_fragment)
		                                       .setListLoadingResId(R.layout.temp_listitem_loading)
		                                       .setListItemResId(R.layout.temp_listitem_watcher)
		                                       .setListEmptyMessageResId(R.string.button_list_watchers_share)
		                                       .setBubbleButtonMessageResId(R.string.button_list_watchers_share)
		                                       .setSelfBindingEnabled(true)
		                                       .setTitleResId(R.string.form_title_watchers);

		getFragmentManager().beginTransaction().add(R.id.fragment_holder, mCurrentFragment).commit();
		draw(null);
	}

	@Override
	public void draw(View view) {
		setActivityTitle(StringManager.getString(((BaseFragment) mCurrentFragment).getTitleResId()));
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe
	public void onProcessingFinished(ProcessingFinishedEvent event) {

		mBusy.hideBusy(false);

		final EntityListFragment fragment = (EntityListFragment) mCurrentFragment;
		final Integer count = fragment.getAdapter().getCount();

		((BaseFragment) mCurrentFragment).onProcessingFinished();

		if (mBubbleButton.isEnabled()) {
			if (count == 0) {
				mBubbleButton.setText(fragment.getListEmptyMessageResId());
				mBubbleButton.fadeIn();
			}
			else {
				mBubbleButton.fadeOut();
			}
		}
	}

	@SuppressWarnings("ucd")
	public void onMoreButtonClick(View view) {
		((EntityListFragment) mCurrentFragment).onMoreButtonClick(view);
	}

	public void onShareButtonClick(View view) {
		Patchr.dispatch.route(this, Route.SHARE, mEntity, null, null);
	}

	@SuppressWarnings("ucd")
	public void onApprovedClick(View view) {
		Entity fromEntity = (Entity) view.getTag();
		Boolean approved = ((CompoundButton) view).isChecked();
		approveMember(fromEntity, fromEntity.linkId, fromEntity.id, mEntity.id, approved);
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
		final AlertDialog declineDialog = Dialogs.alertDialog(null
				, null
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
					deleteMember(entity.id);
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

	protected void setActionBarIcon() {
		if (mActionBar != null) {
			Drawable icon = getResources().getDrawable(R.drawable.ic_img_users_dark);
			mActionBar.setIcon(icon);
		}
	}

	public void approveMember(final Entity entity, final String linkId, final String fromId, final String toId, final Boolean enabled) {

		final String actionEvent = (enabled ? "approve" : "unapprove") + "_watch_entity";
		final Shortcut toShortcut = new Shortcut();
		toShortcut.schema = Constants.SCHEMA_ENTITY_PLACE;

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncStatusUpdate");
				ModelResult result = Patchr.getInstance().getEntityManager().insertLink(linkId
						, fromId
						, toId
						, Constants.TYPE_LINK_WATCH
						, enabled
						, null
						, toShortcut
						, actionEvent
						, true);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				if (isFinishing()) return;
				ModelResult result = (ModelResult) response;

				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					entity.enabled = enabled;
				}
				else {
					Errors.handleError(WatcherList.this, result.serviceResponse);
				}
			}
		}.execute();
	}

	public void deleteMember(final String fromId) {

		final String actionEvent = "declined_watch_entity";

		new AsyncTask() {

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncWatchEntity");
				ModelResult result = Patchr.getInstance().getEntityManager().deleteLink(fromId
						, mEntity.id
						, Constants.TYPE_LINK_WATCH
						, false
						, mEntity.schema
						, actionEvent);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				if (isFinishing()) return;
				ModelResult result = (ModelResult) response;
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					((EntityListFragment) mCurrentFragment).bind(BindingMode.AUTO);
				}
				else {
					if (result.serviceResponse.statusCodeService != null
							&& result.serviceResponse.statusCodeService != ServiceConstants.SERVICE_STATUS_CODE_FORBIDDEN_DUPLICATE) {
						Errors.handleError(WatcherList.this, result.serviceResponse);
					}
				}
			}
		}.execute();
	}

	@Override
	protected int getLayoutId() {
		return R.layout.watcher_list;
	}
}