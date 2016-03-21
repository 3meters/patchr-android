package com.patchr.ui.edit;

import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
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
import com.patchr.components.Dispatcher;
import com.patchr.components.MediaManager;
import com.patchr.components.ModelResult;
import com.patchr.components.StringManager;
import com.patchr.components.UserManager;
import com.patchr.events.ProcessingCanceledEvent;
import com.patchr.objects.Entity;
import com.patchr.objects.Link;
import com.patchr.objects.Message;
import com.patchr.objects.Message.MessageType;
import com.patchr.objects.Photo;
import com.patchr.objects.Route;
import com.patchr.ui.components.EntitySuggestController;
import com.patchr.ui.views.EntityView;
import com.patchr.ui.views.MessageView;
import com.patchr.ui.views.PatchView;
import com.patchr.ui.widgets.AirTokenCompleteTextView;
import com.patchr.ui.widgets.TokenCompleteTextView;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Reporting;
import com.patchr.utilities.UI;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Picasso.LoadedFrom;

import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MessageEdit extends BaseEdit implements TokenCompleteTextView.TokenListener {

	private String message;
	private String shareId;
	private String shareSchema = Constants.SCHEMA_ENTITY_PICTURE;
	private Entity shareEntity;

	private ViewAnimator             animatorTo;
	private ViewAnimator             animatorPhoto;
	private ViewGroup                shareHolder;
	private ViewGroup                share;
	private AirTokenCompleteTextView to;
	private ImageView                buttonToClear;
	private EntitySuggestController  entitySuggest;

	private List<Entity>                recipients   = new ArrayList<Entity>();
	private String                      messageType  = MessageType.ROOT;
	private DataController.SuggestScope suggestScope = SuggestScope.PATCHES;
	private ToMode                      toMode       = ToMode.SINGLE;
	private Boolean                     toEditable   = true;

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		if (intent.getAction() != null
				&& intent.getAction().equals(Intent.ACTION_SEND)
				&& !UserManager.shared().authenticated()) {
			UserManager.shared().showGuestGuard(this, "Sign up for a free account to share using Patchr and more.");
			return;
		}
		bind();
	}

	@Override protected void onStart() {
		super.onStart();
		Dispatcher.getInstance().register(this);
	}

	@Override protected void onStop() {
		Dispatcher.getInstance().unregister(this);
		super.onStop();
	}

    /*--------------------------------------------------------------------------------------------
     * Events
     *--------------------------------------------------------------------------------------------*/

	@Override public boolean onCreateOptionsMenu(Menu menu) {

		this.optionMenu = menu;

		if (editing) {
			getMenuInflater().inflate(R.menu.menu_save, menu);
			getMenuInflater().inflate(R.menu.menu_delete, menu);
		}
		else {
			getMenuInflater().inflate(R.menu.menu_send, menu);
		}

		configureStandardMenuItems(menu);   // Tweaks based on permissions
		return true;
	}

	@Override public boolean onOptionsItemSelected(MenuItem item) {

		if (item.getItemId() == R.id.delete) {
			super.confirmDelete();
		}
		else if (item.getItemId() == R.id.submit) {
			super.submitAction();
		}
		else {
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	@Override public void onTokenAdded(Object o) {

		if (!recipients.contains((Entity) o)) {
			recipients.add((Entity) o);
		}

		if (toMode == ToMode.SINGLE && recipients.size() > 0) {
			final EntityView entityView = (EntityView) findViewById(R.id.entity_view);
			entityView.databind((Entity) recipients.get(0));
			animatorTo.setDisplayedChild(1);
		}
	}

	@Override public void onTokenRemoved(Object o) {

		if (recipients.contains((Entity) o)) {
			recipients.remove((Entity) o);
		}

		if (toMode == ToMode.SINGLE && recipients.size() == 0) {
			animatorTo.setDisplayedChild(0);
		}
	}

	@Override public void onError(final String reason) {
		super.onError(reason);
		/*
		 * ImageChooser error trying to pick or take a photo
		 */
		bindPhoto();
	}

	@Override public void onBitmapLoaded(Bitmap bitmap, LoadedFrom loadedFrom) {
		super.onBitmapLoaded(bitmap, loadedFrom);

		animatorPhoto.setInAnimation(MessageEdit.this, R.anim.slide_in_bottom_long);
		animatorPhoto.requestLayout();
		animatorPhoto.setDisplayedChild(1);
		animatorPhoto.setInAnimation(MessageEdit.this, R.anim.fade_in_medium);
	}

	@Override public void onBitmapFailed(Drawable arg0) {
		UI.showToastNotification(StringManager.getString(R.string.label_photo_missing), Toast.LENGTH_SHORT);
		onCancelPhotoButtonClick(null);
		bindPhoto();
		this.processing = false;
	}

	protected void onPhotoCanceled() {
		bindPhoto();
	}

	public void onCancelPhotoButtonClick(View view) {
		entity.photo = null;
		onPhotoCanceled();
	}

	public void onEntityClearButtonClick(View view) {

        /* Means we are in single mode.*/
		for (int i = to.getObjects().size(); i > 0; i--) {
			to.getObjects().remove(i - 1);
		}
		to.requestFocus();
	}

    /*--------------------------------------------------------------------------------------------
     * Notifications
     *--------------------------------------------------------------------------------------------*/

	@Subscribe public void onCancelEvent(ProcessingCanceledEvent event) {
		if (taskService != null) {
			taskService.cancel(true);
		}
	}

    /*--------------------------------------------------------------------------------------------
     * Methods
     *--------------------------------------------------------------------------------------------*/

	@Override public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			messageType = extras.getString(Constants.EXTRA_MESSAGE_TYPE);
			if (messageType == null) {
				messageType = MessageType.ROOT;
			}
			message = extras.getString(Constants.EXTRA_MESSAGE);
			suggestScope = SuggestScope.values()[extras.getInt(Constants.EXTRA_SEARCH_SCOPE, SuggestScope.PATCHES.ordinal())];
			toMode = ToMode.values()[extras.getInt(Constants.EXTRA_TO_MODE, ToMode.SINGLE.ordinal())];
			toEditable = extras.getBoolean(Constants.EXTRA_TO_EDITABLE, true);
		}
	}

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);   // Handles creating new entity if needed

		entitySchema = Constants.SCHEMA_ENTITY_MESSAGE;

		if (Patchr.getInstance().getCurrentPatch() != null) {
			toEditable = false;
		}

		dirtyExitTitleResId = R.string.alert_dirty_exit_title_message;
		dirtyExitMessageResId = R.string.alert_dirty_exit_message_message;
		dirtyExitPositiveResId = R.string.alert_dirty_send;
		insertProgressResId = R.string.progress_sending;
		insertedResId = R.string.alert_message_sent;

		animatorPhoto = (ViewAnimator) findViewById(R.id.photo_animator);
		if (animatorPhoto != null) {
			animatorPhoto.setInAnimation(MessageEdit.this, R.anim.fade_in_medium);
			animatorPhoto.setOutAnimation(MessageEdit.this, R.anim.fade_out_medium);
		}

		animatorTo = (ViewAnimator) findViewById(R.id.animator_to);
		if (animatorTo != null) {
			animatorTo.setInAnimation(this, R.anim.fade_in_short);
			animatorTo.setOutAnimation(this, R.anim.fade_out_short);
		}

		buttonToClear = (ImageView) findViewById(R.id.to_clear);
		shareHolder = (ViewGroup) findViewById(R.id.share_holder);
		share = (ViewGroup) findViewById(R.id.share_entity);
		to = (AirTokenCompleteTextView) findViewById(R.id.to);
		to.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override public void onFocusChange(View v, boolean hasFocus) {}
		});
		/*
		 * Make sure that we don't already have a patch set when
		 * handling a share intent.
		 */
		Intent intent = getIntent();

		if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_SEND)) {

			messageType = MessageType.SHARE;
			suggestScope = DataController.SuggestScope.USERS;
			shareSchema = Constants.SCHEMA_ENTITY_PICTURE;
			toMode = ToMode.MULTIPLE;
			toEditable = true;

			Patchr.getInstance().setCurrentPatch(null);
			onEntityClearButtonClick(null);

			dirtyExitTitleResId = R.string.alert_dirty_share_exit_title;
			dirtyExitMessageResId = R.string.alert_dirty_share_exit_message;
			dirtyExitPositiveResId = R.string.alert_dirty_share;
			insertProgressResId = R.string.progress_sharing;
			insertedResId = R.string.alert_shared;

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

				shareId = extras.getString(Constants.EXTRA_SHARE_ID);
				shareSchema = extras.getString(Constants.EXTRA_SHARE_SCHEMA, Constants.SCHEMA_ENTITY_PICTURE);

				if (shareSchema.equals(Constants.SCHEMA_ENTITY_PATCH)) {
					shareEntity = DataController.getStoreEntity(shareId);
					entity.description = String.format(StringManager.getString(R.string.label_patch_share_body_self), shareEntity.name);
				}
				else if (shareSchema.equals(Constants.SCHEMA_ENTITY_MESSAGE)) {
					shareEntity = DataController.getStoreEntity(shareId);
					if (shareEntity.patch != null) {
						entity.description = String.format(StringManager.getString(R.string.label_message_share_body_self), shareEntity.creator.name, shareEntity.patch.name);
					}
					else {
						entity.description = String.format(StringManager.getString(R.string.label_message_share_body_self_no_patch), shareEntity.creator.name);
					}
				}
				else if (shareSchema.equals(Constants.SCHEMA_ENTITY_PICTURE)) {

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
											Bitmap bitmap = Picasso.with(Patchr.applicationContext)
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
												dirty = false;
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
										bind();
									}
								}.executeOnExecutor(Constants.EXECUTOR);
							}
						}
					}
					entity.description = StringManager.getString(R.string.label_photo_share_body_self);
				}
			}

            /* Text/image shared from another application */

			else if (intent.getType() != null) {

				/* Intent with text from another application */
				if (intent.getType().equals("text/plain") || intent.getStringExtra(Intent.EXTRA_TEXT) != null) {
					String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
					if (sharedText != null) {
						dirty = true;
						entity.description = sharedText;
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

							@Override protected Object doInBackground(Object... params) {
								Thread.currentThread().setName("AsyncShareBitmap");
								ModelResult result = new ModelResult();

								try {

									Bitmap bitmap = Picasso.with(Patchr.applicationContext)
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

							@Override protected void onPostExecute(Object response) {
								bind();
							}
						}.executeOnExecutor(Constants.EXECUTOR);
					}
				}
			}

            /* Text from other application sent via clipboard */
			else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && intent.getClipData() != null) {

				ClipData.Item item = intent.getClipData().getItemAt(0);
				String sharedText = (String) item.coerceToStyledText(this);
				if (sharedText != null) {
					dirty = true;
					entity.description = sharedText;
				}
			}
		}

		to.setLineSpacing(toMode == ToMode.SINGLE ? 0 : (int) UI.getRawPixelsForDisplayPixels(5f), 1f);
		to.setTokenLayoutResId(toMode == ToMode.SINGLE
		                       ? R.layout.widget_token_view_single
		                       : R.layout.widget_token_view);

		entitySuggest = new EntitySuggestController(this)
				.setSearchInput(to)
				.setTokenListener(this)
				.setSuggestScope(suggestScope);

		entitySuggest.init();

		if (message != null) {
			TextView message = (TextView) findViewById(R.id.content_message);
			message.setText(this.message);
			message.setVisibility(View.VISIBLE);
		}
	}

	@Override public void bind() {
	    /*
	     * This method is only called when the activity is created.
         */
		super.bind();

		UI.setVisibility(animatorTo, View.VISIBLE);

		/* We don't allow the patch to be changed when editing */
		if (editing) {
			UI.setVisibility(animatorTo, View.GONE);
		}
		else {
			Entity currentPatch = Patchr.getInstance().getCurrentPatch();
			if (currentPatch != null) {
				to.addObject(currentPatch);
			}
		}

		if (buttonToClear != null && !toEditable) {
			buttonToClear.setVisibility(View.GONE);
		}

		TextView textView = (TextView) findViewById(R.id.description);
		if (messageType.equals(MessageType.SHARE)) {

			if (shareEntity.schema.equals(Constants.SCHEMA_ENTITY_PATCH)) {
				PatchView patchView = new PatchView(this, R.layout.patch_view_attachment);
				patchView.databind(shareEntity);
				CardView cardView = (CardView) share;
				int padding = UI.getRawPixelsForDisplayPixels(0f);
				cardView.setContentPadding(padding, padding, padding, padding);
				share.addView(patchView);
			}
			else if (shareEntity.schema.equals(Constants.SCHEMA_ENTITY_MESSAGE)) {
				MessageView messageView = new MessageView(this, R.layout.message_view_attachment);
				messageView.databind(shareEntity);
				CardView cardView = (CardView) share;
				int padding = UI.getRawPixelsForDisplayPixels(8f);
				cardView.setContentPadding(padding, padding, padding, padding);
				share.addView(messageView);
			}
			else {
				buttonPhotoDelete.setVisibility(View.GONE);
			}

			if (shareSchema.equals(Constants.SCHEMA_ENTITY_PATCH)
					|| shareSchema.equals(Constants.SCHEMA_ENTITY_MESSAGE)) {
				animatorPhoto.setVisibility(View.GONE);
				shareHolder.setVisibility(View.VISIBLE);
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
		if (editing) {
			this.actionBar.setTitle("Edit " + entity.schema);
		}
		else {
			if (messageType.equals(MessageType.ROOT)) {
				this.actionBar.setTitle("New " + entity.schema);
			}
			else if (messageType.equals(MessageType.SHARE)) {
				this.actionBar.setTitle("Share");
			}
		}
	}

	@Override protected void bindPhoto() {
		super.bindPhoto();
		/*
		 * Can be called from main or background thread.
		 */
		runOnUiThread(
				new Runnable() {
					@Override
					public void run() {
						animatorPhoto.requestLayout();
						animatorPhoto.setDisplayedChild(entity.photo == null ? 0 : 1);
					}
				}
		);
	}

	@Override protected boolean validate() {

		gather();
		Message message = (Message) entity;

		if (!editing && recipients.size() == 0) {

			int messageResId = 0;
			if (messageType.equals(MessageType.ROOT)) {
				messageResId = R.string.error_missing_message_to;
			}
			else if (messageType.equals(MessageType.SHARE)) {
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

	@Override protected void gather() {
		super.gather();

		if (!editing) {

			Message message = (Message) entity;
			message.type = messageType;

			if (messageType.equals(MessageType.ROOT)) {
				if (recipients.size() > 0) {
					message.patchId = recipients.get(0).id;
				}
			}
		}
	}

	@Override protected String getLinkType() {
		return Constants.TYPE_LINK_CONTENT;
	}

	@Override protected void beforeInsert(Entity entity, List<Link> links) {
	    /*
	     * Called on background thread.
		 */
		if (messageType.equals(MessageType.ROOT)) {
			for (Entity to : recipients) {
				links.add(new Link(to.id, Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_PATCH));
			}
		}
		else if (messageType.equals(MessageType.SHARE)) {
			if (shareId != null) {
				links.add(new Link(shareId, Constants.TYPE_LINK_SHARE, shareSchema));  // To support showing the shared entity with the message
			}
			for (Entity to : recipients) {
				links.add(new Link(to.id, Constants.TYPE_LINK_SHARE, Constants.SCHEMA_ENTITY_USER));
			}
		}
	}

	@Override protected boolean afterInsert() {
	    /*
	     * Only called if the insert was successful. Called on main ui thread.
		 */
		if (!messageType.equals(MessageType.SHARE)) {
			Entity currentPatch = Patchr.getInstance().getCurrentPatch();
			if (recipients.size() > 0 && (currentPatch == null || !currentPatch.id.equals(recipients.get(0).id))) {
				Patchr.router.route(this, Route.BROWSE, recipients.get(0), null);
			}
		}
		return true;
	}

	@Override protected int getLayoutId() {
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