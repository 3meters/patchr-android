package com.patchr.ui.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.text.Html;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.patchr.Constants;
import com.patchr.R;
import com.patchr.components.StringManager;
import com.patchr.objects.Count;
import com.patchr.objects.Entity;
import com.patchr.objects.Link.Direction;
import com.patchr.objects.Photo;
import com.patchr.objects.User;
import com.patchr.ui.components.CircleTransform;
import com.patchr.utilities.DateTime;
import com.patchr.utilities.UI;

@SuppressWarnings("ucd")
public class UserView extends RelativeLayout {

	private ViewGroup    mBoundView;
	private AirPhotoView mPhotoView;
	private ImageView    mImageLocked;
	private ImageView    mImageWatched;
	private TextView     mName;
	private TextView     mEmail;
	private TextView     mArea;
	private TextView     mLabel;
	private TextView     mWatchCount;
	private TextView     mTimeSince;

	private Entity  mUser;
	private Integer mLabelResId;
	private Long    mDate;
	private Boolean mLocked = false;

	public UserView(Context context) {
		this(context, null);
	}

	public UserView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public UserView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.UserView, defStyle, 0);
		final int layoutId = ta.getResourceId(R.styleable.UserView_layoutId, R.layout.widget_user_view_detailed);
		mLabelResId = ta.getResourceId(R.styleable.UserView_label, 0);
		mBoundView = (ViewGroup) LayoutInflater.from(getContext()).inflate(layoutId, null);

		ta.recycle();
		initialize();
	}

	private void initialize() {
		if (!isInEditMode()) {
			mPhotoView = (AirPhotoView) mBoundView.findViewById(R.id.widget_photo);
			mName = (TextView) mBoundView.findViewById(R.id.widget_name);
			mArea = (TextView) mBoundView.findViewById(R.id.area);
			mLabel = (TextView) mBoundView.findViewById(R.id.label);
			mTimeSince = (TextView) mBoundView.findViewById(R.id.timesince);
			mImageLocked = (ImageView) mBoundView.findViewById(R.id.image_locked);
			mImageWatched = (ImageView) mBoundView.findViewById(R.id.image_watched);
			mWatchCount = (TextView) mBoundView.findViewById(R.id.watch_count);
		}

		removeAllViews();
		this.addView(mBoundView);
	}

	public void databind(Entity user) {
		databind(user, null);
	}

	public void databind(@NonNull Entity entity, Long date) {
		mUser = entity;
		mDate = date;
		this.setTag(entity);
		draw();
	}

	private void draw() {
		User user = (User) mUser;

		if (user != null) {
			if (mLabel != null) {
				if (mLabelResId != 0) {
					mLabel.setText(StringManager.getString(mLabelResId));
				}
				else {
					mLabel.setVisibility(View.GONE);
				}
			}

			if (mName != null) {
				if (!TextUtils.isEmpty(user.name)) {
					mName.setText(user.name);
				}
			}

			UI.setVisibility(mEmail, View.GONE);
			if (mEmail != null) {
				if (!TextUtils.isEmpty(user.email)) {
					mEmail.setText(user.email);
					UI.setVisibility(mEmail, View.VISIBLE);
				}
			}

			if (mArea != null && !TextUtils.isEmpty(user.area)) {
				mArea.setText(Html.fromHtml(user.area));
			}

			if (mTimeSince != null) {
				if (mDate != null) {
					mTimeSince.setText(DateTime.dateStringAt(mDate));
				}
				else {
					mTimeSince.setVisibility(View.GONE);
				}
			}

			if (mPhotoView != null) {
				if (mPhotoView.getPhoto() == null || !mPhotoView.getPhoto().getDirectUri().equals(user.getPhoto().getDirectUri())) {
					Photo photo = user.getPhoto();
					if (photo != null) {
						UI.drawPhoto(mPhotoView, photo, new CircleTransform());
					}
				}
			}

			if (mImageLocked != null) {
				if (mLocked != null && mLocked) {
					mImageLocked.setVisibility(View.VISIBLE);
				}
				else {
					mImageLocked.setVisibility(View.INVISIBLE);
				}
			}

			if (mBoundView.findViewById(R.id.stats_group) != null) {
				if (mImageWatched != null) {
					Count count = user.getCount(Constants.TYPE_LINK_WATCH, null, null, Direction.in);
					mWatchCount.setText(String.valueOf((count != null) ? count.count.intValue() : 0));
				}
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/
	public void setLabel(Integer labelResId) {
		mLabelResId = labelResId;
	}
}
