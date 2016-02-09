package com.patchr.ui.widgets;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.patchr.R;
import com.patchr.objects.Entity;
import com.patchr.objects.Photo;
import com.patchr.ui.components.CircleTransform;
import com.patchr.utilities.Colors;
import com.patchr.utilities.UI;
import com.patchr.utilities.Utils;

@SuppressWarnings("ucd")
public class UserPhotoView extends AirPhotoView {

	private TextView mName;
	private Entity   mEntity;

	public UserPhotoView(Context context) {
		this(context, null);
	}

	public UserPhotoView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public UserPhotoView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	protected void initialize(Context context) {
		super.initialize(context);

		this.setBackgroundResource(UI.getResIdForAttribute(getContext(), R.attr.backgroundRoundPlaceholder));
		this.setBackgroundTintMode(PorterDuff.Mode.SRC_ATOP);

		mName = new TextView(getContext());
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		params.gravity = Gravity.CENTER;
		mName.setLayoutParams(params);
		mName.setVisibility(GONE);

		mName.setTextColor(Colors.getColor(R.color.white));
		mName.setGravity(Gravity.CENTER);
		addView(mName);
	}

	public void databind(@NonNull Entity entity) {
		mEntity = entity;
		this.setTag(entity);
		draw();
	}

	private void draw() {

		if (mEntity != null) {

			this.setBackgroundTintList(null);   // Clears any existing tint and returns to grey

			if (mEntity.photo == null) {

				mImageMain.setVisibility(GONE);
				mImageMain.setImageDrawable(null);
				mName.setVisibility(VISIBLE);
				mName.setText(null);
				if (mShowBusy) {
					showLoading(false);
				}

				if (!TextUtils.isEmpty(mEntity.name)) {

					String initials = Utils.initialsFromName(mEntity.name);
					long seed = Utils.numberFromName(mEntity.name);
					Integer color = Utils.randomColor(seed);
					this.setBackgroundTintList(ColorStateList.valueOf(color));

					mName.setText(initials);
				}
			}
			else {
				mImageMain.setVisibility(VISIBLE);
				mName.setVisibility(GONE);
				Photo photo = mEntity.getPhoto();
				UI.drawPhoto(this, photo, new CircleTransform());
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/
}
