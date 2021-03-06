/*******************************************************************************
 * Copyright (c) 2016-2017 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ls.core.internal.handlers;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

/**
 * Utility class to access nested Maps via chained keys. Eg.
 *
 * <pre>
 * [foo:[[ bar: value1], [baz: value2]]]
 * </pre>
 *
 * can be queried using keys like 'foo.bar' or 'foo.baz'.
 *
 * @author Fred Bricon
 *
 */
@SuppressWarnings("unchecked")
public final class MapFlattener {

	private MapFlattener() {
		//No need for public instanciation
	}

	public static String getString(Map<String, Object> configuration, String key) {
		return getString(configuration, key, null);
	}

	public static String getString(Map<String, Object> configuration, String key, String def) {
		Object val = getValue(configuration, key);
		if (val instanceof String) {
			return (String) val;
		}
		return def;
	}

	public static List<String> getList(Map<String, Object> configuration, String key) {
		return getList(configuration, key, null);
	}

	public static List<String> getList(Map<String, Object> configuration, String key, List<String> def) {
		Object val = getValue(configuration, key);
		if (val instanceof String) {
			if (!((String) val).trim().startsWith("[")) {
				val = '[' + (String) val + ']';
			}
			try {
				Gson gson = new Gson();
				Type type = new TypeToken<List<String>>() {
				}.getType();
				List<String> list = gson.fromJson((String) val, type);
				return list;
			} catch (JsonSyntaxException e) {
				JavaLanguageServerPlugin.logException(e.getMessage(), e);
				return def;
			}
		}
		if (val instanceof List) {
			List<String> ret;
			try {
				ret = (List<String>) val;
				return ret;
			} catch (Exception e) {
				JavaLanguageServerPlugin.logException(e.getMessage(), e);
			}
		}
		return def;
	}

	public static boolean getBoolean(Map<String, Object> configuration, String key) {
		return getBoolean(configuration, key, false);
	}

	public static boolean getBoolean(Map<String, Object> configuration, String key, boolean def) {
		Object val = getValue(configuration, key);
		if (val instanceof Boolean) {
			return ((Boolean) val).booleanValue();
		}
		if (val instanceof String) {
			return Boolean.parseBoolean((String) val);
		}
		return def;
	}

	public static int getInt(Map<String, Object> configuration, String key) {
		return getInt(configuration, key, 0);
	}

	public static int getInt(Map<String, Object> configuration, String key, int def) {
		Object val = getValue(configuration, key);
		if (val instanceof Integer) {
			return ((Integer) val).intValue();
		} else if (val instanceof String) {
			try {
				return Integer.parseInt((String) val);
			} catch (NumberFormatException nfe) {
				JavaLanguageServerPlugin.logError(key + " value (" + val + ") is not an int, falling back on " + def);
			}
		}
		return def;
	}

	public static Object getValue(Map<String, Object> configuration, String key) {
		Object value = configuration.get(key);
		if (value != null) {
			return value;
		}
		//Probably a chained key, trying nested Maps
		String[] keyParts = key.split("\\.");
		String currKey = null;
		Map<String, Object> currMap = configuration;
		for (int i = 0; i < keyParts.length; i++) {
			currKey = keyParts[i];
			Object val = currMap.get(currKey);
			if (i == keyParts.length - 1) {
				return val;
			}
			if (val instanceof Map) {
				currMap = (Map<String, Object>) val;
			} else {
				return null;
			}
		}
		return null;
	}

}
