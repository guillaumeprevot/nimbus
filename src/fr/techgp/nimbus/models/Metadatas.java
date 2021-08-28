package fr.techgp.nimbus.models;

import java.util.HashMap;
import java.util.Map;

public final class Metadatas {

	private Map<String, Object> values = null;

	public String getString(String name) {
		return (String) get(name);
	}

	public void put(String name, String value) {
		this.set(name, value);
	}

	public Boolean getBoolean(String name) {
		return (Boolean) get(name);
	}

	public void put(String name, Boolean value) {
		this.set(name, value);
	}

	public Integer getInteger(String name) {
		return (Integer) get(name);
	}

	public void put(String name, Integer value) {
		this.set(name, value);
	}

	public Long getLong(String name) {
		return (Long) get(name);
	}

	public void put(String name, Long value) {
		this.set(name, value);
	}

	public Double getDouble(String name) {
		return (Double) get(name);
	}

	public void put(String name, Double value) {
		this.set(name, value);
	}

	public boolean has(String name) {
		return this.values != null && this.values.get(name) != null;
	}

	public void remove(String name) {
		if (this.values != null)
			this.values.remove(name);
	}

	public void clear() {
		this.values = null;
	}

	public void copy(Metadatas source) {
		if (source.values == null)
			this.clear();
		else
			this.values = new HashMap<>(source.values);
	}

	public void copyTo(Map<String, Object> target) {
		target.clear();
		if (this.values != null)
			target.putAll(this.values);
	}

	private Object get(String name) {
		return this.values == null ? null : this.values.get(name);
	}

	private void set(String name, Object value) {
		if (value == null) {
			if (this.values != null)
				this.values.remove(name);
		} else {
			if (this.values == null)
				this.values = new HashMap<>();
			this.values.put(name, value);
		}
	}

}
