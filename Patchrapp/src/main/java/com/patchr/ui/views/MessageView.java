package com.patchr.ui.views;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.patchr.Constants;
import com.patchr.R;
import com.patchr.components.StringManager;
import com.patchr.model.Photo;
import com.patchr.model.RealmEntity;
import com.patchr.ui.widgets.ImageWidget;
import com.patchr.utilities.DateTime;
import com.patchr.utilities.UI;

import java.util.Map;

@SuppressWarnings("ucd")
public class MessageView extends BaseView {

	private static final Object lock = new Object();

	public    RealmEntity entity;
	protected Integer     layoutResId;
	public    boolean     hidePatchName;

	protected ViewGroup   layout;
	protected ImageWidget userPhotoView;
	protected ImageWidget photoView;
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
	protected View        footerGroup;
	protected View        patchGroup;

	public MessageView(Context context) {
		this(context, null, 0);
	}

	public MessageView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public MessageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.layoutResId = R.layout.view_message;
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

		this.layout = (ViewGroup) LayoutInflater.from(getContext()).inflate(this.layoutResId, this, true);

		this.userPhotoView = (ImageWidget) layout.findViewById(R.id.user_photo);
		this.photoView = (ImageWidget) layout.findViewById(R.id.photo);
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
		this.footerGroup = layout.findViewById(R.id.footer_group);
		this.patchGroup = layout.findViewById(R.id.patch_group);
	}

	public void bind(RealmEntity entity, Map options) {

		synchronized (lock) {

			this.entity = entity;

			/* Options */
			if (options != null) {
				if (options.containsKey("hide_patch_name")) {
					this.hidePatchName = (Boolean) options.get("hide_patch_name");
				}
			}

			Boolean share = (entity.type != null && entity.type.equals(Constants.TYPE_LINK_SHARE));

			/* Patch context */
			patchGroup.setVisibility(GONE);
			if (!share) {
				if (!hidePatchName) {
					RealmEntity parentEntity = entity.patch;
					if (parentEntity == null) {
						if (entity.patchId != null) {
							/* TODO */
						}
					}
					if (parentEntity != null) {
						setOrGone(this.patchName, parentEntity.name);
						patchGroup.setVisibility(VISIBLE);
					}
				}
			}

			/* User */
			this.userPhotoView.setImageWithEntity(entity.creator.getPhoto(), entity.creator.name);
			setOrGone(this.userName, entity.creator.name);

			/* Create date */
			String dateFormatted = null;
			if (entity.createdDate != null) {
				dateFormatted = DateTime.intervalCompact(entity.createdDate.longValue(), DateTime.nowDate().getTime(), DateTime.IntervalContext.PAST);
			}
			setOrGone(this.createdDate, dateFormatted);
			setOrGone(this.description, entity.description);

			/* Shared entity */

			UI.setVisibility(this.photoView, GONE);
			UI.setVisibility(this.shareHolder, GONE);
			UI.setVisibility(this.shareRecipientsHolder, GONE);
			UI.setVisibility(this.footerGroup, GONE);

			if (share) {

				RealmEntity shareEntity = null;
				if (entity.patch != null) {
					shareEntity = entity.patch;
				}
				else if (entity.message != null) {
					shareEntity = entity.message;
				}

				if (shareEntity != null) {

					this.shareView.removeAllViews();

					if (shareEntity.schema.equals(Constants.SCHEMA_ENTITY_PATCH)) {
						PatchView patchView = new PatchView(getContext(), R.layout.view_patch_attachment);
						patchView.bind(shareEntity);
						CardView cardView = (CardView) this.shareView;
						int padding = UI.getRawPixelsForDisplayPixels(0f);
						cardView.setContentPadding(padding, padding, padding, padding);
						this.shareView.setTag(shareEntity);
						this.shareView.addView(patchView);
						setOrGone(this.patchName, StringManager.getString(R.string.label_message_invite));
					}
					else if (shareEntity.schema.equals(Constants.SCHEMA_ENTITY_MESSAGE)) {
						MessageView messageView = new MessageView(getContext(), R.layout.view_message_attachment);
						messageView.bind(shareEntity, null);
						CardView cardView = (CardView) this.shareView;
						int padding = UI.getRawPixelsForDisplayPixels(8f);
						cardView.setContentPadding(padding, padding, padding, padding);
						this.shareView.setTag(shareEntity);
						this.shareView.addView(messageView);
						setOrGone(this.patchName, StringManager.getString(R.string.label_message_shared));
					}

					UI.setVisibility(this.shareHolder, VISIBLE);
				}

				/* Show share recipients */

				UI.setVisibility(this.shareRecipientsHolder, View.VISIBLE);
				StringBuilder recipientsString = new StringBuilder();
				for (RealmEntity recipient : entity.recipients) {
					recipientsString.append(recipient.name);
				}
				this.shareRecipients.setText(recipientsString);
			}
			else {

		        /* Photo */
				if (entity.getPhoto() != null) {
					final Photo photo = entity.getPhoto();
					this.photoView.setImageWithEntity(photo, null);
					this.photoView.setTag(photo);
					UI.setVisibility(this.photoView, VISIBLE);
				}

		        /* Likes */
				UI.setVisibility(this.footerGroup, VISIBLE);
				UI.setVisibility(this.likesButton, GONE);

				if (entity.countLikes != null && entity.countLikes > 0) {
					String label = getContext().getResources().getQuantityString(R.plurals.label_likes, entity.countLikes, entity.countLikes);
					this.likesCount.setText(String.valueOf(entity.countLikes));
					this.likesLabel.setText(label);
					UI.setVisibility(this.likesButton, VISIBLE);
				}
			}
		}
	}
}