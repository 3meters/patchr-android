package com.patchr.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.patchr.Constants;
import com.patchr.R;
import com.patchr.objects.CacheStamp;
import com.patchr.objects.Count;
import com.patchr.objects.Entity;
import com.patchr.objects.Link;
import com.patchr.objects.Patch;
import com.patchr.ui.widgets.ImageWidget;
import com.patchr.utilities.Utils;

import java.util.Locale;

@SuppressWarnings("ucd")
public class PatchView extends FrameLayout {

	private static final Object lock = new Object();

	public    Entity     entity;
	protected CacheStamp cacheStamp;
	protected BaseView   base;
	protected Integer    layoutResId;

	protected ViewGroup   layout;
	protected ImageWidget photoView;
	protected TextView    type;
	protected TextView    name;
	protected TextView    index;
	protected TextView    distance;
	protected TextView    messageCount;
	protected TextView    watchCount;
	protected View        privacyGroup;

	public PatchView(Context context) {
		this(context, null, 0);
	}

	public PatchView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public PatchView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.layoutResId = R.layout.view_patch;
		initialize();
	}

	public PatchView(Context context, Integer layoutResId) {
		super(context, null, 0);
		this.layoutResId = layoutResId;
		initialize();
	}

	protected void initialize() {

		this.base = new BaseView();
		this.layout = (ViewGroup) LayoutInflater.from(getContext()).inflate(this.layoutResId, this, true);

		this.photoView = (ImageWidget) layout.findViewById(R.id.photo);
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

	public void bind(Entity entity) {

		synchronized (lock) {

			this.entity = entity;
			this.cacheStamp = entity.getCacheStamp();
			this.photoView.setImageWithEntity(entity);
			base.setOrGone(this.name, entity.name);

			if (entity instanceof Patch) {
				Patch patch = (Patch) entity;

				base.setOrGone(this.index, String.valueOf(patch.index.intValue()));
				base.setOrGone(this.type, (patch.type + " patch").toUpperCase(Locale.US));

				/* Privacy */
				privacyGroup.setVisibility((patch.privacy != null && patch.privacy.equals(Constants.PRIVACY_PRIVATE)) ? VISIBLE : GONE);

				/* Message count for nearby list */
				Count messageCount = patch.getCount(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_MESSAGE, null, Link.Direction.in);
				base.setOrGone(this.messageCount, (messageCount != null) ? String.valueOf(messageCount.count.intValue()) : "0");

				/* Watch count for nearby list */
				Count watchCount = patch.getCount(Constants.TYPE_LINK_MEMBER, Constants.SCHEMA_ENTITY_USER, true, Link.Direction.in);
				base.setOrGone(this.watchCount, (watchCount != null) ? String.valueOf(watchCount.count.intValue()) : "0");

				/* Distance */
				final Float distance = patch.getDistance(true); // In meters
				final String distanceFormatted = Utils.distanceFormatted(distance);
				base.setOrGone(this.distance, distanceFormatted);
			}
		}
	}
}
