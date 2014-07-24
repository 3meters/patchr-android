package com.aircandi.catalina.ui.widgets;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.aircandi.catalina.Constants;
import com.aircandi.catalina.R;
import com.aircandi.objects.Count;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Link.Direction;
import com.aircandi.objects.Photo;
import com.aircandi.objects.Place;
import com.aircandi.objects.Shortcut;
import com.aircandi.objects.ShortcutSettings;
import com.aircandi.utilities.Integers;

@SuppressWarnings("ucd")
public class CandiView extends com.aircandi.ui.widgets.CandiView {

	public static final int	HORIZONTAL	= 0;
	public static final int	VERTICAL	= 1;
	protected TextView		mCount;

	List<Shortcut>			mShortcuts	= new ArrayList<Shortcut>();

	public CandiView(Context context) {
		this(context, null);
	}

	public CandiView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public CandiView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	public void databind(Entity entity, IndicatorOptions options) {
		options.forceUpdate = true;
		super.databind(entity, options);
	}

	@Override
	protected void initialize() {
		super.initialize();
		mCount = (TextView) mLayout.findViewById(R.id.count);
	}

	@Override
	public void showIndicators(Entity entity, IndicatorOptions options) {

		/* Indicators */
		setVisibility(mHolderShortcuts, View.GONE);
		setVisibility(mCount, View.GONE);

		if (entity instanceof Place) {
			if (!((Place) entity).visibleToCurrentUser()) return;
		}

		if (mHolderShortcuts != null) {

			ShortcutSettings settings = new ShortcutSettings(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_MESSAGE, Direction.in, false, false);
			List<Shortcut> shortcuts = (List<Shortcut>) entity.getShortcuts(settings, null, new Shortcut.SortByPositionSortDate());

			Boolean dirty = (mShortcuts.size() != shortcuts.size());
			if (!dirty) {
				Integer i = 0;
				for (Shortcut shortcut : mShortcuts) {
					if (!shortcut.creator.id.equals(shortcuts.get(i).creator.id)) {
						dirty = true;
						break;
					}
					if (shortcut.description != null && !shortcut.description.equals(shortcuts.get(i).description)) {
						dirty = true;
						break;
					}
					if (!Photo.same(shortcut.getPhoto(), shortcuts.get(i).getPhoto())) {
						dirty = true;
						break;
					}
					i++;
				}
			}

			if (dirty) {
				mHolderShortcuts.removeAllViews();

				final LayoutInflater inflater = LayoutInflater.from(this.getContext());

				/* We only show the first two */
				int shortcutCount = 0;
				for (Shortcut shortcut : shortcuts) {
					if (shortcutCount < Integers.getInteger(R.integer.limit_indicators_radar)) {
						View view = inflater.inflate(R.layout.temp_indicator_message, null);
						TextView name = (TextView) view.findViewById(R.id.indicator_name);
						TextView message = (TextView) view.findViewById(R.id.indicator_message);
						name.setText(shortcut.creator.name);
						if (!TextUtils.isEmpty(shortcut.description)) {
							message.setText(shortcut.description);
						}
						else if (shortcut.photo != null) {
							message.setText("+photo");
						}
						mHolderShortcuts.addView(view);
					}
					shortcutCount++;
				}
				mShortcuts = shortcuts;
			}

			if (mShortcuts.size() > 0) {
				setVisibility(mHolderShortcuts, View.VISIBLE);
			}

			Count messageCount = entity.getCount(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_MESSAGE, null, Direction.in);
			if (messageCount != null && messageCount.count.intValue() > Integers.getInteger(R.integer.limit_indicators_radar)) {
				Integer extras = messageCount.count.intValue() - Integers.getInteger(R.integer.limit_indicators_radar);
				mCount.setText("+" + extras);
				setVisibility(mCount, View.VISIBLE);
			}
		}
	}
}
