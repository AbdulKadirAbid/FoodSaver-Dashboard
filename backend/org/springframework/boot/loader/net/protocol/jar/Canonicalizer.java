// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.net.protocol.jar;

final class Canonicalizer
{
    private Canonicalizer() {
    }
    
    static String canonicalizeAfter(final String path, final int pos) {
        final int pathLength = path.length();
        final boolean noDotSlash = path.indexOf("./", pos) == -1;
        if (pos >= pathLength || (noDotSlash && path.charAt(pathLength - 1) != '.')) {
            return path;
        }
        final String before = path.substring(0, pos);
        final String after = path.substring(pos);
        return before + canonicalize(after);
    }
    
    static String canonicalize(String path) {
        path = removeEmbeddedSlashDotDotSlash(path);
        path = removeEmbeddedSlashDotSlash(path);
        path = removeTrailingSlashDotDot(path);
        path = removeTrailingSlashDot(path);
        return path;
    }
    
    private static String removeEmbeddedSlashDotDotSlash(String path) {
        int index;
        while ((index = path.indexOf("/../")) >= 0) {
            final int priorSlash = path.lastIndexOf(47, index - 1);
            final String after = path.substring(index + 3);
            path = ((priorSlash >= 0) ? (path.substring(0, priorSlash) + after) : after);
        }
        return path;
    }
    
    private static String removeEmbeddedSlashDotSlash(String path) {
        int index;
        while ((index = path.indexOf("/./")) >= 0) {
            final String before = path.substring(0, index);
            final String after = path.substring(index + 2);
            path = before + after;
        }
        return path;
    }
    
    private static String removeTrailingSlashDot(final String path) {
        return path.endsWith("/.") ? path.substring(0, path.length() - 1) : path;
    }
    
    private static String removeTrailingSlashDotDot(String path) {
        while (path.endsWith("/..")) {
            final int index = path.indexOf("/..");
            final int priorSlash = path.lastIndexOf(47, index - 1);
            path = ((priorSlash >= 0) ? path.substring(0, priorSlash + 1) : path.substring(0, index));
        }
        return path;
    }
}
