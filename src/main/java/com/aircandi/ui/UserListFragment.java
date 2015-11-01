package com.aircandi.ui;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.TextView;

import com.aircandi.Constants;
import com.aircandi.Patchr;
import com.aircandi.R;
import com.aircandi.events.DataErrorEvent;
import com.aircandi.events.DataNoopEvent;
import com.aircandi.events.DataResultEvent;
import com.aircandi.interfaces.IEntityController;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Patch;
import com.aircandi.objects.Route;
import com.aircandi.objects.User;
import com.aircandi.objects.ViewHolder;
import com.aircandi.ui.base.BaseActivity;
import com.aircandi.utilities.UI;
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
			Boolean itemIsOwner = entity.id.equals(parent.ownerId);

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
					if (parent.schema.equals(Constants.SCHEMA_ENTITY_PATCH)) {
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