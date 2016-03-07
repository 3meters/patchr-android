package com.patchr.ui.widgets;

import android.content.Context;
import android.graphics.PorterDuff;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.patchr.Constants;
import com.patchr.R;
import com.patchr.objects.Entity;
import com.patchr.objects.Photo;
import com.patchr.ui.components.CircleTransform;
import com.patchr.utilities.Colors;
import com.patchr.utilities.UI;
import com.patchr.utilities.Utils;

@SuppressWarnings("ucd")
public class EntityPhotoView extends AirPhotoView {

	private TextView mNameView;
	private Entity   mEntity;
	private String   mName;
	private String   mUri;

	public EntityPhotoView(Context context) {
		this(context, null);
	}

	public EntityPhotoView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public EntityPhotoView(Context context, AttributeSet attrs, int defStyle) {
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

		mNameView = new TextView(getContext());
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		params.gravity = Gravity.CENTER;
		mNameView.setLayoutParams(params);
		mNameView.setVisibility(GONE);

		mNameView.setTextColor(Colors.getColor(R.color.white));
		mNameView.setGravity(Gravity.CENTER);
		addView(mNameView);
	}

	public void databind(@NonNull Entity entity) {
		mEntity = entity;
		this.setTag(entity);
		draw();
	}

	public void databind(@NonNull String uri, @NonNull String name) {
		mName = name;
		mUri = uri;
		draw();
	}

	private void draw() {

		if (mEntity != null) {

			this.getBackground().clearColorFilter();

			if (mEntity.photo == null) {

				mImageMain.setVisibility(GONE);
				mImageMain.setImageDrawable(null);
				mNameView.setVisibility(VISIBLE);
				mNameView.setText(null);

				if (mShowBusy) {
					showLoading(false);
				}

				if (!TextUtils.isEmpty(mEntity.name)) {

					String initials = Utils.initialsFromName(mEntity.name);
					long seed = Utils.numberFromName(mEntity.name);
					Integer color = Utils.randomColor(seed);
					this.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);

					mNameView.setText(initials);
				}
			}
			else {

				Photo photo = mEntity.getPhoto();
				String uri = UI.url(photo.prefix, photo.source, null);  // Will be just the prefix without host, params, etc.

				//if (mUriBound == null || !uri.equals(mUriBound)) {

					mUriBound = uri;
					mImageMain.setVisibility(VISIBLE);
					mNameView.setVisibility(GONE);

					if ((mShape.equals("auto") && mEntity.schema.equals(Constants.SCHEMA_ENTITY_USER)) || mShape.equals("round")) {
						UI.drawPhoto(this, photo, new CircleTransform());
					}
					else {
						UI.drawPhoto(this, photo);
					}
				//}
			}
		}
		else if (mUri != null || mName != null) {

			if (mUri != null) {

				if (mUriBound == null || !mUri.equals(mUriBound)) {
					mImageMain.setVisibility(VISIBLE);
					mNameView.setVisibility(GONE);
					Photo photo = mEntity.getPhoto();
					if (mShape.equals("round")) {
						UI.drawPhoto(this, photo, new CircleTransform());
					}
					else {
						UI.drawPhoto(this, photo);
					}
				}
			}
			else {

				mImageMain.setVisibility(GONE);
				mImageMain.setImageDrawable(null);
				mNameView.setVisibility(VISIBLE);
				mNameView.setText(null);
				if (mShowBusy) {
					showLoading(false);
				}

				if (!TextUtils.isEmpty(mName)) {

					String initials = Utils.initialsFromName(mName);
					long seed = Utils.numberFromName(mName);
					Integer color = Utils.randomColor(seed);
					this.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);

					mNameView.setText(initials);
				}
			}
		}
	}
}
