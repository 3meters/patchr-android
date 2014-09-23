package com.aircandi.catalina.ui;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ViewAnimator;

import com.aircandi.Aircandi;
import com.aircandi.Aircandi.ThemeTone;
import com.aircandi.catalina.Constants;
import com.aircandi.catalina.R;
import com.aircandi.catalina.objects.Message;
import com.aircandi.catalina.objects.Message.MessageType;
import com.aircandi.components.Extras;
import com.aircandi.components.MessagingManager;
import com.aircandi.components.StringManager;
import com.aircandi.controllers.IEntityController;
import com.aircandi.controllers.ViewHolder;
import com.aircandi.events.ProcessingCompleteEvent;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Link;
import com.aircandi.objects.Route;
import com.aircandi.ui.EntityListFragment;
import com.aircandi.ui.base.BaseActivity;
import com.aircandi.ui.base.BaseEntityForm;
import com.aircandi.ui.components.AnimationFactory;
import com.aircandi.ui.components.AnimationFactory.FlipDirection;
import com.aircandi.ui.widgets.AirListView;
import com.aircandi.ui.widgets.CandiView;
import com.aircandi.ui.widgets.ToolTip;
import com.aircandi.ui.widgets.ToolTipRelativeLayout;
import com.aircandi.utilities.UI;
import com.squareup.otto.Subscribe;

public class MessageListFragment extends EntityListFragment {

	private ViewAnimator mHeaderViewAnimator;

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View view = super.onCreateView(inflater, container, savedInstanceState);

		/* Draw the header */
		if (((BaseActivity) getActivity()).getEntity() != null) {
			((BaseEntityForm) getActivity()).draw(view);
		}

		if (mHeaderView != null) {
		    /*
			 * Parallax the photo
			 */
			mHeaderCandiView = mHeaderView.findViewById(R.id.candi_view);
			if (mHeaderCandiView != null) {
				View photo = mHeaderCandiView.findViewById(R.id.entity_photo);
				((AirListView) mListView).addParallaxedView(photo);
			}
			/*
			 * Grab the animator
			 */
			mHeaderViewAnimator = (ViewAnimator) mHeaderView.findViewById(R.id.animator_header);
		}

		if (mListView != null) {
			mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
				@Override
				public void onScrollStateChanged(AbsListView view, int scrollState) {
					if (scrollState == SCROLL_STATE_IDLE && mActivityStream) {
						if (MessagingManager.getInstance().getNewActivity()) {
							MessagingManager.getInstance().setNewActivity(false);
							if (getActivity() != null) {
								((com.aircandi.ui.AircandiForm) getActivity()).updateActivityAlert();
							}
							MessagingManager.getInstance().cancelNotifications();
						}
					}
				}

				@Override
				public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
				}
			});
		}

		return view;
	}

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

		Aircandi.dispatch.route(getActivity(), Route.BROWSE, entity, null, extras.getExtras());
	}

	@SuppressWarnings("ucd")
	public void onHeaderClick(View view) {
		AnimationFactory.flipTransition(mHeaderViewAnimator, FlipDirection.BOTTOM_TOP, 200);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	@Subscribe
	public void onProcessingComplete(ProcessingCompleteEvent event) {
		super.onProcessingComplete(event);
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
	protected void bindListItem(Entity entity, View view) {
		IEntityController controller = Aircandi.getInstance().getControllerForEntity(entity);
		controller.bind(entity, view);

		/* Special highlighting */

		if (mHighlightEntities.size() > 0) {
			view.setBackgroundResource(mBackgroundResId);
			if (mHighlightEntities.containsKey(entity.id)) {
				Highlight highlight = mHighlightEntities.get(entity.id);
				if (!highlight.isOneShot() || !highlight.hasFired()) {
					if (Aircandi.themeTone.equals(ThemeTone.DARK)) {
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

		if (entity.type.equals(Message.MessageType.REPLY)
				&& link != null
				&& (((BaseActivity) getActivity()).getEntity() != null
				&& ((BaseActivity) getActivity()).getEntity().id.equals(link.toId))) {
			indent = UI.getRawPixelsForDisplayPixels(30f);
		}

		view.findViewById(R.id.item_row).setPadding(indent, padding, padding, padding);
		view.requestLayout();
	}

	public void showTooltips() {

		if (getActivity() instanceof PlaceForm) {
			ToolTipRelativeLayout tooltipLayer = ((PlaceForm) getActivity()).mTooltips;
			if (!tooltipLayer.hasShot()) {
				tooltipLayer.setVisibility(View.VISIBLE);
				tooltipLayer.setClickable(true);
				tooltipLayer.clear();
				tooltipLayer.requestLayout();

				View anchor = getActivity().findViewById(R.id.button_watch);
				if (anchor != null) {
					tooltipLayer.showTooltipForView(new ToolTip()
							.withText(StringManager.getString(R.string.tooltip_place_watch))
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
			item.setVisible(Aircandi.getInstance().getMenuManager().canUserEdit(entity));
		}
		item = menu.findItem(R.id.delete);
		if (item != null) {
			item.setVisible(Aircandi.getInstance().getMenuManager().canUserDelete(entity));
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/
}