package com.patchr.ui.views;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.patchr.Constants;
import com.patchr.R;
import com.patchr.components.StringManager;
import com.patchr.model.RealmEntity;
import com.patchr.utilities.Integers;
import com.patchr.utilities.UI;

import java.util.Locale;

@SuppressWarnings("ucd")
public class PatchInfoView extends BaseView implements View.OnClickListener {

	private static final Object lock = new Object();

	public  RealmEntity entity;
	private Integer     layoutResId;

	private ViewGroup layout;
	private TextView  name;
	private TextView  type;
	private TextView  description;
	private View      privacyGroup;
	private TextView  ownerName;
	public  Button    expandoButton;
	private View      moreButton;

	public PatchInfoView(Context context) {
		this(context, null, 0);
	}

	public PatchInfoView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public PatchInfoView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.layoutResId = R.layout.view_patch_info;
		initialize();
	}

	public PatchInfoView(Context context, Integer layoutResId) {
		super(context, null, 0);
		this.layoutResId = layoutResId;
		initialize();
	}

	protected void initialize() {

		this.layout = (ViewGroup) LayoutInflater.from(getContext()).inflate(this.layoutResId, this, true);

		this.name = (TextView) layout.findViewById(R.id.name);
		this.type = (TextView) layout.findViewById(R.id.type);
		this.privacyGroup = (View) layout.findViewById(R.id.privacy_group);
		this.description = (TextView) layout.findViewById(R.id.description);
		this.ownerName = (TextView) layout.findViewById(R.id.owner_name);
		this.expandoButton = (Button) layout.findViewById(R.id.expando_button);
		this.moreButton = (View) layout.findViewById(R.id.next_page_button);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public void onClick(View view) {
		if (view.getId() == R.id.expando_button) {
			toggleExpando();
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void databind(RealmEntity entity) {

		synchronized (lock) {

			this.entity = entity;

			setOrGone(this.name, entity.name);
			setOrGone(this.description, entity.description);
			setOrGone(this.type, (entity.type + " patch").toUpperCase(Locale.US));
			if (entity.owner != null) {
				ownerName.setText(entity.owner.name);
			}

			privacyGroup.setVisibility((entity.visibility != null
				&& entity.visibility.equals(Constants.PRIVACY_PRIVATE)) ? VISIBLE : GONE);

			UI.setVisibility(this.expandoButton, View.GONE);
			if (!TextUtils.isEmpty(entity.description)) {
				this.expandoButton.setText(StringManager.getString(R.string.button_text_expand));
				UI.setVisibility(this.expandoButton, View.VISIBLE);
				this.expandoButton.setOnClickListener(this);
			}
		}
	}

	public void toggleExpando() {
		if (description != null) {
			int maxLines = Integers.getInteger(R.integer.max_lines_patch_description);
			boolean collapsed = ((String) this.expandoButton.getTag()).equals("collapsed");
			description.setMaxLines(collapsed ? Integer.MAX_VALUE : maxLines);
			this.expandoButton.setText(StringManager.getString(collapsed
			                                                   ? R.string.button_text_collapse
			                                                   : R.string.button_text_expand));
			this.expandoButton.setTag(collapsed ? "expanded" : "collapsed");
		}
	}
}
