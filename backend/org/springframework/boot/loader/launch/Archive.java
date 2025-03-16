// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.launch;

import java.net.URI;
import java.security.CodeSource;
import java.nio.file.Path;
import java.security.ProtectionDomain;
import java.io.File;
import java.net.URL;
import java.util.Set;
import java.io.IOException;
import java.util.jar.Manifest;
import java.util.function.Predicate;

public interface Archive extends AutoCloseable
{
    public static final Predicate<Entry> ALL_ENTRIES = entry -> true;
    
    Manifest getManifest() throws IOException;
    
    default Set<URL> getClassPathUrls(final Predicate<Entry> includeFilter) throws IOException {
        return this.getClassPathUrls(includeFilter, Archive.ALL_ENTRIES);
    }
    
    Set<URL> getClassPathUrls(final Predicate<Entry> includeFilter, final Predicate<Entry> directorySearchFilter) throws IOException;
    
    default boolean isExploded() {
        return this.getRootDirectory() != null;
    }
    
    default File getRootDirectory() {
        return null;
    }
    
    default void close() throws Exception {
    }
    
    default Archive create(final Class<?> target) throws Exception {
        return create(target.getProtectionDomain());
    }
    
    default Archive create(final ProtectionDomain protectionDomain) throws Exception {
        final CodeSource codeSource = protectionDomain.getCodeSource();
        final URI location = (codeSource != null) ? codeSource.getLocation().toURI() : null;
        if (location == null) {
            throw new IllegalStateException("Unable to determine code source archive");
        }
        return create(Path.of(location).toFile());
    }
    
    default Archive create(final File target) throws Exception {
        if (!target.exists()) {
            throw new IllegalStateException("Unable to determine code source archive from " + target);
        }
        return target.isDirectory() ? new ExplodedArchive(target) : new JarFileArchive(target);
    }
    
    public interface Entry
    {
        String name();
        
        boolean isDirectory();
    }
}
