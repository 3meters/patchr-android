/*
 * Copyright 2013 Niek Haarman
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aircandi.ui.widgets;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.aircandi.Patch;
import com.aircandi.components.AnimationManager;
import com.aircandi.exceptions.NoOverflowMenuRuntimeException;
import com.aircandi.exceptions.NoTitleViewRuntimeException;
import com.aircandi.exceptions.ViewNotFoundRuntimeException;
import com.aircandi.ui.components.ShotStateStore;

public class ToolTipRelativeLayout extends RelativeLayout implements View.OnTouchListener {

	public static final String ACTION_BAR_TITLE     = "action_bar_title";
	public static final String ID                   = "id";
	public static final String ANDROID              = "android";
	public static final String ACTION_BAR           = "action_bar";
	public static final String ACTION_MENU_VIEW     = "ActionMenuView";
	public static final String OVERFLOW_MENU_BUTTON = "OverflowMenuButton";

	private final ShotStateStore mShotStateStore;
	private boolean mHideOnTouch = true;

	public ToolTipRelativeLayout(final Context context) {
		this(context, null);
	}

	public ToolTipRelativeLayout(final Context context, final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ToolTipRelativeLayout(final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs, defStyle);
		mShotStateStore = new ShotStateStore(Patch.applicationContext);
		setOnTouchListener(this);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public boolean onTouch(View view, MotionEvent motionEvent) {
		if (motionEvent.getAction() == MotionEvent.ACTION_UP && mHideOnTouch) {
			hide(true);
			return true;
		}
		return false;
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public ToolTipView showTooltip(final ToolTip toolTip) {
		final ToolTipView toolTipView = new ToolTipView(getContext());
		toolTipView.setToolTip(toolTip);
		addView(toolTipView);
		return toolTipView;
	}

	public ToolTipView showTooltipForView(final ToolTip toolTip, final View view) {
		final ToolTipView toolTipView = new ToolTipView(getContext());
		toolTipView.setToolTip(toolTip, view);
		addView(toolTipView);
		return toolTipView;
	}

	public ToolTipView showToolTipForViewResId(final Activity activity, final ToolTip toolTip, final int resId) {
		final ToolTipView toolTipView = new ToolTipView(getContext());
		final View decorView = activity.getWindow().getDecorView();
		final View view = decorView.findViewById(resId);

		if (view == null) {
			throw new ViewNotFoundRuntimeException();
		}

		toolTipView.setToolTip(toolTip, view);
		addView(toolTipView);
		return toolTipView;
	}

	public ToolTipView showToolTipForActionBarHome(final Activity activity, final ToolTip toolTip) {
		final int homeResId = android.R.id.home;
		return showToolTipForViewResId(activity, toolTip, homeResId);
	}

	public ToolTipView showToolTipForActionBarTitle(final Activity activity, final ToolTip toolTip) {
		final int titleResId = Resources.getSystem().getIdentifier(ACTION_BAR_TITLE, ID, ANDROID);
		if (titleResId == 0) {
			throw new NoTitleViewRuntimeException();
		}
		return showToolTipForViewResId(activity, toolTip, titleResId);
	}

	public ToolTipView showToolTipForActionBarOverflowMenu(final Activity activity, final ToolTip toolTip) {
		return showTooltipForView(toolTip, findActionBarOverflowMenuView(activity));
	}

	public void hide(boolean animate) {
		mShotStateStore.storeShot();
		if (animate) {
			fadeOutShowcase();
		}
		else {
			setVisibility(GONE);
		}
	}

	public void clear() {
		for (int i = getChildCount() - 1; i >= 0; i--) {
			View child = getChildAt(i);
			if (child instanceof ToolTipView) {
				removeView(child);
			}
		}
	}

	public void setSingleShot(long shotId) {
		mShotStateStore.setSingleShot(shotId);
	}

	public boolean hasShot() {
		return mShotStateStore.hasShot();
	}

	private void fadeOutShowcase() {
		AnimationManager.showViewAnimate(this, false, true, AnimationManager.DURATION_MEDIUM);
	}

	private static View findActionBarOverflowMenuView(final Activity activity) {
		final ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();

		final int actionBarViewResId = Resources.getSystem().getIdentifier(ACTION_BAR, ID, ANDROID);
		final ViewGroup actionBarView = (ViewGroup) decorView.findViewById(actionBarViewResId);

		ViewGroup actionMenuView = null;
		int actionBarViewChildCount = actionBarView.getChildCount();
		for (int i = 0; i < actionBarViewChildCount; ++i) {
			if (actionBarView.getChildAt(i).getClass().getSimpleName().equals(ACTION_MENU_VIEW)) {
				actionMenuView = (ViewGroup) actionBarView.getChildAt(i);
			}
		}

		if (actionMenuView == null) {
			throw new NoOverflowMenuRuntimeException();
		}

		int actionMenuChildCount = actionMenuView.getChildCount();
		View overflowMenuButton = null;
		for (int i = 0; i < actionMenuChildCount; ++i) {
			if (actionMenuView.getChildAt(i).getClass().getSimpleName().equals(OVERFLOW_MENU_BUTTON)) {
				overflowMenuButton = actionMenuView.getChildAt(i);
			}
		}

		if (overflowMenuButton == null) {
			throw new NoOverflowMenuRuntimeException();
		}

		return overflowMenuButton;
	}
}
