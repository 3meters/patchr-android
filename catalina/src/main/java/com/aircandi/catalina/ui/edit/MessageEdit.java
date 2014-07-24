package com.aircandi.catalina.ui.edit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;

import com.aircandi.Aircandi;
import com.aircandi.catalina.Constants;
import com.aircandi.catalina.R;
import com.aircandi.catalina.objects.Message;
import com.aircandi.catalina.objects.Message.MessageType;
import com.aircandi.components.AnimationManager;
import com.aircandi.components.MediaManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.StringManager;
import com.aircandi.controllers.IEntityController;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Link;
import com.aircandi.objects.Photo;
import com.aircandi.objects.Place;
import com.aircandi.objects.Route;
import com.aircandi.ui.base.BaseEntityEdit;
import com.aircandi.ui.base.IBusy.BusyAction;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.Json;
import com.aircandi.utilities.Type;
import com.aircandi.utilities.UI;
import com.squareup.picasso.Picasso.LoadedFrom;
import com.squareup.picasso.Target;

public class MessageEdit extends BaseEntityEdit {

	private Place			mToPlace;
	private String			mMessageType	= MessageType.ROOT;
	private String			mReplyPlaceId;						// Passed in for replies
	private String			mReplyRootId;
	private String			mReplyToId;
	private String			mReplyToName;
	private String			mMessage;

	private ViewAnimator	mAnimatorTo;
	private ViewAnimator	mAnimatorPhoto;
	private Button			mButtonTo;

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
			mReplyPlaceId = extras.getString(Constants.EXTRA_PLACE_ID);
			mReplyRootId = extras.getString(Constants.EXTRA_MESSAGE_ROOT_ID);
			mReplyToId = extras.getString(Constants.EXTRA_MESSAGE_REPLY_TO_ID);
			mReplyToName = extras.getString(Constants.EXTRA_MESSAGE_REPLY_TO_NAME);
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
		if (!Aircandi.firstStartApp && Aircandi.getInstance().getCurrentUser().isAnonymous()) {
			Aircandi.firstStartIntent = getIntent();
			Aircandi.dispatch.route(this, Route.SPLASH, null, null, null);
		}
	}

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mEntitySchema = Constants.SCHEMA_ENTITY_MESSAGE;

		mDirtyExitTitleResId = R.string.alert_dirty_exit_title_message;
		mDirtyExitMessageResId = R.string.alert_dirty_exit_message_message;
		mDirtyExitPositiveResId = R.string.alert_dirty_send;

		mInsertProgressResId = R.string.progress_sending;
		mInsertedResId = R.string.alert_message_sent;

		mAnimatorTo = (ViewAnimator) findViewById(R.id.animator_to);
		mAnimatorTo.setInAnimation(this, R.anim.fade_in_short);
		mAnimatorTo.setOutAnimation(this, R.anim.fade_out_short);

		mAnimatorPhoto = (ViewAnimator) findViewById(R.id.animator_photo);
		mAnimatorPhoto.setInAnimation(MessageEdit.this, R.anim.fade_in_medium);
		mAnimatorPhoto.setOutAnimation(MessageEdit.this, R.anim.fade_out_medium);

		mButtonTo = (Button) findViewById(R.id.button_to);
		mButtonTo.setHint(StringManager.getString(R.string.hint_message_to));

		if (mMessage != null) {
			TextView message = (TextView) findViewById(R.id.content_message);
			message.setText(mMessage);
			message.setVisibility(View.VISIBLE);
		}

		Intent intent = getIntent();

		/*
		 * Make sure that we don't already have a place set when
		 * handling a share intent.
		 */
		if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_SEND)) {
			Aircandi.getInstance().setCurrentPlace(null);
			onClearPlaceButtonClick(null);
		}
	}

	@Override
	protected void configureActionBar() {
		super.configureActionBar();
		/*
		 * Navigation setup for action bar icon and title
		 */
		Drawable icon = getResources().getDrawable(R.drawable.ic_action_message_dark);
		mActionBar.setIcon(icon);
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	@Override
	public void bind(BindingMode mode) {

		if (!mEditing && mEntity == null && mEntitySchema != null) {

			IEntityController controller = Aircandi.getInstance().getControllerForSchema(mEntitySchema);
			mEntity = controller.makeNew();

			if (Aircandi.getInstance().getCurrentUser() != null) {
				mEntity.creator = Aircandi.getInstance().getCurrentUser();
				mEntity.creatorId = Aircandi.getInstance().getCurrentUser().id;
			}

			if (mReplyToId != null) {
				((Message) mEntity).replyToId = mReplyToId;
			}

			/* Action bar title */

			if (mMessageType.equals(MessageType.ROOT)) {
				setActivityTitle("New " + mEntity.getSchemaMapped());
			}
			else if (mReplyToName != null) {
				setActivityTitle("Reply to " + mReplyToName);
			}
			else {
				setActivityTitle("Reply");
			}

			/*
			 * Check to see if some data for this new message was passed
			 * in the intent.
			 */
			Intent intent = getIntent();
			if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_SEND)) {
				if (intent.getType() != null) {

					/* Intent with text */
					if (intent.getType().equals("text/plain") || intent.getStringExtra(Intent.EXTRA_TEXT) != null) {
						String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
						if (sharedText != null) {
							/*
							 * If this is candipatch sharing with itself then strip the install info
							 */
							String installUri = StringManager.getString(R.string.uri_play_store);
							if (sharedText.contains(installUri)) {
								String installLabel = StringManager.getString(R.string.label_place_share_body_install);
								sharedText = sharedText.replace(installLabel, "");
								sharedText = sharedText.replace(installUri, "");
							}

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

					/* Intent with text */
					else if (intent.getType().equals("text/plain")) {
						String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
						if (sharedText != null) {
							mDirty = true;
							mEntity.description = sharedText;
						}
					}

				}
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
		draw();
	}

	@Override
	public void draw() {

		UI.setVisibility(mAnimatorTo, View.VISIBLE);
		
		/* We don't allow the place to be changed when editing */
		if ((mMessageType != null && mMessageType.equals(MessageType.REPLY)) || mEditing) {
			UI.setVisibility(mAnimatorTo, View.GONE);
		}

		Entity currentPlace = Aircandi.getInstance().getCurrentPlace();
		if (currentPlace != null) {
			mToPlace = (Place) currentPlace;
			setPlace();
		}

		super.draw();

		if (mEditing) {
			setActivityTitle("Edit " + mEntity.getSchemaMapped());
		}
	}

	@Override
	protected void drawPhoto() {
		if (mPhotoView != null && mEntity.photo != null) {
			UI.drawPhoto(mPhotoView, mEntity.getPhoto());
			onChangedPhoto();
		}
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@Override
	public void onAccept() {
		if (isDirty()) {
			if (validate()) { // validate() also gathers

				/* Upsize the place we are sending to if needed */
				if (mMessageType != null && mMessageType.equals(MessageType.REPLY)) {
					insert();
				}
				else if (!mEditing && Type.isTrue(mToPlace.synthetic)) {
					/*
					 * Upsized places do not automatically link to nearby beacons because
					 * the browsing action isn't enough of an indicator of proximity.
					 */
					new AsyncTask() {

						@Override
						protected void onPreExecute() {
							mBusy.hideBusy(false);
							mBusy.showBusy(BusyAction.ActionWithMessage, mInsertProgressResId);
						}

						@Override
						protected Object doInBackground(Object... params) {
							Thread.currentThread().setName("AsyncUpsizeSynthetic");
							final ModelResult result = Aircandi.getInstance().getEntityManager().upsizeSynthetic((Place) mToPlace, false);
							return result;
						}

						@Override
						protected void onPostExecute(Object response) {
							final ModelResult result = (ModelResult) response;
							mBusy.hideBusy(false);
							if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
								mToPlace = (Place) result.data;
								mParentId = mToPlace.id;
								gather();  // Regather because we now have an official place
								if (mEditing) {
									update();
								}
								else {
									insert();
								}
							}
							else {
								Errors.handleError(MessageEdit.this, result.serviceResponse);
							}
						}
					}.execute();
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
							final BitmapDrawable bitmapDrawable = new BitmapDrawable(Aircandi.applicationContext.getResources(), bitmap);
							UI.showDrawableInImageView(bitmapDrawable, mPhotoView.getImageView(), true, AnimationManager.fadeInMedium());
							onChangedPhoto();
						}

						@Override
						public void onPrepareLoad(Drawable arg0) {}
					});

					UI.drawPhoto(mPhotoView, mEntity.getPhoto());
				}
			});
		}
	}

	@SuppressWarnings("ucd")
	public void onPlaceSearchClick(View view) {
		Aircandi.dispatch.route(MessageEdit.this, Route.PLACE_SEARCH, null, null, null);
	}

	@SuppressWarnings("ucd")
	public void onClearPlaceButtonClick(View view) {
		mToPlace = null;
		mAnimatorTo.setDisplayedChild(0);
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

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		/*
		 * Called before onResume. If we are returning from the market app, we get a zero result code whether the user
		 * decided to start an install or not.
		 */
		if (resultCode != Activity.RESULT_CANCELED) {
			if (requestCode == Constants.ACTIVITY_PLACE_SEARCH) {

				if (intent != null && intent.getExtras() != null) {
					final Bundle extras = intent.getExtras();
					final String jsonPlace = extras.getString(Constants.EXTRA_ENTITY);
					if (!TextUtils.isEmpty(jsonPlace)) {
						mToPlace = (Place) Json.jsonToObject(jsonPlace, Json.ObjectType.ENTITY);
						mParentId = mToPlace.id;
						setPlace();
					}
				}
			}
		}
		super.onActivityResult(requestCode, resultCode, intent);
	}

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	protected void setPlace() {

		final AirImageView placePhotoView = (AirImageView) findViewById(R.id.place_photo);
		final TextView placeName = (TextView) findViewById(R.id.place_name);
		final TextView placeSubtitle = (TextView) findViewById(R.id.place_subtitle);

		UI.setVisibility(placePhotoView, View.GONE);
		if (placePhotoView != null) {
			UI.drawPhoto(placePhotoView, mToPlace.getPhoto());
			UI.setVisibility(placePhotoView, View.VISIBLE);
		}
		UI.setVisibility(placeName, View.GONE);
		if (!TextUtils.isEmpty(mToPlace.name)) {
			placeName.setText(Html.fromHtml(mToPlace.name));
			UI.setVisibility(placeName, View.VISIBLE);
		}
		UI.setVisibility(placeSubtitle, View.GONE);
		if (placeSubtitle != null) {
			String address = mToPlace.getAddressString(false);

			if (!TextUtils.isEmpty(address)) {
				placeSubtitle.setText(address);
				UI.setVisibility(placeSubtitle, View.VISIBLE);
			}
			else {
				if (mToPlace.category != null && !TextUtils.isEmpty(mToPlace.category.name)) {
					placeSubtitle.setText(Html.fromHtml(mToPlace.category.name));
					UI.setVisibility(placeSubtitle, View.VISIBLE);
				}
			}
		}
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

		if (!mEditing && mToPlace == null && !mMessageType.equals(MessageType.REPLY)) {
			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, null
					, StringManager.getString(R.string.error_missing_message_place)
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
			else {
				if (mToPlace != null) {
					message.placeId = mToPlace.id;
				}
			}
		}
	}

	@Override
	protected String getLinkType() {
		return Constants.TYPE_LINK_CONTENT;
	};

	@Override
	protected void beforeInsert(Entity entity, List<Link> links) {
		/*
		 * We link replies to the places they are associated with. This give us the option
		 * to thread, flatten or do some combo. Called on background thread.
		 */
		if (mMessageType.equals(MessageType.REPLY)) {
			links.add(new Link(mEntity.placeId, Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_PLACE));
		}
	}

	@Override
	protected void afterInsert() {
		/*
		 * Only called if the insert was successful. Called on main ui thread.
		 */
		Entity currentPlace = Aircandi.getInstance().getCurrentPlace();
		if (mToPlace != null && (currentPlace == null || !currentPlace.id.equals(mToPlace.id))) {
			Aircandi.dispatch.route(this, Route.BROWSE, mToPlace, null, null);
		}
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

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.message_edit;
	}

	// --------------------------------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------------------------------

}