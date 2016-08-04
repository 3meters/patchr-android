/**
 * Copyright (c) 2012 Ephraim Tekle genzeb@gmail.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * @author Ephraim A. Tekle
 *
 */
package com.patchr.ui.components;

import android.support.annotation.NonNull;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.Interpolator;
import android.widget.ViewAnimator;

/**
 * This class contains methods for creating {@link Animation} objects for some of the most common animation, including a
 * 3D flip animation, {@link FlipAnimation}.
 * Furthermore, utility methods are provided for initiating fade-in-then-out and flip animations.
 *
 * @author Ephraim A. Tekle
 */
@SuppressWarnings("ucd")
public class AnimationFactory {

	private static final int DEFAULT_FLIP_TRANSITION_DURATION = 500;

	/**
	 * The {@code FlipDirection} enumeration defines the most typical flip view transitions: left-to-right and
	 * right-to-left. {@code FlipDirection} is used during the creation of {@link FlipAnimation} animations.
	 *
	 * @author Ephraim A. Tekle
	 */
	public enum FlipDirection {
		LEFT_RIGHT,
		RIGHT_LEFT,
		TOP_BOTTOM,
		BOTTOM_TOP;

		public float getStartDegreeForFirstView() {
			return 0;
		}

		public float getStartDegreeForSecondView() {
			switch (this) {
				case LEFT_RIGHT:
				case TOP_BOTTOM:
					return -90;
				case RIGHT_LEFT:
				case BOTTOM_TOP:
					return 90;
				default:
					return 0;
			}
		}

		public float getEndDegreeForFirstView() {
			switch (this) {
				case LEFT_RIGHT:
				case TOP_BOTTOM:
					return 90;
				case RIGHT_LEFT:
				case BOTTOM_TOP:
					return -90;
				default:
					return 0;
			}
		}

		public float getEndDegreeForSecondView() {
			return 0;
		}

		@NonNull
		public FlipDirection theOtherDirection() {
			switch (this) {
				case LEFT_RIGHT:
					return RIGHT_LEFT;
				case TOP_BOTTOM:
					return BOTTOM_TOP;
				case RIGHT_LEFT:
					return LEFT_RIGHT;
				case BOTTOM_TOP:
					return TOP_BOTTOM;
				default:
					return TOP_BOTTOM;
			}
		}
	}

	/**
	 * Create a pair of {@link FlipAnimation} that can be used to flip 3D transition from {@code fromView} to
	 * {@code toView}. A typical use case is with {@link ViewAnimator} as an out and in transition.
	 * <p/>
	 * NOTE: Avoid using this method. Instead, use {@link #flipTransition}.
	 *
	 * @param fromView     the view transition away from
	 * @param dir          the flip direction
	 * @param duration     the transition duration in milliseconds
	 * @param interpolator the interpolator to use (pass {@code null} to use the {@link AccelerateInterpolator} interpolator)
	 * @return animation array
	 */
	@NonNull
	public static Animation[] flipAnimation(@NonNull final View fromView, @NonNull FlipDirection dir, long duration, Interpolator interpolator) {
		Animation[] result = new Animation[2];
		float centerX;
		float centerY;

		centerX = fromView.getWidth() / 2.0f;
		centerY = fromView.getHeight() / 2.0f;

		FlipAnimation outFlip = new FlipAnimation(dir.getStartDegreeForFirstView(), dir.getEndDegreeForFirstView(), centerX, centerY,
				FlipAnimation.SCALE_DEFAULT, FlipAnimation.ScaleUpDownEnum.SCALE_DOWN);
		outFlip.setDuration(duration);
		outFlip.setFillAfter(true);
		outFlip.setInterpolator((interpolator == null) ? new AccelerateInterpolator() : interpolator);

		if (dir == FlipDirection.BOTTOM_TOP || dir == FlipDirection.TOP_BOTTOM)
			outFlip.setDirection(FlipAnimation.ROTATION_X);
		else
			outFlip.setDirection(FlipAnimation.ROTATION_Y);

		AnimationSet outAnimation = new AnimationSet(true);
		outAnimation.addAnimation(outFlip);
		result[0] = outAnimation;

		FlipAnimation inFlip = new FlipAnimation(dir.getStartDegreeForSecondView(), dir.getEndDegreeForSecondView(), centerX, centerY,
				FlipAnimation.SCALE_DEFAULT, FlipAnimation.ScaleUpDownEnum.SCALE_UP);
		inFlip.setDuration(duration);
		inFlip.setFillAfter(true);
		inFlip.setInterpolator((interpolator == null) ? new AccelerateInterpolator() : interpolator);
		inFlip.setStartOffset(duration);

		if (dir == FlipDirection.BOTTOM_TOP || dir == FlipDirection.TOP_BOTTOM)
			inFlip.setDirection(FlipAnimation.ROTATION_X);
		else
			inFlip.setDirection(FlipAnimation.ROTATION_Y);

		AnimationSet inAnimation = new AnimationSet(true);
		inAnimation.addAnimation(inFlip);
		result[1] = inAnimation;

		return result;

	}

	/**
	 * Flip to the next view of the {@code ViewAnimator}'s subviews. A call to this method will initiate a
	 * {@link FlipAnimation} to show the next View.
	 * If the currently visible view is the last view, flip direction will be reversed for this transition.
	 *
	 * @param viewAnimator the {@code ViewAnimator}
	 * @param dir          the direction of flip
	 */
	public static void flipTransition(@NonNull final ViewAnimator viewAnimator, @NonNull FlipDirection dir) {
		flipTransition(viewAnimator, dir, DEFAULT_FLIP_TRANSITION_DURATION);
	}

	/**
	 * Flip to the next view of the {@code ViewAnimator}'s subviews. A call to this method will initiate a
	 * {@link FlipAnimation} to show the next View.
	 * If the currently visible view is the last view, flip direction will be reversed for this transition.
	 *
	 * @param viewAnimator the {@code ViewAnimator}
	 * @param dir          the direction of flip
	 * @param duration     the transition duration in milliseconds
	 */
	public static void flipTransition(@NonNull final ViewAnimator viewAnimator, @NonNull FlipDirection dir, long duration) {

		final View fromView = viewAnimator.getCurrentView();
		final int currentIndex = viewAnimator.getDisplayedChild();
		final int nextIndex = (currentIndex + 1) % viewAnimator.getChildCount();

		Animation[] animc = AnimationFactory.flipAnimation(fromView, ((nextIndex < currentIndex) ? dir.theOtherDirection() : dir), duration, null);

		viewAnimator.setOutAnimation(animc[0]);
		viewAnimator.setInAnimation(animc[1]);

		viewAnimator.showNext();
	}
}
