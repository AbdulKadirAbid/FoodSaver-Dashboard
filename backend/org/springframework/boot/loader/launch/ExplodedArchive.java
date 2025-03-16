// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.launch;

import java.util.function.Function;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Collection;
import java.util.LinkedList;
import java.util.LinkedHashSet;
import java.net.URL;
import java.util.function.Predicate;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.jar.Manifest;
import java.io.File;
import java.util.Comparator;
import java.util.Set;

class ExplodedArchive implements Archive
{
    private static final Object NO_MANIFEST;
    private static final Set<String> SKIPPED_NAMES;
    private static final Comparator<File> entryComparator;
    private final File rootDirectory;
    private final String rootUriPath;
    private volatile Object manifest;
    
    ExplodedArchive(final File rootDirectory) {
        if (!rootDirectory.exists() || !rootDirectory.isDirectory()) {
            throw new IllegalArgumentException("Invalid source directory " + rootDirectory);
        }
        this.rootDirectory = rootDirectory;
        this.rootUriPath = this.rootDirectory.toURI().getPath();
    }
    
    @Override
    public Manifest getManifest() throws IOException {
        Object manifest = this.manifest;
        if (manifest == null) {
            manifest = this.loadManifest();
            this.manifest = manifest;
        }
        return (manifest != ExplodedArchive.NO_MANIFEST) ? ((Manifest)manifest) : null;
    }
    
    private Object loadManifest() throws IOException {
        final File file = new File(this.rootDirectory, "META-INF/MANIFEST.MF");
        if (!file.exists()) {
            return ExplodedArchive.NO_MANIFEST;
        }
        try (final FileInputStream inputStream = new FileInputStream(file)) {
            return new Manifest(inputStream);
        }
    }
    
    @Override
    public Set<URL> getClassPathUrls(final Predicate<Entry> includeFilter, final Predicate<Entry> directorySearchFilter) throws IOException {
        final Set<URL> urls = new LinkedHashSet<URL>();
        final LinkedList<File> files = new LinkedList<File>(this.listFiles(this.rootDirectory));
        while (!files.isEmpty()) {
            final File file = files.poll();
            if (ExplodedArchive.SKIPPED_NAMES.contains(file.getName())) {
                continue;
            }
            final String entryName = file.toURI().getPath().substring(this.rootUriPath.length());
            final Entry entry = new FileArchiveEntry(entryName, file);
            if (entry.isDirectory() && directorySearchFilter.test(entry)) {
                files.addAll(0, this.listFiles(file));
            }
            if (!includeFilter.test(entry)) {
                continue;
            }
            urls.add(file.toURI().toURL());
        }
        return urls;
    }
    
    private List<File> listFiles(final File file) {
        final File[] files = file.listFiles();
        if (files == null) {
            return Collections.emptyList();
        }
        Arrays.sort(files, ExplodedArchive.entryComparator);
        return Arrays.asList(files);
    }
    
    @Override
    public File getRootDirectory() {
        return this.rootDirectory;
    }
    
    @Override
    public String toString() {
        return this.rootDirectory.toString();
    }
    
    static {
        NO_MANIFEST = new Object();
        SKIPPED_NAMES = Set.of(".", "..");
        entryComparator = Comparator.comparing((Function<? super File, ? extends Comparable>)File::getAbsolutePath);
    }
    
    record FileArchiveEntry(String name, File file) implements Entry {
        @Override
        public boolean isDirectory() {
            return this.file.isDirectory();
        }
    }
}
