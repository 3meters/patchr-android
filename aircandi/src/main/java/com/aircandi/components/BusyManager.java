package com.aircandi.components;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.WindowManager.BadTokenException;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.ui.base.IBusy;
import com.aircandi.ui.widgets.SmoothProgressBar;
import com.aircandi.utilities.DateTime;

public class BusyManager implements IBusy {

	private Activity          mActivity;
	private ProgressDialog    mProgressDialog;
	private View              mRefreshImage;
	private View              mRefreshProgress;
	private Runnable          mRunnableHide;
	private Runnable          mRunnableShow;
	private Long              mBusyStartedTime;
	private SmoothProgressBar mHeaderProgressBar;

	public BusyManager(Activity activity) {
		mActivity = activity;
		mHeaderProgressBar = (SmoothProgressBar) mActivity.findViewById(R.id.progress_bar);
		addProgressDrawable();
		mRunnableHide = new Runnable() {

			@Override
			public void run() {
				hideBusy(false);
			}
		};
	}

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	public void addProgressDrawable() {
		if (mHeaderProgressBar != null) {
			ShapeDrawable shape = new ShapeDrawable();
			shape.setShape(new RectShape());
			shape.getPaint().setColor(mHeaderProgressBar.getColor());
			ClipDrawable clipDrawable = new ClipDrawable(shape, Gravity.CENTER, ClipDrawable.HORIZONTAL);
			mHeaderProgressBar.setProgressDrawable(clipDrawable);
		}
	}

	@Override
	public void showBusy(final BusyAction busyAction) {
		showBusy(busyAction, null);
	}

	@Override
	public void showBusy(final BusyAction busyAction, final Object message) {

		/*
		 * Make sure there are no pending busys waiting.
		 */
		Aircandi.mainThreadHandler.removeCallbacks(mRunnableShow);
		Aircandi.mainThreadHandler.removeCallbacks(mRunnableHide);

		mRunnableShow = new Runnable() {

			@Override
			public void run() {
				try {
					if (busyAction == BusyAction.Loading) {
						/*
						 * Initial data load for an activity/fragment.
						 */
						startBodyBusyIndicator();
						mBusyStartedTime = null;
						return; // Skips activating busy minimum
					}
					else if (busyAction == BusyAction.Refreshing) {
						/*
						 * Refreshing data for activity/fragment that is already showing data.
						 */
						startBarBusyIndicator();
					}
					else if (busyAction == BusyAction.Scanning) {
						/*
						 * Scanning for places.
						 */
						startBarBusyIndicator();
					}
					else if (busyAction == BusyAction.Action) {
						/*
						 * Making a service call but not showing a message
						 */
						startBodyBusyIndicator();
					}
					else if (busyAction == BusyAction.Update) {
						/*
						 * Updating the UI because of some new data like a location update.
						 */
						startBarBusyIndicator();
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
								mProgressDialog.setCanceledOnTouchOutside(false);
								progressDialog.show();
								if (Aircandi.displayMetrics != null) {
									progressDialog.getWindow().setLayout((int) (Aircandi.displayMetrics.widthPixels * 0.7), LayoutParams.WRAP_CONTENT);
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
					e.printStackTrace();
				}
			}
		};

		Aircandi.mainThreadHandler.postDelayed(mRunnableShow, (busyAction == BusyAction.Loading) ? Constants.INTERVAL_BUSY_DELAY : 0);
	}

	@Override
	public void hideBusy(Boolean noDelay) {

		/*
		 * Make sure there are no pending busys waiting.
		 */
		Aircandi.mainThreadHandler.removeCallbacks(mRunnableShow);
		Aircandi.mainThreadHandler.removeCallbacks(mRunnableHide);
		/*
		 * We delay to enforce a minimum display length for busy if start has been set.
		 */
		if (!noDelay && mBusyStartedTime != null) {
			Long busyDuration = DateTime.nowDate().getTime() - mBusyStartedTime;
			if (busyDuration < Constants.INTERVAL_BUSY_MINIMUM) {
				Aircandi.mainThreadHandler.postDelayed(mRunnableHide, Constants.INTERVAL_BUSY_MINIMUM - busyDuration);
				return;
			}
		}

		/* Safe to call from any thread */
		stopProgressDialog();

		mActivity.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				stopActionbarBusyIndicator();
				stopBarBusyIndicator();
				stopBodyBusyIndicator();
			}
		});

	}

	public void startActionbarBusyIndicator() {
		if (mRefreshImage != null && mRefreshProgress.getVisibility() != View.VISIBLE) {
			mRefreshImage.setVisibility(View.GONE);
			mRefreshProgress.setVisibility(View.VISIBLE);
		}
	}

	public void startBodyBusyIndicator() {
		final View progress = mActivity.findViewById(R.id.progress);
		if (progress != null && progress.getVisibility() != View.VISIBLE) {
			progress.setVisibility(View.VISIBLE);
		}
	}

	public void startBarBusyIndicator() {
		if (mHeaderProgressBar != null && mHeaderProgressBar.getVisibility() != View.VISIBLE) {
			mHeaderProgressBar.setIndeterminate(true);
			mHeaderProgressBar.setVisibility(View.VISIBLE);
		}
	}

	public void stopActionbarBusyIndicator() {
		if (mRefreshImage != null && mRefreshImage.getVisibility() != View.VISIBLE) {
			mRefreshProgress.setVisibility(View.GONE);
			mRefreshImage.setVisibility(View.VISIBLE);
		}
	}

	public void stopBarBusyIndicator() {
		if (mHeaderProgressBar != null && mHeaderProgressBar.getVisibility() != View.GONE) {
			mHeaderProgressBar.setVisibility(View.GONE);
		}
	}

	public void stopBodyBusyIndicator() {
		final View progress = mActivity.findViewById(R.id.progress);
		if (progress != null && progress.getVisibility() != View.GONE) {
			progress.setVisibility(View.GONE);
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

	// --------------------------------------------------------------------------------------------
	// Properties
	// --------------------------------------------------------------------------------------------

	public void setRefreshImage(View refreshImage) {
		mRefreshImage = refreshImage;
	}

	public void setRefreshProgress(View refreshProgress) {
		mRefreshProgress = refreshProgress;
	}

	public View getRefreshImage() {
		return mRefreshImage;
	}

	public View getRefreshProgress() {
		return mRefreshProgress;
	}

	@Override
	public SmoothProgressBar getHeaderProgressBar() {
		return mHeaderProgressBar;
	}
}
