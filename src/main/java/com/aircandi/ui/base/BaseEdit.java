package com.aircandi.ui.base;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;

import com.aircandi.Constants;
import com.aircandi.Patch;
import com.aircandi.R;
import com.aircandi.components.StringManager;
import com.aircandi.interfaces.IBind;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Link;
import com.aircandi.objects.Route;
import com.aircandi.utilities.Dialogs;

import java.util.List;

public abstract class BaseEdit extends BaseActivity implements IBind {

	protected Boolean mEditing                = false;
	protected Boolean mDirty                  = false;
	protected Integer mDirtyExitTitleResId    = R.string.alert_dirty_exit_title;
	protected Integer mDirtyExitMessageResId  = R.string.alert_dirty_exit_message;
	protected Integer mDirtyExitPositiveResId = R.string.alert_dirty_save;

	/* Inputs */
	protected Boolean mSkipSave = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!isFinishing()) {
			bind(BindingMode.AUTO);
		}
	}

	@Override
	public void unpackIntent() {
		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mSkipSave = extras.getBoolean(Constants.EXTRA_SKIP_SAVE, false);
		}
	}

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
		Patch.resultCode = Activity.RESULT_OK;
	}

	@Override
	public void bind(BindingMode mode) {
		draw(null);
	}

	@Override
	public void draw(View view) {}

	protected void actionBarIcon() {
		if (mActionBar != null) {
			Drawable icon = getResources().getDrawable(R.drawable.img_edit_dark);
			mActionBar.setIcon(icon);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onCancel(Boolean force) {
		if (!force && mDirty) {
			confirmDirtyExit();
		}
		else {
			super.onCancel(force);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public Boolean isDirty() {
		return mDirty;
	}

	protected void insert() {}

	protected void update() {}

	protected void confirmDirtyExit() {
		if (!mSkipSave) {
			final AlertDialog dialog = Dialogs.alertDialog(null
					, StringManager.getString(mDirtyExitTitleResId)
					, StringManager.getString(mDirtyExitMessageResId)
					, null
					, BaseEdit.this
					, mDirtyExitPositiveResId
					, android.R.string.cancel
					, R.string.alert_dirty_discard
					, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (which == DialogInterface.BUTTON_POSITIVE) {
						onAccept();
					}
					else if (which == DialogInterface.BUTTON_NEUTRAL) {
						Patch.dispatch.route(BaseEdit.this, Route.CANCEL_FORCE, null, null, null);
					}
				}
			}
					, null);
			dialog.setCanceledOnTouchOutside(false);
		}
		else {
			final AlertDialog dialog = Dialogs.alertDialog(null
					, StringManager.getString(mDirtyExitTitleResId)
					, StringManager.getString(mDirtyExitMessageResId)
					, null
					, BaseEdit.this
					, R.string.alert_dirty_discard
					, android.R.string.cancel
					, null
					, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (which == DialogInterface.BUTTON_POSITIVE) {
						Patch.dispatch.route(BaseEdit.this, Route.CANCEL_FORCE, null, null, null);
					}
				}
			}
					, null);
			dialog.setCanceledOnTouchOutside(false);
		}
	}

	protected boolean validate() {
		return true;
	}

	protected void beforeInsert(Entity entity, List<Link> links) {}

	protected void beforeUpdate(Entity entity) {}

	protected boolean afterInsert() {
		return true;
	}

	protected boolean afterUpdate() {
		return true;
	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	@SuppressWarnings("ucd")
	public Boolean isEditing() {
		return mEditing;
	}

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/
}