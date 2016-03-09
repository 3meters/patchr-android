package com.patchr.ui.views;

import android.content.Context;
import android.graphics.PorterDuff;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.patchr.Constants;
import com.patchr.R;
import com.patchr.objects.CacheStamp;
import com.patchr.objects.Count;
import com.patchr.objects.Entity;
import com.patchr.objects.Link.Direction;
import com.patchr.objects.Patch;
import com.patchr.objects.Photo;
import com.patchr.utilities.UI;
import com.patchr.utilities.Utils;

import java.util.Locale;

@SuppressWarnings("ucd")
public class PatchView extends RelativeLayout {

	private static final Object lock = new Object();

	protected Entity entity;

	protected BaseView base = new BaseView();
	protected ViewGroup  layout;
	protected PhotoView  photoView;
	protected TextView   type;
	protected TextView   name;
	protected TextView   index;
	protected TextView   distance;
	protected TextView   messageCount;
	protected TextView   watchCount;
	protected View       privacyGroup;
	protected CacheStamp cacheStamp;

	public PatchView(Context context) {
		this(context, null);
	}

	public PatchView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public PatchView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initialize();
	}

	protected void initialize() {

		this.layout = (ViewGroup) LayoutInflater.from(getContext()).inflate(R.layout.patch_view, this, true);
		this.photoView = (PhotoView) layout.findViewById(R.id.photo_view);
		this.name = (TextView) layout.findViewById(R.id.name);
		this.distance = (TextView) layout.findViewById(R.id.distance);
		this.type = (TextView) layout.findViewById(R.id.type);
		this.messageCount = (TextView) layout.findViewById(R.id.message_count);
		this.watchCount = (TextView) layout.findViewById(R.id.watch_count);
		this.privacyGroup = (View) layout.findViewById(R.id.privacy_group);
		this.index = (TextView) layout.findViewById(R.id.index);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void databind(Patch entity) {

		synchronized (lock) {

			this.entity = (Entity) entity;
			this.cacheStamp = entity.getCacheStamp();

			/* Primary photo */
			this.photoView.getBackground().clearColorFilter();

			if (entity.photo != null) {
				/* Optimize if we already have the image */
				if (this.photoView.getPhoto() != null && this.photoView.getImageView().getDrawable() != null) {
					if (Photo.same(this.photoView.getPhoto(), entity.getPhoto())) return;
				}

				this.photoView.setTag(entity.photo);
				UI.drawPhoto(this.photoView, entity.photo);
			}
			else {
				this.photoView.getImageView().setImageDrawable(null);
				if (!TextUtils.isEmpty(entity.name)) {
					long seed = Utils.numberFromName(entity.name);
					Integer color = Utils.randomColor(seed);
					this.photoView.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
				}
			}

			/* Name */
			base.setOrGone(this.name, entity.name);
			base.setOrGone(this.index, String.valueOf(entity.index.intValue()));
			base.setOrGone(this.type, (entity.type + " patch").toUpperCase(Locale.US));

			/* Privacy */
			privacyGroup.setVisibility((entity.privacy != null && entity.privacy.equals(Constants.PRIVACY_PRIVATE)) ? VISIBLE : GONE);

			/* Message count for nearby list */
			Count messageCount = entity.getCount(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_MESSAGE, null, Direction.in);
			base.setOrGone(this.messageCount, (messageCount != null) ? String.valueOf(messageCount.count.intValue()) : "0");

			/* Watch count for nearby list */
			Count watchCount = entity.getCount(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_USER, true, Direction.in);
			base.setOrGone(this.watchCount, (watchCount != null) ? String.valueOf(watchCount.count.intValue()) : "0");

			/* Distance */
			final Float distance = entity.getDistance(true); // In meters
			final String distanceFormatted = Utils.distanceFormatted(distance);
			base.setOrGone(this.distance, distanceFormatted);
		}
	}
}
