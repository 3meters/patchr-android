package com.patchr.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ViewAnimator;

import com.patchr.R;
import com.patchr.components.Logger;
import com.patchr.model.RealmEntity;
import com.patchr.objects.CacheStamp;
import com.patchr.ui.components.AnimationFactory;

@SuppressWarnings("ucd")
public class PatchDetailView extends BaseView implements View.OnClickListener {

	private static final Object lock = new Object();

	public    RealmEntity entity;
	protected CacheStamp  cacheStamp;
	protected Integer     layoutResId;

	protected ViewGroup       layout;
	public    PatchBannerView bannerView;
	public    PatchInfoView   infoView;
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

		this.layout = (ViewGroup) LayoutInflater.from(getContext()).inflate(this.layoutResId, this, true);
		this.bannerView = (PatchBannerView) layout.findViewById(R.id.banner_view);
		this.infoView = (PatchInfoView) layout.findViewById(R.id.info_view);
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
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void bind(RealmEntity entity) {

		synchronized (lock) {
			this.entity = entity;
			this.bannerView.bind(entity);
			this.infoView.bind(entity);
			this.bannerView.setOnClickListener(this);
			this.infoView.setOnClickListener(this);
		}
	}

	public void draw() {
		this.bannerView.draw();
		this.infoView.draw();
	}
}
