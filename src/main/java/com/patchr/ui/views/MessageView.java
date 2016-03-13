package com.patchr.ui.views;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.patchr.Constants;
import com.patchr.R;
import com.patchr.components.DataController;
import com.patchr.components.StringManager;
import com.patchr.objects.CacheStamp;
import com.patchr.objects.Count;
import com.patchr.objects.Entity;
import com.patchr.objects.Link;
import com.patchr.objects.Message;
import com.patchr.objects.Photo;
import com.patchr.utilities.DateTime;
import com.patchr.utilities.UI;

import java.util.List;

@SuppressWarnings("ucd")
public class MessageView extends FrameLayout {

	private static final Object lock = new Object();

	public    Entity     entity;
	protected CacheStamp cacheStamp;
	protected BaseView   base;
	protected Integer    layoutResId;

	protected ViewGroup   layout;
	protected ImageLayout userPhotoView;
	protected ImageLayout photoView;
	protected TextView    patchName;
	protected TextView    userName;
	protected TextView    createdDate;
	protected TextView    description;
	public    TextView    likesCount;
	public    TextView    likesLabel;
	protected TextView    shareRecipients;
	protected ViewGroup   likesButton;
	protected ViewGroup   shareHolder;
	protected ViewGroup   shareView;
	protected ViewGroup   shareRecipientsHolder;

	public MessageView(Context context) {
		this(context, null, 0);
	}

	public MessageView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public MessageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.layoutResId = R.layout.message_view;
		initialize();
	}

	public MessageView(Context context, Integer layoutResId) {
		super(context, null, 0);
		this.layoutResId = layoutResId;
		initialize();
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	protected void initialize() {

		this.base = new BaseView();
		this.layout = (ViewGroup) LayoutInflater.from(getContext()).inflate(this.layoutResId, this, true);

		this.userPhotoView = (ImageLayout) layout.findViewById(R.id.user_photo);
		this.photoView = (ImageLayout) layout.findViewById(R.id.image_layout);
		this.patchName = (TextView) layout.findViewById(R.id.patch_name);
		this.userName = (TextView) layout.findViewById(R.id.user_name);
		this.createdDate = (TextView) layout.findViewById(R.id.created_date);
		this.description = (TextView) layout.findViewById(R.id.description);
		this.likesCount = (TextView) layout.findViewById(R.id.likes_count);
		this.likesLabel = (TextView) layout.findViewById(R.id.likes_label);
		this.shareRecipients = (TextView) layout.findViewById(R.id.share_recipients);
		this.likesButton = (ViewGroup) layout.findViewById(R.id.likes_button);
		this.shareHolder = (ViewGroup) layout.findViewById(R.id.share_holder);
		this.shareView = (ViewGroup) layout.findViewById(R.id.share_entity);
		this.shareRecipientsHolder = (ViewGroup) layout.findViewById(R.id.share_recipients_holder);
		this.shareRecipients = (TextView) layout.findViewById(R.id.share_recipients);
	}

	public void databind(Entity entity) {

		synchronized (lock) {

			this.entity = entity;
			this.cacheStamp = entity.getCacheStamp();

			Boolean share = (entity.type != null && entity.type.equals(Constants.TYPE_LINK_SHARE));

			/* Patch context */
			if (!share) {
				Entity parentEntity = entity.patch;
				if (parentEntity == null) {
					if (entity.patchId != null) {
						parentEntity = DataController.getStoreEntity(entity.patchId);
					}
				}
				if (parentEntity != null) {
					base.setOrGone(this.patchName, parentEntity.name);
				}
			}

			/* User */
			this.userPhotoView.setImageWithEntity(entity.creator);
			base.setOrGone(this.userName, entity.creator.name);

			/* Create date */
			String dateFormatted = null;
			if (entity.createdDate != null) {
				dateFormatted = DateTime.intervalCompact(entity.createdDate.longValue(), DateTime.nowDate().getTime(), DateTime.IntervalContext.PAST);
			}
			base.setOrGone(this.createdDate, dateFormatted);
			base.setOrGone(this.description, entity.description);

			/* Shared entity */

			UI.setVisibility(this.photoView, GONE);
			UI.setVisibility(this.shareHolder, GONE);
			UI.setVisibility(this.shareRecipientsHolder, GONE);

			if (share) {

				Entity shareEntity = null;

				Link linkEntity = entity.getParentLink(Constants.TYPE_LINK_SHARE, Constants.SCHEMA_ENTITY_PATCH);
				if (linkEntity != null) {
					shareEntity = linkEntity.shortcut.getAsEntity();
				}
				if (shareEntity == null) {
					linkEntity = entity.getParentLink(Constants.TYPE_LINK_SHARE, Constants.SCHEMA_ENTITY_MESSAGE);
					if (linkEntity != null) {
						shareEntity = linkEntity.shortcut.getAsEntity();
					}
				}

				if (shareEntity != null) {

					this.shareView.removeAllViews();

					if (shareEntity.schema.equals(Constants.SCHEMA_ENTITY_PATCH)) {
						PatchView patchView = new PatchView(getContext(), R.layout.patch_view_attachment);
						patchView.databind(shareEntity);
						CardView cardView = (CardView) this.shareView;
						int padding = UI.getRawPixelsForDisplayPixels(0f);
						cardView.setContentPadding(padding, padding, padding, padding);
						this.shareView.setTag(shareEntity);
						this.shareView.addView(patchView);
						base.setOrGone(this.patchName, StringManager.getString(R.string.label_message_invite));
					}
					else if (shareEntity.schema.equals(Constants.SCHEMA_ENTITY_MESSAGE)) {
						MessageView messageView = new MessageView(getContext(), R.layout.message_view_attachment);
						messageView.databind(shareEntity);
						CardView cardView = (CardView) this.shareView;
						int padding = UI.getRawPixelsForDisplayPixels(8f);
						cardView.setContentPadding(padding, padding, padding, padding);
						this.shareView.setTag(shareEntity);
						this.shareView.addView(messageView);
						base.setOrGone(this.patchName, StringManager.getString(R.string.label_message_shared));
					}

					UI.setVisibility(this.shareHolder, VISIBLE);
				}
				else if (linkEntity != null) {

					/* Message that shares an entity but shortcut was blocked by permissions */

					if (linkEntity.targetSchema.equals(Constants.SCHEMA_ENTITY_MESSAGE)) {

						shareView.removeAllViews();
						View blockView = LayoutInflater.from(getContext()).inflate(R.layout.temp_button_share_message_blocked, null, false);

						Entity message = new Message();
						message.schema = Constants.SCHEMA_ENTITY_MESSAGE;
						message.id = linkEntity.toId;

						shareView.setTag(message);
						shareView.addView(blockView);

						UI.setVisibility(shareHolder, View.VISIBLE);
					}
				}

				/* Show share recipients */

				UI.setVisibility(this.shareRecipientsHolder, View.VISIBLE);
				StringBuilder recipientsString = new StringBuilder();
				List<Link> links = entity.getLinks(Constants.TYPE_LINK_SHARE, Constants.SCHEMA_ENTITY_USER, null, Link.Direction.out);
				for (Link link : links) {
					recipientsString.append(link.shortcut.name);
				}
				this.shareRecipients.setText(recipientsString);
			}
			else {

		        /* Photo */
				if (entity.photo != null) {
					final Photo photo = entity.photo;
					this.photoView.setImageWithPhoto(photo);
					this.photoView.setTag(photo);
					UI.setVisibility(this.photoView, VISIBLE);
				}

		        /* Likes */
				UI.setVisibility(this.likesButton, GONE);

				Count count = entity.getCount(Constants.TYPE_LINK_LIKE, null, null, Link.Direction.in);

				if (count == null) {
					count = new Count(Constants.TYPE_LINK_LIKE, Constants.SCHEMA_ENTITY_PATCH, null, 0);
				}

				if (count.count.intValue() > 0) {
					String label = getContext().getResources().getQuantityString(R.plurals.label_likes, count.count.intValue(), count.count.intValue());
					this.likesCount.setText(String.valueOf(count.count.intValue()));
					this.likesLabel.setText(label);
					UI.setVisibility(this.likesButton, VISIBLE);
				}
			}
		}
	}
}