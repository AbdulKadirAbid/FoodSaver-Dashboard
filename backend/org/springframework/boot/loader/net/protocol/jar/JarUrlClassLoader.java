// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.net.protocol.jar;

import java.util.Iterator;
import java.net.URLConnection;
import java.net.JarURLConnection;
import org.springframework.boot.loader.jar.NestedJarFile;
import java.io.IOException;
import java.util.Enumeration;
import java.util.function.Predicate;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.Map;
import java.net.URL;
import java.net.URLClassLoader;

public abstract class JarUrlClassLoader extends URLClassLoader
{
    private final URL[] urls;
    private final boolean hasJarUrls;
    private final Map<URL, JarFile> jarFiles;
    private final Set<String> undefinablePackages;
    
    public JarUrlClassLoader(final URL[] urls, final ClassLoader parent) {
        super(urls, parent);
        this.jarFiles = new ConcurrentHashMap<URL, JarFile>();
        this.undefinablePackages = (Set<String>)ConcurrentHashMap.newKeySet();
        this.urls = urls;
        this.hasJarUrls = Arrays.stream(urls).anyMatch(this::isJarUrl);
    }
    
    @Override
    public URL findResource(final String name) {
        if (!this.hasJarUrls) {
            return super.findResource(name);
        }
        Optimizations.enable(false);
        try {
            return super.findResource(name);
        }
        finally {
            Optimizations.disable();
        }
    }
    
    @Override
    public Enumeration<URL> findResources(final String name) throws IOException {
        if (!this.hasJarUrls) {
            return super.findResources(name);
        }
        Optimizations.enable(false);
        try {
            return new OptimizedEnumeration(super.findResources(name));
        }
        finally {
            Optimizations.disable();
        }
    }
    
    @Override
    protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
        if (!this.hasJarUrls) {
            return super.loadClass(name, resolve);
        }
        Optimizations.enable(true);
        try {
            try {
                this.definePackageIfNecessary(name);
            }
            catch (final IllegalArgumentException ex) {
                this.tolerateRaceConditionDueToBeingParallelCapable(ex, name);
            }
            return super.loadClass(name, resolve);
        }
        finally {
            Optimizations.disable();
        }
    }
    
    protected final void definePackageIfNecessary(final String className) {
        if (className.startsWith("java.")) {
            return;
        }
        final int lastDot = className.lastIndexOf(46);
        if (lastDot >= 0) {
            final String packageName = className.substring(0, lastDot);
            if (this.getDefinedPackage(packageName) == null) {
                try {
                    this.definePackage(className, packageName);
                }
                catch (final IllegalArgumentException ex) {
                    this.tolerateRaceConditionDueToBeingParallelCapable(ex, packageName);
                }
            }
        }
    }
    
    private void definePackage(final String className, final String packageName) {
        if (this.undefinablePackages.contains(packageName)) {
            return;
        }
        final String packageEntryName = packageName.replace('.', '/');
        final String classEntryName = className.replace('.', '/') + ".class";
        for (final URL url : this.urls) {
            try {
                final JarFile jarFile = this.getJarFile(url);
                if (jarFile != null && this.hasEntry(jarFile, classEntryName) && this.hasEntry(jarFile, packageEntryName) && jarFile.getManifest() != null) {
                    this.definePackage(packageName, jarFile.getManifest(), url);
                    return;
                }
            }
            catch (final IOException ex) {}
        }
        this.undefinablePackages.add(packageName);
    }
    
    private void tolerateRaceConditionDueToBeingParallelCapable(final IllegalArgumentException ex, final String packageName) throws AssertionError {
        if (this.getDefinedPackage(packageName) == null) {
            throw new AssertionError("Package %s has already been defined but it could not be found".formatted(packageName), ex);
        }
    }
    
    private boolean hasEntry(final JarFile jarFile, final String name) {
        boolean hasEntry;
        if (jarFile instanceof final NestedJarFile nestedJarFile) {
            hasEntry = nestedJarFile.hasEntry(name);
        }
        else {
            hasEntry = (jarFile.getEntry(name) != null);
        }
        return hasEntry;
    }
    
    private JarFile getJarFile(final URL url) throws IOException {
        JarFile jarFile = this.jarFiles.get(url);
        if (jarFile != null) {
            return jarFile;
        }
        final URLConnection connection = url.openConnection();
        if (!(connection instanceof JarURLConnection)) {
            return null;
        }
        connection.setUseCaches(false);
        jarFile = ((JarURLConnection)connection).getJarFile();
        synchronized (this.jarFiles) {
            final JarFile previous = this.jarFiles.putIfAbsent(url, jarFile);
            if (previous != null) {
                jarFile.close();
                jarFile = previous;
            }
        }
        return jarFile;
    }
    
    public void clearCache() {
        Handler.clearCache();
        org.springframework.boot.loader.net.protocol.nested.Handler.clearCache();
        try {
            this.clearJarFiles();
        }
        catch (final IOException ex) {}
        for (final URL url : this.urls) {
            if (this.isJarUrl(url)) {
                this.clearCache(url);
            }
        }
    }
    
    private void clearCache(final URL url) {
        try {
            final URLConnection connection = url.openConnection();
            if (connection instanceof final JarURLConnection jarUrlConnection) {
                this.clearCache(jarUrlConnection);
            }
        }
        catch (final IOException ex) {}
    }
    
    private void clearCache(final JarURLConnection connection) throws IOException {
        final JarFile jarFile = connection.getJarFile();
        if (jarFile instanceof final NestedJarFile nestedJarFile) {
            nestedJarFile.clearCache();
        }
    }
    
    private boolean isJarUrl(final URL url) {
        return "jar".equals(url.getProtocol());
    }
    
    @Override
    public void close() throws IOException {
        super.close();
        this.clearJarFiles();
    }
    
    private void clearJarFiles() throws IOException {
        synchronized (this.jarFiles) {
            for (final JarFile jarFile : this.jarFiles.values()) {
                jarFile.close();
            }
            this.jarFiles.clear();
        }
    }
    
    static {
        ClassLoader.registerAsParallelCapable();
    }
    
    private static class OptimizedEnumeration implements Enumeration<URL>
    {
        private final Enumeration<URL> delegate;
        
        OptimizedEnumeration(final Enumeration<URL> delegate) {
            this.delegate = delegate;
        }
        
        @Override
        public boolean hasMoreElements() {
            Optimizations.enable(false);
            try {
                return this.delegate.hasMoreElements();
            }
            finally {
                Optimizations.disable();
            }
        }
        
        @Override
        public URL nextElement() {
            Optimizations.enable(false);
            try {
                return this.delegate.nextElement();
            }
            finally {
                Optimizations.disable();
            }
        }
    }
}
