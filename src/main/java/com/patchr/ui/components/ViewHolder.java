package com.patchr.ui.components;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.patchr.Constants;
import com.patchr.R;
import com.patchr.objects.Entity;
import com.patchr.ui.views.MessageView;
import com.patchr.ui.views.NotificationView;
import com.patchr.ui.views.PatchView;
import com.patchr.ui.views.SearchItemView;
import com.patchr.ui.views.UserDetailView;
import com.patchr.ui.views.UserView;

import java.util.Map;

public class ViewHolder extends RecyclerView.ViewHolder {
	public View entityView;

	public ViewHolder(View itemView) {
		super(itemView);
		entityView = itemView.findViewById(R.id.item_view);
	}

	public void bind(Entity entity) {
		bind(entity, null, null);
	}

	public void bind(Entity entity, Entity scopingEntity, Map options) {

		if (entityView instanceof SearchItemView) {
			SearchItemView view = (SearchItemView) entityView;
			view.setTag(entity);
			view.bind(entity);
		}
		else if (entityView instanceof UserDetailView) {
			UserDetailView view = (UserDetailView) entityView;
			view.setTag(entity);
		}
		else if (entity.schema.equals(Constants.SCHEMA_ENTITY_PATCH)) {
			PatchView patchView = (PatchView) entityView;
			patchView.setTag(entity);
			patchView.bind(entity);
		}
		else if (entity.schema.equals(Constants.SCHEMA_ENTITY_MESSAGE)) {
			MessageView messageView = (MessageView) entityView;
			messageView.setTag(entity);
			messageView.bind(entity, options);
		}
		else if (entity.schema.equals(Constants.SCHEMA_ENTITY_NOTIFICATION)) {
			NotificationView notificationView = (NotificationView) entityView;
			notificationView.setTag(entity);
			notificationView.bind(entity);
		}
		else if (entity.schema.equals(Constants.SCHEMA_ENTITY_USER)) {
			UserView userView = (UserView) entityView;
			userView.setTag(entity);
			if (Constants.SCHEMA_ENTITY_PATCH.equals(scopingEntity.schema)) {
				userView.bind(entity, scopingEntity);
				return;
			}
			userView.bind(entity);
		}
	}
}
