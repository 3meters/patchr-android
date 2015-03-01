package com.aircandi.ui.components;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.BadTokenException;
import android.widget.ProgressBar;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.components.BusProvider;
import com.aircandi.components.Logger;
import com.aircandi.components.StringManager;
import com.aircandi.events.CancelEvent;
import com.aircandi.events.ProgressEvent;
import com.aircandi.interfaces.IBusy;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Reporting;
import com.squareup.otto.Subscribe;

public class BusyController implements IBusy {

	private Runnable           mRunnableHide;
	private Runnable           mRunnableShow;
	private Long               mBusyStartedTime;
	private SwipeRefreshLayout mSwipeRefreshLayout;
	private ProgressDialog     mProgressDialog;
	private ProgressBar        mProgressBar;
	private ProgressBar        mProgressBarToolbar;

	@SuppressLint("ResourceAsColor")
	public BusyController() {
		mRunnableHide = new Runnable() {
			@Override
			public void run() {
				hide(false);
			}
		};
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void show(final BusyAction busyAction) {
		show(busyAction, null, null);
	}

	@Override
	public void show(final BusyAction busyAction, final Object message, final Context context) {

		/*
		 * Make sure there are no pending busys waiting.
		 */
		Patchr.mainThreadHandler.removeCallbacks(mRunnableShow);
		Patchr.mainThreadHandler.removeCallbacks(mRunnableHide);

		mRunnableShow = new Runnable() {

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
						 * Scanning for places.
						 */
						startProgressBar();
					}
					else if (busyAction == BusyAction.Scanning) {
						/*
						 * Scanning for places.
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

					mBusyStartedTime = DateTime.nowDate().getTime();
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

		Patchr.mainThreadHandler.postDelayed(mRunnableShow, (busyAction == BusyAction.Refreshing_Empty) ? Constants.INTERVAL_BUSY_DELAY : 0);
	}

	@Override
	public void showProgressDialog(final Context context) {
		/*
		 * Make sure there are no pending busys waiting.
		 */
		Patchr.mainThreadHandler.removeCallbacks(mRunnableShow);
		Patchr.mainThreadHandler.removeCallbacks(mRunnableHide);

		mRunnableShow = new Runnable() {

			@Override
			public void run() {
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
							@Override
							public void onClick(DialogInterface dialog, int which) {
								BusProvider.getInstance().post(new CancelEvent(false));
								Dialogs.dismiss(progressDialog);
							}
						});

						progressDialog.show();
					}
					mBusyStartedTime = DateTime.nowDate().getTime();
				}

				catch (BadTokenException ignore) {}
			}
		};

		Patchr.mainThreadHandler.postDelayed(mRunnableShow, 0);
	}

	@Override
	public void hide(Boolean noDelay) {
		/*
		 * Make sure there are no pending busys waiting.
		 */
		Patchr.mainThreadHandler.removeCallbacks(mRunnableShow);
		Patchr.mainThreadHandler.removeCallbacks(mRunnableHide);
		/*
		 * We delay to enforce a minimum display length for busy if start has been set.
		 */
		if (!noDelay && mBusyStartedTime != null) {
			Long busyDuration = DateTime.nowDate().getTime() - mBusyStartedTime;
			if (busyDuration < Constants.INTERVAL_BUSY_MINIMUM) {
				Patchr.mainThreadHandler.postDelayed(mRunnableHide, Constants.INTERVAL_BUSY_MINIMUM - busyDuration);
				return;
			}
		}

		/* Safe to call from any thread */
		stopProgressDialog();

		Patchr.mainThreadHandler.post(new Runnable() {

			@Override
			public void run() {
				stopProgressBar();
				stopProgressBarToolbar();
				stopSwipeRefreshIndicator();
			}
		});
	}

	@Subscribe
	public void onProgressEvent(ProgressEvent event) {
		if (mProgressDialog != null && mProgressDialog.isShowing() && !mProgressDialog.isIndeterminate()) {
			Logger.v(this, "Progress event: " + event.percent + "%");
			mProgressDialog.setProgress((int) event.percent);
		}
	}

	public void startProgressBar() {
		if (mProgressBar != null) {
			if (mProgressBar instanceof ContentLoadingProgressBar) {
				((ContentLoadingProgressBar) mProgressBar).show();
			}
			else {
				mProgressBar.setVisibility(View.VISIBLE);
			}
		}
	}

	public void startProgressBarToolbar() {
		if (mProgressBarToolbar != null) {
			if (mProgressBarToolbar instanceof ContentLoadingProgressBar) {
				((ContentLoadingProgressBar) mProgressBarToolbar).show();
			}
			else {
				mProgressBarToolbar.setVisibility(View.VISIBLE);
			}
		}
	}

	public void startSwipeRefreshIndicator() {
		if (mSwipeRefreshLayout != null && !mSwipeRefreshLayout.isRefreshing()) {
			mSwipeRefreshLayout.setEnabled(false);
			mSwipeRefreshLayout.setRefreshing(true);
		}
	}

	public void stopProgressBar() {
		if (mProgressBar != null) {
			if (mProgressBar instanceof ContentLoadingProgressBar) {
				((ContentLoadingProgressBar) mProgressBar).hide();
			}
			else {
				mProgressBar.setVisibility(View.GONE);
			}
		}
	}

	public void stopProgressBarToolbar() {
		if (mProgressBarToolbar != null) {
			if (mProgressBarToolbar instanceof ContentLoadingProgressBar) {
				((ContentLoadingProgressBar) mProgressBarToolbar).hide();
			}
			else {
				mProgressBarToolbar.setVisibility(View.GONE);
			}
		}
	}

	public void stopSwipeRefreshIndicator() {
		if (mSwipeRefreshLayout != null && mSwipeRefreshLayout.isRefreshing()) {
			mSwipeRefreshLayout.setRefreshing(false);
			mSwipeRefreshLayout.setEnabled(true);
		}
	}

	public void stopProgressDialog() {
		Dialogs.dismiss(mProgressDialog);
	}

	private ProgressDialog getProgressDialog(Context context) {

		if (mProgressDialog == null) {
			mProgressDialog = new ProgressDialog(context);
			mProgressDialog.setIndeterminate(true);
			mProgressDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		}

		return mProgressDialog;
	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	public BusyController setSwipeRefresh(View swipeRefreshLayout) {
		if (swipeRefreshLayout != null && swipeRefreshLayout instanceof SwipeRefreshLayout) {
			mSwipeRefreshLayout = (SwipeRefreshLayout) swipeRefreshLayout;
		}
		return this;
	}

	public BusyController setProgressBar(View progressBar) {
		if (progressBar != null && progressBar instanceof ProgressBar) {
			mProgressBar = (ProgressBar) progressBar;
		}
		return this;
	}

	public BusyController setProgressBarToolbar(View progressBar) {
		if (progressBar != null && progressBar instanceof ProgressBar) {
			mProgressBarToolbar = (ProgressBar) progressBar;
		}
		return this;
	}

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/

	public void resume() {
		BusProvider.getInstance().register(this);
	}

	public void pause() {
		BusProvider.getInstance().unregister(this);
		hide(true);
	}
}
