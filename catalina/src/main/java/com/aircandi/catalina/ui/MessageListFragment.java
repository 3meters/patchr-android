package com.aircandi.catalina.ui;

import android.app.Activity;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.aircandi.Aircandi;
import com.aircandi.Aircandi.ThemeTone;
import com.aircandi.catalina.Constants;
import com.aircandi.catalina.R;
import com.aircandi.catalina.objects.Message;
import com.aircandi.catalina.objects.Message.MessageType;
import com.aircandi.components.EntityManager;
import com.aircandi.components.Extras;
import com.aircandi.objects.Count;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Link;
import com.aircandi.objects.Link.Direction;
import com.aircandi.objects.Photo;
import com.aircandi.objects.Route;
import com.aircandi.ui.EntityListFragment;
import com.aircandi.ui.base.BaseActivity;
import com.aircandi.ui.components.AnimationFactory;
import com.aircandi.ui.components.AnimationFactory.FlipDirection;
import com.aircandi.ui.widgets.AirListView;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.DateTime.IntervalContext;
import com.aircandi.utilities.UI;

public class MessageListFragment extends EntityListFragment {

	private ViewAnimator	mHeaderViewAnimator;

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
	public ViewHolder bindHolder(View view, ViewHolder holder) {

		if (holder == null) {
			holder = new ViewHolderExtended();
		}
		((ViewHolderExtended) holder).childCount = (TextView) view.findViewById(R.id.child_count);

		return super.bindHolder(view, holder);
	}

	@Override
	protected void drawListItem(Entity entity, View view, final ViewHolder holder) {

		/* Place context */

		UI.setVisibility(holder.placeName, View.GONE);
		if (holder.placeName != null) {
			Entity parentEntity = entity.place;
			if (parentEntity == null) {
				parentEntity = EntityManager.getCacheEntity(entity.placeId);
			}
			if (parentEntity != null) {
				holder.placeName.setText(parentEntity.name);
				UI.setVisibility(holder.placeName, View.VISIBLE);
			}
		}

		/* User photo */

		UI.setVisibility(holder.userPhotoView, View.GONE);
		if (holder.userPhotoView != null && entity.creator != null) {
			/*
			 * Acting a cheap proxy for user view so setting photoview to entity instead of photo.
			 */
			Photo photo = entity.creator.getPhoto();
			if (holder.userPhotoView.getPhoto() == null || !holder.userPhotoView.getPhoto().getUri().equals(photo.getUri())) {
				UI.drawPhoto(holder.userPhotoView, photo);
			}
			holder.userPhotoView.setTag(entity.creator);
			UI.setVisibility(holder.userPhotoView, View.VISIBLE);
		}

		/* User name */

		UI.setVisibility(holder.userName, View.GONE);
		if (holder.userName != null && entity.creator != null && entity.creator.name != null && entity.creator.name.length() > 0) {
			holder.userName.setText(entity.creator.name);
			UI.setVisibility(holder.userName, View.VISIBLE);
		}

		/* Message 'to' context */

		UI.setVisibility(holder.toName, View.GONE);
		UI.setVisibility(view.findViewById(R.id.symbol_at), View.GONE);

		if (entity.type.equals(MessageType.REPLY)) {

			if (holder.toName != null) {

				Message message = (Message) entity;
				Link linkMessage = entity.getParentLink(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_MESSAGE);

				String toLabel = null;
				if (message.replyTo != null) {
					if (!entity.creator.name.equals(message.replyTo.name)) {
						toLabel = message.replyTo.name;
					}
					else {
						toLabel = "Added";
					}
				}
				else {
					if (entity.creator != null && entity.creator.name != null) {

						if (linkMessage != null
								&& linkMessage.shortcut.creator != null
								&& linkMessage.shortcut.creator.name != null) {
							
							if (!entity.creator.name.equals(linkMessage.shortcut.creator.name)) {
								toLabel = linkMessage.shortcut.creator.name;
							}
							else {
								toLabel = "Added";
							}
						}
						else {
							toLabel = "[Removed]";
						}
					}
					else {
						toLabel = "[Unknown]";
					}
				}

				if (toLabel != null) {
					holder.toName.setText(toLabel);
					UI.setVisibility(holder.toName, View.VISIBLE);
					UI.setVisibility(view.findViewById(R.id.symbol_at), View.VISIBLE);
				}
			}
		}

		/* Created date */

		UI.setVisibility(holder.createdDate, View.GONE);
		if (holder.createdDate != null && entity.createdDate != null) {
			String compactAgo = DateTime.intervalCompact(entity.createdDate.longValue(), DateTime.nowDate().getTime(), IntervalContext.PAST);
			holder.createdDate.setText(compactAgo);
			UI.setVisibility(holder.createdDate, View.VISIBLE);
		}

		/* Description */

		UI.setVisibility(holder.description, View.GONE);
		if (holder.description != null && entity.description != null && entity.description.length() > 0) {
			holder.description.setText(entity.description);
			UI.setVisibility(holder.description, View.VISIBLE);
		}

		/* Photo */

		UI.setVisibility(holder.photoView, View.GONE);
		if (holder.photoView != null) {
			final Photo photo = entity.getPhoto();

			if (entity.photo != null) {
				if (holder.photoView.getPhoto() == null || !photo.getUri().equals(holder.photoView.getPhoto().getUri())) {
					holder.photoView.setCenterCrop(false);
					UI.drawPhoto(holder.photoView, photo);
				}
				UI.setVisibility(holder.photoView, View.VISIBLE);
			}
		}

		/* Info about child links */

		UI.setVisibility(((ViewHolderExtended) holder).childCount, View.GONE);
		if (((ViewHolderExtended) holder).childCount != null) {
			Count count = entity.getCount(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_MESSAGE, null, Direction.in);
			Integer linkCount = (count != null) ? count.count.intValue() : 0;
			if (linkCount != null && linkCount > 0) {
				((ViewHolderExtended) holder).childCount.setText(String.valueOf(linkCount) + ((linkCount == 1) ? " reply" : " replies"));
				UI.setVisibility(((ViewHolderExtended) holder).childCount, View.VISIBLE);
			}
		}

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
		Integer padding = UI.getRawPixelsForDisplayPixels(getSherlockActivity(), 10f);
		Integer indent = UI.getRawPixelsForDisplayPixels(getSherlockActivity(), 10f);

		if (entity.type.equals(MessageType.REPLY)
				&& link != null
				&& (((BaseActivity) getSherlockActivity()).getEntity() != null
				&& ((BaseActivity) getSherlockActivity()).getEntity().id.equals(link.toId))) {
			indent = UI.getRawPixelsForDisplayPixels(getSherlockActivity(), 30f);
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

	public class ViewHolderExtended extends ViewHolder {

		public TextView		childCount;
	}

}