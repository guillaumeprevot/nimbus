package fr.techgp.nimbus.models;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.bson.Document;

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

	public void copy(Document document) {
		this.clear();
		for (Map.Entry<String, Object> entry : document.entrySet()) {
			if (entry.getValue() instanceof String)
				this.put(entry.getKey(), (String) entry.getValue());
			else if (entry.getValue() instanceof Boolean)
				this.put(entry.getKey(), (Boolean) entry.getValue());
			else if (entry.getValue() instanceof Integer)
				this.put(entry.getKey(), (Integer) entry.getValue());
			else if (entry.getValue() instanceof Long)
				this.put(entry.getKey(), (Long) entry.getValue());
			else if (entry.getValue() instanceof Double)
				this.put(entry.getKey(), (Double) entry.getValue());
			else if (entry.getValue() instanceof Date)
				this.put(entry.getKey(), ((Date) entry.getValue()).getTime());
			else if (entry.getValue() != null)
				throw new UnsupportedOperationException("Unsupported BSON value " + entry.getValue().getClass().getName());
		}
	}

	public Document asDocument() {
		Document result = new Document();
		if (this.values != null)
			// this.values maps to String, Boolean, Integer, Long and Double, all supported by Document
			result.putAll(this.values);
		return result;
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
