package com.patchr.ui.views;

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

import com.patchr.Constants;
import com.patchr.R;
import com.patchr.components.LocationManager;
import com.patchr.components.StringManager;
import com.patchr.objects.CacheStamp;
import com.patchr.objects.Count;
import com.patchr.objects.Entity;
import com.patchr.objects.Link.Direction;
import com.patchr.objects.Patch;
import com.patchr.objects.Photo;
import com.patchr.objects.Shortcut;
import com.patchr.objects.ShortcutSettings;
import com.patchr.objects.User;
import com.patchr.utilities.Integers;
import com.patchr.utilities.Utils;

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

	protected ImageLayout  mPhotoView;
	protected ImageLayout  mUserPhotoView;
	protected ImageLayout  mCategoryPhoto;
	protected TextView     mCategoryName;
	protected TextView     mType;
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

	List<Shortcut> mShortcuts = new ArrayList<Shortcut>();

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
			ta.recycle();
			initialize();
		}
	}

	protected void initialize() {

		mLayout = (ViewGroup) LayoutInflater.from(getContext()).inflate(mLayoutId, this, true);

		mCandiViewGroup = mLayout.findViewById(R.id.candi_view_group);
		mPhotoView = (ImageLayout) mLayout.findViewById(R.id.photo);
		mUserPhotoView = (ImageLayout) mLayout.findViewById(R.id.user_photo);
		mName = (TextView) mLayout.findViewById(R.id.name);
		mSubhead = (TextView) mLayout.findViewById(R.id.subhead);
		mArea = (TextView) mLayout.findViewById(R.id.area);
		mEmail = (TextView) mLayout.findViewById(R.id.email);
		mDistance = (TextView) mLayout.findViewById(R.id.distance);
		mType = (TextView) mLayout.findViewById(R.id.type);
		mCategoryName = (TextView) mLayout.findViewById(R.id.category_name);
		mCategoryPhoto = (ImageLayout) mLayout.findViewById(R.id.category_photo);
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
		}
	}

	protected void drawPhoto(String groupId) {

		if (mUserPhotoView != null) {
			if (mEntity.schema.equals(Constants.SCHEMA_ENTITY_USER)) {
				mUserPhotoView.setImageWithEntity(mEntity);
			}
		}
		else if (mPhotoView != null) {
			mPhotoView.setImageWithEntity(mEntity);
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
					if (!Photo.same(shortcut.photo, shortcuts.get(i).photo)) {
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
			else {
				if (distance == -1f) { // $codepro.audit.disable floatComparison
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

					if (Utils.devModeEnabled()) {
						info = target + info;
					}
					else if (feet <= 60) {
						info = "here";
					}
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

	public void setLayoutId(Integer layoutId) {
		mLayoutId = layoutId;
	}

	public ImageLayout getCandiImage() {
		return mPhotoView;
	}

	public void setCandiImage(ImageLayout candiImage) {
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
