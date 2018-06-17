package fr.techgp.nimbus.utils;

public final class StringUtils {

	public StringUtils() {
		//
	}

	public static final boolean isBlank(String s) {
		return s == null || s.trim().length() == 0;
	}

	public static final boolean isNotBlank(String s) {
		return !isBlank(s);
	}

	public static final String withDefault(String value, String defaultValue) {
		return isBlank(value) ? defaultValue : value;
	}

}
