// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.net.protocol.nested;

import java.util.concurrent.ConcurrentHashMap;
import java.io.File;
import java.net.URI;
import org.springframework.boot.loader.net.util.UrlDecoder;
import java.net.URL;
import java.util.Map;
import java.nio.file.Path;

record NestedLocation(Path path, String nestedEntryName) {
    private static final Map<String, NestedLocation> locationCache;
    private static final Map<String, Path> pathCache;
    
    public NestedLocation(final Path path, final String nestedEntryName) {
        if (path == null) {
            throw new IllegalArgumentException("'path' must not be null");
        }
        this.path = path;
        this.nestedEntryName = ((nestedEntryName != null && !nestedEntryName.isEmpty()) ? nestedEntryName : null);
    }
    
    public static NestedLocation fromUrl(final URL url) {
        if (url == null || !"nested".equalsIgnoreCase(url.getProtocol())) {
            throw new IllegalArgumentException("'url' must not be null and must use 'nested' protocol");
        }
        return parse(UrlDecoder.decode(url.toString().substring(7)));
    }
    
    public static NestedLocation fromUri(final URI uri) {
        if (uri == null || !"nested".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("'uri' must not be null and must use 'nested' scheme");
        }
        return parse(uri.getSchemeSpecificPart());
    }
    
    static NestedLocation parse(final String location) {
        if (location == null || location.isEmpty()) {
            throw new IllegalArgumentException("'location' must not be empty");
        }
        return NestedLocation.locationCache.computeIfAbsent(location, key -> create(location));
    }
    
    private static NestedLocation create(final String location) {
        final int index = location.lastIndexOf("/!");
        final String locationPath = (index != -1) ? location.substring(0, index) : location;
        final String nestedEntryName = (index != -1) ? location.substring(index + 2) : null;
        return new NestedLocation(locationPath.isEmpty() ? null : asPath(locationPath), nestedEntryName);
    }
    
    private static Path asPath(final String locationPath) {
        return NestedLocation.pathCache.computeIfAbsent(locationPath, key -> Path.of(isWindows() ? fixWindowsLocationPath(locationPath) : locationPath, new String[0]));
    }
    
    private static boolean isWindows() {
        return File.separatorChar == '\\';
    }
    
    private static String fixWindowsLocationPath(final String locationPath) {
        if (locationPath.length() > 2 && locationPath.charAt(2) == ':') {
            return locationPath.substring(1);
        }
        if (locationPath.startsWith("///") && locationPath.charAt(4) == ':') {
            return locationPath.substring(3);
        }
        return locationPath;
    }
    
    static void clearCache() {
        NestedLocation.locationCache.clear();
        NestedLocation.pathCache.clear();
    }
    
    static {
        locationCache = new ConcurrentHashMap<String, NestedLocation>();
        pathCache = new ConcurrentHashMap<String, Path>();
    }
}
