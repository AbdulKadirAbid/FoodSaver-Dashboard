// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.net.protocol.jar;

import java.util.zip.ZipEntry;
import java.util.jar.JarEntry;
import java.util.jar.Manifest;
import java.io.IOException;
import java.io.File;
import java.util.jar.JarFile;
import java.util.function.Consumer;
import org.springframework.boot.loader.jar.NestedJarFile;

class UrlNestedJarFile extends NestedJarFile
{
    private final UrlJarManifest manifest;
    private final Consumer<JarFile> closeAction;
    
    UrlNestedJarFile(final File file, final String nestedEntryName, final Runtime.Version version, final Consumer<JarFile> closeAction) throws IOException {
        super(file, nestedEntryName, version);
        this.manifest = new UrlJarManifest(() -> super.getManifest());
        this.closeAction = closeAction;
    }
    
    @Override
    public Manifest getManifest() throws IOException {
        return this.manifest.get();
    }
    
    @Override
    public JarEntry getEntry(final String name) {
        return UrlJarEntry.of(super.getEntry(name), this.manifest);
    }
    
    @Override
    public void close() throws IOException {
        if (this.closeAction != null) {
            this.closeAction.accept(this);
        }
        super.close();
    }
}
