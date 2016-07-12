package com.patchr.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.patchr.Constants;
import com.patchr.R;
import com.patchr.model.RealmEntity;
import com.patchr.objects.CacheStamp;
import com.patchr.ui.widgets.ImageWidget;
import com.patchr.utilities.Utils;

import java.util.Locale;

@SuppressWarnings("ucd")
public class PatchView extends BaseView {

	private static final Object lock = new Object();

	public    RealmEntity entity;
	protected CacheStamp  cacheStamp;
	protected Integer     layoutResId;

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

	public void bind(RealmEntity entity) {

		synchronized (lock) {

			this.entity = entity;
			this.photoView.setImageWithEntity(entity, null);
			setOrGone(this.name, entity.name);

			if (entity.schema.equals(Constants.SCHEMA_ENTITY_PATCH)) {

				setOrGone(this.index, String.valueOf(entity.index.intValue()));
				setOrGone(this.type, (entity.type + " patch").toUpperCase(Locale.US));

				/* Privacy */
				privacyGroup.setVisibility((entity.visibility != null && entity.visibility.equals(Constants.PRIVACY_PRIVATE)) ? VISIBLE : GONE);

				/* Message count for nearby list */
				setOrGone(this.messageCount, String.valueOf(entity.countMessages));

				/* Watch count for nearby list */
				setOrGone(this.watchCount, String.valueOf(entity.countMembers));

				/* Distance */
				final Float distance = entity.distance; // In meters
				final String distanceFormatted = Utils.distanceFormatted(distance);
				setOrGone(this.distance, distanceFormatted);
			}
		}
	}
}
