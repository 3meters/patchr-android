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

		layout = (ViewGroup) LayoutInflater.from(getContext()).inflate(layoutResId, this, true);

		userPhotoView = (ImageWidget) layout.findViewById(R.id.user_photo);
		photoView = (ImageWidget) layout.findViewById(R.id.photo);
		patchName = (TextView) layout.findViewById(R.id.patch_name);
		userName = (TextView) layout.findViewById(R.id.user_name);
		createdDate = (TextView) layout.findViewById(R.id.created_date);
		description = (TextView) layout.findViewById(R.id.description);
		likesCount = (TextView) layout.findViewById(R.id.likes_count);
		likesLabel = (TextView) layout.findViewById(R.id.likes_label);
		likesButton = (ViewGroup) layout.findViewById(R.id.likes_button);
		shareHolder = (ViewGroup) layout.findViewById(R.id.share_holder);
		shareView = (ViewGroup) layout.findViewById(R.id.share_entity);
		shareRecipientsHolder = (ViewGroup) layout.findViewById(R.id.share_recipients_holder);
		shareRecipients = (TextView) layout.findViewById(R.id.share_recipients);
		footerGroup = layout.findViewById(R.id.footer_group);
		patchGroup = layout.findViewById(R.id.patch_group);
	}

	public void bind(RealmEntity message, Map options) {

		synchronized (lock) {

			this.entity = message;

			/* Options */
			if (options != null) {
				if (options.containsKey("hide_patch_name")) {
					hidePatchName = (Boolean) options.get("hide_patch_name");
				}
			}

			Boolean share = (Constants.TYPE_ENTITY_SHARE.equals(message.type));

			/* Patch context */
			patchGroup.setVisibility(GONE);
			if (!share) {
				if (!hidePatchName) {
					RealmEntity parentEntity = message.patch;
					if (parentEntity != null) {
						setOrGone(patchName, parentEntity.name);
						patchGroup.setVisibility(VISIBLE);
					}
				}
			}

			/* User */
			userPhotoView.setImageWithPhoto(message.owner.getPhoto(), message.owner.name, null);
			setOrGone(userName, message.owner.name);

			/* Create date */
			String dateFormatted = null;
			if (message.createdDate != null) {
				dateFormatted = DateTime.intervalCompact(message.createdDate.longValue(), DateTime.nowDate().getTime(), DateTime.IntervalContext.PAST);
			}
			setOrGone(createdDate, dateFormatted);
			setOrGone(description, message.description);

			/* Shared entity */

			UI.setVisibility(photoView, GONE);
			UI.setVisibility(shareHolder, GONE);
			UI.setVisibility(shareRecipientsHolder, GONE);
			UI.setVisibility(footerGroup, GONE);

			if (share) {

				RealmEntity shareEntity = null;
				if (message.patch != null) {
					shareEntity = message.patch;
				}
				else if (message.message != null) {
					shareEntity = message.message;
				}

				if (shareEntity != null) {

					shareView.removeAllViews();

					if (shareEntity.schema.equals(Constants.SCHEMA_ENTITY_PATCH)) {
						PatchView patchView = new PatchView(getContext(), R.layout.view_patch_attachment);
						patchView.bind(shareEntity);
						CardView cardView = (CardView) shareView;
						int padding = UI.getRawPixelsForDisplayPixels(0f);
						cardView.setContentPadding(padding, padding, padding, padding);
						shareView.setTag(shareEntity);
						shareView.addView(patchView);
						setOrGone(patchName, StringManager.getString(R.string.label_message_invite));
					}
					else if (shareEntity.schema.equals(Constants.SCHEMA_ENTITY_MESSAGE)) {
						MessageView messageView = new MessageView(getContext(), R.layout.view_message_attachment);
						messageView.bind(shareEntity, null);
						CardView cardView = (CardView) shareView;
						int padding = UI.getRawPixelsForDisplayPixels(8f);
						cardView.setContentPadding(padding, padding, padding, padding);
						shareView.setTag(shareEntity);
						shareView.addView(messageView);
						setOrGone(patchName, StringManager.getString(R.string.label_message_shared));
					}

					UI.setVisibility(shareHolder, VISIBLE);
				}

				/* Show share recipients */

				UI.setVisibility(shareRecipientsHolder, View.VISIBLE);
				StringBuilder recipientsString = new StringBuilder();
				for (RealmEntity recipient : message.recipients) {
					recipientsString.append(recipient.name);
				}
				shareRecipients.setText(recipientsString);
			}
			else {

		        /* Photo */
				if (message.getPhoto() != null) {
					final Photo photo = message.getPhoto();
					photoView.setImageWithPhoto(photo, null, null);
					photoView.setTag(photo);
					UI.setVisibility(photoView, VISIBLE);
				}

		        /* Likes */
				UI.setVisibility(footerGroup, VISIBLE);
				UI.setVisibility(likesButton, GONE);

				if (likesCount != null && message.countLikes != null && message.countLikes > 0) {
					String label = getContext().getResources().getQuantityString(R.plurals.label_likes, message.countLikes, message.countLikes);
					likesCount.setText(String.valueOf(message.countLikes));
					likesLabel.setText(label);
					UI.setVisibility(likesButton, VISIBLE);
				}
			}
		}
	}
}