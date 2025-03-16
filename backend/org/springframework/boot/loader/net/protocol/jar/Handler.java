// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.net.protocol.jar;

import java.net.MalformedURLException;
import java.io.IOException;
import java.net.URLConnection;
import java.net.URL;
import java.net.URLStreamHandler;

public class Handler extends URLStreamHandler
{
    private static final String PROTOCOL = "jar";
    private static final String SEPARATOR = "!/";
    static final Handler INSTANCE;
    
    @Override
    protected URLConnection openConnection(final URL url) throws IOException {
        return JarUrlConnection.open(url);
    }
    
    @Override
    protected void parseURL(final URL url, final String spec, final int start, final int limit) {
        if (spec.regionMatches(true, start, "jar:", 0, 4)) {
            throw new IllegalStateException("Nested JAR URLs are not supported");
        }
        final int anchorIndex = spec.indexOf(35, limit);
        final String path = this.extractPath(url, spec, start, limit, anchorIndex);
        final String ref = (anchorIndex != -1) ? spec.substring(anchorIndex + 1) : null;
        this.setURL(url, "jar", "", -1, null, null, path, null, ref);
    }
    
    private String extractPath(final URL url, final String spec, final int start, final int limit, final int anchorIndex) {
        if (anchorIndex == start) {
            return this.extractAnchorOnlyPath(url);
        }
        if (spec.length() >= 4 && spec.regionMatches(true, 0, "jar:", 0, 4)) {
            return this.extractAbsolutePath(spec, start, limit);
        }
        return this.extractRelativePath(url, spec, start, limit);
    }
    
    private String extractAnchorOnlyPath(final URL url) {
        return url.getPath();
    }
    
    private String extractAbsolutePath(final String spec, final int start, final int limit) {
        final int indexOfSeparator = indexOfSeparator(spec, start, limit);
        if (indexOfSeparator == -1) {
            throw new IllegalStateException("no !/ in spec");
        }
        final String innerUrl = spec.substring(start, indexOfSeparator);
        this.assertInnerUrlIsNotMalformed(spec, innerUrl);
        return spec.substring(start, limit);
    }
    
    private String extractRelativePath(final URL url, final String spec, final int start, final int limit) {
        final String contextPath = this.extractContextPath(url, spec, start);
        final String path = contextPath + spec.substring(start, limit);
        return Canonicalizer.canonicalizeAfter(path, indexOfSeparator(path) + 1);
    }
    
    private String extractContextPath(final URL url, final String spec, final int start) {
        final String contextPath = url.getPath();
        if (spec.regionMatches(false, start, "/", 0, 1)) {
            final int indexOfContextPathSeparator = indexOfSeparator(contextPath);
            if (indexOfContextPathSeparator == -1) {
                throw new IllegalStateException("malformed context url:%s: no !/".formatted(url));
            }
            return contextPath.substring(0, indexOfContextPathSeparator + 1);
        }
        else {
            final int lastSlash = contextPath.lastIndexOf(47);
            if (lastSlash == -1) {
                throw new IllegalStateException("malformed context url:%s".formatted(url));
            }
            return contextPath.substring(0, lastSlash + 1);
        }
    }
    
    private void assertInnerUrlIsNotMalformed(final String spec, final String innerUrl) {
        if (innerUrl.startsWith("nested:")) {
            org.springframework.boot.loader.net.protocol.nested.Handler.assertUrlIsNotMalformed(innerUrl);
            return;
        }
        try {
            new URL(innerUrl);
        }
        catch (final MalformedURLException ex) {
            throw new IllegalStateException("invalid url: %s (%s)".formatted(spec, ex));
        }
    }
    
    @Override
    protected int hashCode(final URL url) {
        final String protocol = url.getProtocol();
        int hash = (protocol != null) ? protocol.hashCode() : 0;
        final String file = url.getFile();
        final int indexOfSeparator = file.indexOf("!/");
        if (indexOfSeparator == -1) {
            return hash + file.hashCode();
        }
        final String fileWithoutEntry = file.substring(0, indexOfSeparator);
        try {
            hash += new URL(fileWithoutEntry).hashCode();
        }
        catch (final MalformedURLException ex) {
            hash += fileWithoutEntry.hashCode();
        }
        final String entry = file.substring(indexOfSeparator + 2);
        return hash + entry.hashCode();
    }
    
    @Override
    protected boolean sameFile(final URL url1, final URL url2) {
        if (!url1.getProtocol().equals("jar") || !url2.getProtocol().equals("jar")) {
            return false;
        }
        final String file1 = url1.getFile();
        final String file2 = url2.getFile();
        final int indexOfSeparator1 = file1.indexOf("!/");
        final int indexOfSeparator2 = file2.indexOf("!/");
        if (indexOfSeparator1 == -1 || indexOfSeparator2 == -1) {
            return super.sameFile(url1, url2);
        }
        final String entry1 = file1.substring(indexOfSeparator1 + 2);
        final String entry2 = file2.substring(indexOfSeparator2 + 2);
        if (!entry1.equals(entry2)) {
            return false;
        }
        try {
            final URL innerUrl1 = new URL(file1.substring(0, indexOfSeparator1));
            final URL innerUrl2 = new URL(file2.substring(0, indexOfSeparator2));
            if (!super.sameFile(innerUrl1, innerUrl2)) {
                return false;
            }
        }
        catch (final MalformedURLException unused) {
            return super.sameFile(url1, url2);
        }
        return true;
    }
    
    static int indexOfSeparator(final String spec) {
        return indexOfSeparator(spec, 0, spec.length());
    }
    
    static int indexOfSeparator(final String spec, final int start, final int limit) {
        for (int i = limit - 1; i >= start; --i) {
            if (spec.charAt(i) == '!' && i + 1 < limit && spec.charAt(i + 1) == '/') {
                return i;
            }
        }
        return -1;
    }
    
    public static void clearCache() {
        JarFileUrlKey.clearCache();
        JarUrlConnection.clearCache();
    }
    
    static {
        INSTANCE = new Handler();
    }
}
