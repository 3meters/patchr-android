package com.aircandi.catalina.ui;

import android.app.Activity;
import android.view.View;
import android.widget.CompoundButton;

import com.aircandi.Aircandi;
import com.aircandi.catalina.Constants;
import com.aircandi.catalina.R;
import com.aircandi.components.Extras;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Route;
import com.aircandi.ui.EntityListFragment;
import com.aircandi.ui.widgets.ComboButton;

public class WatcherListFragment extends EntityListFragment {

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@Override
	public void onClick(View v) {
		final Entity entity = (Entity) ((ViewHolder) v.getTag()).data;

		Extras extras = new Extras()
				.setEntitySchema(Constants.SCHEMA_ENTITY_USER)
				.setEntityId(entity.id);

		Aircandi.dispatch.route(getSherlockActivity(), Route.BROWSE, entity, null, extras.getExtras());
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	@Override
	protected void postBind() {
		super.postBind();
	}

	@Override
	public ViewHolder bindHolder(View view, ViewHolder holder) {

		if (holder == null) {
			holder = new ViewHolderExtended();
		}

		((ViewHolderExtended) holder).enable = (CompoundButton) view.findViewById(R.id.switch_enable);
		((ViewHolderExtended) holder).delete = (ComboButton) view.findViewById(R.id.button_delete);

		return super.bindHolder(view, holder);
	}

	@Override
	protected void drawListItem(Entity entity, View view, final ViewHolder holder) {
		super.drawListItem(entity, view, holder);

		if (holder.candiView != null) {
//			ViewGroup layout = holder.candiView.getLayout();
//			Entity parent = ((BaseActivity) getSherlockActivity()).getEntity();
//			Boolean isOwner = entity.id.equals(parent.ownerId);

//			layout.findViewById(R.id.holder_owner_edit).setVisibility((parent.isOwnedByCurrentUser() && !isOwner) ? View.VISIBLE : View.GONE);
//			layout.findViewById(R.id.button_delete).setVisibility((parent.isOwnedByCurrentUser() && !isOwner) ? View.VISIBLE : View.GONE);

//			((ViewHolderExtended) holder).delete.setTag(entity);
//			((ViewHolderExtended) holder).enable.setTag(entity);
//			((ViewHolderExtended) holder).enable.setChecked(entity.enabled);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Properties
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Menus
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Lifecycle
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------------------------------

	public class ViewHolderExtended extends ViewHolder {
		public CompoundButton	enable; // NO_UCD (unused code)
		public ComboButton		delete; // NO_UCD (unused code)
	}
}