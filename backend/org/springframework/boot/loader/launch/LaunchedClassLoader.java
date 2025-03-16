// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.launch;

import java.util.function.Supplier;
import java.util.jar.Manifest;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import org.springframework.boot.loader.net.protocol.jar.JarUrlClassLoader;

public class LaunchedClassLoader extends JarUrlClassLoader
{
    private static final String JAR_MODE_PACKAGE_PREFIX = "org.springframework.boot.loader.jarmode.";
    private static final String JAR_MODE_RUNNER_CLASS_NAME;
    private final boolean exploded;
    private final Archive rootArchive;
    private final Object definePackageLock;
    private volatile DefinePackageCallType definePackageCallType;
    
    public LaunchedClassLoader(final boolean exploded, final URL[] urls, final ClassLoader parent) {
        this(exploded, null, urls, parent);
    }
    
    public LaunchedClassLoader(final boolean exploded, final Archive rootArchive, final URL[] urls, final ClassLoader parent) {
        super(urls, parent);
        this.definePackageLock = new Object();
        this.exploded = exploded;
        this.rootArchive = rootArchive;
    }
    
    @Override
    protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
        if (!name.startsWith("org.springframework.boot.loader.jarmode.")) {
            if (!name.equals(LaunchedClassLoader.JAR_MODE_RUNNER_CLASS_NAME)) {
                return super.loadClass(name, resolve);
            }
        }
        try {
            final Class<?> result = this.loadClassInLaunchedClassLoader(name);
            if (resolve) {
                this.resolveClass(result);
            }
            return result;
        }
        catch (final ClassNotFoundException ex) {}
        return super.loadClass(name, resolve);
    }
    
    private Class<?> loadClassInLaunchedClassLoader(final String name) throws ClassNotFoundException {
        try {
            final String internalName = name.replace('.', '/') + ".class";
            try (final InputStream inputStream = this.getParent().getResourceAsStream(internalName);
                 final ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                if (inputStream == null) {
                    throw new ClassNotFoundException(name);
                }
                inputStream.transferTo(outputStream);
                final byte[] bytes = outputStream.toByteArray();
                final Class<?> definedClass = this.defineClass(name, bytes, 0, bytes.length);
                this.definePackageIfNecessary(name);
                return definedClass;
            }
        }
        catch (final IOException ex) {
            throw new ClassNotFoundException("Cannot load resource for class [" + name, (Throwable)ex);
        }
    }
    
    @Override
    protected Package definePackage(final String name, final Manifest man, final URL url) throws IllegalArgumentException {
        return this.exploded ? this.definePackageForExploded(name, man, url) : super.definePackage(name, man, url);
    }
    
    private Package definePackageForExploded(final String name, final Manifest man, final URL url) {
        synchronized (this.definePackageLock) {
            return this.definePackage(DefinePackageCallType.MANIFEST, () -> super.definePackage(name, man, url));
        }
    }
    
    @Override
    protected Package definePackage(final String name, final String specTitle, final String specVersion, final String specVendor, final String implTitle, final String implVersion, final String implVendor, final URL sealBase) throws IllegalArgumentException {
        if (!this.exploded) {
            return super.definePackage(name, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor, sealBase);
        }
        return this.definePackageForExploded(name, sealBase, () -> super.definePackage(name, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor, sealBase));
    }
    
    private Package definePackageForExploded(final String name, final URL sealBase, final Supplier<Package> call) {
        synchronized (this.definePackageLock) {
            if (this.definePackageCallType == null) {
                final Manifest manifest = this.getManifest(this.rootArchive);
                if (manifest != null) {
                    return this.definePackage(name, manifest, sealBase);
                }
            }
            return this.definePackage(DefinePackageCallType.ATTRIBUTES, call);
        }
    }
    
    private <T> T definePackage(final DefinePackageCallType type, final Supplier<T> call) {
        final DefinePackageCallType existingType = this.definePackageCallType;
        try {
            this.definePackageCallType = type;
            return call.get();
        }
        finally {
            this.definePackageCallType = existingType;
        }
    }
    
    private Manifest getManifest(final Archive archive) {
        try {
            return (archive != null) ? archive.getManifest() : null;
        }
        catch (final IOException ex) {
            return null;
        }
    }
    
    static {
        JAR_MODE_RUNNER_CLASS_NAME = JarModeRunner.class.getName();
        ClassLoader.registerAsParallelCapable();
    }
    
    private enum DefinePackageCallType
    {
        MANIFEST, 
        ATTRIBUTES;
    }
}
