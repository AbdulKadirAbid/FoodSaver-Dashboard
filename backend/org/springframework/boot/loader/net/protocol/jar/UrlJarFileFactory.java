// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.net.protocol.jar;

import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;
import java.io.InputStream;
import org.springframework.boot.loader.net.protocol.nested.NestedLocation;
import java.io.File;
import org.springframework.boot.loader.net.util.UrlDecoder;
import java.io.IOException;
import java.util.jar.JarFile;
import java.util.function.Consumer;
import java.net.URL;

class UrlJarFileFactory
{
    JarFile createJarFile(final URL jarFileUrl, final Consumer<JarFile> closeAction) throws IOException {
        final Runtime.Version version = this.getVersion(jarFileUrl);
        if (this.isLocalFileUrl(jarFileUrl)) {
            return this.createJarFileForLocalFile(jarFileUrl, version, closeAction);
        }
        if (isNestedUrl(jarFileUrl)) {
            return this.createJarFileForNested(jarFileUrl, version, closeAction);
        }
        return this.createJarFileForStream(jarFileUrl, version, closeAction);
    }
    
    private Runtime.Version getVersion(final URL url) {
        return "base".equals(url.getRef()) ? JarFile.baseVersion() : JarFile.runtimeVersion();
    }
    
    private boolean isLocalFileUrl(final URL url) {
        return url.getProtocol().equalsIgnoreCase("file") && this.isLocal(url.getHost());
    }
    
    private boolean isLocal(final String host) {
        return host == null || host.isEmpty() || host.equals("~") || host.equalsIgnoreCase("localhost");
    }
    
    private JarFile createJarFileForLocalFile(final URL url, final Runtime.Version version, final Consumer<JarFile> closeAction) throws IOException {
        final String path = UrlDecoder.decode(url.getPath());
        return new UrlJarFile(new File(path), version, closeAction);
    }
    
    private JarFile createJarFileForNested(final URL url, final Runtime.Version version, final Consumer<JarFile> closeAction) throws IOException {
        final NestedLocation location = NestedLocation.fromUrl(url);
        return new UrlNestedJarFile(location.path().toFile(), location.nestedEntryName(), version, closeAction);
    }
    
    private JarFile createJarFileForStream(final URL url, final Runtime.Version version, final Consumer<JarFile> closeAction) throws IOException {
        try (final InputStream in = url.openStream()) {
            return this.createJarFileForStream(in, version, closeAction);
        }
    }
    
    private JarFile createJarFileForStream(final InputStream in, final Runtime.Version version, final Consumer<JarFile> closeAction) throws IOException {
        final Path local = Files.createTempFile("jar_cache", null, (FileAttribute<?>[])new FileAttribute[0]);
        try {
            Files.copy(in, local, StandardCopyOption.REPLACE_EXISTING);
            final JarFile jarFile = new UrlJarFile(local.toFile(), version, closeAction);
            local.toFile().deleteOnExit();
            return jarFile;
        }
        catch (final Throwable ex) {
            this.deleteIfPossible(local, ex);
            throw ex;
        }
    }
    
    private void deleteIfPossible(final Path local, final Throwable cause) {
        try {
            Files.delete(local);
        }
        catch (final IOException ex) {
            cause.addSuppressed(ex);
        }
    }
    
    static boolean isNestedUrl(final URL url) {
        return url.getProtocol().equalsIgnoreCase("nested");
    }
}
