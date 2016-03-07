package com.patchr.ui;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.TextView;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.events.ActionEvent;
import com.patchr.events.DataErrorEvent;
import com.patchr.events.DataNoopEvent;
import com.patchr.events.DataResultEvent;
import com.patchr.interfaces.IEntityController;
import com.patchr.objects.Entity;
import com.patchr.objects.Patch;
import com.patchr.objects.Route;
import com.patchr.objects.User;
import com.patchr.objects.ViewHolder;
import com.patchr.ui.base.BaseActivity;
import com.patchr.utilities.UI;
import com.squareup.otto.Subscribe;

public class UserListFragment extends EntityListFragment {

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Subscribe
	public void onDataResult(final DataResultEvent event) {
		super.onDataResult(event);
	}

	@Subscribe
	public void onDataError(DataErrorEvent event) {
		super.onDataError(event);
	}

	@Subscribe
	public void onDataNoop(DataNoopEvent event) {
		super.onDataNoop(event);
	}

	@Override
	public void onClick(View v) {

		final Entity entity = (Entity) ((ViewHolder) v.getTag()).data;
		final Bundle extras = new Bundle();

		extras.putString(Constants.EXTRA_ENTITY_SCHEMA, Constants.SCHEMA_ENTITY_USER);
		extras.putString(Constants.EXTRA_ENTITY_ID, entity.id);

		Patchr.router.route(getActivity(), Route.BROWSE, entity, extras);
	}

	@Subscribe
	public void onViewClick(ActionEvent event) {
		super.onViewClick(event);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected void bindListItem(Entity entity, View view, String groupTag) {

		IEntityController controller = Patchr.getInstance().getControllerForEntity(entity);

		ViewHolder holder = (ViewHolder) view.getTag();
		if (holder == null) {
			holder = new ViewHolderExtended();

			((ViewHolderExtended) holder).enable = (CompoundButton) view.findViewById(R.id.switch_enable);
			((ViewHolderExtended) holder).delete = (ImageButton) view.findViewById(R.id.button_delete_watcher);

			controller.bindHolder(view, holder);
			view.setTag(holder);
		}
		holder.data = entity;

		controller.bind(entity, view, groupTag);

		/* Special binding if list of patch watchers */
		if (holder.candiView != null) {

			ViewGroup layout = holder.candiView.getLayout();
			Entity parent = ((BaseActivity) getActivity()).getEntity();
			Boolean itemIsOwner = (parent != null && entity.id.equals(parent.ownerId));

			TextView role = (TextView) layout.findViewById(R.id.role);
			View editGroup = layout.findViewById(R.id.owner_edit_group);
			View deleteButton = layout.findViewById(R.id.button_delete_watcher);

			if (editGroup != null) {

				UI.setVisibility(role, View.GONE);
				UI.setVisibility(editGroup, View.GONE);
				UI.setVisibility(deleteButton, View.GONE);

				if (itemIsOwner) {
					role.setText(User.Role.OWNER);
					UI.setVisibility(layout.findViewById(R.id.role), View.VISIBLE);
				}
				else {
					if (parent != null && parent.schema.equals(Constants.SCHEMA_ENTITY_PATCH)) {
						Patch patch = (Patch) parent;
						if (patch.privacy != null
								&& patch.privacy.equals(Constants.PRIVACY_PRIVATE)
								&& patch.isOwnedByCurrentUser()) {
							((ViewHolderExtended) holder).delete.setTag(entity);
							((ViewHolderExtended) holder).enable.setTag(entity);
							((ViewHolderExtended) holder).enable.setChecked(entity.linkEnabled);
							UI.setVisibility(editGroup, View.VISIBLE);
							UI.setVisibility(deleteButton, View.VISIBLE);
						}
					}
				}
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	public class ViewHolderExtended extends ViewHolder {
		public CompoundButton enable; // NO_UCD (unused code)
		public ImageButton    delete; // NO_UCD (unused code)
	}
}