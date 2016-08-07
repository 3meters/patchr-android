package com.patchr.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.patchr.Constants;
import com.patchr.R;
import com.patchr.model.RealmEntity;
import com.patchr.objects.enums.MemberStatus;
import com.patchr.ui.widgets.ImageWidget;
import com.patchr.utilities.UI;

import java.util.Locale;

@SuppressWarnings("ucd")
public class PatchBannerView extends BaseView {

	private static final Object lock = new Object();

	public    RealmEntity entity;
	protected Integer     layoutResId;

	protected ViewGroup   layout;
	public    ImageWidget photoView;
	protected TextView    nameView;
	protected TextView    typeView;
	protected View        privacyGroup;
	protected View        membersButton;
	public    ImageView   muteImageView;
	protected View        moreButton;

	public PatchBannerView(Context context) {
		this(context, null, 0);
	}

	public PatchBannerView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public PatchBannerView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.layoutResId = R.layout.view_patch_banner;
		initialize();
	}

	public PatchBannerView(Context context, Integer layoutResId) {
		super(context, null, 0);
		this.layoutResId = layoutResId;
		initialize();
	}

	protected void initialize() {

		this.layout = (ViewGroup) LayoutInflater.from(getContext()).inflate(this.layoutResId, this, true);

		this.photoView = (ImageWidget) layout.findViewById(R.id.patch_photo);
		this.nameView = (TextView) layout.findViewById(R.id.name);
		this.typeView = (TextView) layout.findViewById(R.id.type);
		this.privacyGroup = (View) layout.findViewById(R.id.privacy_group);
		this.membersButton = (View) layout.findViewById(R.id.members_button);
		this.muteImageView = (ImageView) layout.findViewById(R.id.mute_image);
		this.moreButton = (View) layout.findViewById(R.id.next_page_button);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void draw() {

		this.photoView.setImageWithEntity(entity, null);

		setOrGone(this.nameView, entity.name);
		setOrGone(this.typeView, (entity.type + " patch").toUpperCase(Locale.US));

		privacyGroup.setVisibility((entity.visibility != null
			&& entity.visibility.equals(Constants.PRIVACY_PRIVATE)) ? VISIBLE : GONE);

		/* Members count */
		if (entity.countMembers > 0) {
			TextView watchingCount = (TextView) this.membersButton.findViewById(R.id.members_count);
			TextView watchingLabel = (TextView) this.membersButton.findViewById(R.id.members_label);
			if (watchingCount != null) {
				String label = getResources().getQuantityString(R.plurals.label_watching, entity.countMembers, entity.countMembers);
				watchingCount.setText(String.valueOf(entity.countMembers));
				watchingLabel.setText(label);
				UI.setVisibility(this.membersButton, View.VISIBLE);
			}
		}
		else {
			UI.setVisibility(this.membersButton, View.GONE);
		}

		/* Mute button */
		if (!entity.userMemberStatus.equals(MemberStatus.Member)) {
			UI.setVisibility(this.muteImageView, View.GONE);
		}
		else {
			if (entity.userMemberMuted) {
				/* Sound is off */
				muteImageView.setImageResource(R.drawable.ic_img_mute_off_dark);
			}
			else {
				/* Sound is on */
				muteImageView.setImageResource(R.drawable.ic_img_mute_dark);
			}
			UI.setVisibility(this.muteImageView, View.VISIBLE);
		}
	}

	public void bind(RealmEntity entity) {

		synchronized (lock) {
			this.entity = entity;
			draw();
		}
	}
}
