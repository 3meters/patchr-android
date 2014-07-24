package com.aircandi.ui;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase.DisplayType;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase.OnScaleChangeListener;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.ShareCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.ShareActionProvider;
import com.actionbarsherlock.widget.ShareActionProvider.OnShareTargetSelectedListener;
import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.AnimationManager;
import com.aircandi.components.EntityManager;
import com.aircandi.components.Logger;
import com.aircandi.components.MediaManager;
import com.aircandi.components.StringManager;
import com.aircandi.objects.Entity;
import com.aircandi.objects.ImageResult;
import com.aircandi.objects.Link.Direction;
import com.aircandi.objects.Photo;
import com.aircandi.objects.Place;
import com.aircandi.objects.ServiceBase;
import com.aircandi.objects.Shortcut;
import com.aircandi.objects.ShortcutSettings;
import com.aircandi.ui.base.BaseActivity;
import com.aircandi.ui.base.IBind;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.ui.widgets.AirViewPager;
import com.aircandi.ui.widgets.UserView;
import com.aircandi.utilities.Json;
import com.aircandi.utilities.UI;

public class PhotoForm extends BaseActivity implements IBind {

	protected static int		DEFAULT_ANIMATION_DURATION	= 200;

	private Photo				mPhoto;
	private List<Photo>			mPhotosForPaging			= new ArrayList<Photo>();
	private AirViewPager		mViewPager;
	private Boolean				mPagingEnabled				= true;
	private String				mForEntityId;
	private Entity				mForEntity;
	private String				mListLinkType;
	private String				mListLinkSchema;
	private ImageViewTouch		mImageViewTouch;
	private ShareActionProvider	mShareActionProvider;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		/* This has to be called before setContentView */
		super.onCreate(savedInstanceState);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
	}

	@Override
	public void unpackIntent() {
		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			final String jsonPhoto = extras.getString(Constants.EXTRA_PHOTO);
			mPhoto = (Photo) Json.jsonToObject(jsonPhoto, Json.ObjectType.PHOTO);
			mForEntityId = extras.getString(Constants.EXTRA_ENTITY_PARENT_ID);
			mListLinkSchema = extras.getString(Constants.EXTRA_LIST_LINK_SCHEMA);
			mListLinkType = extras.getString(Constants.EXTRA_LIST_LINK_TYPE);
			mPagingEnabled = extras.getBoolean(Constants.EXTRA_PAGING_ENABLED, true);
		}
	}

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
		setSupportProgressBarIndeterminateVisibility(true);
		if (mForEntityId == null) {
			mPagingEnabled = false;
		}
		bind(BindingMode.AUTO);
	}

	@Override
	protected void configureActionBar() {
		super.configureActionBar();

		if (mActionBar != null) {
			mActionBar.setSubtitle("double-tap to zoom");
		}
	}

	@Override
	public void bind(BindingMode mode) {

		if (!mPagingEnabled) {
			final ViewGroup layout = (ViewGroup) ((ViewStub) findViewById(R.id.stub_picture_detail)).inflate();
			buildPictureDetail(mPhoto, layout);
			bindImageViewTouch(layout);
		}
		else {
			if (mForEntityId != null) {
				if (mListLinkType == null) {
					mPagingEnabled = false;
					bind(mode);
				}
				else if (mListLinkType.equals(Constants.TYPE_LINK_WATCH)
						|| mListLinkType.equals(Constants.TYPE_LINK_CREATE)) {
					mForEntity = EntityManager.getCacheEntity(mForEntityId);
					if (mForEntity != null) {
						ShortcutSettings settings = new ShortcutSettings(mListLinkType, mListLinkSchema, Direction.out, false, false);
						settings.appClass = Place.class;
						List<Shortcut> shortcuts = mForEntity.getShortcuts(settings, new ServiceBase.SortByPositionSortDate(), null);
						for (Shortcut shortcut : shortcuts) {
							Photo photo = shortcut.getPhoto();
							photo.setCreatedAt(shortcut.sortDate.longValue());
							photo.setName(shortcut.name);
							mPhotosForPaging.add(photo);
						}
					}
					else {
						mPagingEnabled = false;
						bind(mode);
					}
				}
				else {
					List<Entity> entities = (List<Entity>) EntityManager.getEntityCache().getCacheEntitiesForEntity(mForEntityId, mListLinkSchema, null,
							null, null, null);
					/*
					 * We might get here just using a link without a downloaded entity. Treat it like paging
					 * is not enabled.
					 */
					if (entities == null || entities.size() == 0) {
						mPagingEnabled = false;
						bind(mode);
					}
					else {
						Collections.sort(entities, new Entity.SortByPositionSortDate());
						for (Entity entity : entities) {
							if (entity.photo != null) {
								Photo photo = entity.getPhoto();
								photo.setCreatedAt(entity.modifiedDate.longValue());
								photo.setName(entity.name);
								photo.setUser(entity.creator);
								mPhotosForPaging.add(photo);
							}
						}
					}
				}
				updateViewPager();
			}
		}
	}

	@Override
	public void draw() {}

	public ViewGroup buildPictureDetail(Photo photo, ViewGroup layout) {

		final AirImageView photoView = (AirImageView) layout.findViewById(R.id.entity_photo);
		layout.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				UI.showToastNotification("touched", Toast.LENGTH_SHORT);
				return false;
			}
		});

		final TextView name = (TextView) layout.findViewById(R.id.name);
		final UserView user = (UserView) layout.findViewById(R.id.author);
		final ImageView imageView = (ImageView) photoView.getImageView();

		((ImageViewTouch) imageView).setDisplayType(DisplayType.FIT_TO_SCREEN);
		((ImageViewTouch) imageView).setScrollEnabled(true);

		/* Title */
		UI.setVisibility(name, View.GONE);
		if (photo.getName() != null && !photo.getName().equals("")) {
			name.setText(photo.getName());
			UI.setVisibility(name, View.VISIBLE);
		}

		/* Author block */
		UI.setVisibility(user, View.GONE);
		if (photo.getUser() != null) {
			user.databind(photo.getUser(), photo.getCreatedAt().longValue(), false);
			UI.setVisibility(user, View.VISIBLE);
		}

		/* Photo */
		final ViewHolder holder = new ViewHolder();
		holder.photoView = photoView;
		holder.photoView.setTag(photo);
		holder.photoView.setCenterCrop(false);
		UI.drawPhoto(holder.photoView, photo);

		/* Sharing */
		setShareIntent(photo);

		return layout;
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	public void onZoomIn() {
		mImageViewTouch.setDoubleTapDirection(1);
		float scale = mImageViewTouch.getScale();
		float targetScale = scale;
		targetScale = mImageViewTouch.onDoubleTapPost(scale, mImageViewTouch.getMaxScale());
		targetScale = Math.min(mImageViewTouch.getMaxScale(), Math.max(targetScale, mImageViewTouch.getMinScale()));
		mImageViewTouch.zoomTo(targetScale, DEFAULT_ANIMATION_DURATION);
	}

	public void onZoomOut() {
		mImageViewTouch.setDoubleTapDirection(-1);
		float scale = mImageViewTouch.getScale();
		float targetScale = scale;
		targetScale = mImageViewTouch.onDoubleTapPost(scale, mImageViewTouch.getMaxScale());
		targetScale = Math.min(mImageViewTouch.getMaxScale(), Math.max(targetScale, mImageViewTouch.getMinScale()));
		mImageViewTouch.zoomTo(targetScale, DEFAULT_ANIMATION_DURATION);

	}

	private void updateViewPager()
	{
		if (mViewPager == null) {

			mViewPager = (AirViewPager) findViewById(R.id.view_pager);
			mViewPager.setPageTransformer(true, new AnimationManager.DepthPageTransformer());
			mViewPager.setVisibility(View.VISIBLE);
			mViewPager.setOnTouchListener(new OnTouchListener() {

				@Override
				public boolean onTouch(View v, MotionEvent event) {
					/*
					 * ViewPager ignores any gesture it doesn't see as a horizontal move or fling.
					 */
					int action = event.getAction();
					switch (action & MotionEvent.ACTION_MASK) {
						case MotionEvent.ACTION_UP:
							return false;
					}
					return false;
				}
			});

			mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {

				@Override
				public void onPageScrollStateChanged(int state) {
					super.onPageScrollStateChanged(state);
					if (state == ViewPager.SCROLL_STATE_IDLE) {
						Logger.v(this, "Page idle");
						bindImageViewTouch(null);
					}
				}

				@Override
				public void onPageSelected(int position) {
					Logger.v(this, "Page selected");
				}
			});

			mViewPager.setAdapter(new PhotoPagerAdapter());

			synchronized (mPhotosForPaging) {
				for (int i = 0; i < mPhotosForPaging.size(); i++) {
					if (mPhotosForPaging.get(i).getUri() != null) {
						if (mPhotosForPaging.get(i).getUri().equals(mPhoto.getUri())) {
							mViewPager.setCurrentItem(i, false);
							break;
						}
					}
				}
			}
		}
	}

	protected void bindImageViewTouch(ViewGroup layout) {
		ViewGroup view = layout;
		if (mPagingEnabled) {
			view = (ViewGroup) mViewPager.findViewWithTag("page" + mViewPager.getCurrentItem());
		}

		if (view != null) {
			AirImageView image = (AirImageView) view.findViewById(R.id.entity_photo);
			mImageViewTouch = (ImageViewTouch) image.getImageView();
			mImageViewTouch.setOnScaleChangeListener(new OnScaleChangeListener() {

				@Override
				public void onScaleChanged(float scale) {
					if (mViewPager != null) {
						mViewPager.setSwipeable(scale <= 1.01f);
					}
				}
			});
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		final AirImageView photoView = (AirImageView) findViewById(R.id.entity_photo);
		final ImageViewTouch imageView = (ImageViewTouch) photoView.getImageView();
		imageView.setDisplayType(DisplayType.FIT_TO_SCREEN);
		super.onConfigurationChanged(newConfig);
	}

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	private void setShareIntent(Photo photo) {

		ShareCompat.IntentBuilder builder = ShareCompat.IntentBuilder.from(this)
				.setSubject(String.format(StringManager.getString(R.string.label_photo_share_subject)
						, Aircandi.getInstance().getCurrentUser().name))
				.setType("image/jpeg").setStream(MediaManager.getSharePathUri());

		Intent intent = builder.getIntent();

		if (mShareActionProvider != null) {
			mShareActionProvider.setShareIntent(intent);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.photo_form;
	}

	// --------------------------------------------------------------------------------------------
	// Menus
	// --------------------------------------------------------------------------------------------

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		// Locate MenuItem with ShareActionProvider
		MenuItem item = menu.findItem(R.id.share);

		mShareActionProvider = new ShareActionProvider(getSupportActionBar().getThemedContext());
		mShareActionProvider.setShareHistoryFileName(ShareActionProvider.DEFAULT_SHARE_HISTORY_FILE_NAME);
		mShareActionProvider.setOnShareTargetSelectedListener(new OnShareTargetSelectedListener() {

			@Override
			public boolean onShareTargetSelected(ShareActionProvider source, Intent intent) {

				final AirImageView photoView = (AirImageView) findViewById(R.id.entity_photo);
				Bitmap bitmap = ((BitmapDrawable) photoView.getImageView().getDrawable()).getBitmap();
				File file = MediaManager.copyBitmapToSharePath(bitmap);

				if (file == null) {
					UI.showToastNotification(StringManager.getString(R.string.error_storage_unmounted), Toast.LENGTH_SHORT);
					return true;
				}

				return false;
			}
		});

		item.setActionProvider(mShareActionProvider);

		if (mPhoto != null) {
			setShareIntent(mPhoto);
		}

		return true;
	}

	// --------------------------------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------------------------------

	public class PhotoPagerAdapter extends PagerAdapter {

		@Override
		public int getCount() {
			return mPhotosForPaging.size();
		}

		@Override
		public Object instantiateItem(ViewGroup collection, int position) {
			final Photo photo = mPhotosForPaging.get(position);
			ViewGroup layout = (ViewGroup) LayoutInflater.from(PhotoForm.this).inflate(R.layout.temp_photo_form, null);
			layout.setTag("page" + String.valueOf(position));
			buildPictureDetail(photo, layout);
			collection.addView(layout, 0);
			if (position == mViewPager.getCurrentItem()) {
				bindImageViewTouch(layout);
			}

			return layout;
		}

		@Override
		public int getItemPosition(Object object) {
			/*
			 * Causes the view pager to recreate all the pages
			 * when notifyDataSetChanged is call on the adapter.
			 */
			return POSITION_NONE;
		}

		@Override
		public void destroyItem(ViewGroup collection, int position, Object o) {
			/*
			 * We help free up bitmap memory by clearing the imageview reference.
			 */
			View view = (View) o;
			((ViewPager) collection).removeView(view);
			view = null;
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return (view.equals(object));
		}

		@SuppressWarnings("deprecation")
        @Override
		public void finishUpdate(View arg0) {}

		@Override
		public Parcelable saveState() {
			return null;
		}

		@Override
		public void restoreState(Parcelable state, ClassLoader loader) {}

	}

	public static class ViewHolder {

		public AirImageView	photoView;
		public ImageResult	data;		// NO_UCD (unused code)
	}
}