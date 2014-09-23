package com.aircandi.catalina.ui.edit;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.aircandi.Aircandi;
import com.aircandi.catalina.Constants;
import com.aircandi.catalina.R;
import com.aircandi.catalina.objects.Message;
import com.aircandi.components.EntityManager;
import com.aircandi.components.ModelResult;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.StringManager;
import com.aircandi.controllers.IEntityController;
import com.aircandi.events.BeaconsLockedEvent;
import com.aircandi.events.CancelEvent;
import com.aircandi.events.QueryWifiScanReceivedEvent;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Link;
import com.aircandi.objects.Route;
import com.aircandi.objects.TransitionType;
import com.aircandi.ui.base.IBusy;
import com.aircandi.ui.components.EntitySuggestController;
import com.aircandi.ui.widgets.AirTokenCompleteTextView;
import com.aircandi.ui.widgets.TokenCompleteTextView;
import com.aircandi.ui.widgets.ToolTip;
import com.aircandi.ui.widgets.ToolTipRelativeLayout;
import com.aircandi.ui.widgets.ToolTipView;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.UI;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.List;

public class PlaceEdit extends com.aircandi.ui.edit.PlaceEdit implements TokenCompleteTextView.TokenListener {

	private AirTokenCompleteTextView mTo;
	private EntitySuggestController  mEntitySuggest;
	private List<Entity> mTos = new ArrayList<Entity>();
	protected ToolTipRelativeLayout mTooltips;

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mTooltips = (ToolTipRelativeLayout) findViewById(R.id.tooltips);
		if (mTooltips != null) {
			mTooltips.setSingleShot(Constants.TOOLTIPS_PLACE_EDIT_ID);
		}

		mTo = (AirTokenCompleteTextView) findViewById(com.aircandi.R.id.to);
		if (mTo != null) {
			mTo.setLineSpacing((int) UI.getRawPixelsForDisplayPixels(5f), 1f);
			mTo.setTokenLayoutResId(com.aircandi.R.layout.widget_token_view);
			mTo.setOnFocusChangeListener(new View.OnFocusChangeListener() {
				@Override
				public void onFocusChange(View v, boolean hasFocus) {
					if (hasFocus) {
						UI.showSoftInput(mTo);
					}
				}
			});
			mEntitySuggest = new EntitySuggestController(this)
					.setInput(mTo)
					.setTokenListener(this)
					.setSuggestScope(EntityManager.SuggestScope.USERS);
			mEntitySuggest.init();
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe
	public void onQueryWifiScanReceived(final QueryWifiScanReceivedEvent event) {
		super.onQueryWifiScanReceived(event);
	}

	@Subscribe
	public void onBeaconsLocked(BeaconsLockedEvent event) {
		super.onBeaconsLocked(event);
	}

	@Subscribe
	public void onCancelEvent(CancelEvent event) {
		if (mTaskService != null) {
			mTaskService.cancel(true);
		}
	}

	@Override
	public void onTokenAdded(Object o) {
		if (!mTos.contains((Entity) o)) {
			mTos.add((Entity) o);
			mDirty = true;
		}
	}

	@Override
	public void onTokenRemoved(Object o) {
		if (mTos.contains((Entity) o)) {
			mTos.remove((Entity) o);
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (mTooltips != null) {
			View view = findViewById(R.id.tooltips);
			view.post(new Runnable() {
				@Override
				public void run() {
					showTooltips();
				}
			});
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected boolean afterInsert() {
	    /*
	     * Only called if the insert was successful. Called on main ui thread.
         * We link replies to the places they are associated with. This give us the option
		 * to thread, flatten or do some combo. Called on background thread.
		 */
		if (!mEditing) {
			if (mTos.size() > 0) {

				mInsertedResId = R.string.alert_saved_and_shared;

				new AsyncTask() {

					@Override
					protected void onPreExecute() {
						mBusy.hideBusy(false);
						mBusy.showBusy(IBusy.BusyAction.ActionWithMessage, mInsertProgressResId);
					}

					@Override
					protected Object doInBackground(Object... params) {
						Thread.currentThread().setName("AsyncAutoSharePlace");

                    /* Create message entity */
						IEntityController controller = Aircandi.getInstance().getControllerForSchema(Constants.SCHEMA_ENTITY_MESSAGE);
						Entity message = controller.makeNew();
						message.description = String.format(StringManager.getString(R.string.label_place_share_body_self_oncreate), mEntity.name);
						message.type = Message.MessageType.SHARE;
						if (Aircandi.getInstance().getCurrentUser() != null) {
							message.creator = Aircandi.getInstance().getCurrentUser();
							message.creatorId = Aircandi.getInstance().getCurrentUser().id;
						}

                    /* Links */
						List<Link> links = new ArrayList<Link>();
						links.add(new Link(mEntity.id, Constants.TYPE_LINK_SHARE, Constants.SCHEMA_ENTITY_PLACE));
						for (Entity to : mTos) {
							links.add(new Link(to.id, Constants.TYPE_LINK_SHARE, Constants.SCHEMA_ENTITY_USER));
						}
						final ModelResult result = Aircandi.getInstance().getEntityManager().insertEntity(message, links, null, null, null, true);
						return result;
					}

					@Override
					protected void onPostExecute(Object response) {
						final ModelResult result = (ModelResult) response;
						mBusy.hideBusy(false);

						if (result.serviceResponse.responseCode == NetworkManager.ResponseCode.SUCCESS) {
							UI.showToastNotification(StringManager.getString(mInsertedResId), Toast.LENGTH_SHORT);
							setResultCode(Activity.RESULT_OK);
							finish();
							Aircandi.getInstance().getAnimationManager().doOverridePendingTransition(PlaceEdit.this, TransitionType.FORM_TO_PAGE);
						}
						else {
							Errors.handleError(PlaceEdit.this, result.serviceResponse);
						}
					}
				}.execute();

				return false; // Tells caller that we will handle finishing
			}
			else {
				Aircandi.dispatch.route(this, Route.BROWSE, mEntity, null, null);
			}
		}

		return true;
	}

	public void showTooltips() {

		if (mTooltips != null && !mTooltips.hasShot()) {
			mTooltips.setClickable(true);
			mTooltips.setVisibility(View.VISIBLE);
			mTooltips.clear();
			mTooltips.requestLayout();

			ToolTipView part1 = mTooltips.showTooltip(new ToolTip()
					.withText(StringManager.getString(R.string.tooltip_place_new_part1))
					.withShadow(true)
					.withArrow(false)
					.setMaxWidth(UI.getRawPixelsForDisplayPixels(250f))
					.withAnimationType(ToolTip.AnimationType.FROM_SELF));
			part1.setId(View.generateViewId());
			part1.addRule(RelativeLayout.CENTER_HORIZONTAL);

			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) part1.getLayoutParams();
			params.setMargins(0, UI.getRawPixelsForDisplayPixels(40f), 0, 0);
			part1.setLayoutParams(params);

			ToolTipView part2 = mTooltips.showTooltip(new ToolTip()
					.withText(StringManager.getString(R.string.tooltip_place_new_part2))
					.withShadow(true)
					.withArrow(false)
					.setMaxWidth(UI.getRawPixelsForDisplayPixels(250f))
					.withAnimationType(ToolTip.AnimationType.FROM_SELF));
			part2.setId(View.generateViewId());
			part2.setMinimumWidth(UI.getRawPixelsForDisplayPixels(250f));
			part2.addRule(RelativeLayout.CENTER_HORIZONTAL);
			part2.addRule(RelativeLayout.BELOW, part1.getId());

			ToolTipView part3 = mTooltips.showTooltip(new ToolTip()
					.withText(StringManager.getString(R.string.tooltip_place_new_part3))
					.withShadow(true)
					.withArrow(false)
					.setMaxWidth(UI.getRawPixelsForDisplayPixels(250f))
					.withAnimationType(ToolTip.AnimationType.FROM_SELF));
			part3.addRule(RelativeLayout.CENTER_HORIZONTAL);
			part3.addRule(RelativeLayout.BELOW, part2.getId());
		}
	}

 	/*--------------------------------------------------------------------------------------------
	 * Menus
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		if (mTooltips != null) {
			View view = findViewById(R.id.tooltips);
			view.post(new Runnable() {
				@Override
				public void run() {
					showTooltips();
				}
			});
		}
		return result;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (mTooltips != null) {
			mTooltips.hide(false);
		}
		return super.onOptionsItemSelected(item);
	}
}
