// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.net.protocol.jar;

import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.io.IOException;
import org.springframework.boot.loader.ref.Cleaner;
import java.io.File;
import java.util.function.Consumer;
import java.util.jar.JarFile;

class UrlJarFile extends JarFile
{
    private final UrlJarManifest manifest;
    private final Consumer<JarFile> closeAction;
    
    UrlJarFile(final File file, final Runtime.Version version, final Consumer<JarFile> closeAction) throws IOException {
        super(file, true, 1, version);
        Cleaner.instance.register(this, null);
        this.manifest = new UrlJarManifest(() -> super.getManifest());
        this.closeAction = closeAction;
    }
    
    @Override
    public ZipEntry getEntry(final String name) {
        return UrlJarEntry.of(super.getEntry(name), this.manifest);
    }
    
    @Override
    public Manifest getManifest() throws IOException {
        return this.manifest.get();
    }
    
    @Override
    public void close() throws IOException {
        if (this.closeAction != null) {
            this.closeAction.accept(this);
        }
        super.close();
    }
}
