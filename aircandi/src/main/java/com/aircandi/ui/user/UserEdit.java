package com.aircandi.ui.user;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ViewFlipper;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.StringManager;
import com.aircandi.components.TabManager;
import com.aircandi.events.CancelEvent;
import com.aircandi.objects.Photo;
import com.aircandi.objects.Route;
import com.aircandi.objects.User;
import com.aircandi.ui.base.BaseEntityEdit;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Type;
import com.aircandi.utilities.UI;
import com.aircandi.utilities.Utilities;
import com.squareup.otto.Subscribe;

public class UserEdit extends BaseEntityEdit {

	private EditText mBio;
	private EditText mWebUri;
	private EditText mArea;
	private EditText mEmail;

	private TabManager mTabManager;

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mTabManager = new TabManager(Constants.TABS_USER_EDIT_ID, mActionBar, (ViewFlipper) findViewById(R.id.flipper_form));
		mTabManager.initialize();
		mTabManager.doRestoreInstanceState(savedInstanceState);

		mBio = (EditText) findViewById(R.id.bio);
		mWebUri = (EditText) findViewById(R.id.web_uri);
		mArea = (EditText) findViewById(R.id.area);
		mEmail = (EditText) findViewById(R.id.email);

		if (mBio != null) {
			mBio.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					if (!s.toString().equals(((User) mEntity).bio)) {
						if (!mFirstDraw) {
							mDirty = true;
						}
					}
				}
			});
		}
		if (mWebUri != null) {
			mWebUri.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					if (!s.toString().equals(((User) mEntity).webUri)) {
						if (!mFirstDraw) {
							mDirty = true;
						}
					}
				}
			});
		}
		if (mArea != null) {
			mArea.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					if (!s.toString().equals(((User) mEntity).area)) {
						if (!mFirstDraw) {
							mDirty = true;
						}
					}
				}
			});
		}
		if (mEmail != null) {
			mEmail.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					if (!s.toString().equals(((User) mEntity).email)) {
						if (!mFirstDraw) {
							mDirty = true;
						}
					}
				}
			});
		}
	}

	@Override
	public void draw() {
		super.draw();

		User user = (User) mEntity;
		if (mBio != null && !TextUtils.isEmpty(user.bio)) {
			mBio.setText(user.bio);
		}
		if (mWebUri != null && !TextUtils.isEmpty(user.webUri)) {
			mWebUri.setText(user.webUri);
		}
		if (mArea != null && !TextUtils.isEmpty(user.area)) {
			mArea.setText(user.area);
		}
		if (mEmail != null && !TextUtils.isEmpty(user.email)) {
			mEmail.setText(user.email);
		}

		((ViewGroup) findViewById(R.id.flipper_form)).setVisibility(View.VISIBLE);

	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@SuppressWarnings("ucd")
	public void onChangePasswordButtonClick(View view) {
		Aircandi.dispatch.route(this, Route.PASSWORD_CHANGE, null, null, null);
	}

	@Override
	protected void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		mTabManager.doSaveInstanceState(savedInstanceState);
	}

	@Override
	public void onPhotoSelected(Photo photo) {

		mDirty = !Photo.same(mEntity.photo, photo);
		if (mDirty) {

			mEntity.photo = photo;
			mEntity.photo.setStore(true);
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					onChangingPhoto();
					UI.drawPhoto(mPhotoView, mEntity.getPhoto());
					onChangedPhoto();
				}
			});
		}
	}

	@Subscribe
	public void onCancelEvent(CancelEvent event) {
		if (mTaskService != null) {
			mTaskService.cancel(true);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected String getLinkType() {
		return null;
	}

	@Override
	protected void gather() {
		super.gather();

		User user = (User) mEntity;
		if (mEmail != null) {
			user.email = Type.emptyAsNull(mEmail.getText().toString().trim());
		}
		if (mBio != null) {
			user.bio = Type.emptyAsNull(mBio.getText().toString().trim());
		}
		if (mArea != null) {
			user.area = Type.emptyAsNull(mArea.getText().toString().trim());
		}
		if (mWebUri != null) {
			user.webUri = Type.emptyAsNull(mWebUri.getText().toString().trim());
		}
	}

	@Override
	protected boolean validate() {
		if (!super.validate()) return false;
		gather();
		User user = (User) mEntity;

		if (user.name == null) {
			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, null
					, StringManager.getString(R.string.error_missing_fullname)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}

		if (!Utilities.validEmail(mEmail.getText().toString())) {
			Dialogs.alertDialogSimple(this, null, StringManager.getString(R.string.error_invalid_email));
			return false;
		}
		if (mWebUri != null && mWebUri.getText().toString() != null && !mWebUri.getText().toString().equals("")) {
			if (!Utilities.validWebUri(mWebUri.getText().toString())) {
				Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
						, null
						, StringManager.getString(R.string.error_weburi_invalid)
						, null
						, this
						, android.R.string.ok
						, null, null, null, null);
				return false;
			}
		}
		return true;
	}

	/*--------------------------------------------------------------------------------------------
	 * Services
	 *--------------------------------------------------------------------------------------------*/ 	/*--------------------------------------------------------------------------------------------
	 * Misc
	 *--------------------------------------------------------------------------------------------*/
	@Override
	protected int getLayoutId() {
		return R.layout.user_edit;
	}
}