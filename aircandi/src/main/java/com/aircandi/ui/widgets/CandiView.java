package com.aircandi.ui.widgets;

import java.util.Locale;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.Html;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.LocationManager;
import com.aircandi.components.StringManager;
import com.aircandi.objects.CacheStamp;
import com.aircandi.objects.Category;
import com.aircandi.objects.Count;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Link.Direction;
import com.aircandi.objects.Photo;
import com.aircandi.objects.Photo.PhotoSource;
import com.aircandi.objects.Place;
import com.aircandi.objects.User;
import com.aircandi.utilities.UI;

@SuppressWarnings("ucd")
public class CandiView extends RelativeLayout {

	public static final int			HORIZONTAL						= 0;
	public static final int			VERTICAL						= 1;

	protected Entity				mEntity;
	protected Integer				mLayoutId;
	protected ViewGroup				mLayout;
	protected Boolean				mColorize;

	protected AirImageView			mPhotoView;
	protected AirImageView			mCategoryPhoto;
	protected TextView				mName;
	protected TextView				mSubtitle;
	protected TextView				mEmail;
	protected TextView				mArea;
	protected TextView				mDistance;
	protected View					mCandiViewGroup;
	protected LinearLayout			mHolderShortcuts;
	protected LinearLayout			mHolderInfo;
	protected CacheStamp			mCacheStamp;
	private float					mAspectRatio;
	private boolean					mAspectRatioEnabled;
	private int						mDominantMeasurement;

	public static final int			MEASUREMENT_WIDTH				= 0;
	public static final int			MEASUREMENT_HEIGHT				= 1;
	private static final float		DEFAULT_ASPECT_RATIO			= 1f;
	private static final boolean	DEFAULT_ASPECT_RATIO_ENABLED	= false;
	private static final int		DEFAULT_DOMINANT_MEASUREMENT	= MEASUREMENT_WIDTH;

	public CandiView(Context context) {
		this(context, null);
	}

	public CandiView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public CandiView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		if (attrs != null) {
			final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.CandiView, defStyle, 0);
			mLayoutId = ta.getResourceId(R.styleable.CandiView_layout, R.layout.widget_candi_view);
			mColorize = ta.getBoolean(R.styleable.CandiView_colorize, true);
			mAspectRatio = ta.getFloat(R.styleable.AirImageView_aspectRatio, DEFAULT_ASPECT_RATIO);
			mAspectRatioEnabled = ta.getBoolean(R.styleable.AirImageView_aspectRatioEnabled,
					DEFAULT_ASPECT_RATIO_ENABLED);
			mDominantMeasurement = ta.getInt(R.styleable.AirImageView_dominantMeasurement,
					DEFAULT_DOMINANT_MEASUREMENT);
			ta.recycle();
			initialize();
		}
	}

	protected void initialize() {

		mLayout = (ViewGroup) LayoutInflater.from(getContext()).inflate(mLayoutId, this, true);

		mCandiViewGroup = mLayout.findViewById(R.id.candi_view_group);
		mPhotoView = (AirImageView) mLayout.findViewById(R.id.entity_photo);
		mName = (TextView) mLayout.findViewById(R.id.name);
		mSubtitle = (TextView) mLayout.findViewById(R.id.subtitle);
		mArea = (TextView) mLayout.findViewById(R.id.area);
		mEmail = (TextView) mLayout.findViewById(R.id.email);
		mDistance = (TextView) mLayout.findViewById(R.id.distance);
		mCategoryPhoto = (AirImageView) mLayout.findViewById(R.id.subtitle_badge);
		mHolderShortcuts = (LinearLayout) mLayout.findViewById(R.id.shortcuts);
		mHolderInfo = (LinearLayout) mLayout.findViewById(R.id.info_holder);
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@Override
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		if (!mAspectRatioEnabled) return;

		int newWidth;
		int newHeight;
		switch (mDominantMeasurement) {
			case MEASUREMENT_WIDTH:
				newWidth = getMeasuredWidth();
				newHeight = (int) (newWidth * mAspectRatio);
				break;

			case MEASUREMENT_HEIGHT:
				newHeight = getMeasuredHeight();
				newWidth = (int) (newHeight * mAspectRatio);
				break;

			default:
				throw new IllegalStateException("Unknown measurement with ID " + mDominantMeasurement);
		}

		setMeasuredDimension(newWidth, newHeight);
		
		if (mCandiViewGroup != null) {
			int widthSpec = MeasureSpec.makeMeasureSpec(newWidth, View.MeasureSpec.EXACTLY);
			int heightSpec = MeasureSpec.makeMeasureSpec(newHeight, View.MeasureSpec.EXACTLY);
			mCandiViewGroup.measure(widthSpec, heightSpec);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	public void databind(Entity entity, IndicatorOptions options) {
		synchronized (entity) {
			/*
			 * If it is the same entity and it hasn't changed then nothing to do
			 */
			if (!entity.synthetic) { // Leaving this in case we ever use a candiview to display a suggested place.
				if (mEntity != null && entity.id.equals(mEntity.id) && mCacheStamp.equals(entity.getCacheStamp())) {
					mEntity = entity;
					showDistance(entity);
					if (options.forceUpdate) {
						showIndicators(entity, options);
					}
					return;
				}
			}
			else {
				if (mEntity != null && entity.id != null && mEntity.id != null && entity.id.equals(mEntity.id)) {
					mEntity = entity;
					showDistance(entity);
					if (options.forceUpdate) {
						showIndicators(entity, options);
					}
					return;
				}
			}

			mEntity = entity;
			mCacheStamp = entity.getCacheStamp();

			/* Primary candi image */

			drawPhoto();

			/* Background color */

			if (mCandiViewGroup != null && mColorize) {
				String colorizeKey = null;
				if (mEntity instanceof Place && ((Place) mEntity).category != null) {
					colorizeKey = ((Place) mEntity).category.name;
				}
				Integer colorResId = Place.getCategoryColorResId(colorizeKey);
				mCandiViewGroup.setBackgroundResource(colorResId);
			}

			/* Name */

			setVisibility(mName, View.GONE);
			if (mName != null && entity.name != null && !entity.name.equals("")) {
				mName.setText(Html.fromHtml(entity.name));
				setVisibility(mName, View.VISIBLE);
			}

			/* Subtitle */

			setVisibility(mSubtitle, View.GONE);
			if (mSubtitle != null && entity.subtitle != null && !entity.subtitle.equals("")) {
				mSubtitle.setText(Html.fromHtml(entity.subtitle.toUpperCase(Locale.US)));
				setVisibility(mSubtitle, View.VISIBLE);
			}

			if (entity instanceof User) {
				User user = (User) entity;

				/* Email */

				setVisibility(mEmail, View.GONE);
				if (mEmail != null && !TextUtils.isEmpty(user.email)) {
					mEmail.setText(Html.fromHtml(user.email));
					setVisibility(mEmail, View.VISIBLE);
				}

				/* Area */

				setVisibility(mArea, View.GONE);
				if (mArea != null && !TextUtils.isEmpty(user.area)) {
					mArea.setText(Html.fromHtml(user.area.toUpperCase(Locale.US)));
					setVisibility(mArea, View.VISIBLE);
				}
			}

			/* Place specific info */

			/* We take over the subtitle field and use it for categories */
			if (entity instanceof Place) {

				Place place = (Place) entity;

				Category category = place.category;
				if (mSubtitle != null) {
					setVisibility(mSubtitle, View.GONE);
					if (category != null && category.name != null && !category.id.equals("generic")) {
						if (entity.visibility.equals(Constants.VISIBILITY_PRIVATE)) {
							mSubtitle.setText(Html.fromHtml(category.name.toUpperCase(Locale.US))
									+ " " + StringManager.getString(R.string.label_place_private_flag).toUpperCase(Locale.US));
						}
						else {
							mSubtitle.setText(Html.fromHtml(category.name.toUpperCase(Locale.US))
									+ " " + StringManager.getString(R.string.label_place_public_flag).toUpperCase(Locale.US));
						}
						setVisibility(mSubtitle, View.VISIBLE);
					}
					else {
						/* No category so show public/private flag by itself */
						mSubtitle.setText(StringManager.getString(R.string.label_place_public_flag).toUpperCase(Locale.US));
						setVisibility(mSubtitle, View.VISIBLE);
					}
				}

				/* Category photo */

				setVisibility(mCategoryPhoto, View.GONE);
				if (mCategoryPhoto != null) {
					if (category != null) {
						Photo photo = category.photo.clone();
						if (!Photo.same(mCategoryPhoto.getPhoto(), photo)) {
							photo.colorize = false;
							mCategoryPhoto.setSizeHint(UI.getRawPixelsForDisplayPixels(50f));
							UI.drawPhoto(mCategoryPhoto, photo);
						}
						mCategoryPhoto.setVisibility(View.VISIBLE);
					}
				}
			}

			/* Indicators */

			showIndicators(entity, options);

			/* Distance */

			showDistance(entity);
		}
	}

	protected void drawPhoto() {

		if (mPhotoView != null) {

			if (mPhotoView.getImageView().getDrawable() != null) {
				if (Photo.same(mPhotoView.getPhoto(), mEntity.getPhoto())) return;
			}

			Photo photo = mEntity.getPhoto();
			if (mPhotoView.getPhoto() == null || !photo.getUri().equals(mPhotoView.getPhoto().getUri())) {
				photo.setWidth(mPhotoView.getSizeHint());
				mPhotoView.setTag(photo);
				UI.drawPhoto(mPhotoView, photo);
			}
		}
	}

	public void showIndicators(Entity entity, IndicatorOptions options) {

		/* Indicators */
		setVisibility(mHolderShortcuts, View.GONE);
		if (mHolderShortcuts != null) {

			mHolderShortcuts.removeAllViews();
			final int sizePixels = UI.getRawPixelsForDisplayPixels((float) options.imageSizePixels);

			if (options.watchingEnabled) {
				Integer watching = 0;
				Count count = entity.getCount(Constants.TYPE_LINK_WATCH, null, true, Direction.in);
				if (options.showIfZero && count == null) {
					count = new Count(Constants.TYPE_LINK_WATCH, null, null, watching);
				}
				if (options.showIfZero || (count != null && (count.count.intValue() > 0))) {
					String label = options.iconsEnabled
							? String.valueOf(count.count.intValue())
							: String.valueOf(count.count.intValue()) + " "
									+ StringManager.getString(R.string.label_indicator_watching).toUpperCase(Locale.US);
					addIndicator(R.id.holder_indicator_watching, "ic_watched_holo_dark", label, sizePixels, options);
				}
			}
			setVisibility(mHolderShortcuts, View.VISIBLE);
		}
	}

	protected void addIndicator(Integer id, String photoUri, String name, Integer sizePixels, IndicatorOptions options) {

		View view = LayoutInflater.from(getContext()).inflate(R.layout.temp_radar_link_item, null);
		view.setId(id);
		AirImageView photoView = (AirImageView) view.findViewById(R.id.entity_photo);
		TextView label = (TextView) view.findViewById(R.id.name);

		label.setVisibility(View.GONE);
		if (name != null) {
			label.setText(name);
			label.setVisibility(View.VISIBLE);
		}

		if (options.iconsEnabled) {
			photoView.setSizeHint(sizePixels);
			Photo photo = new Photo(photoUri, null, null, null, PhotoSource.resource);
			UI.drawPhoto(photoView, photo);
		}
		else {
			photoView.setVisibility(View.GONE);
		}

		mHolderShortcuts.addView(view);
	}

	public void updateIndicator(Integer id, String value) {

		View view = findViewById(id);
		if (view == null) return;

		AirImageView photoView = (AirImageView) view.findViewById(R.id.entity_photo);
		TextView label = (TextView) view.findViewById(R.id.name);

		if (photoView != null && label != null) {
			if (!label.getText().equals(value)) {
				label.setText(value);
				if (photoView.getVisibility() != View.GONE) {
					UI.drawPhoto(photoView, photoView.getPhoto());
				}
			}
		}
	}

	public void showDistance(Entity entity) {
		setVisibility(mDistance, View.GONE);
		if (mDistance != null) {

			String info = "here";
			final Float distance = entity.getDistance(true); // In meters
			final String target = entity.hasActiveProximity() ? "B:" : "L:";
			/*
			 * If distance = -1 then we don't have the location info
			 * yet needed to correctly determine distance.
			 */
			if (distance == null) {
				info = "--";
			}
			else if (entity.fuzzy) {
				info = "nearby";
			}
			else if (distance == -1f) { // $codepro.audit.disable floatComparison
				info = "--";
			}
			else {
				final float miles = distance * LocationManager.MetersToMilesConversion;
				final float feet = distance * LocationManager.MetersToFeetConversion;
				final float yards = distance * LocationManager.MetersToYardsConversion;

				if (feet >= 0) {
					if (miles >= 0.1) {
						info = String.format(Locale.US, "%.1f mi", miles);
					}
					else if (feet >= 50) {
						info = String.format(Locale.US, "%.0f yds", yards);
					}
					else {
						info = String.format(Locale.US, "%.0f ft", feet);
					}
				}

				if (Aircandi.getInstance().getCurrentUser() != null
						&& Aircandi.settings.getBoolean(StringManager.getString(R.string.pref_enable_dev), false)
						&& Aircandi.getInstance().getCurrentUser().developer != null
						&& Aircandi.getInstance().getCurrentUser().developer) {
					info = target + info;
				}
				else {
					if (feet <= 60) {
						info = "here";
					}
				}
			}

			if (!info.equals("")) {
				mDistance.setText(Html.fromHtml(info));
				setVisibility(mDistance, View.VISIBLE);
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Properties
	// --------------------------------------------------------------------------------------------

	protected static void setVisibility(View view, Integer visibility) {
		if (view != null) {
			view.setVisibility(visibility);
		}
	}

	public void setPlace(Place place) {
		mEntity = place;
	}

	public void setLayoutId(Integer layoutId) {
		mLayoutId = layoutId;
	}

	public AirImageView getCandiImage() {
		return mPhotoView;
	}

	public void setCandiImage(AirImageView candiImage) {
		mPhotoView = candiImage;
	}

	public ViewGroup getLayout() {
		return mLayout;
	}

	public LinearLayout getTextGroup() {
		return mHolderInfo;
	}

	public void setTextGroup(LinearLayout textGroup) {
		mHolderInfo = textGroup;
	}

	/** Get the aspect ratio for this image view. */
	public float getAspectRatio() {
		return mAspectRatio;
	}

	/** Set the aspect ratio for this image view. This will update the view instantly. */
	public void setAspectRatio(float aspectRatio) {
		this.mAspectRatio = aspectRatio;
		if (mAspectRatioEnabled) {
			requestLayout();
		}
	}

	/** Get whether or not forcing the aspect ratio is enabled. */
	public boolean getAspectRatioEnabled() {
		return mAspectRatioEnabled;
	}

	/** set whether or not forcing the aspect ratio is enabled. This will re-layout the view. */
	public void setAspectRatioEnabled(boolean aspectRatioEnabled) {
		this.mAspectRatioEnabled = aspectRatioEnabled;
		requestLayout();
	}

	/** Get the dominant measurement for the aspect ratio. */
	public int getDominantMeasurement() {
		return mDominantMeasurement;
	}

	/**
	 * Set the dominant measurement for the aspect ratio.
	 * 
	 * @see #MEASUREMENT_WIDTH
	 * @see #MEASUREMENT_HEIGHT
	 */
	public void setDominantMeasurement(int dominantMeasurement) {
		if (dominantMeasurement != MEASUREMENT_HEIGHT && dominantMeasurement != MEASUREMENT_WIDTH) {
			throw new IllegalArgumentException("Invalid measurement type.");
		}
		this.mDominantMeasurement = dominantMeasurement;
		requestLayout();
	}

	// --------------------------------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------------------------------

	public static class IndicatorOptions {
		public int		imageSizePixels	= 20;
		public boolean	showIfZero		= false;
		public boolean	forceUpdate		= false;
		public boolean	watchingEnabled	= true;
		public boolean	iconsEnabled	= true;
	}
}
