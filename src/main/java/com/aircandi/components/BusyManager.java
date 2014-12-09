package com.aircandi.components;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.WindowManager.BadTokenException;
import android.widget.ProgressBar;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.events.CancelEvent;
import com.aircandi.events.ProgressEvent;
import com.aircandi.interfaces.IBusy;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Reporting;
import com.squareup.otto.Subscribe;

public class BusyManager implements IBusy {

	private Activity           mActivity;
	private Runnable           mRunnableHide;
	private Runnable           mRunnableShow;
	private Long               mBusyStartedTime;
	private SwipeRefreshLayout mSwipeRefreshLayout;
	private ProgressDialog     mProgressDialog;
	private ProgressBar        mProgressBar;

	@SuppressLint("ResourceAsColor")
	public BusyManager(Activity activity) {
		mActivity = activity;
		mProgressBar = (ProgressBar) mActivity.findViewById(R.id.progress);
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
		show(busyAction, null);
	}

	@Override
	public void show(final BusyAction busyAction, final Object message) {

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
						startBodyBusyIndicator();
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
						startBodyBusyIndicator();
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
						startBodyBusyIndicator();
						startSwipeRefreshIndicator();
					}
					else if (busyAction == BusyAction.ActionWithMessage) {
						/*
						 * Making a service call and showing a message
						 */
						if (message != null) {
							final ProgressDialog progressDialog = getProgressDialog();
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
								if (Patchr.displayMetrics != null) {
									progressDialog.getWindow().setLayout((int) (Patchr.displayMetrics.widthPixels * 0.7), LayoutParams.WRAP_CONTENT);
								}
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

	public void showProgressDialog() {
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
					final ProgressDialog progressDialog = getProgressDialog();

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
								progressDialog.dismiss();
							}
						});

						progressDialog.show();

						if (Patchr.displayMetrics != null) {
							progressDialog.getWindow().setLayout((int) (Patchr.displayMetrics.widthPixels * 0.7), LayoutParams.WRAP_CONTENT);
						}
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

		mActivity.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				stopBodyBusyIndicator();
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

	public void startBodyBusyIndicator() {
		if (mProgressBar != null) {
			if (mProgressBar instanceof ContentLoadingProgressBar) {
				((ContentLoadingProgressBar) mProgressBar).show();
			}
			else {
				mProgressBar.setVisibility(View.VISIBLE);
			}
		}
	}

	public void startSwipeRefreshIndicator() {
		if (mSwipeRefreshLayout != null && !mSwipeRefreshLayout.isRefreshing()) {
			mSwipeRefreshLayout.setEnabled(false);
			mSwipeRefreshLayout.setRefreshing(true);
		}
	}

	public void stopBodyBusyIndicator() {
		if (mProgressBar != null) {
			if (mProgressBar instanceof ContentLoadingProgressBar) {
				((ContentLoadingProgressBar) mProgressBar).hide();
			}
			else {
				mProgressBar.setVisibility(View.GONE);
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
		if (mProgressDialog != null
				&& mProgressDialog.isShowing()
				&& mProgressDialog.getWindow().getWindowManager() != null) {
			try {
				mProgressDialog.dismiss();
			}
			catch (Exception e) {
				/*
				 * Sometime we get a harmless exception that the view is not attached to window manager.
				 * It could be that the activity is getting destroyed before the dismiss can happen.
				 * We catch it and move on.
				 */
				Logger.v(mActivity, e.getMessage());
			}
		}
	}

	private ProgressDialog getProgressDialog() {

		if (mProgressDialog == null) {

			mProgressDialog = new ProgressDialog(mActivity);
			mProgressDialog.setIndeterminate(true);
			mProgressDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		}

		return mProgressDialog;
	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	public void setSwipeRefresh(SwipeRefreshLayout swipeRefreshLayout) {
		mSwipeRefreshLayout = swipeRefreshLayout;
	}

	public SwipeRefreshLayout getSwipeRefresh() {
		return mSwipeRefreshLayout;
	}

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/

	public void onPause() {
		hide(true);
	}
}
