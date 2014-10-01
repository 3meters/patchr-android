package com.aircandi.ui.edit;

import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;

import com.aircandi.Constants;
import com.aircandi.Patch;
import com.aircandi.R;
import com.aircandi.components.AnimationManager;
import com.aircandi.components.DownloadManager;
import com.aircandi.components.EntityManager;
import com.aircandi.components.MediaManager;
import com.aircandi.components.StringManager;
import com.aircandi.events.CancelEvent;
import com.aircandi.interfaces.IEntityController;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Link;
import com.aircandi.objects.Message;
import com.aircandi.objects.Message.MessageType;
import com.aircandi.objects.Photo;
import com.aircandi.objects.Route;
import com.aircandi.ui.base.BaseEntityEdit;
import com.aircandi.ui.components.EntitySuggestController;
import com.aircandi.ui.widgets.AirTokenCompleteTextView;
import com.aircandi.ui.widgets.EntityView;
import com.aircandi.ui.widgets.TokenCompleteTextView;
import com.aircandi.utilities.Colors;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.UI;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso.LoadedFrom;
import com.squareup.picasso.Target;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MessageEdit extends BaseEntityEdit implements TokenCompleteTextView.TokenListener {

	private String mReplyPlaceId;                        // Passed in for replies
	private String mReplyRootId;
	private String mReplyToId;
	private String mReplyToName;
	private String mMessage;
	private String mShareId;
	private String mShareSchema;
	private Entity mShareEntity;

	private ViewAnimator             mAnimatorTo;
	private ViewAnimator             mAnimatorPhoto;
	private ViewGroup                mShareHolder;
	private ViewGroup                mShare;
	private AirTokenCompleteTextView mTo;
	private ImageView                mButtonToClear;
	private ImageView                mButtonPhotoDelete;
	private EntitySuggestController  mEntitySuggest;

	private List<Entity>               mTos          = new ArrayList<Entity>();
	private String                     mMessageType  = MessageType.ROOT;
	private EntityManager.SuggestScope mSuggestScope = EntityManager.SuggestScope.PLACES;
	private ToMode                     mToMode       = ToMode.SINGLE;
	private Boolean                    mToEditable   = true;

	@Override
	public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mMessageType = extras.getString(com.aircandi.Constants.EXTRA_MESSAGE_TYPE);
			if (mMessageType == null) {
				mMessageType = MessageType.ROOT;
			}
			mMessage = extras.getString(Constants.EXTRA_MESSAGE);
			mReplyPlaceId = extras.getString(Constants.EXTRA_PLACE_ID);
			mReplyRootId = extras.getString(com.aircandi.Constants.EXTRA_MESSAGE_ROOT_ID);
			mReplyToId = extras.getString(com.aircandi.Constants.EXTRA_MESSAGE_REPLY_TO_ID);
			mReplyToName = extras.getString(com.aircandi.Constants.EXTRA_MESSAGE_REPLY_TO_NAME);
			mSuggestScope = EntityManager.SuggestScope.values()[extras.getInt(Constants.EXTRA_SUGGEST_SCOPE
					, EntityManager.SuggestScope.PLACES.ordinal())];
			mToMode = ToMode.values()[extras.getInt(Constants.EXTRA_TO_MODE, ToMode.SINGLE.ordinal())];
			mToEditable = extras.getBoolean(Constants.EXTRA_TO_EDITABLE, true);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		/*
		 * Special pre-handling because this form can be called directly
		 * because of a sharing intent and we need a signed in user. If user
		 * signs in they will be routed back to this form again.
		 */
		if (!Patch.firstStartApp && Patch.getInstance().getCurrentUser().isAnonymous()) {
			Patch.firstStartIntent = getIntent();
			Patch.dispatch.route(this, Route.SPLASH, null, null, null);
		}
	}

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mEntitySchema = com.aircandi.Constants.SCHEMA_ENTITY_MESSAGE;

		if (Patch.getInstance().getCurrentPlace() != null) {
			mToEditable = false;
		}

		mDirtyExitTitleResId = R.string.alert_dirty_exit_title_message;
		mDirtyExitMessageResId = R.string.alert_dirty_exit_message_message;
		mDirtyExitPositiveResId = R.string.alert_dirty_send;
		mInsertProgressResId = R.string.progress_sending;
		mInsertedResId = R.string.alert_message_sent;

		mAnimatorPhoto = (ViewAnimator) findViewById(R.id.animator_photo);
		mAnimatorPhoto.setInAnimation(MessageEdit.this, R.anim.fade_in_medium);
		mAnimatorPhoto.setOutAnimation(MessageEdit.this, R.anim.fade_out_medium);

		mAnimatorTo = (ViewAnimator) findViewById(R.id.animator_to);
		mAnimatorTo.setInAnimation(this, R.anim.fade_in_short);
		mAnimatorTo.setOutAnimation(this, R.anim.fade_out_short);
		mButtonToClear = (ImageView) findViewById(R.id.to_clear);
		mButtonPhotoDelete = (ImageView) findViewById(R.id.photo_delete);
		mShareHolder = (ViewGroup) findViewById(R.id.share_holder);
		mShare = (ViewGroup) findViewById(R.id.share);
		mTo = (AirTokenCompleteTextView) findViewById(R.id.to);
		mTo.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
				}
				else {
				}
			}
		});

		/*
         * Make sure that we don't already have a place set when
		 * handling a share intent.
		 */
		Intent intent = getIntent();
		if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_SEND)) {

			mMessageType = MessageType.SHARE;
			mSuggestScope = EntityManager.SuggestScope.USERS;
			mToMode = ToMode.MULTIPLE;
			mToEditable = true;

			Patch.getInstance().setCurrentPlace(null);
			onEntityClearButtonClick(null);

			mDirtyExitTitleResId = R.string.alert_dirty_share_exit_title;
			mDirtyExitMessageResId = R.string.alert_dirty_share_exit_message;
			mDirtyExitPositiveResId = R.string.alert_dirty_share;
			mInsertProgressResId = R.string.progress_sharing;
			mInsertedResId = R.string.alert_shared;
		}

		mTo.setLineSpacing(mToMode == ToMode.SINGLE ? 0 : (int) UI.getRawPixelsForDisplayPixels(5f), 1f);
		mTo.setTokenLayoutResId(mToMode == ToMode.SINGLE
		                        ? R.layout.widget_token_view_single
		                        : R.layout.widget_token_view);

		mEntitySuggest = new EntitySuggestController(this)
				.setInput(mTo)
				.setTokenListener(this)
				.setSuggestScope(mSuggestScope);
		mEntitySuggest.init();

		if (mMessage != null) {
			TextView message = (TextView) findViewById(R.id.content_message);
			message.setText(mMessage);
			message.setVisibility(View.VISIBLE);
		}
	}

	protected void actionBarIcon() {
		if (mActionBar != null) {
			Drawable icon = getResources().getDrawable(R.drawable.img_message_edit_dark);
			mActionBar.setIcon(icon);
		}
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	@Override
	public void bind(BindingMode mode) {

		if (!mEditing && mEntity == null && mEntitySchema != null) {

			IEntityController controller = Patch.getInstance().getControllerForSchema(mEntitySchema);
			mEntity = controller.makeNew();

			if (Patch.getInstance().getCurrentUser() != null) {
				mEntity.creator = Patch.getInstance().getCurrentUser();
				mEntity.creatorId = Patch.getInstance().getCurrentUser().id;
			}

			if (mReplyToId != null) {
				((Message) mEntity).replyToId = mReplyToId;
			}

			/*
             * Check to see if some data for this new message was passed
			 * in the intent.
			 */
			Intent intent = getIntent();
			if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_SEND)) {

                /* Try to determine if it from us */
				Boolean selfSend = false;
				Bundle extras = intent.getExtras();
				if (extras != null) {
					String sendSource = intent.getExtras().getString(Constants.EXTRA_SHARE_SOURCE);
					if (sendSource != null && sendSource.equals(getPackageName())) {
						selfSend = true;
					}
				}

				if (selfSend) {

					mShareId = extras.getString(Constants.EXTRA_SHARE_ID);
					mShareSchema = extras.getString(Constants.EXTRA_SHARE_SCHEMA);
					mShareEntity = EntityManager.getCacheEntity(mShareId);

					if (mShareSchema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
						mEntity.description = String.format(StringManager.getString(R.string.label_place_share_body_self), mShareEntity.name);
					}
					else if (mShareSchema.equals(com.aircandi.Constants.SCHEMA_ENTITY_MESSAGE)) {
						if (mShareEntity.place != null) {
							mEntity.description = String.format(StringManager.getString(R.string.label_message_share_body_self), mShareEntity.creator.name, mShareEntity.place.name);
						}
						else {
							mEntity.description = String.format(StringManager.getString(R.string.label_message_share_body_self_no_place), mShareEntity.creator.name);
						}
					}
					else if (mShareSchema.equals(Constants.SCHEMA_ENTITY_PICTURE)) {

                        /* Check for a photo */
						if (intent.getType() != null) {
							if (intent.getType().indexOf("image/") != -1 || intent.getParcelableExtra(Intent.EXTRA_STREAM) != null) {

								Uri photoUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
								if (photoUri != null) {

									try {
										InputStream stream = getContentResolver().openInputStream(photoUri);
										Bitmap bitmap = BitmapFactory.decodeStream(stream);
										stream.close();
										File file = MediaManager.copyBitmapToSharePath(bitmap);

										if (file != null) {
											Photo photo = new Photo()
													.setPrefix(MediaManager.getSharePathUri().toString())
													.setStore(true);
											onPhotoSelected(photo); // mDirty gets set in this method
											mDirty = false;
										}
										else {
											UI.showToastNotification(StringManager.getString(R.string.error_storage_unmounted), Toast.LENGTH_SHORT);
										}
									}
									catch (FileNotFoundException exception) {
										exception.printStackTrace();
									}
									catch (IOException exception) {
										exception.printStackTrace();
									}
								}
							}
						}
						mEntity.description = StringManager.getString(R.string.label_photo_share_body_self);
					}
				}

                /* Text/image shared from another application */

				else if (intent.getType() != null) {

					/* Intent with text from another application */
					if (!selfSend && (intent.getType().equals("text/plain") || intent.getStringExtra(Intent.EXTRA_TEXT) != null)) {
						String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
						if (sharedText != null) {
							mDirty = true;
							mEntity.description = sharedText;
						}
					}

					/*
                     * Intent with image data. We get a uri we use to open a stream to get the bitmap.
					 * We then copy the bitmap to a our pinned share file. We are not the source of the
					 * image so we don't want to track it in the Candipatch collection but
					 */
					if (intent.getType().indexOf("image/") != -1 || intent.getParcelableExtra(Intent.EXTRA_STREAM) != null) {

						Uri photoUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
						if (photoUri != null) {

							try {
								InputStream stream = getContentResolver().openInputStream(photoUri);
								Bitmap bitmap = BitmapFactory.decodeStream(stream);
								stream.close();
								File file = MediaManager.copyBitmapToSharePath(bitmap);

								if (file != null) {
									Photo photo = new Photo()
											.setPrefix(MediaManager.getSharePathUri().toString())
											.setStore(true);
									onPhotoSelected(photo); // mDirty gets set in this method
								}
								else {
									UI.showToastNotification(StringManager.getString(R.string.error_storage_unmounted), Toast.LENGTH_SHORT);
								}
							}
							catch (FileNotFoundException exception) {
								exception.printStackTrace();
							}
							catch (IOException exception) {
								exception.printStackTrace();
							}
						}
					}
				}

                /* Text from other application sent via clipboard */
				else if (Constants.SUPPORTS_JELLY_BEAN && intent.getClipData() != null) {

					ClipData.Item item = intent.getClipData().getItemAt(0);
					String sharedText = (String) item.coerceToStyledText(this);
					if (sharedText != null) {
						mDirty = true;
						mEntity.description = sharedText;
					}
				}
			}
		}
		draw(null);
	}

	@Override
	public void draw(View view) {
        /*
         * This method is only called when the activity is created.
         */
		if (view == null) {
			view = findViewById(android.R.id.content);
		}
		UI.setVisibility(mAnimatorTo, View.VISIBLE);

		/* We don't allow the place to be changed when editing */
		if ((mMessageType != null && mMessageType.equals(MessageType.REPLY))
				|| mEditing) {
			UI.setVisibility(mAnimatorTo, View.GONE);
		}
		else {
			Entity currentPlace = Patch.getInstance().getCurrentPlace();
			if (currentPlace != null) {
				mTo.addObject(currentPlace);
			}
		}

		if (mButtonToClear != null && !mToEditable) {
			mButtonToClear.setVisibility(View.GONE);
		}

		TextView textView = (TextView) findViewById(R.id.description);
		if (mMessageType == MessageType.SHARE) {

			int layoutResId = 0;
			if (mShareSchema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
				layoutResId = R.layout.temp_share_place;
			}
			else if (mShareSchema.equals(com.aircandi.Constants.SCHEMA_ENTITY_MESSAGE)) {
				layoutResId = R.layout.temp_share_message;
			}
			else if (mShareSchema.equals(Constants.SCHEMA_ENTITY_PICTURE)) {
				mButtonPhotoDelete.setVisibility(View.GONE);
			}

			if (mShareSchema.equals(Constants.SCHEMA_ENTITY_PLACE)
					|| mShareSchema.equals(com.aircandi.Constants.SCHEMA_ENTITY_MESSAGE)) {
				mAnimatorPhoto.setVisibility(View.GONE);
				mShareHolder.setVisibility(View.VISIBLE);
				View shareView = LayoutInflater.from(this).inflate(layoutResId, mShare, true);
				IEntityController controller = Patch.getInstance().getControllerForSchema(mShareSchema);
				controller.bind(mShareEntity, shareView);

				if (mShareSchema.equals(com.aircandi.Constants.SCHEMA_ENTITY_MESSAGE)) {
					UI.setEnabled(shareView, false);
				}
			}

			if (textView != null) {
				textView.setMinLines(3);
			}
		}
		else {
			if (textView != null) {
				textView.setCompoundDrawables(null, null, null, null);
			}
		}

		super.draw(view);

		/* Action bar title */
		if (mEditing) {
			setActivityTitle("Edit " + mEntity.schema);
		}
		else {
			if (mMessageType.equals(MessageType.ROOT)) {
				setActivityTitle("New " + mEntity.schema);
			}
			else if (mMessageType.equals(MessageType.SHARE)) {
				setActivityTitle("Share");
			}
			else if (mReplyToName != null) {
				setActivityTitle("Reply to " + mReplyToName);
			}
			else {
				setActivityTitle("Reply");
			}
		}
	}

	@Override
	protected void drawPhoto() {
		if (mPhotoView != null && mEntity.photo != null) {
			UI.drawPhoto(mPhotoView, mEntity.getPhoto());
			onChangedPhoto();
		}
	}

    /*--------------------------------------------------------------------------------------------
     * Events
     *--------------------------------------------------------------------------------------------*/

	@Override
	public void onAccept() {
		if (isDirty() || mMessageType.equals(MessageType.SHARE)) {
			if (validate()) { // validate() also gathers

				/* Upsize the place we are sending to if needed */
				if (mMessageType != null && mMessageType.equals(MessageType.REPLY)) {
					insert();
				}
				else {
					if (mEditing) {
						update();
					}
					else {
						insert();
					}
				}
			}
		}
		else {
			onCancel(false);
		}
	}

	@Subscribe
	public void onCancelEvent(CancelEvent event) {
		if (mTaskService != null) {
			mTaskService.cancel(true);
		}
	}

	@Override
	public void onTokenAdded(Object o) {

		if (!mTos.contains(o)) {
			mTos.add((Entity) o);
		}

		if (mToMode == ToMode.SINGLE && mTos.size() > 0) {
			final EntityView entityView = (EntityView) findViewById(R.id.entity_view);
			entityView.databind((Entity) mTos.get(0));
			mAnimatorTo.setDisplayedChild(1);
		}
	}

	@Override
	public void onTokenRemoved(Object o) {

		if (mTos.contains(o)) {
			mTos.remove((Entity) o);
		}

		if (mToMode == ToMode.SINGLE && mTos.size() == 0) {
			mAnimatorTo.setDisplayedChild(0);
		}
	}

	@Override
	protected void onChangedPhoto() {
		if (mAnimatorPhoto != null) {

			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if (mPhotoView.getPhoto() != null) {
						mAnimatorPhoto.requestLayout();
						mAnimatorPhoto.setInAnimation(MessageEdit.this, R.anim.slide_in_bottom_long);
						mAnimatorPhoto.setOutAnimation(MessageEdit.this, R.anim.fade_out_medium);
						mAnimatorPhoto.setDisplayedChild(2);
					}
					else {
						onDeletePhotoButtonClick(null);
					}
				}
			});
		}
	}

	@Override
	protected void onChangingPhoto() {
		if (mAnimatorPhoto != null) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					mAnimatorPhoto.requestLayout();
					mAnimatorPhoto.setInAnimation(MessageEdit.this, R.anim.fade_in_medium);
					mAnimatorPhoto.setOutAnimation(MessageEdit.this, R.anim.fade_out_medium);
					mAnimatorPhoto.setDisplayedChild(1);
				}
			});
		}
	}

	@Override
	protected void onCanceledPhoto() {
		if (mAnimatorPhoto != null) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					mAnimatorPhoto.requestLayout();
					mAnimatorPhoto.setInAnimation(MessageEdit.this, R.anim.fade_in_medium);
					mAnimatorPhoto.setOutAnimation(MessageEdit.this, R.anim.fade_out_medium);
					mAnimatorPhoto.setDisplayedChild(0);
				}
			});
		}
	}

	@Override
	public void onPhotoSelected(Photo photo) {
        /*
         * All photo selection sources and types end up here
		 */
		mDirty = !Photo.same(mEntity.photo, photo);
		if (mDirty) {

			mEntity.photo = photo;
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					mPhotoView.setTarget(new Target() {

						@Override
						public void onBitmapFailed(Drawable arg0) {
							UI.showToastNotification(StringManager.getString(R.string.label_photo_missing), Toast.LENGTH_SHORT);
							onCanceledPhoto();
						}

						@Override
						public void onBitmapLoaded(Bitmap bitmap, LoadedFrom loadedFrom) {
							DownloadManager.checkDebug(bitmap, loadedFrom);
							final BitmapDrawable bitmapDrawable = new BitmapDrawable(Patch.applicationContext.getResources(), bitmap);
							UI.showDrawableInImageView(bitmapDrawable, mPhotoView.getImageView(), true, AnimationManager.fadeInMedium());
							onChangedPhoto();
						}

						@Override
						public void onPrepareLoad(Drawable arg0) {
						}
					});

					UI.drawPhoto(mPhotoView, mEntity.getPhoto());
				}
			});
		}
	}

	@SuppressWarnings("ucd")
	public void onCancelPhotoButtonClick(View view) {
		onCanceledPhoto();
		mEntity.photo = null;
		mPhotoView.setPhoto(null);
	}

	@SuppressWarnings("ucd")
	public void onDeletePhotoButtonClick(View view) {
		if (mAnimatorPhoto != null) {
			mAnimatorPhoto.requestLayout();
			mAnimatorPhoto.setInAnimation(MessageEdit.this, R.anim.fade_in_medium);
			mAnimatorPhoto.setOutAnimation(MessageEdit.this, R.anim.fade_out_medium);
			mAnimatorPhoto.setDisplayedChild(0);
		}
		mDirty = (mEditing);
		mEntity.photo = null;
		mPhotoView.setPhoto(null);
	}

	@SuppressWarnings("ucd")
	public void onEntityClearButtonClick(View view) {

        /* Means we are in single mode.*/
		for (int i = mTo.getObjects().size(); i > 0; i--) {
			mTo.getObjects().remove(i - 1);
		}
		mTo.requestFocus();
	}

    /*--------------------------------------------------------------------------------------------
     * Methods
     *--------------------------------------------------------------------------------------------*/

	protected void setEntity() {
		final EntityView entityView = (EntityView) findViewById(R.id.entity_view);
		entityView.databind(mTos.get(0));
		mAnimatorTo.setDisplayedChild(1);
	}

	@Override
	protected boolean validate() {
		if (!super.validate()) return false;
        /*
         * Transfering values from the controls to the entity is easier
		 * with candigrams.
		 */
		gather();
		Message message = (Message) mEntity;

		if (!mEditing && mTos.size() == 0 && !mMessageType.equals(MessageType.REPLY)) {

			int messageResId = 0;
			if (mMessageType.equals(MessageType.ROOT)) {
				messageResId = R.string.error_missing_message_to;
			}
			else if (mMessageType.equals(MessageType.SHARE)) {
				messageResId = R.string.error_missing_share_to;
			}

			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, null
					, StringManager.getString(messageResId)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}

		if (message.photo == null && TextUtils.isEmpty(message.description)) {
			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, null
					, StringManager.getString(R.string.error_missing_message_content)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}

		return true;
	}

	@Override
	protected void gather() {
		super.gather();

		if (!mEditing) {

			Message message = (Message) mEntity;
			message.type = mMessageType;

			if (mMessageType.equals(MessageType.REPLY)) {
				if (mReplyPlaceId != null) {
					message.placeId = mReplyPlaceId;
				}
				if (mReplyRootId != null) {
					message.rootId = mReplyRootId;
				}
			}
			else if (mMessageType.equals(MessageType.ROOT)) {
				if (mTos.size() > 0) {
					message.placeId = mTos.get(0).id;
				}
			}
			else if (mMessageType.equals(MessageType.SHARE)) {

			}
		}
	}

	@Override
	protected String getLinkType() {
		return Constants.TYPE_LINK_CONTENT;
	}

	@Override
	protected void beforeInsert(Entity entity, List<Link> links) {
        /*
         * We link replies to the places they are associated with. This give us the option
		 * to thread, flatten or do some combo. Called on background thread.
		 */
		if (mMessageType.equals(MessageType.REPLY)) {
			if (mParentId != null) {
				links.add(new Link(mParentId, getLinkType(), mEntity.schema));
			}
			links.add(new Link(mEntity.placeId, Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_PLACE));
		}
		else if (mMessageType.equals(MessageType.ROOT)) {
			for (Entity to : mTos) {
				links.add(new Link(to.id, Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_PLACE));
			}
		}
		else if (mMessageType.equals(MessageType.SHARE)) {
			if (mShareId != null) {
				links.add(new Link(mShareId, Constants.TYPE_LINK_SHARE, mShareSchema));  // To support showing the shared entity with the message
			}
			for (Entity to : mTos) {
				links.add(new Link(to.id, Constants.TYPE_LINK_SHARE, Constants.SCHEMA_ENTITY_USER));
			}
		}
	}

	@Override
	protected boolean afterInsert() {
        /*
         * Only called if the insert was successful. Called on main ui thread.
		 */
		if (!mMessageType.equals(MessageType.SHARE)) {
			Entity currentPlace = Patch.getInstance().getCurrentPlace();
			if (mTos.size() > 0 && (currentPlace == null || !currentPlace.id.equals(mTos.get(0).id))) {
				Patch.dispatch.route(this, Route.BROWSE, mTos.get(0), null, null);
			}
		}
		return true;
	}

	@Override
	public void setResultCode(int resultCode) {
		if (mMessageType.equals(MessageType.REPLY)) {
			Intent intent = new Intent();
			intent.putExtra(Constants.EXTRA_ENTITY_CHILD_ID, mEntity.id);
			super.setResultCode(resultCode, intent);
		}
		else {
			super.setResultCode(resultCode);
		}
	}

    /*--------------------------------------------------------------------------------------------
     * Properties
     *--------------------------------------------------------------------------------------------*/

	@Override
	protected int getLayoutId() {
		return R.layout.message_edit;
	}


    /*--------------------------------------------------------------------------------------------
     * Classes
     *--------------------------------------------------------------------------------------------*/

	public enum ToMode {
		SINGLE,
		MULTIPLE
	}
}