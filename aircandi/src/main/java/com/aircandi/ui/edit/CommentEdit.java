package com.aircandi.ui.edit;

import java.util.List;

import android.os.Bundle;
import android.view.WindowManager;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.StringManager;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Link;
import com.aircandi.ui.base.BaseEntityEdit;
import com.aircandi.utilities.Dialogs;

public class CommentEdit extends BaseEntityEdit {

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
	}

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	@Override
	protected String getLinkType() {
		return Constants.TYPE_LINK_CONTENT;
	};

	@Override
	protected boolean validate() {
		if (!super.validate()) return false;
		if (mDescription.getText().length() == 0) {
			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, null
					, StringManager.getString(R.string.error_missing_message)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}
		return true;
	}

	@Override
	protected void beforeInsert(Entity entity, List<Link> links) {
		if (Aircandi.getInstance().getCurrentPlace() != null) {
			entity.placeId = Aircandi.getInstance().getCurrentPlace().id;
		}
	}

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.comment_edit;
	}
}