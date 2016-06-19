package com.patchr.objects;

import com.patchr.service.Expose;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class Category extends ServiceObject implements Cloneable, Serializable {

	private static final long serialVersionUID = 455904759787968585L;

	@Expose
	public String name;
	@Expose
	public String id;
	@Expose
	public Photo  photo;

	@Expose(serialize = false, deserialize = true)
	public List<Category> categories;

	@Override
	public Category clone() {
		try {
			final Category category = (Category) super.clone();
			if (categories != null) {
				category.categories = (List<Category>) ((ArrayList) categories).clone();
			}
			return category;
		}
		catch (final CloneNotSupportedException ex) {
			throw new AssertionError();
		}
	}

	public static Category setPropertiesFromMap(Category category, Map map, Boolean nameMapping) {

		category.name = (String) map.get("name");
		category.id = (String) map.get("id");

		if (map.get("photo") != null) {
			category.photo = Photo.setPropertiesFromMap(new Photo(), (HashMap<String, Object>) map.get("photo"), nameMapping);
		}

		if (map.get("categories") != null) {
			final List<LinkedHashMap<String, Object>> categoryMaps = (List<LinkedHashMap<String, Object>>) map.get("categories");

			category.categories = new ArrayList<Category>();
			for (Map<String, Object> categoryMap : categoryMaps) {
				category.categories.add(Category.setPropertiesFromMap(new Category(), categoryMap, nameMapping));
			}
		}

		return category;
	}

	public Category setName(String name) {
		this.name = name;
		return this;
	}

	public Category setId(String id) {
		this.id = id;
		return this;
	}

	public Category setPhoto(Photo photo) {
		this.photo = photo;
		return this;
	}


}