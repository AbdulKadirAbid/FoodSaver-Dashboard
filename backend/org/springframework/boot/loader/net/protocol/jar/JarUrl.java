// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.net.protocol.jar;

import java.net.MalformedURLException;
import java.net.URLStreamHandler;
import java.util.jar.JarEntry;
import java.net.URL;
import java.io.File;

public final class JarUrl
{
    private JarUrl() {
    }
    
    public static URL create(final File file) {
        return create(file, (String)null);
    }
    
    public static URL create(final File file, final JarEntry nestedEntry) {
        return create(file, (nestedEntry != null) ? nestedEntry.getName() : null);
    }
    
    public static URL create(final File file, final String nestedEntryName) {
        return create(file, nestedEntryName, null);
    }
    
    public static URL create(final File file, final String nestedEntryName, String path) {
        try {
            path = ((path != null) ? path : "");
            return new URL((URL)null, "jar:" + getJarReference(file, nestedEntryName) + "!/" + path, (URLStreamHandler)Handler.INSTANCE);
        }
        catch (final MalformedURLException ex) {
            throw new IllegalStateException("Unable to create JarFileArchive URL", ex);
        }
    }
    
    private static String getJarReference(final File file, final String nestedEntryName) {
        final String jarFilePath = file.toURI().getRawPath().replace("!", "%21");
        return (nestedEntryName != null) ? ("nested:" + jarFilePath + "/!" + nestedEntryName) : ("file:" + jarFilePath);
    }
}
