package com.aircandi.ui;

import android.content.res.Configuration;
import android.graphics.drawable.TransitionDrawable;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ViewAnimator;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.Patchr.ThemeTone;
import com.aircandi.R;
import com.aircandi.components.Extras;
import com.aircandi.components.StringManager;
import com.aircandi.interfaces.IEntityController;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Link;
import com.aircandi.objects.Message;
import com.aircandi.objects.Message.MessageType;
import com.aircandi.objects.Route;
import com.aircandi.objects.ViewHolder;
import com.aircandi.ui.base.BaseActivity;
import com.aircandi.ui.widgets.ToolTip;
import com.aircandi.ui.widgets.ToolTipRelativeLayout;
import com.aircandi.utilities.UI;

public class MessageListFragment extends EntityListFragment {

	private ViewAnimator mHeaderViewAnimator;

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

//	@Override
//	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//
//		View view = super.onCreateView(inflater, container, savedInstanceState);
//
//		/* Draw the header */
//		if (((BaseActivity) getActivity()).getEntity() != null) {
//			((BaseEntityForm) getActivity()).draw(view);
//		}
//
//		return view;
//	}

	@Override
	public void onClick(View v) {
		final Entity entity = (Entity) ((ViewHolder) v.getTag()).data;

		Extras extras = new Extras().setEntitySchema(Constants.SCHEMA_ENTITY_MESSAGE);
		Link link = entity.getParentLink(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_MESSAGE);
		/*
		 * We show replies as part of the parent message when the user is clicking from a list
		 * that isn't showing the parent message.
		 */
		extras.setEntityForId(mQuery.getEntityId());
		if (entity.type.equals(MessageType.REPLY)
				&& link != null
				&& (((BaseActivity) getActivity()).getEntity() == null
				|| !((BaseActivity) getActivity()).getEntity().id.equals(link.toId))) {
			extras.setEntityChildId(entity.id);
			extras.setEntityId(link.toId);
		}
		else {
			extras.setEntityId(entity.id);
		}

		Patchr.dispatch.route(getActivity(), Route.BROWSE, entity, null, extras.getExtras());
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		showTooltips();
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected void postBind() {
		super.postBind();
	}

	@Override
	protected void bindListItem(Entity entity, View view, String groupTag) {
		IEntityController controller = Patchr.getInstance().getControllerForEntity(entity);
		controller.bind(entity, view, groupTag);

		/* Special highlighting */

		if (mHighlightEntities.size() > 0) {
			view.setBackgroundResource(mBackgroundResId);
			if (mHighlightEntities.containsKey(entity.id)) {
				Highlight highlight = mHighlightEntities.get(entity.id);
				if (!highlight.isOneShot() || !highlight.hasFired()) {
					if (Patchr.themeTone.equals(ThemeTone.DARK)) {
						view.setBackgroundResource(R.drawable.bg_transition_fade);
					}
					else {
						view.setBackgroundResource(R.drawable.bg_transition_fade);
					}
					TransitionDrawable transition = (TransitionDrawable) view.getBackground();
					transition.startTransition(600);
					transition.reverseTransition(1200);
					highlight.setFired(true);
				}
			}
		}

		/* Special formatting for replies when show with parent message */

		Link link = entity.getParentLink(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_MESSAGE);
		Integer padding = UI.getRawPixelsForDisplayPixels(10f);
		Integer indent = UI.getRawPixelsForDisplayPixels(10f);

		if (entity.type != null && entity.type.equals(Message.MessageType.REPLY)
				&& link != null
				&& (((BaseActivity) getActivity()).getEntity() != null
				&& ((BaseActivity) getActivity()).getEntity().id.equals(link.toId))) {
			indent = UI.getRawPixelsForDisplayPixels(30f);
		}

		view.findViewById(R.id.item_row).setPadding(indent, padding, padding, padding);
		view.requestLayout();
	}

	public void showTooltips() {

		if (getActivity() instanceof PatchForm) {
			ToolTipRelativeLayout tooltipLayer = ((PatchForm) getActivity()).mTooltips;
			if (!tooltipLayer.hasShot()) {
				tooltipLayer.setVisibility(View.VISIBLE);
				tooltipLayer.setClickable(true);
				tooltipLayer.clear();
				tooltipLayer.requestLayout();

				View anchor = getActivity().findViewById(R.id.button_watch);
				if (anchor != null) {
					tooltipLayer.showTooltipForView(new ToolTip()
							.withText(StringManager.getString(R.string.tooltip_patch_watch))
							.setMaxWidth(UI.getRawPixelsForDisplayPixels(120f))
							.withShadow(true)
							.setArrowPosition(ToolTip.ArrowPosition.BELOW)
							.withAnimationType(ToolTip.AnimationType.FROM_TOP), anchor);
				}
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

 	/*--------------------------------------------------------------------------------------------
	 * Menus
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onCreateOptionsMenu(final Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		showTooltips();
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		/*
		 * Remove menu items per policy
		 */
		Entity entity = ((BaseActivity) getActivity()).getEntity();
		MenuItem item = menu.findItem(R.id.edit);
		if (item != null) {
			item.setVisible(Patchr.getInstance().getMenuManager().canUserEdit(entity));
		}
		item = menu.findItem(R.id.delete);
		if (item != null) {
			item.setVisible(Patchr.getInstance().getMenuManager().canUserDelete(entity));
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/
}