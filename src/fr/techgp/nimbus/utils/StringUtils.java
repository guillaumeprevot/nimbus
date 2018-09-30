package fr.techgp.nimbus.utils;

import java.util.Random;

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

	public static final String repeat(String value, int count) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < count; i++) {
			sb.append(value);
		}
		return sb.toString();
	}

	public static final String randomString(int count, boolean chars, boolean digits) {
		Random r = new Random();
		char[] vals = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
		int min = chars ? 0 : 26;
		int max = digits ? 36 : 26;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < count; i++) {
			sb.append(vals[min + r.nextInt(max - min)]);
		}
		return sb.toString();
	}

}
