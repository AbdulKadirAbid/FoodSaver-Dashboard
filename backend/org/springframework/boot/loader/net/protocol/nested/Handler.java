// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.net.protocol.nested;

import java.io.IOException;
import java.net.URLConnection;
import java.net.URL;
import java.net.URLStreamHandler;

public class Handler extends URLStreamHandler
{
    private static final String PREFIX = "nested:";
    
    @Override
    protected URLConnection openConnection(final URL url) throws IOException {
        return new NestedUrlConnection(url);
    }
    
    public static void assertUrlIsNotMalformed(final String url) {
        if (url == null || !url.startsWith("nested:")) {
            throw new IllegalArgumentException("'url' must not be null and must use 'nested' protocol");
        }
        NestedLocation.parse(url.substring("nested:".length()));
    }
    
    public static void clearCache() {
        NestedLocation.clearCache();
    }
}
