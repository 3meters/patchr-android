package com.aircandi.catalina.ui.edit;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.aircandi.Aircandi;
import com.aircandi.catalina.Constants;
import com.aircandi.catalina.R;
import com.aircandi.catalina.objects.Message;
import com.aircandi.components.EntityManager;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.ProximityManager;
import com.aircandi.components.StringManager;
import com.aircandi.controllers.IEntityController;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Link;
import com.aircandi.objects.TransitionType;
import com.aircandi.ui.base.IBusy;
import com.aircandi.ui.components.EntitySuggestController;
import com.aircandi.ui.widgets.AirTokenCompleteTextView;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.UI;
import com.tokenautocomplete.TokenCompleteTextView;

import java.util.ArrayList;
import java.util.List;

public class PlaceEdit extends com.aircandi.ui.edit.PlaceEdit implements TokenCompleteTextView.TokenListener {

	private AirTokenCompleteTextView mTo;
	private EntitySuggestController  mEntitySuggest;
	private List<Entity> mTos = new ArrayList<Entity>();

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mTo = (AirTokenCompleteTextView) findViewById(com.aircandi.R.id.to);
		if (mTo != null) {
			mTo.setLineSpacing((int) UI.getRawPixelsForDisplayPixels(5f), 1f);
			mTo.setTokenLayoutResId(com.aircandi.R.layout.widget_token_view);
			mTo.setOnFocusChangeListener(new View.OnFocusChangeListener() {
				@Override
				public void onFocusChange(View v, boolean hasFocus) {
					if (hasFocus) {
						UI.showSoftInput(PlaceEdit.this);
					}
				}
			});
			mEntitySuggest = new EntitySuggestController(this)
					.setInput(mTo)
					.setTokenListener(this)
					.setSuggestScope(EntityManager.SuggestScope.USERS);
			mEntitySuggest.init();
		}
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@Override
	public void onTokenAdded(Object o) {
		if (!mTos.contains(o)) {
			mTos.add((Entity) o);
			mDirty = true;
		}
	}

	@Override
	public void onTokenRemoved(Object o) {
		if (mTos.contains(o)) {
			mTos.remove((Entity) o);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	@Override
	protected boolean afterInsert() {
	    /*
         * Only called if the insert was successful. Called on main ui thread.
         * We link replies to the places they are associated with. This give us the option
		 * to thread, flatten or do some combo. Called on background thread.
		 */
		if (!mEditing && mTos.size() > 0) {

			mInsertedResId = R.string.alert_saved_and_shared;

			new AsyncTask() {

				@Override
				protected void onPreExecute() {
					mBusy.hideBusy(false);
					mBusy.showBusy(IBusy.BusyAction.ActionWithMessage, mInsertProgressResId);
				}

				@Override
				protected Object doInBackground(Object... params) {
					Thread.currentThread().setName("AsyncAutoSharePlace");

                    /* Create message entity */
					IEntityController controller = Aircandi.getInstance().getControllerForSchema(Constants.SCHEMA_ENTITY_MESSAGE);
					Entity message = controller.makeNew();
					message.description = String.format(StringManager.getString(R.string.label_place_share_body_self_oncreate), mEntity.name);
					message.type = Message.MessageType.SHARE;
					if (Aircandi.getInstance().getCurrentUser() != null) {
						message.creator = Aircandi.getInstance().getCurrentUser();
						message.creatorId = Aircandi.getInstance().getCurrentUser().id;
					}

                    /* Links */
					List<Link> links = new ArrayList<Link>();
					links.add(new Link(mEntity.id, Constants.TYPE_LINK_SHARE, Constants.SCHEMA_ENTITY_PLACE));
					for (Entity to : mTos) {
						links.add(new Link(to.id, Constants.TYPE_LINK_SHARE, Constants.SCHEMA_ENTITY_USER));
					}
					final ProximityManager.ModelResult result = Aircandi.getInstance().getEntityManager().insertEntity(message, links, null, null, null, true);
					return result;
				}

				@Override
				protected void onPostExecute(Object response) {
					final ProximityManager.ModelResult result = (ProximityManager.ModelResult) response;
					mBusy.hideBusy(false);

					if (result.serviceResponse.responseCode == NetworkManager.ResponseCode.SUCCESS) {
						UI.showToastNotification(StringManager.getString(mInsertedResId), Toast.LENGTH_SHORT);
						setResultCode(Activity.RESULT_OK);
						finish();
						Aircandi.getInstance().getAnimationManager().doOverridePendingTransition(PlaceEdit.this, TransitionType.FORM_TO_PAGE);
					}
					else {
						Errors.handleError(PlaceEdit.this, result.serviceResponse);
					}
				}
			}.execute();

			return false; // Tells caller that we will handle finishing
		}

		return true;
	}
}
