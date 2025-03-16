// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.launch;

import java.util.function.Predicate;
import java.nio.file.Files;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.stream.Collector;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.LinkedHashSet;
import java.util.function.Function;
import java.util.List;
import java.util.Set;
import java.io.File;

final class ClassPathIndexFile
{
    private final File root;
    private final Set<String> lines;
    
    private ClassPathIndexFile(final File root, final List<String> lines) {
        this.root = root;
        this.lines = lines.stream().map((Function<? super Object, ?>)this::extractName).collect((Collector<? super Object, ?, Set<String>>)Collectors.toCollection((Supplier<R>)LinkedHashSet::new));
    }
    
    private String extractName(final String line) {
        if (line.startsWith("- \"") && line.endsWith("\"")) {
            return line.substring(3, line.length() - 1);
        }
        throw new IllegalStateException("Malformed classpath index line [" + line);
    }
    
    int size() {
        return this.lines.size();
    }
    
    boolean containsEntry(final String name) {
        return name != null && !name.isEmpty() && this.lines.contains(name);
    }
    
    List<URL> getUrls() {
        return this.lines.stream().map((Function<? super Object, ? extends URL>)this::asUrl).toList();
    }
    
    private URL asUrl(final String line) {
        try {
            return new File(this.root, line).toURI().toURL();
        }
        catch (final MalformedURLException ex) {
            throw new IllegalStateException(ex);
        }
    }
    
    static ClassPathIndexFile loadIfPossible(final File root, final String location) throws IOException {
        return loadIfPossible(root, new File(root, location));
    }
    
    private static ClassPathIndexFile loadIfPossible(final File root, final File indexFile) throws IOException {
        if (indexFile.exists() && indexFile.isFile()) {
            final List<String> lines = Files.readAllLines(indexFile.toPath()).stream().filter(ClassPathIndexFile::lineHasText).toList();
            return new ClassPathIndexFile(root, lines);
        }
        return null;
    }
    
    private static boolean lineHasText(final String line) {
        return !line.trim().isEmpty();
    }
}
