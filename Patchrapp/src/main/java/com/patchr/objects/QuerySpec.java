package com.patchr.objects;

import com.patchr.R;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class QuerySpec {

	public String  name;
	public Integer listItemResId;
	public Integer listEmptyMessage;
	public Integer listTitleResId;
	public boolean sortAscending;
	public String sortField = "modifiedDate";

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public static @NotNull QuerySpec Factory(String queryName) {
		if (queryName.equals(QueryName.LikesForMessage)) {
			return new QuerySpec()
				.setName(QueryName.LikesForMessage)
				.setSortField("createdDate")
				.setSortAscending(false)
				.setListEmptyMessage(R.string.empty_members)
				.setListItemResId(R.layout.listitem_user)
				.setListTitleResId(R.string.screen_title_likes);
		}
		else if (queryName.equals(QueryName.MembersForPatch)) {
			return new QuerySpec()
				.setName(QueryName.MembersForPatch)
				.setSortField("createdDate")
				.setSortAscending(false)
				.setListEmptyMessage(R.string.empty_members)
				.setListItemResId(R.layout.listitem_user)
				.setListTitleResId(R.string.screen_title_members);
		}
		else if (queryName.equals(QueryName.MessagesByUser)) {
			return new QuerySpec()
				.setName(QueryName.MessagesByUser)
				.setSortField("createdDate")
				.setSortAscending(false)
				.setListEmptyMessage(R.string.empty_posted_messages)
				.setListItemResId(R.layout.listitem_message)
				.setListTitleResId(R.string.screen_title_profile);
		}
		else if (queryName.equals(QueryName.MessagesForPatch)) {
			return new QuerySpec()
				.setName(QueryName.MessagesForPatch)
				.setSortField("createdDate")
				.setSortAscending(false)
				.setListEmptyMessage(R.string.empty_patch_messages)
				.setListItemResId(R.layout.listitem_message);
		}
		else if (queryName.equals(QueryName.NotificationsForUser)) {
			return new QuerySpec()
				.setName(QueryName.NotificationsForUser)
				.setSortField("createdDate")
				.setSortAscending(false)
				.setListEmptyMessage(R.string.empty_notifications)
				.setListItemResId(R.layout.listitem_notification);
		}
		else if (queryName.equals(QueryName.PatchesNearby)) {
			return new QuerySpec()
				.setName(QueryName.PatchesNearby)
				.setSortField("distance")
				.setSortAscending(true)
				.setListEmptyMessage(R.string.empty_nearby)
				.setListItemResId(R.layout.listitem_patch)
				.setListTitleResId(R.string.screen_title_nearby);
		}
		else if (queryName.equals(QueryName.PatchesOwnedByUser)) {
			return new QuerySpec()
				.setName(QueryName.PatchesOwnedByUser)
				.setSortField("activityDate")
				.setSortAscending(false)
				.setListEmptyMessage(R.string.empty_owner_of)
				.setListItemResId(R.layout.listitem_patch)
				.setListTitleResId(R.string.screen_title_owner_of);
		}
		else if (queryName.equals(QueryName.PatchesToExplore)) {
			return new QuerySpec()
				.setName(QueryName.PatchesToExplore)
				.setSortField("countMessages")
				.setSortAscending(false)
				.setListEmptyMessage(R.string.empty_explore)
				.setListItemResId(R.layout.listitem_patch)
				.setListTitleResId(R.string.screen_title_explore);
		}
		else if (queryName.equals(QueryName.PatchesUserMemberOf)) {
			return new QuerySpec()
				.setName(QueryName.PatchesUserMemberOf)
				.setSortField("activityDate")
				.setSortAscending(false)
				.setListEmptyMessage(R.string.empty_member_of)
				.setListItemResId(R.layout.listitem_patch)
				.setListTitleResId(R.string.screen_title_member_of);
		}
		return new QuerySpec();
	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	public int getListItemResId() {
		return listItemResId;
	}

	public QuerySpec setListItemResId(int listItemResId) {
		this.listItemResId = listItemResId;
		return this;
	}

	public int getListEmptyMessage() {
		return listEmptyMessage;
	}

	public QuerySpec setListEmptyMessage(int listEmptyMessage) {
		this.listEmptyMessage = listEmptyMessage;
		return this;
	}

	public String getId(String rootId) {
		String id = this.name.toLowerCase(Locale.US);

		if (rootId != null) {
			id = this.name.toLowerCase(Locale.US) + "." + rootId;
		}
		return id;
	}

	public String getName() {
		return name;
	}

	public String getSortField() {
		return sortField;
	}

	public QuerySpec setSortField(String sortField) {
		this.sortField = sortField;
		return this;
	}

	public boolean isSortAscending() {
		return sortAscending;
	}

	public QuerySpec setSortAscending(boolean sortAscending) {
		this.sortAscending = sortAscending;
		return this;
	}

	public QuerySpec setName(String name) {

		this.name = name;
		return this;
	}

	public int getListTitleResId() {
		return listTitleResId;
	}

	public QuerySpec setListTitleResId(int listTitleResId) {
		this.listTitleResId = listTitleResId;
		return this;
	}
}
