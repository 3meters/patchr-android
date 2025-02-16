package com.patchr.ui.components;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.patchr.Constants;
import com.patchr.R;
import com.patchr.model.RealmEntity;
import com.patchr.ui.views.MessageView;
import com.patchr.ui.views.NotificationView;
import com.patchr.ui.views.PatchView;
import com.patchr.ui.views.SearchItemView;
import com.patchr.ui.views.UserDetailView;
import com.patchr.ui.views.UserView;

import java.util.Map;

public class RealmRecyclerViewHolder extends RecyclerView.ViewHolder {
	public View entityView;

	public RealmRecyclerViewHolder(View itemView) {
		super(itemView);
		entityView = itemView.findViewById(R.id.item_view);
	}

	public void bind(RealmEntity entity) {
		bind(entity, null, null);
	}

	public void bind(RealmEntity entity, RealmEntity contextEntity, Map options) {

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
			if (Constants.SCHEMA_ENTITY_PATCH.equals(contextEntity.schema)) {
				userView.bind(entity, contextEntity);
				return;
			}
			userView.bind(entity);
		}
	}
}
