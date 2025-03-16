// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.net.protocol.jar;

import java.util.zip.ZipEntry;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;

final class UrlJarEntry extends JarEntry
{
    private final UrlJarManifest manifest;
    
    private UrlJarEntry(final JarEntry entry, final UrlJarManifest manifest) {
        super(entry);
        this.manifest = manifest;
    }
    
    @Override
    public Attributes getAttributes() throws IOException {
        return this.manifest.getEntryAttributes(this);
    }
    
    static UrlJarEntry of(final ZipEntry entry, final UrlJarManifest manifest) {
        return (entry != null) ? new UrlJarEntry((JarEntry)entry, manifest) : null;
    }
}
