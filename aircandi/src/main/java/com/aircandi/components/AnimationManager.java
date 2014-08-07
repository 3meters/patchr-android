package com.aircandi.components;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.content.res.XmlResourceParser;
import android.os.Build;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;

import com.aircandi.Aircandi;
import com.aircandi.R;
import com.aircandi.objects.TransitionType;

public class AnimationManager {

	public static Integer DURATION_MEDIUM = 500;
	private static Animation mFadeInMedium;

	public static Animation fadeInMedium() {
		/*
		 * We make a new animation object each time because when I
		 * tried sharing one, there was lots of flashing and weird behavior.
		 * 
		 * If there is a better way to do this later then this will serve
		 * as a choke point for the implementation.
		 */
		mFadeInMedium = loadAnimation(R.anim.fade_in_medium);
		return mFadeInMedium;
	}

	public void doOverridePendingTransition(Activity activity, Integer transitionType) {
		doOverridePendingTransitionDefault(activity, transitionType);
	}

	@SuppressWarnings("ucd")
	public void doOverridePendingTransitionDefault(Activity activity, Integer transitionType) {
		/*
		 * Default android animations are used unless overridden here.
		 */
		if (transitionType == TransitionType.PAGE_TO_HELP) {
			activity.overridePendingTransition(R.anim.fade_in_short, R.anim.hold);
		}
		else if (transitionType == TransitionType.HELP_TO_PAGE) {
			activity.overridePendingTransition(R.anim.hold, R.anim.fade_out_short);
		}

	}

	public static Animation loadAnimation(int animationResId) throws NotFoundException {
		/*
		 * Loads an animation object from a resource
		 * 
		 * @param id The resource id of the animation to load
		 * 
		 * @return The animation object reference by the specified id
		 * 
		 * @throws NotFoundException when the animation cannot be loaded
		 */

		XmlResourceParser parser = null;
		try {
			parser = Aircandi.applicationContext.getResources().getAnimation(animationResId);
			return createAnimationFromXml(Aircandi.applicationContext, parser);
		}
		catch (XmlPullParserException ex) {
			final NotFoundException rnf = new NotFoundException("Can't load animation resource ID #0x" + Integer.toHexString(animationResId));
			rnf.initCause(ex);
			throw rnf;
		}
		catch (IOException ex) {
			final NotFoundException rnf = new NotFoundException("Can't load animation resource ID #0x" + Integer.toHexString(animationResId));
			rnf.initCause(ex);
			throw rnf;
		}
		finally {
			if (parser != null) {
				parser.close();
			}
		}
	}

	private static Animation createAnimationFromXml(Context c, XmlPullParser parser) throws XmlPullParserException, IOException {
		return createAnimationFromXml(c, parser, null, Xml.asAttributeSet(parser));
	}

	private static Animation createAnimationFromXml(Context c, XmlPullParser parser, AnimationSet parent, AttributeSet attrs) throws XmlPullParserException,
			IOException {

		Animation anim = null;

		/* Make sure we are on a start tag. */
		int type;
		final int depth = parser.getDepth();

		while (((type = parser.next()) != XmlPullParser.END_TAG || parser.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {

			if (type != XmlPullParser.START_TAG) {
				continue;
			}

			String name = parser.getName();

			if (name.equals("set")) {
				anim = new AnimationSet(c, attrs);
				createAnimationFromXml(c, parser, (AnimationSet) anim, attrs);
			}
			else if (name.equals("alpha")) {
				anim = new AlphaAnimation(c, attrs);
			}
			else if (name.equals("scale")) {
				anim = new ScaleAnimation(c, attrs);
			}
			else if (name.equals("rotate")) {
				anim = new RotateAnimation(c, attrs);
			}
			else if (name.equals("translate")) {
				anim = new TranslateAnimation(c, attrs);
			}
			else
				throw new RuntimeException("Unknown animation name: " + parser.getName());

			if (parent != null) {
				parent.addAnimation(anim);
			}
		}
		return anim;
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class DepthPageTransformer implements ViewPager.PageTransformer {
		private float MIN_SCALE = 0.75f;

		@Override
		public void transformPage(View view, float position) {
			int pageWidth = view.getWidth();

			if (position < -1) { // [-Infinity,-1)
				// This page is way off-screen to the left.
				view.setAlpha(0);

			}
			else if (position <= 0) { // [-1,0]
				// Use the default slide transition when moving to the left page
				view.setAlpha(1);
				view.setTranslationX(0);
				view.setScaleX(1);
				view.setScaleY(1);

			}
			else if (position <= 1) { // (0,1]
				// Fade the page out.
				view.setAlpha(1 - position);

				// Counteract the default slide transition
				view.setTranslationX(pageWidth * -position);

				// Scale the page down (between MIN_SCALE and 1)
				float scaleFactor = MIN_SCALE
						+ (1 - MIN_SCALE) * (1 - Math.abs(position));
				view.setScaleX(scaleFactor);
				view.setScaleY(scaleFactor);

			}
			else { // (1,+Infinity]
				// This page is way off-screen to the right.
				view.setAlpha(0);
			}
		}
	}

}
