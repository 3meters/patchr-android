package com.aircandi.components;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.WindowManager.BadTokenException;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.events.CancelEvent;
import com.aircandi.events.ProgressEvent;
import com.aircandi.interfaces.IBusy;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Reporting;
import com.squareup.otto.Subscribe;

import java.lang.ref.WeakReference;

public class BusyManager implements IBusy {

	private   Activity            mActivity;
	private   ProgressDialog      mProgressDialog;
	private   WeakReference<View> mRefreshImage;
	private   WeakReference<View> mRefreshProgress;
	private   Runnable            mRunnableHide;
	private   Runnable            mRunnableShow;
	private   Long                mBusyStartedTime;
	protected SwipeRefreshLayout  mSwipeRefreshLayout;

	@SuppressLint("ResourceAsColor")
	public BusyManager(Activity activity) {
		mActivity = activity;
		mRunnableHide = new Runnable() {

			@Override
			public void run() {
				hideBusy(false);
			}
		};
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void showBusy(final BusyAction busyAction) {
		showBusy(busyAction, null);
	}

	@Override
	public void showBusy(final BusyAction busyAction, final Object message) {

		/*
		 * Make sure there are no pending busys waiting.
		 */
		Patchr.mainThreadHandler.removeCallbacks(mRunnableShow);
		Patchr.mainThreadHandler.removeCallbacks(mRunnableHide);

		mRunnableShow = new Runnable() {

			@Override
			public void run() {
				try {
					if (busyAction == BusyAction.Loading) {
						/*
						 * Initial data load for an activity/fragment.
						 */
						startSwipeRefreshIndicator();
					}
					else if (busyAction == BusyAction.Refreshing) {
						/*
						 * Refreshing data for activity/fragment that is already showing data.
						 */
						startSwipeRefreshIndicator();
					}
					else if (busyAction == BusyAction.Scanning) {
						/*
						 * Scanning for places.
						 */
						startSwipeRefreshIndicator();
					}
					else if (busyAction == BusyAction.Update) {
						/*
						 * Updating the UI because of some new data like a location update.
						 */
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

		Patchr.mainThreadHandler.postDelayed(mRunnableShow, (busyAction == BusyAction.Loading) ? Constants.INTERVAL_BUSY_DELAY : 0);
	}

	public void showProgress() {
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

	@Subscribe
	public void onProgressEvent(ProgressEvent event) {
		if (mProgressDialog != null && mProgressDialog.isShowing() && !mProgressDialog.isIndeterminate()) {
			Logger.v(this, "Progress event: " + event.percent + "%");
			mProgressDialog.setProgress((int) event.percent);
		}
	}

	@Override
	public void hideBusy(Boolean noDelay) {
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
				stopActionbarBusyIndicator();
				stopBodyBusyIndicator();
				stopSwipeRefreshIndicator();
			}
		});
	}

	public void startActionbarBusyIndicator() {
		if (mRefreshImage != null && mRefreshImage.get() != null && mRefreshProgress.get().getVisibility() != View.VISIBLE) {
			mRefreshImage.get().setVisibility(View.GONE);
			mRefreshProgress.get().setVisibility(View.VISIBLE);
		}
	}

	public void startBodyBusyIndicator() {
		final View progress = mActivity.findViewById(R.id.progress);
		if (progress != null && progress.getVisibility() != View.VISIBLE) {
			progress.setVisibility(View.VISIBLE);
		}
	}

	public void startSwipeRefreshIndicator() {
		if (mSwipeRefreshLayout != null && !mSwipeRefreshLayout.isRefreshing()) {
			mSwipeRefreshLayout.setEnabled(false);
			mSwipeRefreshLayout.setRefreshing(true);
		}
	}

	public void stopActionbarBusyIndicator() {
		if (mRefreshImage != null && mRefreshImage.get() != null && mRefreshImage.get().getVisibility() != View.VISIBLE) {
			mRefreshProgress.get().setVisibility(View.GONE);
			mRefreshImage.get().setVisibility(View.VISIBLE);
		}
	}

	public void stopBodyBusyIndicator() {
		final View progress = mActivity.findViewById(R.id.progress);
		if (progress != null && progress.getVisibility() != View.GONE) {
			progress.setVisibility(View.GONE);
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

	public void setRefreshImage(View refreshImage) {
		mRefreshImage = new WeakReference<View>(refreshImage);
	}

	public void setRefreshProgress(View refreshProgress) {
		mRefreshProgress = new WeakReference<View>(refreshProgress);
	}

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
		hideBusy(true);
	}
}
