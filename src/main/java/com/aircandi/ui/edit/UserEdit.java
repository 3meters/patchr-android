package com.aircandi.ui.edit;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.components.StringManager;
import com.aircandi.events.CancelEvent;
import com.aircandi.objects.Photo;
import com.aircandi.objects.Route;
import com.aircandi.objects.User;
import com.aircandi.ui.base.BaseEntityEdit;
import com.aircandi.ui.components.SimpleTextWatcher;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Type;
import com.aircandi.utilities.UI;
import com.aircandi.utilities.Utilities;
import com.squareup.otto.Subscribe;

public class UserEdit extends BaseEntityEdit {

	private EditText mArea;
	private EditText mEmail;

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mArea = (EditText) findViewById(R.id.area);
		mEmail = (EditText) findViewById(R.id.email);

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
	public void draw(View view) {
		super.draw(view);

		User user = (User) mEntity;
		if (mArea != null && !TextUtils.isEmpty(user.area)) {
			mArea.setText(user.area);
		}
		if (mEmail != null && !TextUtils.isEmpty(user.email)) {
			mEmail.setText(user.email);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@SuppressWarnings("ucd")
	public void onChangePasswordButtonClick(View view) {
		Patchr.dispatch.route(this, Route.PASSWORD_CHANGE, null, null);
	}

	@Override
	public void onPhotoSelected(Photo photo) {

		mDirty = !Photo.same(mEntity.photo, photo);
		if (mDirty) {

			mEntity.photo = photo;
			if (mEntity.photo != null) {
				mEntity.photo.setStore(true);
			}
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
		if (mArea != null) {
			user.area = Type.emptyAsNull(mArea.getText().toString().trim());
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