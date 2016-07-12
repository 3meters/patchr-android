package com.patchr.ui.edit;

import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.MediaManager;
import com.patchr.components.StringManager;
import com.patchr.components.UserManager;
import com.patchr.model.Photo;
import com.patchr.model.RealmEntity;
import com.patchr.objects.Entity;
import com.patchr.objects.LinkOld;
import com.patchr.objects.Message.MessageType;
import com.patchr.objects.Recipient;
import com.patchr.objects.SimpleMap;
import com.patchr.objects.enums.AnalyticsCategory;
import com.patchr.objects.enums.Suggest;
import com.patchr.ui.components.EntitySuggestController;
import com.patchr.ui.views.MessageView;
import com.patchr.ui.views.PatchView;
import com.patchr.ui.widgets.ImageWidget;
import com.patchr.ui.widgets.RecipientsCompletionView;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Reporting;
import com.patchr.utilities.UI;
import com.segment.analytics.Properties;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;
import java.util.List;

public class ShareEdit extends BaseEdit {

	private String      inputShareEntityId;
	private String      inputShareEntitySchema;
	private String      inputShareSource;        // Package name of the sharing host app
	private String      inputShareType;          // Share or invite
	private RealmEntity inputShareEntity;

	private RealmEntity shareEntity;
	private String      descriptionDefault;

	private ImageWidget              userPhoto;
	private RecipientsCompletionView recipientsField;
	private ViewGroup                shareEntityView;
	private RecyclerView             listView;
	private EntitySuggestController  entitySuggest;

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

    /*--------------------------------------------------------------------------------------------
     * Events
     *--------------------------------------------------------------------------------------------*/

	@Override public boolean onCreateOptionsMenu(Menu menu) {

		getMenuInflater().inflate(R.menu.menu_send, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override public boolean onOptionsItemSelected(MenuItem item) {

		if (item.getItemId() == R.id.submit) {
			super.submitAction();
		}
		else {
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	public void onClick(View view) {
		if (view.getTag() != null) {
			if (view.getTag() instanceof Entity) {
				final Entity entity = (Entity) view.getTag();
				recipientsField.deleteText();
				recipientsField.addObject(new Recipient(entity.id, entity.name, null));
				dirty = true;
				entitySuggest.clear();
			}
		}
	}

	protected void onPhotoCanceled() {
		bindPhoto();
	}

	public void onCancelPhotoButtonClick(View view) {
		entity.setPhoto(null);
		onPhotoCanceled();
	}

    /*--------------------------------------------------------------------------------------------
     * Methods
     *--------------------------------------------------------------------------------------------*/

	@Override public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			String shareJson = extras.getString(Constants.EXTRA_SHARE_PATCH);
			if (shareJson != null) {
				this.inputShareEntity = Patchr.gson.fromJson(shareJson, RealmEntity.class);
			}
			this.inputShareType = extras.getString(Constants.EXTRA_MESSAGE_TYPE);
			this.inputShareEntityId = extras.getString(Constants.EXTRA_SHARE_ID);
			this.inputShareEntitySchema = extras.getString(Constants.EXTRA_SHARE_SCHEMA, Constants.SCHEMA_ENTITY_PICTURE);
			this.inputShareSource = extras.getString(Constants.EXTRA_SHARE_SOURCE);
		}
	}

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);   // Handles creating new entity if needed

		this.entity.type = "share";

		this.userPhoto = (ImageWidget) findViewById(R.id.user_photo);
		this.shareEntityView = (ViewGroup) findViewById(R.id.share_entity);
		this.listView = (RecyclerView) findViewById(R.id.results_list);
		ViewGroup shareHolder = (ViewGroup) findViewById(R.id.share_holder);

		this.recipientsField = (RecipientsCompletionView) findViewById(R.id.recipients);
		this.recipientsField.setLineSpacing((int) UI.getRawPixelsForDisplayPixels(4f), 1f);
		this.recipientsField.setPrefix(" To: ");
		this.recipientsField.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override public void onFocusChange(View v, boolean hasFocus) {
				listView.setVisibility(hasFocus ? View.VISIBLE : View.GONE);
			}
		});

		this.entitySuggest = new EntitySuggestController(this);
		this.entitySuggest.searchInput = this.recipientsField;
		this.entitySuggest.busyPresenter = this.busyController;
		this.entitySuggest.suggestScope = Suggest.Users;
		this.entitySuggest.recyclerView = this.listView;
		this.entitySuggest.initialize();

		Intent intent = getIntent();

		if (inputShareType != null) {
			if (inputShareType.equals(MessageType.Invite)) {
				this.actionBarTitle.setText(R.string.screen_title_invite);
				this.dirtyExitTitleResId = R.string.alert_dirty_invite_exit_title;
				this.dirtyExitMessageResId = R.string.alert_dirty_invite_exit_message;
				this.dirtyExitPositiveResId = R.string.alert_dirty_invite;
				this.insertProgressResId = R.string.progress_inviting;
				this.insertedResId = R.string.alert_invited;
				this.description.setHint(R.string.hint_invite_description);
			}
			else if (inputShareType.equals(MessageType.Share)) {
				this.actionBarTitle.setText(R.string.screen_title_share);
				this.dirtyExitTitleResId = R.string.alert_dirty_share_exit_title;
				this.dirtyExitMessageResId = R.string.alert_dirty_share_exit_message;
				this.dirtyExitPositiveResId = R.string.alert_dirty_share;
				this.insertProgressResId = R.string.progress_sharing;
				this.insertedResId = R.string.alert_shared;
				this.description.setHint(R.string.hint_share_description);
			}
		}

		/* Try to determine if it from us */
		Boolean selfSend = (this.inputShareSource != null && this.inputShareSource.equals(getPackageName()));

		UI.setVisibility(this.photoEditWidget, View.GONE);
		UI.setVisibility(shareHolder, View.GONE);

		if (selfSend) {

			switch (this.inputShareEntitySchema) {

				case Constants.SCHEMA_ENTITY_PATCH:
					if (this.inputShareEntity != null) {
						this.shareEntity = this.inputShareEntity;
					}
					else {
						//this.shareEntity = DataController.getStoreEntity(this.inputShareEntityId);
					}
					this.descriptionDefault = String.format("%1$s invited you to the \'%2$s\' patch.", UserManager.userName, this.shareEntity.name);
					UI.setVisibility(shareHolder, View.VISIBLE);
					break;

				case Constants.SCHEMA_ENTITY_MESSAGE:
					//this.shareEntity = DataController.getStoreEntity(this.inputShareEntityId);
					if (this.shareEntity.patch != null) {
						this.descriptionDefault = String.format("%1$s shared %2$s\'s message posted to the \'%3$s\' patch.", UserManager.userName, this.shareEntity.creator.name, this.shareEntity.patch.name);
					}
					else {
						this.descriptionDefault = String.format("%1$s shared %2$s\'s message posted to a patch.", UserManager.userName, this.shareEntity.creator.name);
					}
					UI.setVisibility(shareHolder, View.VISIBLE);
					break;

				case Constants.SCHEMA_ENTITY_PICTURE:
					UI.setVisibility(this.photoEditWidget, View.VISIBLE);
					this.descriptionDefault = String.format("%1$s shared a photo.", UserManager.userName);

					/* Check for a photo */
					if (intent.getType() != null) {
						if (intent.getType().contains("image/") || intent.getParcelableExtra(Intent.EXTRA_STREAM) != null) {

							final Uri photoUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
							if (photoUri != null) {

								new AsyncTask() {

									@Override protected Object doInBackground(Object... params) {
										Thread.currentThread().setName("AsyncShareBitmap");
										Photo photo = null;

										try {
											Bitmap bitmap = Picasso.with(Patchr.applicationContext)
												.load(photoUri)
												.centerInside()
												.resize(Constants.IMAGE_DIMENSION_MAX, Constants.IMAGE_DIMENSION_MAX)
												.get();

											File file = MediaManager.copyBitmapToSharePath(bitmap);
											Uri uri = MediaManager.getSharePathUri();

											if (file != null && uri != null) {
												photo = new Photo(uri.toString(), Photo.PhotoSource.file);
												photo.store = true;
											}
											else {
												UI.toast(StringManager.getString(R.string.error_storage_unmounted));
											}
										}
										catch (ConnectException e) {
											Reporting.breadcrumb("Picasso failed to load bitmap: connect");
										}
										catch (FileNotFoundException e) {
											Reporting.breadcrumb("Picasso failed to load bitmap: file not found");
										}
										catch (IOException e) {
											Reporting.breadcrumb("Picasso failed to load bitmap: io");
										}
										return photo;
									}

									@Override protected void onPostExecute(Object response) {
										if (response != null) {
											Photo photo = (Photo) response;
											onPhotoSelected(photo); // mDirty gets set in this method
											dirty = false;
											bind();
										}
									}
								}.executeOnExecutor(Constants.EXECUTOR);
							}
						}
					}
					break;
			}
		}

		/* Text/image shared from another application */
		else if (intent.getType() != null) {

			/* Intent with text from another application */
			if (intent.getType().equals("text/plain") || intent.getStringExtra(Intent.EXTRA_TEXT) != null) {
				String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
				if (sharedText != null) {
					this.dirty = true;
					this.entity.description = sharedText;
				}
			}

			/*
			 * Intent with image data. We get a uri we use to open a stream to get the bitmap.
			 * We then copy the bitmap to a our pinned share file. We are not the source of the
			 * image so we don't want to track it in the Pictures/Patchr folder.
			 */
			if (intent.getType().contains("image/") || intent.getParcelableExtra(Intent.EXTRA_STREAM) != null) {

				final Uri photoUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
				UI.setVisibility(this.photoEditWidget, View.VISIBLE);
				if (photoUri != null) {

					new AsyncTask() {

						@Override protected Object doInBackground(Object... params) {
							Thread.currentThread().setName("AsyncShareBitmap");
							Photo photo = null;

							try {

								Bitmap bitmap = Picasso.with(Patchr.applicationContext)
									.load(photoUri)
									.centerInside()
									.resize(Constants.IMAGE_DIMENSION_MAX, Constants.IMAGE_DIMENSION_MAX)
									.get();

								File file = MediaManager.copyBitmapToSharePath(bitmap);
								Uri uri = MediaManager.getSharePathUri();

								if (file != null && uri != null) {
									photo = new Photo(uri.toString(), Photo.PhotoSource.file);
									photo.store = true;
								}
								else {
									UI.toast(StringManager.getString(R.string.error_storage_unmounted));
								}
							}
							catch (FileNotFoundException e) {
								Reporting.breadcrumb("Picasso failed to load bitmap");
							}
							catch (IOException e) {
								Reporting.breadcrumb("Picasso failed to load bitmap");
							}
							return photo;
						}

						@Override protected void onPostExecute(Object response) {
							if (response != null) {
								Photo photo = (Photo) response;
								onPhotoSelected(photo); // mDirty gets set in this method
								dirty = false;
								bind();
							}
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
				this.dirty = true;
				this.entity.description = sharedText;
			}
		}
	}

	@Override public void bind() {
	    /*
	     * This method is only called when the activity is created.
         */
		super.bind();

		//UI.setImageWithEntity(this.userPhoto, UserManager.currentUser);

		if (this.description != null) {
			this.description.setMinLines(3);
		}

		if (!this.inputShareEntitySchema.equals(Constants.SCHEMA_ENTITY_PICTURE)) {
			if (shareEntity.schema.equals(Constants.SCHEMA_ENTITY_PATCH)) {
				PatchView patchView = new PatchView(this, R.layout.view_patch_attachment);
				patchView.bind(shareEntity);
				CardView cardView = (CardView) shareEntityView;
				int padding = UI.getRawPixelsForDisplayPixels(0f);
				cardView.setContentPadding(padding, padding, padding, padding);
				shareEntityView.addView(patchView);
			}
			else if (shareEntity.schema.equals(Constants.SCHEMA_ENTITY_MESSAGE)) {
				MessageView messageView = new MessageView(this, R.layout.view_message_attachment);
				//messageView.bind(shareEntity, null);
				CardView cardView = (CardView) shareEntityView;
				int padding = UI.getRawPixelsForDisplayPixels(8f);
				cardView.setContentPadding(padding, padding, padding, padding);
				shareEntityView.addView(messageView);
			}
		}
	}

	@Override public void gather(SimpleMap parameters) {
		super.gather(parameters);
		if (TextUtils.isEmpty(this.description.getText())) {
			this.entity.description = this.descriptionDefault;
		}
	}

	@Override protected boolean isValid() {

		if (this.recipientsField.getObjects().size() == 0) {

			int messageResId = 0;
			if (inputShareType.equals(MessageType.Invite)) {
				messageResId = R.string.error_missing_invite_to;
			}
			else if (inputShareType.equals(MessageType.Share)) {
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

		return true;
	}

	protected void beforeInsert(RealmEntity entity, List<LinkOld> links) {
	    /* Called on background thread. */
		if (inputShareEntityId != null) {
			links.add(new LinkOld(inputShareEntityId, Constants.TYPE_LINK_SHARE, inputShareEntitySchema));  // To support showing the shared entity with the message
		}
		for (Recipient recipient : this.recipientsField.getObjects()) {
			links.add(new LinkOld(recipient.id, Constants.TYPE_LINK_SHARE, Constants.SCHEMA_ENTITY_USER));
		}
	}

	protected boolean afterInsert(Entity entity) {
		if (inputShareType != null) {
			if (inputShareType.equals(MessageType.Invite)) {
				Reporting.track(AnalyticsCategory.EDIT, "Sent Patch Invitation", new Properties().putValue("network", "Patchr"));
			}
			else if (inputShareType.equals(MessageType.Share)) {
				Reporting.track(AnalyticsCategory.EDIT, "Shared Message", new Properties().putValue("network", "Patchr"));
			}
		}
		return true;
	}

	@Override protected int getLayoutId() {
		return R.layout.edit_share;
	}
}