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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.AnimationManager;
import com.patchr.components.MediaManager;
import com.patchr.components.StringManager;
import com.patchr.components.UserManager;
import com.patchr.model.Photo;
import com.patchr.model.RealmEntity;
import com.patchr.objects.Recipient;
import com.patchr.objects.SimpleMap;
import com.patchr.objects.enums.AnalyticsCategory;
import com.patchr.objects.enums.MessageType;
import com.patchr.objects.enums.State;
import com.patchr.objects.enums.Suggest;
import com.patchr.objects.enums.TransitionType;
import com.patchr.service.EntitySerializer;
import com.patchr.service.RestClient;
import com.patchr.ui.components.BusyController;
import com.patchr.ui.components.EntitySuggestController;
import com.patchr.ui.views.MessageView;
import com.patchr.ui.views.PatchView;
import com.patchr.ui.widgets.ImageWidget;
import com.patchr.ui.widgets.RecipientsCompletionView;
import com.patchr.utilities.Dialogs;
import com.patchr.utilities.Errors;
import com.patchr.utilities.Reporting;
import com.patchr.utilities.UI;
import com.segment.analytics.Properties;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;

public class ShareEdit extends BaseEdit {

	private String inputShareEntityId;
	private String inputShareEntitySchema;
	private String inputShareSource;        // Package name of the sharing host app
	private String inputShareType;          // Share or invite

	private RealmEntity shareEntity;
	private String      descriptionDefault;
	public  boolean     dirty;

	private ImageWidget              userPhoto;
	private RecipientsCompletionView recipientsField;
	private ViewGroup                shareEntityView;
	private RecyclerView             listView;
	private EntitySuggestController  suggestController;

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		bind();
	}

	@Override protected void onStop() {
		super.onStop();
		if (suggestController != null) {
			suggestController.onStop();
		}
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
			submitAction();
		}
		else {
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	@Override public void onClick(View view) {
		if (view.getTag() != null) {
			if (view.getTag() instanceof RealmEntity) {
				final RealmEntity entity = (RealmEntity) view.getTag();
				recipientsField.deleteText();
				recipientsField.addObject(new Recipient(entity.id, entity.name, null));
				dirty = true;
				suggestController.clear();
			}
		}
	}

	@Override public void submitAction() {

		if (!isValid()) return;
		if (!processing) {
			processing = true;
			SimpleMap parameters = new SimpleMap();
			gather(parameters);
			post(parameters);
		}
	}

    /*--------------------------------------------------------------------------------------------
     * Methods
     *--------------------------------------------------------------------------------------------*/

	@Override public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			inputShareType = extras.getString(Constants.EXTRA_MESSAGE_TYPE);
			inputShareEntityId = extras.getString(Constants.EXTRA_SHARE_ENTITY_ID);
			inputShareEntitySchema = extras.getString(Constants.EXTRA_SHARE_SCHEMA, Constants.SCHEMA_ENTITY_PICTURE);
			inputShareSource = extras.getString(Constants.EXTRA_SHARE_SOURCE);
		}
	}

	@Override public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);   // Handles creating new entity if needed

		if (inputState == null) {
			inputState = State.Inserting;    // Not set when called via android sharing
		}

		userPhoto = (ImageWidget) findViewById(R.id.user_photo);
		shareEntityView = (ViewGroup) findViewById(R.id.share_entity);
		listView = (RecyclerView) findViewById(R.id.results_list);
		ViewGroup shareHolder = (ViewGroup) findViewById(R.id.share_holder);

		recipientsField = (RecipientsCompletionView) findViewById(R.id.recipients);
		recipientsField.setLineSpacing((int) UI.getRawPixelsForDisplayPixels(4f), 1f);
		recipientsField.setPrefix(" To: ");
		recipientsField.setOnFocusChangeListener((view, hasFocus) -> {
			listView.setVisibility(hasFocus ? View.VISIBLE : View.GONE);
		});

		suggestController = new EntitySuggestController(this);
		suggestController.searchInput = recipientsField;
		suggestController.busyPresenter = busyController;
		suggestController.suggestScope = Suggest.Users;
		suggestController.recyclerView = listView;
		suggestController.bind();

		Intent intent = getIntent();

		if (inputShareType != null) {
			if (inputShareType.equals(MessageType.Invite)) {
				actionBarTitle.setText(R.string.screen_title_invite);
				dirtyExitTitleResId = R.string.alert_dirty_invite_exit_title;
				dirtyExitMessageResId = R.string.alert_dirty_invite_exit_message;
				dirtyExitPositiveResId = R.string.alert_dirty_invite;
				insertProgressResId = R.string.progress_inviting;
				insertedResId = R.string.alert_invited;
				descriptionView.setHint(R.string.hint_invite_description);
			}
			else if (inputShareType.equals(MessageType.Share)) {
				actionBarTitle.setText(R.string.screen_title_share);
				dirtyExitTitleResId = R.string.alert_dirty_share_exit_title;
				dirtyExitMessageResId = R.string.alert_dirty_share_exit_message;
				dirtyExitPositiveResId = R.string.alert_dirty_share;
				insertProgressResId = R.string.progress_sharing;
				insertedResId = R.string.alert_shared;
				descriptionView.setHint(R.string.hint_share_description);
			}
		}

		/* Try to determine if it from us */
		Boolean selfSend = (inputShareSource != null && inputShareSource.equals(getPackageName()));

		UI.setVisibility(photoEditWidget, View.GONE);
		UI.setVisibility(shareHolder, View.GONE);

		if (selfSend) {

			switch (inputShareEntitySchema) {

				case Constants.SCHEMA_ENTITY_PATCH:
					shareEntity = realm.where(RealmEntity.class).equalTo("id", inputShareEntityId).findFirst();
					descriptionDefault = String.format("%1$s invited you to the \'%2$s\' patch.", UserManager.userName, shareEntity.name);
					UI.setVisibility(shareHolder, View.VISIBLE);
					break;

				case Constants.SCHEMA_ENTITY_MESSAGE:
					shareEntity = realm.where(RealmEntity.class).equalTo("id", inputShareEntityId).findFirst();
					if (shareEntity.patch != null) {
						descriptionDefault = String.format("%1$s shared %2$s\'s message posted to the \'%3$s\' patch.", UserManager.userName, shareEntity.owner.name, shareEntity.patch.name);
					}
					else {
						descriptionDefault = String.format("%1$s shared %2$s\'s message posted to a patch.", UserManager.userName, shareEntity.owner.name);
					}
					UI.setVisibility(shareHolder, View.VISIBLE);
					break;

				case Constants.SCHEMA_ENTITY_PICTURE:
					UI.setVisibility(photoEditWidget, View.VISIBLE);
					descriptionDefault = String.format("%1$s shared a photo.", UserManager.userName);

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
					dirty = true;
					entity.description = sharedText;
				}
			}

			/*
			 * Intent with image data. We get a uri we use to open a stream to get the bitmap.
			 * We then copy the bitmap to a our pinned share file. We are not the source of the
			 * image so we don't want to track it in the Pictures/Patchr folder.
			 */
			if (intent.getType().contains("image/") || intent.getParcelableExtra(Intent.EXTRA_STREAM) != null) {

				final Uri photoUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
				UI.setVisibility(photoEditWidget, View.VISIBLE);
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
				dirty = true;
				entity.description = sharedText;
			}
		}
	}

	@Override public void bind() {
	    /*
	     * This method is only called when the activity is created.
         */
		super.bind();

		UI.setImageWithEntity(this.userPhoto, UserManager.currentUser);

		if (this.descriptionView != null) {
			this.descriptionView.setMinLines(3);
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
				messageView.bind(shareEntity, null);
				CardView cardView = (CardView) shareEntityView;
				int padding = UI.getRawPixelsForDisplayPixels(8f);
				cardView.setContentPadding(padding, padding, padding, padding);
				shareEntityView.addView(messageView);
			}
		}
	}

	@Override public void gather(SimpleMap parameters) {
		super.gather(parameters); // Name, photo, description

		parameters.put("type", Constants.TYPE_LINK_SHARE);

		if (TextUtils.isEmpty(descriptionView.getText())) {
			parameters.put("description", descriptionDefault);
		}

		List<SimpleMap> links = new ArrayList<SimpleMap>();

		if (inputShareEntityId != null) {
			SimpleMap link = new SimpleMap();
			link.put("type", Constants.TYPE_LINK_SHARE);
			link.put("_to", inputShareEntityId);
			links.add(link);
		}

		for (Recipient recipient : this.recipientsField.getObjects()) {
			SimpleMap link = new SimpleMap();
			link.put("type", Constants.TYPE_LINK_SHARE);
			link.put("_to", recipient.id);
			links.add(link);
		}

		parameters.put("links", links);
	}

	protected void post(SimpleMap data) {

		String path = entity == null ? "data/messages" : String.format("data/messages/%1$s", entity.id);
		busyController.show(BusyController.BusyAction.ActionWithMessage, insertProgressResId, ShareEdit.this);

		AsyncTask.execute(() -> {

			if (data.containsKey("photo")) {
				Photo photo = Photo.setPropertiesFromMap(new Photo(), (SimpleMap) data.get("photo"));
				if (photo != null) {
					Photo photoFinal = postPhotoToS3(photo);
					data.put("photo", photoFinal);
				}
			}

			subscription = RestClient.getInstance().postEntity(path, data)
				.subscribe(
					response -> {
						processing = false;
						busyController.hide(true);
						if (inputShareType != null) {
							if (inputShareType.equals(MessageType.Invite)) {
								Reporting.track(AnalyticsCategory.EDIT, "Sent Patch Invitation", new Properties().putValue("network", "Patchr"));
							}
							else if (inputShareType.equals(MessageType.Share)) {
								Reporting.track(AnalyticsCategory.EDIT, "Shared Message", new Properties().putValue("network", "Patchr"));
							}
						}
						finish();
						AnimationManager.doOverridePendingTransition(ShareEdit.this, TransitionType.FORM_BACK);
					},
					error -> {
						processing = false;
						busyController.hide(true);
						Errors.handleError(this, error);
					});
		});
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

			Dialogs.alert(StringManager.getString(messageResId), this);
			return false;
		}

		return true;
	}

	@Override protected int getLayoutId() {
		return R.layout.edit_share;
	}
}