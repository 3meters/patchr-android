package com.aircandi.ui.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.Html;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.StringManager;
import com.aircandi.objects.Count;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Link.Direction;
import com.aircandi.objects.Photo;
import com.aircandi.objects.User;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.UI;

@SuppressWarnings("ucd")
public class UserView extends RelativeLayout {

	private ViewGroup    mBoundView;
	private AirImageView mPhotoView;
	private ImageView    mImageLocked;
	private ImageView    mImageWatched;
	private TextView     mName;
	private TextView     mEmail;
	private TextView     mArea;
	private TextView     mLabel;
	private TextView     mWatchCount;
	private TextView     mTimeSince;
	private ComboButton  mOverflowButton;

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
		final int layoutId = ta.getResourceId(R.styleable.UserView_layout, R.layout.widget_user_view_detailed);
		mLabelResId = ta.getResourceId(R.styleable.UserView_label, 0);
		mBoundView = (ViewGroup) LayoutInflater.from(getContext()).inflate(layoutId, null);

		ta.recycle();
		initialize();
	}

	private void initialize() {
		if (!isInEditMode()) {
			mPhotoView = (AirImageView) mBoundView.findViewById(R.id.widget_photo);
			mName = (TextView) mBoundView.findViewById(R.id.widget_name);
			mEmail = (TextView) mBoundView.findViewById(R.id.widget_email);
			mArea = (TextView) mBoundView.findViewById(R.id.area);
			mLabel = (TextView) mBoundView.findViewById(R.id.label);
			mTimeSince = (TextView) mBoundView.findViewById(R.id.timesince);
			mImageLocked = (ImageView) mBoundView.findViewById(R.id.image_locked);
			mImageWatched = (ImageView) mBoundView.findViewById(R.id.image_watched);
			mWatchCount = (TextView) mBoundView.findViewById(R.id.watch_count);
			mOverflowButton = (ComboButton) mBoundView.findViewById(R.id.button_overflow);
		}

		removeAllViews();
		this.addView(mBoundView);
	}

	public void databind(Entity user) {
		databind(user, null);
	}

	public void databind(Entity entity, Long date) {
		mUser = entity;
		mDate = date;
		this.setTag(entity);
		if (mOverflowButton != null) {
			mOverflowButton.setTag(entity);
		}
		draw();
	}

	private void draw() {
		User user = (User) mUser;

		if (user != null) {
			if (mOverflowButton != null) {
				if (user.isAnonymous()) {
					mOverflowButton.setVisibility(user.isAnonymous() ? View.GONE : View.VISIBLE);
				}
			}

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

			if (mEmail != null) {
				if (!TextUtils.isEmpty(user.email)) {
					mEmail.setText(user.email);
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
				if (mPhotoView.getPhoto() == null || !mPhotoView.getPhoto().getUri().equals(user.getPhoto().getUri())) {
					Photo photo = user.getPhoto();
					UI.drawPhoto(mPhotoView, photo);
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

	// --------------------------------------------------------------------------------------------
	// Properties
	// --------------------------------------------------------------------------------------------

	public void setLabel(Integer labelResId) {
		mLabelResId = labelResId;
	}
}
