// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.net.protocol.jar;

import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.io.IOException;
import java.util.Map;
import java.util.jar.Manifest;

class UrlJarManifest
{
    private static final Object NONE;
    private final ManifestSupplier supplier;
    private volatile Object supplied;
    
    UrlJarManifest(final ManifestSupplier supplier) {
        this.supplier = supplier;
    }
    
    Manifest get() throws IOException {
        final Manifest manifest = this.supply();
        if (manifest == null) {
            return null;
        }
        final Manifest copy = new Manifest();
        copy.getMainAttributes().putAll((Map<?, ?>)manifest.getMainAttributes().clone());
        manifest.getEntries().forEach((key, value) -> copy.getEntries().put(key, this.cloneAttributes(value)));
        return copy;
    }
    
    Attributes getEntryAttributes(final JarEntry entry) throws IOException {
        final Manifest manifest = this.supply();
        if (manifest == null) {
            return null;
        }
        final Attributes attributes = manifest.getEntries().get(entry.getName());
        return this.cloneAttributes(attributes);
    }
    
    private Attributes cloneAttributes(final Attributes attributes) {
        return (attributes != null) ? ((Attributes)attributes.clone()) : null;
    }
    
    private Manifest supply() throws IOException {
        Object supplied = this.supplied;
        if (supplied == null) {
            supplied = this.supplier.getManifest();
            this.supplied = ((supplied != null) ? supplied : UrlJarManifest.NONE);
        }
        return (supplied != UrlJarManifest.NONE) ? ((Manifest)supplied) : null;
    }
    
    static {
        NONE = new Object();
    }
    
    @FunctionalInterface
    interface ManifestSupplier
    {
        Manifest getManifest() throws IOException;
    }
}
