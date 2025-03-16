// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.launch;

import java.util.Locale;
import java.util.Set;
import java.util.HashSet;
import java.util.Properties;

final class SystemPropertyUtils
{
    private static final String PLACEHOLDER_PREFIX = "${";
    private static final String PLACEHOLDER_SUFFIX = "}";
    private static final String VALUE_SEPARATOR = ":";
    private static final String SIMPLE_PREFIX;
    
    private SystemPropertyUtils() {
    }
    
    static String resolvePlaceholders(final Properties properties, final String text) {
        return (text != null) ? parseStringValue(properties, text, text, new HashSet<String>()) : null;
    }
    
    private static String parseStringValue(final Properties properties, final String value, final String current, final Set<String> visitedPlaceholders) {
        final StringBuilder result = new StringBuilder(current);
        int startIndex = current.indexOf("${");
        while (startIndex != -1) {
            final int endIndex = findPlaceholderEndIndex(result, startIndex);
            if (endIndex == -1) {
                startIndex = -1;
            }
            else {
                final String originalPlaceholder;
                String placeholder = originalPlaceholder = result.substring();
                if (!visitedPlaceholders.add(originalPlaceholder)) {
                    throw new IllegalArgumentException("Circular placeholder reference '" + originalPlaceholder + "' in property definitions");
                }
                placeholder = parseStringValue(properties, value, placeholder, visitedPlaceholders);
                String propertyValue = resolvePlaceholder(properties, value, placeholder);
                if (propertyValue == null) {
                    final int separatorIndex = placeholder.indexOf(":");
                    if (separatorIndex != -1) {
                        final String actualPlaceholder = placeholder.substring(0, separatorIndex);
                        final String defaultValue = placeholder.substring(separatorIndex + ":".length());
                        propertyValue = resolvePlaceholder(properties, value, actualPlaceholder);
                        propertyValue = ((propertyValue != null) ? propertyValue : defaultValue);
                    }
                }
                if (propertyValue != null) {
                    propertyValue = parseStringValue(properties, value, propertyValue, visitedPlaceholders);
                    result.replace(startIndex, endIndex + "}".length(), propertyValue);
                    startIndex = result.indexOf("${", startIndex + propertyValue.length());
                }
                else {
                    startIndex = result.indexOf("${", endIndex + "}".length());
                }
                visitedPlaceholders.remove(originalPlaceholder);
            }
        }
        return result.toString();
    }
    
    private static String resolvePlaceholder(final Properties properties, final String text, final String placeholderName) {
        final String propertyValue = getProperty(placeholderName, null, text);
        if (propertyValue != null) {
            return propertyValue;
        }
        return (properties != null) ? properties.getProperty(placeholderName) : null;
    }
    
    static String getProperty(final String key) {
        return getProperty(key, null, "");
    }
    
    private static String getProperty(final String key, final String defaultValue, final String text) {
        try {
            String value = System.getProperty(key);
            value = ((value != null) ? value : System.getenv(key));
            value = ((value != null) ? value : System.getenv(key.replace('.', '_')));
            value = ((value != null) ? value : System.getenv(key.toUpperCase(Locale.ENGLISH).replace('.', '_')));
            return (value != null) ? value : defaultValue;
        }
        catch (final Throwable ex) {
            System.err.println("Could not resolve key '" + key + "' in '" + text + "' as system property or in environment: " + ex);
            return defaultValue;
        }
    }
    
    private static int findPlaceholderEndIndex(final CharSequence buf, final int startIndex) {
        int index = startIndex + "${".length();
        int withinNestedPlaceholder = 0;
        while (index < buf.length()) {
            if (substringMatch(buf, index, "}")) {
                if (withinNestedPlaceholder <= 0) {
                    return index;
                }
                --withinNestedPlaceholder;
                index += "}".length();
            }
            else if (substringMatch(buf, index, SystemPropertyUtils.SIMPLE_PREFIX)) {
                ++withinNestedPlaceholder;
                index += SystemPropertyUtils.SIMPLE_PREFIX.length();
            }
            else {
                ++index;
            }
        }
        return -1;
    }
    
    private static boolean substringMatch(final CharSequence str, final int index, final CharSequence substring) {
        for (int j = 0; j < substring.length(); ++j) {
            final int i = index + j;
            if (i >= str.length() || str.charAt(i) != substring.charAt(j)) {
                return false;
            }
        }
        return true;
    }
    
    static {
        SIMPLE_PREFIX = "${".substring(1);
    }
}
