package com.aircandi.ui.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.text.Html;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.components.DownloadManager;
import com.aircandi.components.LocationManager;
import com.aircandi.components.StringManager;
import com.aircandi.objects.CacheStamp;
import com.aircandi.objects.Category;
import com.aircandi.objects.Count;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Link;
import com.aircandi.objects.Link.Direction;
import com.aircandi.objects.Patch;
import com.aircandi.objects.Photo;
import com.aircandi.objects.Photo.PhotoSource;
import com.aircandi.objects.Place;
import com.aircandi.objects.Shortcut;
import com.aircandi.objects.ShortcutSettings;
import com.aircandi.objects.User;
import com.aircandi.utilities.Colors;
import com.aircandi.utilities.Integers;
import com.aircandi.utilities.UI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@SuppressWarnings("ucd")
public class CandiView extends RelativeLayout {

	public static final int HORIZONTAL = 0;
	public static final int VERTICAL   = 1;

	protected Entity    mEntity;
	protected Integer   mLayoutId;
	protected ViewGroup mLayout;

	protected AirImageView mPhotoView;
	protected AirImageView mCategoryPhoto;
	protected TextView     mCategoryName;
	protected TextView     mType;
	protected TextView     mPlaceName;
	protected TextView     mName;
	protected TextView     mIndex;
	protected TextView     mSubhead;
	protected TextView     mEmail;
	protected TextView     mArea;
	protected TextView     mDistance;
	protected TextView     mCount;
	protected TextView     mMessageCount;
	protected TextView     mWatchCount;
	protected TextView     mAddress;
	protected View         mCandiViewGroup;
	protected View         mPrivacyGroup;
	protected LinearLayout mHolderPreviews;
	protected LinearLayout mHolderInfo;
	protected CacheStamp   mCacheStamp;
	private   float        mAspectRatio;
	private   boolean      mAspectRatioEnabled;
	private   int          mDominantMeasurement;

	List<Shortcut> mShortcuts = new ArrayList<Shortcut>();

	public static final  int     MEASUREMENT_WIDTH            = 0;
	public static final  int     MEASUREMENT_HEIGHT           = 1;
	private static final float   DEFAULT_ASPECT_RATIO         = 1f;
	private static final boolean DEFAULT_ASPECT_RATIO_ENABLED = false;
	private static final int     DEFAULT_DOMINANT_MEASUREMENT = MEASUREMENT_WIDTH;

	public CandiView(Context context) {
		this(context, null);
	}

	public CandiView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public CandiView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		if (attrs != null) {
			final TypedArray ta = context.getTheme().obtainStyledAttributes(attrs, R.styleable.CandiView, defStyle, 0);
			mLayoutId = ta.getResourceId(R.styleable.CandiView_layoutRef, R.layout.widget_candi_view);
			mAspectRatio = ta.getFloat(R.styleable.CandiView_aspectRatio, DEFAULT_ASPECT_RATIO);
			mAspectRatioEnabled = ta.getBoolean(R.styleable.CandiView_aspectRatioEnabled,
					DEFAULT_ASPECT_RATIO_ENABLED);
			mDominantMeasurement = ta.getInt(R.styleable.CandiView_dominantMeasurement,
					DEFAULT_DOMINANT_MEASUREMENT);
			ta.recycle();
			initialize();
		}
	}

	protected void initialize() {

		mLayout = (ViewGroup) LayoutInflater.from(getContext()).inflate(mLayoutId, this, true);

		mCandiViewGroup = mLayout.findViewById(R.id.candi_view_group);
		mPhotoView = (AirImageView) mLayout.findViewById(R.id.photo);
		mName = (TextView) mLayout.findViewById(R.id.name);
		mSubhead = (TextView) mLayout.findViewById(R.id.subhead);
		mArea = (TextView) mLayout.findViewById(R.id.area);
		mEmail = (TextView) mLayout.findViewById(R.id.email);
		mDistance = (TextView) mLayout.findViewById(R.id.distance);
		mType = (TextView) mLayout.findViewById(R.id.type);
		mCategoryName = (TextView) mLayout.findViewById(R.id.category_name);
		mCategoryPhoto = (AirImageView) mLayout.findViewById(R.id.category_photo);
		mPlaceName = (TextView) mLayout.findViewById(R.id.place_name);
		mHolderPreviews = (LinearLayout) mLayout.findViewById(R.id.previews);
		mHolderInfo = (LinearLayout) mLayout.findViewById(R.id.info_holder);
		mCount = (TextView) mLayout.findViewById(R.id.count);
		mMessageCount = (TextView) mLayout.findViewById(R.id.message_count);
		mWatchCount = (TextView) mLayout.findViewById(R.id.watch_count);
		mPrivacyGroup = (View) mLayout.findViewById(R.id.privacy_group);
		mAddress = (TextView) mLayout.findViewById(R.id.candi_form_address);
		mIndex = (TextView) mLayout.findViewById(R.id.index);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

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

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void databind(Entity entity, IndicatorOptions options, String groupId) {
		synchronized (entity) {
			options.forceUpdate = true;
			/*
			 * If it is the same entity and it hasn't changed then nothing to do
			 */
			if (!entity.synthetic) { // Leaving this in case we ever use a candiview to display a suggested patch.
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

			drawPhoto(groupId);

			/* Name */

			setVisibility(mName, View.GONE);
			if (mName != null && !TextUtils.isEmpty(entity.name)) {
				mName.setText(Html.fromHtml(entity.name));
				setVisibility(mName, View.VISIBLE);
			}

			setVisibility(mSubhead, View.GONE);

			/* Index */
			setVisibility(mIndex, View.GONE);
			if (mIndex != null && entity.index != null) {
				mIndex.setText(String.valueOf(entity.index.intValue()));
				setVisibility(mIndex, View.VISIBLE);
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

			/* Patch specific info */

			if (entity instanceof Patch) {

				Patch patch = (Patch) entity;

				/* Place name */

				setVisibility(mPlaceName, View.GONE);
				if (mPlaceName != null) {
					Link linkPlace = entity.getParentLink(Constants.TYPE_LINK_PROXIMITY, Constants.SCHEMA_ENTITY_PLACE);
					if (linkPlace != null) {
						mPlaceName.setText(linkPlace.shortcut.name);
						UI.setVisibility(mPlaceName, View.VISIBLE);
					}
				}

				/* Type */

				setVisibility(mType, View.GONE);
				if (mType != null) {
					setVisibility(mType, View.INVISIBLE);
					if (patch.type != null) {
						mType.setText(Html.fromHtml((patch.type + " patch").toUpperCase(Locale.US)));
						setVisibility(mType, View.VISIBLE);
					}
					else {
						/* No type so show default label */
						String type = (StringManager.getString(R.string.label_patch_type_default) + " patch").toUpperCase(Locale.US);
						mType.setText(type);
						setVisibility(mType, View.VISIBLE);
					}
				}

				/* Privacy */

				if (mPrivacyGroup != null) {
					mPrivacyGroup.setVisibility((patch.privacy != null && patch.privacy.equals(Constants.PRIVACY_PRIVATE)) ? VISIBLE : GONE);
				}

				/* Indicators */

				showIndicators(entity, options);

				/* Distance */

				showDistance(entity);
			}

			if (entity instanceof Place) {
				Place place = (Place) entity;

				/* Category */

				Category category = place.category;
				setVisibility(mCategoryName, View.GONE);
				if (mCategoryName != null) {
					setVisibility(mCategoryName, View.INVISIBLE);
					if (category != null && category.name != null && !category.id.equals("generic")) {
						mCategoryName.setText(Html.fromHtml((category.name).toUpperCase(Locale.US)));
						setVisibility(mCategoryName, View.VISIBLE);
					}
					else {
						/* No category so show default label */
						String categoryName = StringManager.getString(R.string.label_place_category_default).toUpperCase(Locale.US);
						mCategoryName.setText(categoryName);
						setVisibility(mCategoryName, View.VISIBLE);
					}
				}

				/* Category photo */

				setVisibility(mCategoryPhoto, View.INVISIBLE);
				if (mCategoryPhoto != null) {
					mCategoryPhoto.setSizeHint(UI.getRawPixelsForDisplayPixels(50f));
					if (category != null) {
						Photo photo = category.photo.clone();
						if (!Photo.same(mCategoryPhoto.getPhoto(), photo)) {
							mCategoryPhoto.setGroupTag(groupId);
							UI.drawPhoto(mCategoryPhoto, photo);
						}
					}
					else {
							/*
							 * Fall back to default.
							 */
						DownloadManager.with(Patchr.applicationContext).load(R.drawable.default_88)
								.resize(mCategoryPhoto.getSizeHint(), mCategoryPhoto.getSizeHint())    // Memory size
								.centerCrop()
								.into(mCategoryPhoto);
					}
					mCategoryPhoto.getImageView().setColorFilter(Colors.getColor(R.color.white), PorterDuff.Mode.SRC_ATOP);
					mCategoryPhoto.setVisibility(View.VISIBLE);
				}

				/* Address */

				setVisibility(mAddress, View.GONE);
				if (mAddress != null) {
					String addressBlock = place.getAddressBlock();

					if (place.phone != null) {
						addressBlock += "<br/>" + place.getFormattedPhone();
					}

					if (!"".equals(addressBlock)) {
						mAddress.setText(Html.fromHtml(addressBlock));
						UI.setVisibility(mAddress, View.VISIBLE);
					}
				}
			}
		}
	}

	protected void drawPhoto(String groupId) {

		if (mPhotoView != null) {

			if (mPhotoView.getImageView().getDrawable() != null) {
				if (Photo.same(mPhotoView.getPhoto(), mEntity.getPhoto())) return;
			}

			Photo photo = mEntity.getPhoto();
			if (mPhotoView.getPhoto() == null || !photo.getUri().equals(mPhotoView.getPhoto().getUri())) {
				mPhotoView.setTag(photo);
				mPhotoView.setGroupTag(groupId);
				UI.drawPhoto(mPhotoView, photo);
			}
		}
	}

	public void showIndicators(Entity entity, IndicatorOptions options) {

		/* Indicators */
		setVisibility(mHolderPreviews, View.GONE);
		setVisibility(mCount, View.GONE);

		if (mHolderPreviews != null) {

			ShortcutSettings settings = new ShortcutSettings(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_MESSAGE, Direction.in, false, false);
			List<Shortcut> shortcuts = (List<Shortcut>) entity.getShortcuts(settings, null, new Shortcut.SortByPositionSortDate());

			Boolean dirty = (mShortcuts.size() != shortcuts.size());
			if (!dirty) {
				Integer i = 0;
				for (Shortcut shortcut : mShortcuts) {
					if (!shortcut.creator.id.equals(shortcuts.get(i).creator.id)) {
						dirty = true;
						break;
					}
					if (shortcut.description != null && !shortcut.description.equals(shortcuts.get(i).description)) {
						dirty = true;
						break;
					}
					if (!Photo.same(shortcut.getPhoto(), shortcuts.get(i).getPhoto())) {
						dirty = true;
						break;
					}
					i++;
				}
			}

			if (dirty) {
				mHolderPreviews.removeAllViews();

				final LayoutInflater inflater = LayoutInflater.from(this.getContext());

				/* We only show the first two */
				int shortcutCount = 0;
				Map<String, Boolean> shortcutMap = new HashMap<String, Boolean>();
				for (Shortcut shortcut : shortcuts) {
					if (!shortcutMap.containsKey(shortcut.id)) {
						if (shortcutCount < Integers.getInteger(R.integer.limit_indicators_radar)) {
							View view = inflater.inflate(R.layout.temp_indicator_message, mHolderPreviews, false);
							TextView message = (TextView) view.findViewById(R.id.indicator_message);
							if (!TextUtils.isEmpty(shortcut.description)) {
								message.setText(shortcut.description);
							}
							else if (shortcut.photo != null) {
								message.setText("+photo");
							}
							mHolderPreviews.addView(view);
						}
						shortcutMap.put(shortcut.id, true);
						shortcutCount++;
					}
				}
				mShortcuts = shortcuts;
			}

			if (mShortcuts.size() > 0) {
				setVisibility(mHolderPreviews, View.VISIBLE);
			}
		}

		/* Message count for patch list */
		Count messageCount = entity.getCount(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_MESSAGE, null, Direction.in);
		if (mCount != null && messageCount != null && messageCount.count.intValue() > Integers.getInteger(R.integer.limit_indicators_radar)) {
			Integer extras = messageCount.count.intValue() - Integers.getInteger(R.integer.limit_indicators_radar);
			mCount.setText("+" + extras);
			setVisibility(mCount, View.VISIBLE);
		}

		/* Message count for nearby list */
		if (mMessageCount != null) {
			mMessageCount.setText((messageCount != null) ? String.valueOf(messageCount.count.intValue()) : "0");
		}

		/* Watch count for nearby list */
		if (mWatchCount != null) {
			Count watchCount = entity.getCount(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_USER, true, Direction.in);
			mWatchCount.setText((watchCount != null) ? String.valueOf(watchCount.count.intValue()) : "0");
		}
	}

	protected void addIndicator(Integer id, String photoUri, String name, Integer sizePixels, IndicatorOptions options) {

		View view = LayoutInflater.from(getContext()).inflate(R.layout.temp_nearby_link_item, null);
		view.setId(id);
		AirImageView photoView = (AirImageView) view.findViewById(R.id.photo);
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

		mHolderPreviews.addView(view);
	}

	public void updateIndicator(Integer id, String value) {

		View view = findViewById(id);
		if (view == null) return;

		AirImageView photoView = (AirImageView) view.findViewById(R.id.photo);
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

				if (Patchr.getInstance().getCurrentUser() != null
						&& Patchr.settings.getBoolean(StringManager.getString(R.string.pref_enable_dev), false)
						&& Patchr.getInstance().getCurrentUser().developer != null
						&& Patchr.getInstance().getCurrentUser().developer) {
					info = target + info;
				}
				else if (feet <= 60) {
					info = "here";
				}
			}

			if (!TextUtils.isEmpty(info)) {
				mDistance.setText(Html.fromHtml(info));
				setVisibility(mDistance, View.VISIBLE);
			}
			else {
				setVisibility(mDistance, View.GONE);
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	protected static void setVisibility(View view, Integer visibility) {
		if (view != null) {
			view.setVisibility(visibility);
		}
	}

	public void setPlace(Patch place) {
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

	/**
	 * Get the aspect ratio for this image view.
	 */
	public float getAspectRatio() {
		return mAspectRatio;
	}

	/**
	 * Set the aspect ratio for this image view. This will update the view instantly.
	 */
	public void setAspectRatio(float aspectRatio) {
		this.mAspectRatio = aspectRatio;
		if (mAspectRatioEnabled) {
			requestLayout();
		}
	}

	/**
	 * Get whether or not forcing the aspect ratio is enabled.
	 */
	public boolean getAspectRatioEnabled() {
		return mAspectRatioEnabled;
	}

	/**
	 * set whether or not forcing the aspect ratio is enabled. This will re-layout the view.
	 */
	public void setAspectRatioEnabled(boolean aspectRatioEnabled) {
		this.mAspectRatioEnabled = aspectRatioEnabled;
		requestLayout();
	}

	/**
	 * Get the dominant measurement for the aspect ratio.
	 */
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

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	public static class IndicatorOptions {
		public int     imageSizePixels = 20;
		public boolean showIfZero      = false;
		public boolean forceUpdate     = false;
		public boolean watchingEnabled = true;
		public boolean iconsEnabled    = true;
	}
}
