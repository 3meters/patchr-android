package com.aircandi.ui;

import android.app.Activity;
import android.view.View;
import android.widget.CompoundButton;

import com.aircandi.Constants;
import com.aircandi.Patch;
import com.aircandi.R;
import com.aircandi.components.Extras;
import com.aircandi.events.ProcessingCompleteEvent;
import com.aircandi.interfaces.IEntityController;
import com.aircandi.objects.Entity;
import com.aircandi.objects.Route;
import com.aircandi.objects.ViewHolder;
import com.aircandi.ui.widgets.ComboButton;
import com.squareup.otto.Subscribe;

public class WatcherListFragment extends EntityListFragment {

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onClick(View v) {
		final Entity entity = (Entity) ((ViewHolder) v.getTag()).data;

		Extras extras = new Extras()
				.setEntitySchema(Constants.SCHEMA_ENTITY_USER)
				.setEntityId(entity.id);

		Patch.dispatch.route(getActivity(), Route.BROWSE, entity, null, extras.getExtras());
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	@Subscribe
	public void onProcessingComplete(ProcessingCompleteEvent event) {
		super.onProcessingComplete(event);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	@Override
	protected void postBind() {
		super.postBind();
	}

	@Override
	protected void bindListItem(Entity entity, View view) {

		IEntityController controller = Patch.getInstance().getControllerForEntity(entity);

		ViewHolder holder = (ViewHolder) view.getTag();
		if (holder == null) {
			holder = new ViewHolderExtended();

			((ViewHolderExtended) holder).enable = (CompoundButton) view.findViewById(R.id.switch_enable);
			((ViewHolderExtended) holder).delete = (ComboButton) view.findViewById(R.id.button_delete);

			controller.bindHolder(view, holder);
			view.setTag(holder);
		}
		holder.data = entity;

		controller.bind(entity, view);

		//		if (holder.candiView != null) {
		//			ViewGroup layout = holder.candiView.getLayout();
		//			Entity parent = ((BaseActivity) getActivity()).getEntity();
		//			Boolean isOwner = entity.id.equals(parent.ownerId);

		//			layout.findViewById(R.id.holder_owner_edit).setVisibility((parent.isOwnedByCurrentUser() && !isOwner) ? View.VISIBLE : View.GONE);
		//			layout.findViewById(R.id.button_delete).setVisibility((parent.isOwnedByCurrentUser() && !isOwner) ? View.VISIBLE : View.GONE);

		//			((ViewHolderExtended) holder).delete.setTag(entity);
		//			((ViewHolderExtended) holder).enable.setTag(entity);
		//			((ViewHolderExtended) holder).enable.setChecked(entity.enabled);
		//		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/
	/*--------------------------------------------------------------------------------------------
	 * Menus
	 *--------------------------------------------------------------------------------------------*/
	/*--------------------------------------------------------------------------------------------
	 * Lifecycle
	 *--------------------------------------------------------------------------------------------*/
	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	public class ViewHolderExtended extends ViewHolder {
		public CompoundButton enable; // NO_UCD (unused code)
		public ComboButton    delete; // NO_UCD (unused code)
	}
}