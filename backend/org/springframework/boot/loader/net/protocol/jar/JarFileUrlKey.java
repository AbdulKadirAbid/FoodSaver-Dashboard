// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.net.protocol.jar;

import java.util.Locale;
import java.util.function.Function;
import java.util.concurrent.ConcurrentHashMap;
import java.net.URL;
import java.util.Map;
import java.lang.ref.SoftReference;

final class JarFileUrlKey
{
    private static volatile SoftReference<Map<URL, String>> cache;
    
    private JarFileUrlKey() {
    }
    
    static String get(final URL url) {
        Map<URL, String> cache = (JarFileUrlKey.cache != null) ? JarFileUrlKey.cache.get() : null;
        if (cache == null) {
            cache = new ConcurrentHashMap<URL, String>();
            JarFileUrlKey.cache = new SoftReference<Map<URL, String>>(cache);
        }
        return cache.computeIfAbsent(url, JarFileUrlKey::create);
    }
    
    private static String create(final URL url) {
        final StringBuilder value = new StringBuilder();
        final String protocol = url.getProtocol();
        final String host = url.getHost();
        final int port = (url.getPort() != -1) ? url.getPort() : url.getDefaultPort();
        final String file = url.getFile();
        value.append(protocol.toLowerCase(Locale.ROOT));
        value.append(":");
        if (host != null && !host.isEmpty()) {
            value.append(host.toLowerCase(Locale.ROOT));
            value.append((port != -1) ? (":" + port) : "");
        }
        value.append((file != null) ? file : "");
        if ("runtime".equals(url.getRef())) {
            value.append("#runtime");
        }
        return value.toString();
    }
    
    static void clearCache() {
        JarFileUrlKey.cache = null;
    }
}
