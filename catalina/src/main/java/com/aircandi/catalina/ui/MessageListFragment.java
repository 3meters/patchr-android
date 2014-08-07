package com.aircandi.catalina.ui;

import android.app.Activity;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewAnimator;

import com.aircandi.Aircandi;
import com.aircandi.Aircandi.ThemeTone;
import com.aircandi.catalina.Constants;
import com.aircandi.catalina.R;
import com.aircandi.catalina.objects.Message;
import com.aircandi.catalina.objects.Message.MessageType;
import com.aircandi.components.Extras;
import com.aircandi.controllers.IEntityController;
import com.aircandi.controllers.ViewHolder;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Link;
import com.aircandi.objects.Route;
import com.aircandi.ui.EntityListFragment;
import com.aircandi.ui.base.BaseActivity;
import com.aircandi.ui.components.AnimationFactory;
import com.aircandi.ui.components.AnimationFactory.FlipDirection;
import com.aircandi.ui.widgets.AirListView;
import com.aircandi.utilities.UI;

public class MessageListFragment extends EntityListFragment {

	private ViewAnimator mHeaderViewAnimator;

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View view = super.onCreateView(inflater, container, savedInstanceState);

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
				&& (((BaseActivity) getSherlockActivity()).getEntity() == null
				|| !((BaseActivity) getSherlockActivity()).getEntity().id.equals(link.toId))) {
			extras.setEntityChildId(entity.id);
			extras.setEntityId(link.toId);
		}
		else {
			extras.setEntityId(entity.id);
		}

		Aircandi.dispatch.route(getSherlockActivity(), Route.BROWSE, entity, null, extras.getExtras());
	}

	@SuppressWarnings("ucd")
	public void onHeaderClick(View view) {
		AnimationFactory.flipTransition(mHeaderViewAnimator, FlipDirection.BOTTOM_TOP, 200);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	@Override
	protected void postBind() {
		super.postBind();
	}

	@Override
	protected void bindListItem(Entity entity, View view) {
		IEntityController controller = Aircandi.getInstance().getControllerForEntity(entity);
		controller.bind(entity, view);

		/* Special highlighting */

		if (Constants.SUPPORTS_ICE_CREAM_SANDWICH) {
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
		}

		/* Special formatting for replies when show with parent message */

		Link link = entity.getParentLink(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_MESSAGE);
		Integer padding = UI.getRawPixelsForDisplayPixels(10f);
		Integer indent = UI.getRawPixelsForDisplayPixels(10f);

		if (entity.type.equals(Message.MessageType.REPLY)
				&& link != null
				&& (((BaseActivity) getSherlockActivity()).getEntity() != null
				&& ((BaseActivity) getSherlockActivity()).getEntity().id.equals(link.toId))) {
			indent = UI.getRawPixelsForDisplayPixels(30f);
		}

		view.findViewById(R.id.item_row).setPadding(indent, padding, padding, padding);
		view.requestLayout();
	}

	// --------------------------------------------------------------------------------------------
	// Properties
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Menus
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Lifecycle
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------------------------------
}