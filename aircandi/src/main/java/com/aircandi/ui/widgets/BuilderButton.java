package com.aircandi.ui.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aircandi.Aircandi;
import com.aircandi.Aircandi.ThemeTone;
import com.aircandi.R;

@SuppressWarnings("ucd")
public class BuilderButton extends RelativeLayout {

	private TextView     mTextView;
	private LinearLayout mViewGroup;
	private String       mHint;
	private Integer      mLayoutId;

	public BuilderButton(Context context) {
		this(context, null);
	}

	public BuilderButton(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public BuilderButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.BuilderButton, defStyle, 0);
		mHint = ta.getString(R.styleable.BuilderButton_hint);
		mLayoutId = ta.getResourceId(R.styleable.BuilderButton_layout, R.layout.widget_builder_button);
		ta.recycle();

		initialize();
	}

	private void initialize() {
		final View view = LayoutInflater.from(getContext()).inflate(mLayoutId, this);

		mTextView = (TextView) view.findViewById(R.id.builder_text);
		mViewGroup = (LinearLayout) view.findViewById(R.id.builder_images);

		if (mTextView != null && mHint != null) {
			if (Aircandi.themeTone.equals(ThemeTone.DARK)) {
				mTextView.setTextColor(getResources().getColor(R.color.text_secondary_dark));
			}
			else {
				mTextView.setTextColor(getResources().getColor(R.color.text_secondary_light));
			}
			mTextView.setText(mHint);
		}
	}

	public void setText(String text) {
		if (mTextView != null) {
			if (text != null && !text.equals("")) {
				if (Aircandi.themeTone.equals(ThemeTone.DARK)) {
					mTextView.setTextColor(getResources().getColor(R.color.text_dark));
				}
				else {
					mTextView.setTextColor(getResources().getColor(R.color.text_light));
				}
				mTextView.setText(text);
			}
			else {
				if (Aircandi.themeTone.equals(ThemeTone.DARK)) {
					mTextView.setTextColor(getResources().getColor(R.color.text_secondary_dark));
				}
				else {
					mTextView.setTextColor(getResources().getColor(R.color.text_secondary_light));
				}
				mTextView.setText(mHint);
			}
		}
	}

	public String getText() {
		if (mTextView != null) {
			final String text = mTextView.getText().toString();
			if (!text.equals(mHint)) return text;
		}
		return null;
	}

	public LinearLayout getViewGroup() {
		return mViewGroup;
	}

	public void setViewGroup(LinearLayout viewGroup) {
		mViewGroup = viewGroup;
	}

	public TextView getTextView() {
		return mTextView;
	}

	public void setTextView(TextView textView) {
		mTextView = textView;
	}
}