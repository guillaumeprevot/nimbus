package fr.techgp.nimbus.models;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

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

	public void visit(
			BiConsumer<String, String> strings,
			BiConsumer<String, Boolean> booleans,
			BiConsumer<String, Integer> integers,
			BiConsumer<String, Long> longs,
			BiConsumer<String, Double> doubles) {
		if (this.values == null)
			return;
		for (Map.Entry<String, Object> entry : this.values.entrySet()) {
			if (entry.getValue() instanceof String)
				strings.accept(entry.getKey(), (String) entry.getValue());
			else if (entry.getValue() instanceof Boolean)
				booleans.accept(entry.getKey(), (Boolean) entry.getValue());
			else if (entry.getValue() instanceof Integer)
				integers.accept(entry.getKey(), (Integer) entry.getValue());
			else if (entry.getValue() instanceof Long)
				longs.accept(entry.getKey(), (Long) entry.getValue());
			else if (entry.getValue() instanceof Double)
				doubles.accept(entry.getKey(), (Double) entry.getValue());
			else
				throw new IllegalStateException("Type non supporté");
		}
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
