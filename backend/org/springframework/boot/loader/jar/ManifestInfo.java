// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.jar;

import java.util.jar.Manifest;
import java.util.jar.Attributes;

class ManifestInfo
{
    private static final Attributes.Name MULTI_RELEASE;
    static final ManifestInfo NONE;
    private final Manifest manifest;
    private volatile Boolean multiRelease;
    
    ManifestInfo(final Manifest manifest) {
        this(manifest, null);
    }
    
    private ManifestInfo(final Manifest manifest, final Boolean multiRelease) {
        this.manifest = manifest;
        this.multiRelease = multiRelease;
    }
    
    Manifest getManifest() {
        return this.manifest;
    }
    
    boolean isMultiRelease() {
        if (this.manifest == null) {
            return false;
        }
        Boolean multiRelease = this.multiRelease;
        if (multiRelease != null) {
            return multiRelease;
        }
        final Attributes attributes = this.manifest.getMainAttributes();
        multiRelease = attributes.containsKey(ManifestInfo.MULTI_RELEASE);
        this.multiRelease = multiRelease;
        return multiRelease;
    }
    
    static {
        MULTI_RELEASE = new Attributes.Name("Multi-Release");
        NONE = new ManifestInfo(null, false);
    }
}
