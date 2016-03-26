package com.patchr.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ViewAnimator;

import com.patchr.R;
import com.patchr.objects.CacheStamp;
import com.patchr.objects.Entity;
import com.patchr.ui.components.AnimationFactory;

@SuppressWarnings("ucd")
public class PatchDetailView extends FrameLayout implements View.OnClickListener {

	private static final Object lock = new Object();

	public    Entity     entity;
	protected CacheStamp cacheStamp;
	protected BaseView   base;
	protected Integer    layoutResId;

	protected ViewGroup       layout;
	public    PatchBannerView bannerView;
	public    PatchInfoView   infoView;
	protected ViewGroup       actionGroup;
	public    Button          actionButton;
	protected ViewAnimator    headerAnimator;

	public PatchDetailView(Context context) {
		this(context, null, 0);
	}

	public PatchDetailView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public PatchDetailView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.layoutResId = R.layout.view_patch_header;
		initialize();
	}

	public PatchDetailView(Context context, Integer layoutResId) {
		super(context, null, 0);
		this.layoutResId = layoutResId;
		initialize();
	}

	protected void initialize() {

		this.base = new BaseView();
		this.layout = (ViewGroup) LayoutInflater.from(getContext()).inflate(this.layoutResId, this, true);

		this.bannerView = (PatchBannerView) layout.findViewById(R.id.banner_view);
		this.infoView = (PatchInfoView) layout.findViewById(R.id.info_view);
		this.actionGroup = (ViewGroup) layout.findViewById(R.id.action_group);
		this.actionButton = (Button) layout.findViewById(R.id.action_button);
		this.headerAnimator = (ViewAnimator) layout.findViewById(R.id.animator_header);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public void onClick(View view) {
		if (view.getId() == R.id.banner_view) {
			AnimationFactory.flipTransition(this.headerAnimator, AnimationFactory.FlipDirection.BOTTOM_TOP, 200);
		}
		else if (view.getId() == R.id.info_view) {
			if (!((String) this.infoView.expandoButton.getTag()).equals("collapsed")) {
				this.infoView.toggleExpando();
			}
			AnimationFactory.flipTransition(this.headerAnimator, AnimationFactory.FlipDirection.BOTTOM_TOP, 200);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void bind(Entity entity) {

		synchronized (lock) {

			this.entity = entity;
			this.cacheStamp = entity.getCacheStamp();

			this.bannerView.databind(entity);
			this.infoView.databind(entity);

			this.bannerView.setOnClickListener(this);
			this.infoView.setOnClickListener(this);
		}
	}
}
