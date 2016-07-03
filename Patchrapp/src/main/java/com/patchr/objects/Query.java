package com.patchr.objects;

import com.patchr.Constants;
import com.patchr.R;
import com.patchr.components.UserManager;

public class Query {

	public String name;
	public String schema;
	public String rootId;
	public String rootCollection;
	public String entityCollection;
	public String linkType;
	public String filterField;
	public String filterValue;
	public int    listItemResId;

	public int     listEmptyMessage;
	public boolean pagingDisabled;
	public boolean entityCacheDisabled;
	public boolean fetchOnResumeDisabled;
	public boolean sortAscending;
	public String sortField = "modifiedDate";
	public int    pageSize  = Constants.PAGE_SIZE;

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public static Query Factory(String queryName, String entityId) {
		if (queryName.equals(QueryName.MessagesByUser)) {
			return new Query()
				.setName(QueryName.MessagesByUser)
				.setSchema(Constants.SCHEMA_ENTITY_MESSAGE)
				.setRootId(entityId)
				.setRootCollection(Constants.COLLECTION_ENTITY_USERS)
				.setEntityCollection(Constants.COLLECTION_ENTITY_MESSAGE)
				.setFilterField("ownerId")
				.setFilterValue(UserManager.userId)
				.setSortField("createdDate")
				.setSortAscending(false)
				.setPageSize(Constants.PAGE_SIZE)
				.setLinkType(Constants.TYPE_LINK_CREATE)
				.setListEmptyMessage(R.string.empty_posted_messages)
				.setListItemResId(R.layout.view_message)
				;
		}
		else if (queryName.equals(QueryName.MessagesForPatch)) {
			return new Query()
				.setName(QueryName.MessagesForPatch)
				.setSchema(Constants.SCHEMA_ENTITY_MESSAGE)
				.setRootId(entityId)
				.setRootCollection(Constants.COLLECTION_ENTITY_PATCHES)
				.setEntityCollection(Constants.COLLECTION_ENTITY_MESSAGE)
				.setFilterField("patchId")
				.setFilterValue(entityId)
				.setSortField("createdDate")
				.setSortAscending(false)
				.setPageSize(Constants.PAGE_SIZE)
				.setLinkType(Constants.TYPE_LINK_CONTENT)
				.setListEmptyMessage(R.string.empty_general)
				.setListItemResId(R.layout.view_message)
				;
		}
		else if (queryName.equals(QueryName.NotificationsForUser)) {
			return new Query()
				.setName(QueryName.NotificationsForUser)
				.setSchema(Constants.SCHEMA_ENTITY_NOTIFICATION)
				.setRootId(entityId)
				.setRootCollection(Constants.COLLECTION_ENTITY_PATCHES)
				.setEntityCollection(Constants.COLLECTION_ENTITY_NOTIFICATIONS)
				.setFilterField("patchId")
				.setFilterValue(entityId)
				.setSortField("createdDate")
				.setSortAscending(false)
				.setPageSize(Constants.PAGE_SIZE)
				.setLinkType(Constants.TYPE_LINK_CONTENT)
				.setListEmptyMessage(R.string.empty_notifications)
				.setListItemResId(R.layout.listitem_notification)
				;
		}
		else if (queryName.equals(QueryName.MembersForPatch)) {
			return new Query()
				.setName(QueryName.MembersForPatch)
				.setSchema(Constants.SCHEMA_ENTITY_USER)
				.setRootId(entityId)
				.setRootCollection(Constants.COLLECTION_ENTITY_PATCHES)
				.setEntityCollection(Constants.COLLECTION_ENTITY_USERS)
				.setFilterField("patchId")
				.setFilterValue(entityId)
				.setSortField("createdDate")
				.setSortAscending(false)
				.setPageSize(Constants.PAGE_SIZE)
				.setLinkType(Constants.TYPE_LINK_MEMBER)
				;
		}
		else if (queryName.equals(QueryName.PatchesNearby)) {
			return new Query()
				.setName(QueryName.PatchesNearby)
				.setSchema(Constants.SCHEMA_ENTITY_PATCH)
				.setEntityCollection(Constants.COLLECTION_ENTITY_PATCHES)
				.setSortField("distance")
				.setSortAscending(true)
				.setPageSize(Constants.PAGE_SIZE)
				.setPagingDisabled(true)
				.setListEmptyMessage(R.string.empty_nearby)
				.setListItemResId(R.layout.listitem_patch)
				;
		}
		else if (queryName.equals(QueryName.PatchesUserMemberOf)) {
			return new Query()
				.setName(QueryName.PatchesUserMemberOf)
				.setSchema(Constants.SCHEMA_ENTITY_PATCH)
				.setRootId(entityId)
				.setRootCollection(Constants.COLLECTION_ENTITY_USERS)
				.setEntityCollection(Constants.COLLECTION_ENTITY_PATCHES)
				.setSortField("createdDate")
				.setSortAscending(false)
				.setPageSize(Constants.PAGE_SIZE)
				.setLinkType(Constants.TYPE_LINK_MEMBER)
				.setListEmptyMessage(R.string.empty_general)
				.setListItemResId(R.layout.listitem_patch)
				;
		}
		else if (queryName.equals(QueryName.PatchesOwnedByUser)) {
			return new Query()
				.setName(QueryName.PatchesOwnedByUser)
				.setSchema(Constants.SCHEMA_ENTITY_PATCH)
				.setRootId(entityId)
				.setRootCollection(Constants.COLLECTION_ENTITY_USERS)
				.setEntityCollection(Constants.COLLECTION_ENTITY_PATCHES)
				.setSortField("createdDate")
				.setSortAscending(false)
				.setPageSize(Constants.PAGE_SIZE)
				.setLinkType(Constants.TYPE_LINK_CREATE)
				.setListEmptyMessage(R.string.empty_owner_of)
				.setListItemResId(R.layout.listitem_patch)
				;
		}
		else if (queryName.equals(QueryName.PatchesToExplore)) {
			return new Query()
				.setName(QueryName.PatchesOwnedByUser)
				.setSchema(Constants.SCHEMA_ENTITY_PATCH)
				.setRootId(entityId)
				.setRootCollection(Constants.COLLECTION_ENTITY_USERS)
				.setEntityCollection(Constants.COLLECTION_ENTITY_PATCHES)
				.setSortField("createdDate")
				.setSortAscending(false)
				.setPageSize(Constants.PAGE_SIZE)
				.setLinkType(Constants.TYPE_LINK_CREATE)
				.setEntityCacheDisabled(true)
				.setListEmptyMessage(R.string.empty_general)
				.setListItemResId(R.layout.listitem_patch)
				;
		}
		return null;
	}

	/*--------------------------------------------------------------------------------------------
	 * Properties
	 *--------------------------------------------------------------------------------------------*/

	public int getListItemResId() {
		return listItemResId;
	}

	public Query setListItemResId(int listItemResId) {
		this.listItemResId = listItemResId;
		return this;
	}

	public int getListEmptyMessage() {
		return listEmptyMessage;
	}

	public Query setListEmptyMessage(int listEmptyMessage) {
		this.listEmptyMessage = listEmptyMessage;
		return this;
	}

	public String getId() {
		String id = this.name;

		if (this.rootId != null) {
			id = this.name + "." + this.rootId;
		}
		return id;
	}

	public String getName() {
		return name;
	}

	public String getSchema() {
		return schema;
	}

	public Query setSchema(String schema) {
		this.schema = schema;
		return this;
	}

	public String getRootCollection() {
		return rootCollection;
	}

	public Query setRootCollection(String rootCollection) {
		this.rootCollection = rootCollection;
		return this;
	}

	public String getEntityCollection() {
		return entityCollection;
	}

	public Query setEntityCollection(String entityCollection) {
		this.entityCollection = entityCollection;
		return this;
	}

	public String getLinkType() {
		return linkType;
	}

	public Query setLinkType(String linkType) {
		this.linkType = linkType;
		return this;
	}

	public String getFilterField() {
		return filterField;
	}

	public Query setFilterField(String filterField) {
		this.filterField = filterField;
		return this;
	}

	public String getFilterValue() {
		return filterValue;
	}

	public Query setFilterValue(String filterValue) {
		this.filterValue = filterValue;
		return this;
	}

	public String getSortField() {
		return sortField;
	}

	public Query setSortField(String sortField) {
		this.sortField = sortField;
		return this;
	}

	public boolean isSortAscending() {
		return sortAscending;
	}

	public Query setSortAscending(boolean sortAscending) {
		this.sortAscending = sortAscending;
		return this;
	}

	public Query setName(String name) {

		this.name = name;
		return this;
	}

	public int getPageSize() {
		return pageSize;
	}

	public Query setPageSize(int pageSize) {
		this.pageSize = pageSize;
		return this;
	}

	public String getRootId() {
		return rootId;
	}

	public Query setRootId(String rootId) {
		this.rootId = rootId;
		return this;
	}

	public boolean isPagingDisabled() {
		return pagingDisabled;
	}

	public Query setPagingDisabled(boolean pagingDisabled) {
		this.pagingDisabled = pagingDisabled;
		return this;
	}

	public boolean isEntityCacheDisabled() {
		return entityCacheDisabled;
	}

	public Query setEntityCacheDisabled(boolean entityCacheDisabled) {
		this.entityCacheDisabled = entityCacheDisabled;
		return this;
	}

	public boolean isFetchOnResumeDisabled() {
		return fetchOnResumeDisabled;
	}

	public Query setFetchOnResumeDisabled(boolean fetchOnResumeDisabled) {
		this.fetchOnResumeDisabled = fetchOnResumeDisabled;
		return this;
	}
}
