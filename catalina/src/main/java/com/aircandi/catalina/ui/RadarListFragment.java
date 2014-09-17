package com.aircandi.catalina.ui;

import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;

import com.aircandi.catalina.R;
import com.aircandi.components.LocationManager;
import com.aircandi.components.StringManager;
import com.aircandi.events.BeaconsLockedEvent;
import com.aircandi.events.EntitiesByProximityFinishedEvent;
import com.aircandi.events.EntitiesChangedEvent;
import com.aircandi.events.MonitoringWifiScanReceivedEvent;
import com.aircandi.events.PlacesNearLocationFinishedEvent;
import com.aircandi.events.QueryWifiScanReceivedEvent;
import com.aircandi.ui.base.BaseActivity;
import com.aircandi.ui.widgets.ToolTip;
import com.aircandi.ui.widgets.ToolTipRelativeLayout;
import com.aircandi.ui.widgets.ToolTipView;
import com.aircandi.utilities.Colors;
import com.aircandi.utilities.UI;
import com.squareup.otto.Subscribe;

public class RadarListFragment extends com.aircandi.ui.RadarListFragment {

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe
	@Override
	public void onQueryWifiScanReceived(QueryWifiScanReceivedEvent event) {
		super.onQueryWifiScanReceived(event);
	}

	@Subscribe
	@Override
	public void onBeaconsLocked(BeaconsLockedEvent event) {
		super.onBeaconsLocked(event);
	}

	@Subscribe
	@Override
	public void onEntitiesByProximityFinished(EntitiesByProximityFinishedEvent event) {
		super.onEntitiesByProximityFinished(event);
	}

	@Subscribe
	@Override
	public void onPlacesNearLocationFinished(PlacesNearLocationFinishedEvent event) {
		super.onPlacesNearLocationFinished(event);
	}

	@Subscribe
	@Override
	public void onEntitiesChanged(EntitiesChangedEvent event) {
		super.onEntitiesChanged(event);
	}

	@Subscribe
	@Override
	public void onMonitoringWifiScanReceived(MonitoringWifiScanReceivedEvent event) {
		super.onMonitoringWifiScanReceived(event);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (mEntities.size() > 0 || LocationManager.getInstance().getLocationLocked() != null) {
			MenuItem menuItem = ((BaseActivity) getActivity()).getMenu().findItem(R.id.search);
			final View searchView = menuItem.getActionView();
			searchView.post(new Runnable() {
				@Override
				public void run() {
					showTooltips(true);
				}
			});
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void showTooltips(boolean force) {

		ToolTipRelativeLayout tooltipLayer = ((AircandiForm) getActivity()).mTooltips;

		if ((force || tooltipLayer.getVisibility() != View.VISIBLE) && !tooltipLayer.hasShot()) {
			tooltipLayer.setClickable(true);
			tooltipLayer.setVisibility(View.VISIBLE);
			tooltipLayer.clear();
			tooltipLayer.requestLayout();

			ToolTipView tooltipView = tooltipLayer.showTooltip(new ToolTip()
					.withText(StringManager.getString(R.string.tooltip_list_nearby))
					.withShadow(true)
					.withArrow(false)
					.setMaxWidth(UI.getRawPixelsForDisplayPixels(250f))
					.withAnimationType(ToolTip.AnimationType.FROM_SELF));

			tooltipView.addRule(RelativeLayout.CENTER_IN_PARENT);

			View searchView = getActivity().getWindow().getDecorView().findViewById(R.id.new_place);
			if (searchView != null) {
				tooltipLayer.showTooltipForView(new ToolTip()
						.withText(StringManager.getString(R.string.tooltip_action_item_place_new))
						.withShadow(true)
						.withArrow(true)
						.setMaxWidth(UI.getRawPixelsForDisplayPixels(120f))
						.withAnimationType(ToolTip.AnimationType.FROM_TOP), searchView);
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Menus
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onCreateOptionsMenu(final Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		/*
		 * SUPER HACK: runnable will only be posted once the view for search has
		 * gone through measure/layout. That's when it's safe to process tooltips
		 * for action bar items.
		 */
//		MenuItem menuItem = menu.findItem(R.id.search);
//		final View searchView = menuItem.getActionView();
//		searchView.post(new Runnable() {
//			@Override
//			public void run() {
//				showTooltips();
//			}
//		});
	}

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/
}
