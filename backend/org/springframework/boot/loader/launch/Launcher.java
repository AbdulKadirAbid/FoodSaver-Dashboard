// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.launch;

import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.io.IOException;
import java.lang.reflect.Method;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Collection;
import org.springframework.boot.loader.net.protocol.Handlers;

public abstract class Launcher
{
    private static final String JAR_MODE_RUNNER_CLASS_NAME;
    protected static final String BOOT_CLASSPATH_INDEX_ATTRIBUTE = "Spring-Boot-Classpath-Index";
    protected static final String DEFAULT_CLASSPATH_INDEX_FILE_NAME = "classpath.idx";
    protected ClassPathIndexFile classPathIndex;
    
    protected void launch(final String[] args) throws Exception {
        if (!this.isExploded()) {
            Handlers.register();
        }
        try {
            final ClassLoader classLoader = this.createClassLoader(this.getClassPathUrls());
            final String jarMode = System.getProperty("jarmode");
            final String mainClassName = this.hasLength(jarMode) ? Launcher.JAR_MODE_RUNNER_CLASS_NAME : this.getMainClass();
            this.launch(classLoader, mainClassName, args);
        }
        catch (final UncheckedIOException ex) {
            throw ex.getCause();
        }
    }
    
    private boolean hasLength(final String jarMode) {
        return jarMode != null && !jarMode.isEmpty();
    }
    
    protected ClassLoader createClassLoader(final Collection<URL> urls) throws Exception {
        return this.createClassLoader(urls.toArray(new URL[0]));
    }
    
    private ClassLoader createClassLoader(final URL[] urls) {
        final ClassLoader parent = this.getClass().getClassLoader();
        return new LaunchedClassLoader(this.isExploded(), this.getArchive(), urls, parent);
    }
    
    protected void launch(final ClassLoader classLoader, final String mainClassName, final String[] args) throws Exception {
        Thread.currentThread().setContextClassLoader(classLoader);
        final Class<?> mainClass = Class.forName(mainClassName, false, classLoader);
        final Method mainMethod = mainClass.getDeclaredMethod("main", String[].class);
        mainMethod.setAccessible(true);
        mainMethod.invoke(null, args);
    }
    
    protected boolean isExploded() {
        final Archive archive = this.getArchive();
        return archive != null && archive.isExploded();
    }
    
    ClassPathIndexFile getClassPathIndex(final Archive archive) throws IOException {
        if (!archive.isExploded()) {
            return null;
        }
        final String location = this.getClassPathIndexFileLocation(archive);
        return ClassPathIndexFile.loadIfPossible(archive.getRootDirectory(), location);
    }
    
    private String getClassPathIndexFileLocation(final Archive archive) throws IOException {
        final Manifest manifest = archive.getManifest();
        final Attributes attributes = (manifest != null) ? manifest.getMainAttributes() : null;
        final String location = (attributes != null) ? attributes.getValue("Spring-Boot-Classpath-Index") : null;
        return (location != null) ? location : (this.getEntryPathPrefix() + "classpath.idx");
    }
    
    protected abstract Archive getArchive();
    
    protected abstract String getMainClass() throws Exception;
    
    protected abstract Set<URL> getClassPathUrls() throws Exception;
    
    protected String getEntryPathPrefix() {
        return "BOOT-INF/";
    }
    
    protected boolean isIncludedOnClassPath(final Archive.Entry entry) {
        return this.isLibraryFileOrClassesDirectory(entry);
    }
    
    protected boolean isLibraryFileOrClassesDirectory(final Archive.Entry entry) {
        final String name = entry.name();
        if (entry.isDirectory()) {
            return name.equals("BOOT-INF/classes/");
        }
        return name.startsWith("BOOT-INF/lib/");
    }
    
    protected boolean isIncludedOnClassPathAndNotIndexed(final Archive.Entry entry) {
        return this.isIncludedOnClassPath(entry) && (this.classPathIndex == null || !this.classPathIndex.containsEntry(entry.name()));
    }
    
    static {
        JAR_MODE_RUNNER_CLASS_NAME = JarModeRunner.class.getName();
    }
}
