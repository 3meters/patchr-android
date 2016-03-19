package com.patchr.ui.views;

import android.content.Context;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.patchr.Constants;
import com.patchr.R;
import com.patchr.objects.Count;
import com.patchr.objects.Entity;
import com.patchr.objects.Link;
import com.patchr.objects.Patch;
import com.patchr.utilities.Colors;
import com.patchr.utilities.UI;

import java.util.Locale;

@SuppressWarnings("ucd")
public class PatchBannerView extends FrameLayout {

	private static final Object lock = new Object();

	public    Entity   entity;
	protected BaseView base;
	protected Integer  layoutResId;

	protected ViewGroup    layout;
	protected ImageLayout  photoView;
	protected TextView     name;
	protected TextView     type;
	protected View         privacyGroup;
	public    View         tuneButton;
	protected View         membersButton;
	public    ViewAnimator muteButton;
	protected View         moreButton;

	public PatchBannerView(Context context) {
		this(context, null, 0);
	}

	public PatchBannerView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public PatchBannerView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.layoutResId = R.layout.patch_banner_view;
		initialize();
	}

	public PatchBannerView(Context context, Integer layoutResId) {
		super(context, null, 0);
		this.layoutResId = layoutResId;
		initialize();
	}

	protected void initialize() {

		this.base = new BaseView();
		this.layout = (ViewGroup) LayoutInflater.from(getContext()).inflate(this.layoutResId, this, true);

		this.photoView = (ImageLayout) layout.findViewById(R.id.patch_photo);
		this.name = (TextView) layout.findViewById(R.id.name);
		this.type = (TextView) layout.findViewById(R.id.type);
		this.privacyGroup = (View) layout.findViewById(R.id.privacy_group);
		this.tuneButton = (View) layout.findViewById(R.id.tune_button);
		this.membersButton = (View) layout.findViewById(R.id.members_button);
		this.muteButton = (ViewAnimator) layout.findViewById(R.id.mute_button);
		this.moreButton = (View) layout.findViewById(R.id.next_page_button);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void databind(Entity entity) {

		synchronized (lock) {

			this.entity = entity;
			this.photoView.setImageWithEntity(entity);

			base.setOrGone(this.name, entity.name);
			base.setOrGone(this.type, (entity.type + " patch").toUpperCase(Locale.US));

			privacyGroup.setVisibility((((Patch) entity).privacy != null
					&& ((Patch) entity).privacy.equals(Constants.PRIVACY_PRIVATE)) ? VISIBLE : GONE);

			this.tuneButton.setVisibility(entity.isOwnedByCurrentUser() ? VISIBLE : GONE);

			/* Members count */
			Count count = entity.getCount(Constants.TYPE_LINK_WATCH, null, true, Link.Direction.in);
			if (count == null) {
				count = new Count(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_PATCH, null, 0);
			}
			if (count.count.intValue() > 0) {
				TextView watchingCount = (TextView) this.membersButton.findViewById(R.id.members_count);
				TextView watchingLabel = (TextView) this.membersButton.findViewById(R.id.members_label);
				if (watchingCount != null) {
					String label = getResources().getQuantityString(R.plurals.label_watching, count.count.intValue(), count.count.intValue());
					watchingCount.setText(String.valueOf(count.count.intValue()));
					watchingLabel.setText(label);
					UI.setVisibility(this.membersButton, View.VISIBLE);
				}
			}
			else {
				UI.setVisibility(this.membersButton, View.GONE);
			}

			/* Mute button */
			this.muteButton.setDisplayedChild(0);
			Link link = entity.linkFromAppUser(Constants.TYPE_LINK_WATCH);
			if (link == null || !link.enabled) {
				UI.setVisibility(this.muteButton, View.GONE);
			}
			else {
				ImageView image = (ImageView) this.muteButton.findViewById(R.id.mute_image);
				if (link.mute != null && link.mute) {
					/* Sound is off */
					image.setImageResource(R.drawable.ic_img_mute_off_dark);
					image.setColorFilter(null);
					image.setAlpha(0.5f);
				}
				else {
					/* Sound is on */
					image.setImageResource(R.drawable.ic_img_mute_dark);
					final int color = Colors.getColor(R.color.brand_primary);
					image.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
					image.setAlpha(1.0f);
				}
				UI.setVisibility(this.muteButton, View.VISIBLE);
			}
		}
	}
}
