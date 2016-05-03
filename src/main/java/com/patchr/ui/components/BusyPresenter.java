package com.patchr.ui.components;

import android.animation.ObjectAnimator;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.BadTokenException;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.components.Dispatcher;
import com.patchr.components.Logger;
import com.patchr.components.StringManager;
import com.patchr.events.ProcessingCanceledEvent;
import com.patchr.events.ProcessingProgressEvent;
import com.patchr.ui.BaseScreen;
import com.patchr.ui.widgets.AirProgressBar;
import com.patchr.utilities.DateTime;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Reporting;

import org.greenrobot.eventbus.Subscribe;

public class BusyPresenter {

	private Runnable           runnableHide;
	private Runnable           runnableShow;
	private Long               busyStartedTime;
	public  SwipeRefreshLayout swipeRefreshLayout;
	private ProgressDialog     progressDialog;
	private AirProgressBar     progressBar;

	private ObjectAnimator fadeInAnim  = ObjectAnimator.ofFloat(null, "alpha", 1f);
	private ObjectAnimator fadeOutAnim = ObjectAnimator.ofFloat(null, "alpha", 0f);

	public BusyPresenter() {
		runnableHide = new Runnable() {
			@Override
			public void run() {
				hide(false);
			}
		};
	}

	public void onResume() {
		Dispatcher.getInstance().register(this);
	}

	public void onPause() {
		Dispatcher.getInstance().unregister(this);
		hide(true);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void show(final BusyAction busyAction) {
		show(busyAction, null, null);
	}

	public void show(final BusyAction busyAction, final Object message, final Context context) {

		/*
		 * Make sure there are no pending busys waiting.
		 */
		Patchr.mainThreadHandler.removeCallbacks(runnableShow);
		Patchr.mainThreadHandler.removeCallbacks(runnableHide);

		runnableShow = new Runnable() {

			@Override
			public void run() {
				try {
					if (busyAction == BusyAction.Refreshing_Empty) {
						/*
						 * Initial data load for an activity/fragment.
						 */
						startProgressBar();
					}
					else if (busyAction == BusyAction.Refreshing) {
						/*
						 * Refreshing data for activity/fragment that is already showing data.
						 */
						startSwipeRefreshIndicator();
					}
					else if (busyAction == BusyAction.Scanning_Empty) {
						/*
						 * Scanning for patches.
						 */
						startProgressBar();
					}
					else if (busyAction == BusyAction.Scanning) {
						/*
						 * Scanning for patches.
						 */
						startSwipeRefreshIndicator();
					}
					else if (busyAction == BusyAction.Update) {
						/*
						 * Pushing an edit or insert to the server. We show progress dialog if a photo
						 * update can be canceled.
						 */
						startProgressBar();
						startSwipeRefreshIndicator();
					}
					else if (busyAction == BusyAction.ActionWithMessage) {
						/*
						 * Making a service call and showing a message
						 */
						if (message != null) {
							final ProgressDialog progressDialog = getProgressDialog(context);
							if (message instanceof Integer) {
								progressDialog.setMessage(StringManager.getString((Integer) message));
							}
							else {
								progressDialog.setMessage((String) message);
							}

							if (!progressDialog.isShowing()) {
								progressDialog.setCancelable(false);
								progressDialog.setCanceledOnTouchOutside(false);
								progressDialog.show();
							}
						}
					}

					busyStartedTime = DateTime.nowDate().getTime();
				}
				catch (BadTokenException e) {
					/*
					 * Sometimes the activity has been destroyed out from under us
					 * so we trap this and continue.
					 */
					Reporting.logException(e);
				}
			}
		};

		Patchr.mainThreadHandler.postDelayed(runnableShow, (busyAction == BusyAction.Refreshing_Empty) ? Constants.INTERVAL_BUSY_DELAY : 0);
	}

	public void hide(Boolean noDelay) {
		/*
		 * Make sure there are no pending busys waiting.
		 */
		Patchr.mainThreadHandler.removeCallbacks(runnableShow);
		Patchr.mainThreadHandler.removeCallbacks(runnableHide);
		/*
		 * We delay to enforce a minimum display length for busy if start has been set.
		 */
		if (!noDelay && busyStartedTime != null) {
			Long busyDuration = DateTime.nowDate().getTime() - busyStartedTime;
			if (busyDuration < Constants.INTERVAL_BUSY_MINIMUM) {
				Patchr.mainThreadHandler.postDelayed(runnableHide, Constants.INTERVAL_BUSY_MINIMUM - busyDuration);
				return;
			}
		}

		/* Safe to call from any thread */
		stopProgressDialog();

		Patchr.mainThreadHandler.post(new Runnable() {

			@Override
			public void run() {
				stopProgressBar();
				stopSwipeRefreshIndicator();
			}
		});
	}

	public void showHorizontalProgressBar(final Context context) {
		/*
		 * Make sure there are no pending busys waiting.
		 */
		Patchr.mainThreadHandler.removeCallbacks(runnableShow);
		Patchr.mainThreadHandler.removeCallbacks(runnableHide);

		runnableShow = new Runnable() {

			@Override public void run() {
				try {
					/*
					 * Making a service call and showing a message
					 */
					final ProgressDialog progressDialog = getProgressDialog(context);

					if (!progressDialog.isShowing()) {
						progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
						progressDialog.setProgress(0);
						progressDialog.setMax(100);
						progressDialog.setProgressNumberFormat(null);
						progressDialog.setIndeterminate(false);
						progressDialog.setCanceledOnTouchOutside(false);
						progressDialog.setCancelable(false);
						progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
							@Override public void onClick(DialogInterface dialog, int which) {
								Dispatcher.getInstance().post(new ProcessingCanceledEvent(false));
								Dialogs.dismiss(progressDialog);
							}
						});

						progressDialog.show();
					}
					busyStartedTime = DateTime.nowDate().getTime();
				}

				catch (BadTokenException ignore) {}
			}
		};

		Patchr.mainThreadHandler.postDelayed(runnableShow, 0);
	}

	private void startProgressBar() {
		if (progressBar != null) {
			progressBar.show();
		}
	}

	private void stopProgressBar() {
		if (progressBar != null) {
			progressBar.hide();
		}
	}

	private void startSwipeRefreshIndicator() {
		if (swipeRefreshLayout != null && !swipeRefreshLayout.isRefreshing()) {
			swipeRefreshLayout.setEnabled(false);
			swipeRefreshLayout.setRefreshing(true);
		}
	}

	private void stopSwipeRefreshIndicator() {
		if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
			swipeRefreshLayout.setRefreshing(false);
			swipeRefreshLayout.setEnabled(true);
		}
	}

	private void stopProgressDialog() {
		Dialogs.dismiss(progressDialog);
	}

	private ProgressDialog getProgressDialog(Context context) {

		if (progressDialog == null) {
			progressDialog = new ProgressDialog(context);
			progressDialog.setIndeterminate(true);
			progressDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		}

		return progressDialog;
	}

	/*--------------------------------------------------------------------------------------------
	 * Notifications
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe public void onProgressEvent(ProcessingProgressEvent event) {
		if (progressDialog != null && progressDialog.isShowing() && !progressDialog.isIndeterminate()) {
			Logger.v(this, "Progress event: " + event.percent + "%");
			progressDialog.setProgress((int) event.percent);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	public BusyPresenter setSwipeRefresh(View swipeRefreshLayout) {
		if (swipeRefreshLayout != null && swipeRefreshLayout instanceof SwipeRefreshLayout) {
			this.swipeRefreshLayout = (SwipeRefreshLayout) swipeRefreshLayout;
		}
		return this;
	}

	public BusyPresenter setProgressBar(View progressBar) {
		if (progressBar != null && progressBar instanceof AirProgressBar) {
			this.progressBar = (AirProgressBar) progressBar;
		}
		return this;
	}

	public void positionBelow(final View header, final Integer headerHeightProjected) {
		BaseScreen.position(this.progressBar, header, headerHeightProjected);
	}

	public enum BusyAction {
		Refreshing_Empty,
		Refreshing,
		Scanning_Empty,
		Scanning,
		ActionWithMessage,
		Update,
	}
}
