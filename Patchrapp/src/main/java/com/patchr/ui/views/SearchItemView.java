package com.patchr.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.patchr.Constants;
import com.patchr.R;
import com.patchr.model.RealmEntity;
import com.patchr.ui.widgets.ImageWidget;
import com.patchr.utilities.UI;

@SuppressWarnings("ucd")
public class SearchItemView extends BaseView {

	private static final Object lock = new Object();

	public    RealmEntity entity;
	protected Integer     layoutResId;

	protected ViewGroup   layout;
	private   ImageWidget userPhoto;
	private   ImageWidget patchPhoto;
	private   TextView    name;
	private   TextView    type;
	private   TextView    subtitle;

	public SearchItemView(Context context) {
		this(context, null, 0);
	}

	public SearchItemView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public SearchItemView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.layoutResId = R.layout.view_search_item;
		initialize();
	}

	public SearchItemView(Context context, Integer layoutResId) {
		super(context, null, 0);
		this.layoutResId = layoutResId;
		initialize();
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	private void initialize() {

		this.layout = (ViewGroup) LayoutInflater.from(getContext()).inflate(this.layoutResId, this, true);

		ListView.LayoutParams params = new ListView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		this.setLayoutParams(params);

		this.userPhoto = (ImageWidget) layout.findViewById(R.id.user_photo);
		this.patchPhoto = (ImageWidget) layout.findViewById(R.id.patch_photo);
		this.name = (TextView) layout.findViewById(R.id.name);
		this.type = (TextView) layout.findViewById(R.id.type);
		this.subtitle = (TextView) layout.findViewById(R.id.subtitle);
	}

	public void bind(RealmEntity entity) {

		synchronized (lock) {

			this.entity = entity;

			UI.setVisibility(this.userPhoto, GONE);
			UI.setVisibility(this.patchPhoto, GONE);

			if (entity.schema.equals(Constants.SCHEMA_ENTITY_PATCH)) {
				this.patchPhoto.setImageWithEntity(entity);
				UI.setVisibility(this.patchPhoto, VISIBLE);
			}
			else if (entity.schema.equals(Constants.SCHEMA_ENTITY_USER)) {
				this.userPhoto.setImageWithEntity(entity);
				UI.setVisibility(this.userPhoto, VISIBLE);
			}

			setOrGone(this.name, entity.name);
			setOrGone(this.type, entity.type);
			setOrGone(this.subtitle, entity.subtitle);
		}
	}
}
