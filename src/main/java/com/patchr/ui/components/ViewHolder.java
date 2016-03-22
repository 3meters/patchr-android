package com.patchr.ui.components;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.patchr.Constants;
import com.patchr.R;
import com.patchr.events.AbsEntitiesQueryEvent;
import com.patchr.objects.Entity;
import com.patchr.ui.views.MessageView;
import com.patchr.ui.views.NotificationView;
import com.patchr.ui.views.PatchView;
import com.patchr.ui.views.UserView;

public class ViewHolder extends RecyclerView.ViewHolder {
	public View entityView;

	public ViewHolder(View itemView) {
		super(itemView);
		entityView = itemView.findViewById(R.id.item_view);
	}

	public void bind(Entity entity) {
		bind(entity, null, null);
	}

	public void bind(Entity entity, Entity scopingEntity, AbsEntitiesQueryEvent query) {

		if (entity.schema.equals(Constants.SCHEMA_ENTITY_PATCH)) {
			PatchView patchView = (PatchView) entityView;
			patchView.setTag(entity);
			patchView.bind(entity);
		}
		else if (entity.schema.equals(Constants.SCHEMA_ENTITY_MESSAGE)) {
			MessageView messageView = (MessageView) entityView;
			messageView.setTag(entity);
			messageView.bind(entity);
		}
		else if (entity.schema.equals(Constants.SCHEMA_ENTITY_NOTIFICATION)) {
			NotificationView notificationView = (NotificationView) entityView;
			notificationView.setTag(entity);
			notificationView.bind(entity);
		}
		else if (entity.schema.equals(Constants.SCHEMA_ENTITY_USER)) {
			UserView userView = (UserView) entityView;
			userView.setTag(entity);
			if (query != null && query.cursor.linkTypes.get(0).equals(Constants.TYPE_LINK_MEMBER)) {
				if (scopingEntity != null) {
					Boolean itemIsOwner = (entity.id.equals(scopingEntity.ownerId));
					userView.bind(entity, !itemIsOwner, itemIsOwner);
					userView.patch = scopingEntity;
					return;
				}
			}
			userView.bind(entity);
		}
	}
}
