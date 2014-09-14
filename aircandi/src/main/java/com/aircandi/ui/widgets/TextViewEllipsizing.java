package com.aircandi.ui.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.text.Layout;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;

import com.aircandi.R;

@SuppressWarnings("ucd")
public class TextViewEllipsizing extends AirTextView {

	private static final String ELLIPSIS = "&#8230;";

	private boolean mIsEllipsized;
	private boolean mIsStale;
	private boolean mProgrammaticChange;
	private String  mFullText;
	private int     mMaxLines                      = -1;
	private float   mLineSpacingMultiplier         = 1.0f;
	private float   mLineAdditionalVerticalPadding = 0.0f;
	private boolean mMirrorText                    = false;

	public TextViewEllipsizing(Context context) {
		this(context, null);
	}

	public TextViewEllipsizing(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public TextViewEllipsizing(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.TextViewEllipsizing, defStyle, 0);
		mMaxLines = ta.getInteger(R.styleable.TextViewEllipsizing_maxLines, -1);
		ta.recycle();
	}

	@Override
	protected void onTextChanged(CharSequence text, int start, int before, int after) {
		super.onTextChanged(text, start, before, after);
		if (!mProgrammaticChange) {
			mFullText = text.toString();
			mIsStale = true;
		}
	}

	@Override
	public void setText(CharSequence text, BufferType type) {
		super.setText(text, type);
		mFullText = text.toString();
		updateText();
	}

	@Override
	protected void onDraw(Canvas canvas) {

		if (mIsStale) {
			super.setEllipsize(null);
			updateText();
		}

		if (mMirrorText) {
			/* This saves off the matrix that the canvas applies to draws, so it can be restored later. */
			canvas.save();
			canvas.scale(1.0f, -1.0f, super.getWidth() * 0.5f, super.getHeight() * 0.5f);
			super.onDraw(canvas);
			canvas.restore();
		}
		else {
			super.onDraw(canvas);
		}
	}

	private void updateText() {

		final int maxLines = mMaxLines;
		String workingText = mFullText;
		boolean ellipsized = false;

		try {
			if (maxLines != -1) {
				final Layout layout = createWorkingLayout(workingText);
				if (layout.getLineCount() > maxLines) {
					workingText = workingText.substring(0, layout.getLineEnd(maxLines - 1)).trim();
					while (createWorkingLayout(workingText + ELLIPSIS).getLineCount() > maxLines) {
						int lastSpace = workingText.lastIndexOf(' ');
						if (lastSpace == -1) {
							break;
						}
						workingText = workingText.substring(0, lastSpace);
					}
					workingText = workingText + ELLIPSIS;
					ellipsized = true;
				}
			}

			if (ellipsized != mIsEllipsized) {
				mIsEllipsized = ellipsized;
			}
		}
		catch (Exception exception) { // $codepro.audit.disable emptyCatchClause
			/*
			 * Most likely happened because of rebuilding/recycling so we eat it
			 * i.e. StringIndexOutOfBoundsException
			 */
		}
	}

	private Layout createWorkingLayout(String workingText) {
		return new StaticLayout(workingText, getPaint(), getWidth() - getPaddingLeft() - getPaddingRight(),
				Alignment.ALIGN_NORMAL, mLineSpacingMultiplier, mLineAdditionalVerticalPadding, false);
	}

	public boolean isEllipsized() {
		return mIsEllipsized;
	}

	public boolean isMirrorText() {
		return mMirrorText;
	}

	@Override
	public void setMaxLines(int maxLines) {
		super.setMaxLines(maxLines);
		mMaxLines = maxLines;
		mIsStale = true;
	}

	@Override
	public int getMaxLines() {
		return mMaxLines;
	}

	@Override
	public void setLineSpacing(float add, float mult) {
		mLineAdditionalVerticalPadding = add;
		mLineSpacingMultiplier = mult;
		super.setLineSpacing(add, mult);
	}

	@Override
	public void setEllipsize(TruncateAt where) {
		// Ellipsize settings are not respected
	}

	public void setMirrorText(boolean mirrorText) {
		mMirrorText = mirrorText;
	}

	private interface EllipsizeListener {
		void ellipsizeStateChanged(boolean ellipsized);
	}

}