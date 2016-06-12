package com.patchr.objects;

import android.support.annotation.NonNull;

import com.patchr.Constants;
import com.patchr.service.Expose;
import com.patchr.service.SerializedName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Jayma
 */

@SuppressWarnings("ucd")
public class Link extends ServiceBase {

	private static final long   serialVersionUID = 8839291281700760437L;
	public static final  String collectionId     = "links";

	@Expose
	@SerializedName(name = "_from")
	public String    fromId;
	@Expose
	@SerializedName(name = "_to")
	public String    toId;
	@Expose
	public Boolean   enabled;
	@Expose
	public Boolean   mute;

	@Expose(serialize = false, deserialize = true)
	public String fromSchema;
	@Expose(serialize = false, deserialize = true)
	public String toSchema;
	@Expose(serialize = false, deserialize = true)
	public String targetSchema;                                // Used when stored in linksIn or linksOut

	@Expose(serialize = false, deserialize = true)
	public Shortcut    shortcut;
	@Expose(serialize = false, deserialize = true)
	public List<Count> stats;

	public Link() {}

	public Link(@NonNull String toId, @NonNull String type, @NonNull String targetSchema) {
		this.toId = toId;
		this.type = type;
		this.targetSchema = targetSchema;
	}

	public Link(@NonNull String fromId, @NonNull String toId, @NonNull String type, @NonNull String targetSchema) {
		this.toId = toId;
		this.type = type;
		this.targetSchema = targetSchema;
		this.fromId = fromId;
	}

	/*--------------------------------------------------------------------------------------------
	 * Set and get
	 *--------------------------------------------------------------------------------------------*/

	@NonNull
	public Integer getProximityScore() {
		Integer score = 0;
		if (this.stats != null) {
			for (Count count : stats) {
				if (count.type.equals(Constants.TYPE_COUNT_LINK_PROXIMITY)) {
					score += count.count.intValue();
				}
				else if (count.type.equals(Constants.TYPE_COUNT_LINK_PROXIMITY_MINUS)) {
					score -= count.count.intValue();
				}
			}
		}
		return score;
	}

	public Count getStat(String type, String schema) {
		if (this.stats != null) {
			for (Count count : stats) {
				if (count.type.equals(type)
						&& (schema == null || count.schema.equals(schema))
						&& (count.enabled == null || count.enabled)) return count;
			}
		}
		return null;
	}

	@NonNull
	public Count incrementStat(String type, String schema) {
		Count count;
		if (this.stats == null) {
			this.stats = new ArrayList<Count>();
		}
		if (getStat(type, schema) == null) {
			count = new Count(type, schema, null, 1);
			this.stats.add(count);
		}
		else {
			count = getStat(type, schema);
			count.count = count.count.intValue() + 1;
		}
		return count;
	}

	public Count decrementStat(String type, String schema) {
		Count count = null;
		if (this.stats != null) {
			count = getStat(type, schema);
			if (count != null) {
				count.count = count.count.intValue() - 1;
				if (count.count.intValue() <= 0) {
					this.stats.remove(count);
				}
			}
		}
		return count;
	}

	@Override
	public String getCollection() {
		return collectionId;
	}

	/*--------------------------------------------------------------------------------------------
	 * Copy and serialization
	 *--------------------------------------------------------------------------------------------*/

	public static Link setPropertiesFromMap(Link link, Map map, Boolean nameMapping) {

		link = (Link) ServiceBase.setPropertiesFromMap(link, map, nameMapping);

		link.fromId = (String) (nameMapping ? map.get("_from") : map.get("fromId"));
		link.toId = (String) (nameMapping ? map.get("_to") : map.get("toId"));
		link.fromSchema = (String) map.get("fromSchema");
		link.toSchema = (String) map.get("toSchema");
		link.targetSchema = (String) map.get("targetSchema");
		link.enabled = (Boolean) map.get("enabled");
		link.mute = (Boolean) map.get("mute");

		if (map.get("stats") != null) {
			final List<LinkedHashMap<String, Object>> statMaps = (List<LinkedHashMap<String, Object>>) map.get("stats");

			link.stats = new ArrayList<Count>();
			for (Map<String, Object> statMap : statMaps) {
				link.stats.add(Count.setPropertiesFromMap(new Count(), statMap, nameMapping));
			}
		}

		if (map.get("shortcut") != null) {
			link.shortcut = Shortcut.setPropertiesFromMap(new Shortcut(), (HashMap<String, Object>) map.get("shortcut"), nameMapping);
		}

		return link;
	}

	@Override
	public Link clone() {
		final Link link = (Link) super.clone();
		if (link != null && stats != null) {
			link.stats = (List<Count>) ((ArrayList) stats).clone();
		}

		return link;
	}

	/*--------------------------------------------------------------------------------------------
	 * Classes
	 *--------------------------------------------------------------------------------------------*/

	public enum Direction {
		in,
		out,
		both
	}
}