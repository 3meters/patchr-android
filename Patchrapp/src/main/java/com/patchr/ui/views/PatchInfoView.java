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

	private TextView nameView;
	private TextView typeView;
	private TextView descriptionView;
	private View     privacyGroup;
	private TextView ownerNameView;
	public  Button   expandoButton;

	public PatchInfoView(Context context) {
		this(context, null, 0);
	}

	public PatchInfoView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public PatchInfoView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		layoutResId = R.layout.view_patch_info;
		initialize();
	}

	public PatchInfoView(Context context, Integer layoutResId) {
		super(context, null, 0);
		this.layoutResId = layoutResId;
		initialize();
	}

	protected void initialize() {

		ViewGroup layout = (ViewGroup) LayoutInflater.from(getContext()).inflate(this.layoutResId, this, true);

		nameView = (TextView) layout.findViewById(R.id.name);
		typeView = (TextView) layout.findViewById(R.id.type);
		privacyGroup = (View) layout.findViewById(R.id.privacy_group);
		descriptionView = (TextView) layout.findViewById(R.id.description);
		ownerNameView = (TextView) layout.findViewById(R.id.owner_name);
		expandoButton = (Button) layout.findViewById(R.id.expando_button);
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

	public void bind(RealmEntity entity) {

		synchronized (lock) {
			this.entity = entity;
			draw();
		}
	}

	public void draw() {

		setOrGone(nameView, entity.name);
		setOrGone(descriptionView, entity.description);
		setOrGone(typeView, (entity.type + " patch").toUpperCase(Locale.US));
		if (entity.owner != null) {
			ownerNameView.setText(entity.owner.name);
		}

		privacyGroup.setVisibility((entity.visibility != null
			&& entity.visibility.equals(Constants.PRIVACY_PRIVATE)) ? VISIBLE : GONE);

		UI.setVisibility(expandoButton, View.GONE);
		if (!TextUtils.isEmpty(entity.description)) {
			expandoButton.setText(StringManager.getString(R.string.button_text_expand));
			UI.setVisibility(expandoButton, View.VISIBLE);
			expandoButton.setOnClickListener(this);
		}
	}

	public void toggleExpando() {
		if (descriptionView != null) {
			int maxLines = Integers.getInteger(R.integer.max_lines_patch_description);
			boolean collapsed = ((String) expandoButton.getTag()).equals("collapsed");
			descriptionView.setMaxLines(collapsed ? Integer.MAX_VALUE : maxLines);
			expandoButton.setText(StringManager.getString(collapsed
			                                                   ? R.string.button_text_collapse
			                                                   : R.string.button_text_expand));
			expandoButton.setTag(collapsed ? "expanded" : "collapsed");
		}
	}
}
