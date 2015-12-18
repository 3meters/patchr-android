package com.patchr.ui.edit;

import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
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

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.DataController;
import com.patchr.components.DataController.SuggestScope;
import com.patchr.components.DownloadManager;
import com.patchr.components.Logger;
import com.patchr.components.MediaManager;
import com.patchr.components.ModelResult;
import com.patchr.components.StringManager;
import com.patchr.events.ProcessingCanceledEvent;
import com.patchr.interfaces.IEntityController;
import com.patchr.objects.Entity;
import com.patchr.objects.Link;
import com.patchr.objects.Message;
import com.patchr.objects.Message.MessageType;
import com.patchr.objects.Photo;
import com.patchr.objects.Route;
import com.patchr.ui.base.BaseEntityEdit;
import com.patchr.ui.components.EntitySuggestController;
import com.patchr.ui.widgets.AirTokenCompleteTextView;
import com.patchr.ui.widgets.EntityView;
import com.patchr.ui.widgets.TokenCompleteTextView;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Reporting;
import com.patchr.utilities.UI;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso.LoadedFrom;
import com.squareup.picasso.Target;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MessageEdit extends BaseEntityEdit implements TokenCompleteTextView.TokenListener, Target {

	private String mMessage;
	private String mShareId;
	private String mShareSchema = Constants.SCHEMA_ENTITY_PICTURE;
	private Entity mShareEntity;

	private ViewAnimator             mAnimatorTo;
	private ViewAnimator             mAnimatorPhoto;
	private ViewGroup                mShareHolder;
	private ViewGroup                mShare;
	private AirTokenCompleteTextView mTo;
	private ImageView                mButtonToClear;
	private EntitySuggestController  mEntitySuggest;

	private List<Entity>                mTos          = new ArrayList<Entity>();
	private String                      mMessageType  = MessageType.ROOT;
	private DataController.SuggestScope mSuggestScope = SuggestScope.PATCHES;
	private ToMode                      mToMode       = ToMode.SINGLE;
	private Boolean                     mToEditable   = true;

	@Override
	public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mMessageType = extras.getString(Constants.EXTRA_MESSAGE_TYPE);
			if (mMessageType == null) {
				mMessageType = MessageType.ROOT;
			}
			mMessage = extras.getString(Constants.EXTRA_MESSAGE);
			mSuggestScope = SuggestScope.values()[extras.getInt(Constants.EXTRA_SEARCH_SCOPE, SuggestScope.PATCHES.ordinal())];
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
		Intent intent = getIntent();
		if (intent.getAction() != null
				&& intent.getAction().equals(Intent.ACTION_SEND)
				&& Patchr.getInstance().getCurrentUser().isAnonymous()) {
			Patchr.sendIntent = getIntent();
			Patchr.router.route(this, Route.SPLASH, null, null);
			finish();

			String message = StringManager.getString(R.string.alert_signin_message_share);
			Dialogs.signinRequired(this, message);
		}
	}

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mEntitySchema = Constants.SCHEMA_ENTITY_MESSAGE;

		if (Patchr.getInstance().getCurrentPatch() != null) {
			mToEditable = false;
		}

		mDirtyExitTitleResId = R.string.alert_dirty_exit_title_message;
		mDirtyExitMessageResId = R.string.alert_dirty_exit_message_message;
		mDirtyExitPositiveResId = R.string.alert_dirty_send;
		mInsertProgressResId = R.string.progress_sending;
		mInsertedResId = R.string.alert_message_sent;

		mAnimatorPhoto = (ViewAnimator) findViewById(R.id.animator_photo);
		if (mAnimatorPhoto != null) {
			mAnimatorPhoto.setInAnimation(MessageEdit.this, R.anim.fade_in_medium);
			mAnimatorPhoto.setOutAnimation(MessageEdit.this, R.anim.fade_out_medium);
		}

		mAnimatorTo = (ViewAnimator) findViewById(R.id.animator_to);
		if (mAnimatorTo != null) {
			mAnimatorTo.setInAnimation(this, R.anim.fade_in_short);
			mAnimatorTo.setOutAnimation(this, R.anim.fade_out_short);
		}
		mButtonToClear = (ImageView) findViewById(R.id.to_clear);
		mShareHolder = (ViewGroup) findViewById(R.id.share_holder);
		mShare = (ViewGroup) findViewById(R.id.share_entity);
		mTo = (AirTokenCompleteTextView) findViewById(R.id.to);
		mTo.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
			}
		});

		/*
		 * Make sure that we don't already have a patch set when
		 * handling a share intent.
		 */
		Intent intent = getIntent();
		if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_SEND)) {

			mMessageType = MessageType.SHARE;
			mSuggestScope = DataController.SuggestScope.USERS;
			mShareSchema = Constants.SCHEMA_ENTITY_PICTURE;
			mToMode = ToMode.MULTIPLE;
			mToEditable = true;

			Patchr.getInstance().setCurrentPatch(null);
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
				.setSearchInput(mTo)
				.setTokenListener(this)
				.setSuggestScope(mSuggestScope);

		mEntitySuggest.init();

		if (mMessage != null) {
			TextView message = (TextView) findViewById(R.id.content_message);
			message.setText(mMessage);
			message.setVisibility(View.VISIBLE);
		}

		if (mPhotoView != null) {
			mPhotoView.setTarget(this);
		}
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	@Override
	public void bind(BindingMode mode) {

		if (!mEditing && mEntity == null && mEntitySchema != null) {

			IEntityController controller = Patchr.getInstance().getControllerForSchema(mEntitySchema);
			mEntity = controller.makeNew();

			if (Patchr.getInstance().getCurrentUser() != null) {
				mEntity.creator = Patchr.getInstance().getCurrentUser();
				mEntity.creatorId = Patchr.getInstance().getCurrentUser().id;
			}

			/*
			 * Check to see if some data for this new message was passed in the intent.
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
					mShareSchema = extras.getString(Constants.EXTRA_SHARE_SCHEMA, Constants.SCHEMA_ENTITY_PICTURE);

					if (mShareSchema.equals(Constants.SCHEMA_ENTITY_PATCH)) {
						mShareEntity = DataController.getStoreEntity(mShareId);
						mEntity.description = String.format(StringManager.getString(R.string.label_patch_share_body_self), mShareEntity.name);
					}
					else if (mShareSchema.equals(Constants.SCHEMA_ENTITY_MESSAGE)) {
						mShareEntity = DataController.getStoreEntity(mShareId);
						if (mShareEntity.patch != null) {
							mEntity.description = String.format(StringManager.getString(R.string.label_message_share_body_self), mShareEntity.creator.name, mShareEntity.patch.name);
						}
						else {
							mEntity.description = String.format(StringManager.getString(R.string.label_message_share_body_self_no_patch), mShareEntity.creator.name);
						}
					}
					else if (mShareSchema.equals(Constants.SCHEMA_ENTITY_PICTURE)) {

                        /* Check for a photo */
						if (intent.getType() != null) {
							if (intent.getType().indexOf("image/") != -1 || intent.getParcelableExtra(Intent.EXTRA_STREAM) != null) {

								final Uri photoUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
								if (photoUri != null) {

									new AsyncTask() {

										@Override
										protected Object doInBackground(Object... params) {
											Thread.currentThread().setName("AsyncShareBitmap");
											ModelResult result = new ModelResult();

											try {
												Bitmap bitmap = DownloadManager.with(Patchr.applicationContext)
												                               .load(photoUri)
												                               .centerInside()
												                               .resize(Constants.IMAGE_DIMENSION_MAX, Constants.IMAGE_DIMENSION_MAX)
												                               .get();

												File file = MediaManager.copyBitmapToSharePath(bitmap);
												Uri uri = MediaManager.getSharePathUri();

												if (file != null && uri != null) {
													Photo photo = new Photo()
															.setPrefix(uri.toString())
															.setStore(true)
															.setSource(Photo.PhotoSource.file);
													onPhotoSelected(photo); // mDirty gets set in this method
													mDirty = false;
												}
												else {
													UI.showToastNotification(StringManager.getString(R.string.error_storage_unmounted), Toast.LENGTH_SHORT);
												}
											}
											catch (FileNotFoundException e) {
												Reporting.logException(new FileNotFoundException("Picasso failed to load bitmap"));
											}
											catch (IOException e) {
												Reporting.logMessage("Picasso failed to load bitmap");
												Reporting.logException(new IOException("Picasso failed to load bitmap", e));
											}
											return result;
										}

										@Override
										protected void onPostExecute(Object response) {
											draw(null);
										}
									}.executeOnExecutor(Constants.EXECUTOR);
								}
							}
						}
						mEntity.description = StringManager.getString(R.string.label_photo_share_body_self);
					}
				}

                /* Text/image shared from another application */

				else if (intent.getType() != null) {

					/* Intent with text from another application */
					if (intent.getType().equals("text/plain") || intent.getStringExtra(Intent.EXTRA_TEXT) != null) {
						String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
						if (sharedText != null) {
							mDirty = true;
							mEntity.description = sharedText;
						}
					}

					/*
					 * Intent with image data. We get a uri we use to open a stream to get the bitmap.
					 * We then copy the bitmap to a our pinned share file. We are not the source of the
					 * image so we don't want to track it in the Pictures/Patchr folder.
					 */
					if (intent.getType().indexOf("image/") != -1 || intent.getParcelableExtra(Intent.EXTRA_STREAM) != null) {

						final Uri photoUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
						if (photoUri != null) {

							new AsyncTask() {

								@Override
								protected Object doInBackground(Object... params) {
									Thread.currentThread().setName("AsyncShareBitmap");
									ModelResult result = new ModelResult();

									try {

										Bitmap bitmap = DownloadManager.with(Patchr.applicationContext)
										                               .load(photoUri)
										                               .centerInside()
										                               .resize(Constants.IMAGE_DIMENSION_MAX, Constants.IMAGE_DIMENSION_MAX)
										                               .get();

										File file = MediaManager.copyBitmapToSharePath(bitmap);
										Uri uri = MediaManager.getSharePathUri();

										if (file != null && uri != null) {
											Photo photo = new Photo()
													.setPrefix(uri.toString())
													.setStore(true)
													.setSource(Photo.PhotoSource.file);
											onPhotoSelected(photo); // mDirty gets set in this method
										}
										else {
											UI.showToastNotification(StringManager.getString(R.string.error_storage_unmounted), Toast.LENGTH_SHORT);
										}
									}
									catch (FileNotFoundException e) {
										Reporting.logException(new FileNotFoundException("Picasso failed to load bitmap"));
									}
									catch (IOException e) {
										Reporting.logMessage("Picasso failed to load bitmap");
										Reporting.logException(new IOException("Picasso failed to load bitmap", e));
									}
									return result;
								}

								@Override
								protected void onPostExecute(Object response) {
									draw(null);
								}
							}.executeOnExecutor(Constants.EXECUTOR);
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

		super.draw(view);

		UI.setVisibility(mAnimatorTo, View.VISIBLE);

		/* We don't allow the patch to be changed when editing */
		if (mEditing) {
			UI.setVisibility(mAnimatorTo, View.GONE);
		}
		else {
			Entity currentPatch = Patchr.getInstance().getCurrentPatch();
			if (currentPatch != null) {
				mTo.addObject(currentPatch);
			}
		}

		if (mButtonToClear != null && !mToEditable) {
			mButtonToClear.setVisibility(View.GONE);
		}

		TextView textView = (TextView) findViewById(R.id.description);
		if (mMessageType.equals(MessageType.SHARE)) {

			int layoutResId = 0;
			if (mShareSchema.equals(Constants.SCHEMA_ENTITY_PATCH)) {
				layoutResId = R.layout.temp_share_patch;
			}
			else if (mShareSchema.equals(Constants.SCHEMA_ENTITY_MESSAGE)) {
				layoutResId = R.layout.temp_share_message;
			}
			else if (mShareSchema.equals(Constants.SCHEMA_ENTITY_PICTURE)) {
				mButtonPhotoDelete.setVisibility(View.GONE);
			}

			if (mShareSchema.equals(Constants.SCHEMA_ENTITY_PATCH)
					|| mShareSchema.equals(Constants.SCHEMA_ENTITY_MESSAGE)) {
				mAnimatorPhoto.setVisibility(View.GONE);
				mShareHolder.setVisibility(View.VISIBLE);
				View shareView = LayoutInflater.from(this).inflate(layoutResId, mShare, true);
				IEntityController controller = Patchr.getInstance().getControllerForSchema(mShareSchema);
				controller.bind(mShareEntity, shareView, null);

				if (mShareSchema.equals(Constants.SCHEMA_ENTITY_MESSAGE)) {
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
		}
	}

	@Override
	protected void drawPhoto() {
		/*
		 * Can be called from main or background thread.
		 */
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (mPhotoView != null) {
					if (mEntity.photo != null) {
						if (mPhotoView.getPhoto() == null
								|| !mPhotoView.getPhoto().sameAs(mEntity.getPhoto())) {
							/* This activity implements target */
							UI.setVisibility(mButtonPhotoEdit, View.GONE);
							UI.setVisibility(mButtonPhotoDelete, View.GONE);
							mPhotoView.showLoading(true);
							mProcessing = true;                             // So user can't post while we a trying to fetch the photo

							Photo photo = mEntity.getPhoto();
							if (photo.source.equals(Photo.PhotoSource.file)) {
								Logger.d(MessageEdit.this, "Loading image from file: " + photo.getDirectUri());
							}
							else if (!photo.source.equals(Photo.PhotoSource.resource)) {
								Logger.d(MessageEdit.this, "Loading image from network: " + photo.getDirectUri());
							}

							UI.drawPhoto(mPhotoView, mEntity.getPhoto());   // Only place we try to load a photo
						}
					}

					mAnimatorPhoto.requestLayout();
					mAnimatorPhoto.setDisplayedChild(mEntity.photo == null ? 0 : 1);
				}
			}
		});
	}

    /*--------------------------------------------------------------------------------------------
     * Events
     *--------------------------------------------------------------------------------------------*/

	@Override
	public void onAccept() {

		if (mProcessing) return;
		mProcessing = true;

		if (isDirty() || mMessageType.equals(MessageType.SHARE)) {
			if (validate()) { // validate() also gathers
				if (mEditing) {
					update();
				}
				else {
					insert();
				}
			}
		}
		else {
			onCancel(false);
		}
		mProcessing = false;
	}

	@Subscribe
	public void onCancelEvent(ProcessingCanceledEvent event) {
		if (mTaskService != null) {
			mTaskService.cancel(true);
		}
	}

	@Override
	public void onTokenAdded(Object o) {

		if (!mTos.contains((Entity) o)) {
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

		if (mTos.contains((Entity) o)) {
			mTos.remove((Entity) o);
		}

		if (mToMode == ToMode.SINGLE && mTos.size() == 0) {
			mAnimatorTo.setDisplayedChild(0);
		}
	}

	public void onError(final String reason) {
		super.onError(reason);
		/*
		 * ImageChooser error trying to pick or take a photo
		 */
		drawPhoto();
	}

	protected void onPhotoCanceled() {
		drawPhoto();
	}

	@Override
	public void onBitmapFailed(Drawable arg0) {
		UI.showToastNotification(StringManager.getString(R.string.label_photo_missing), Toast.LENGTH_SHORT);
		onCancelPhotoButtonClick(null);
		drawPhoto();
		mProcessing = false;
	}

	@Override
	public void onBitmapLoaded(Bitmap bitmap, LoadedFrom loadedFrom) {

		final BitmapDrawable bitmapDrawable = new BitmapDrawable(Patchr.applicationContext.getResources(), bitmap);
		UI.showDrawableInImageView(bitmapDrawable, mPhotoView.getImageView(), true);

		mProcessing = false;

		UI.setVisibility(mButtonPhotoEdit, View.VISIBLE);
		UI.setVisibility(mButtonPhotoDelete, View.VISIBLE);

		mPhotoView.showLoading(false);
		mAnimatorPhoto.setInAnimation(MessageEdit.this, R.anim.slide_in_bottom_long);
		mAnimatorPhoto.requestLayout();
		mAnimatorPhoto.setDisplayedChild(1);
		mAnimatorPhoto.setInAnimation(MessageEdit.this, R.anim.fade_in_medium);
	}

	@Override
	public void onPrepareLoad(Drawable drawable) {
		//		runOnUiThread(new Runnable() {
		//
		//			@Override
		//			public void run() {
		//				mAnimatorPhoto.requestLayout();
		//				mAnimatorPhoto.setDisplayedChild(1);
		//			}
		//		});
	}

	public void onCancelPhotoButtonClick(View view) {
		mEntity.photo = null;
		mPhotoView.setPhoto(null);
		onPhotoCanceled();
	}

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

	public void configureActionBar() {
		super.configureActionBar();
		if (getSupportActionBar() != null) {
			getSupportActionBar().setTitle(mEditing ? R.string.form_title_message_edit : R.string.form_title_message_new);
		}
	}

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

		if (!mEditing && mTos.size() == 0) {

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

			if (mMessageType.equals(MessageType.ROOT)) {
				if (mTos.size() > 0) {
					message.patchId = mTos.get(0).id;
				}
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
	     * Called on background thread.
		 */
		if (mMessageType.equals(MessageType.ROOT)) {
			for (Entity to : mTos) {
				links.add(new Link(to.id, Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_PATCH));
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
			Entity currentPatch = Patchr.getInstance().getCurrentPatch();
			if (mTos.size() > 0 && (currentPatch == null || !currentPatch.id.equals(mTos.get(0).id))) {
				Patchr.router.route(this, Route.BROWSE, mTos.get(0), null);
			}
		}
		return true;
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