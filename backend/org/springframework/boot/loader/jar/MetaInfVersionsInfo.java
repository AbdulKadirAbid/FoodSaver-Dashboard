// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.jar;

import java.util.Collections;
import java.util.TreeSet;
import java.util.function.IntFunction;
import java.util.Objects;
import org.springframework.boot.loader.zip.ZipContent;
import java.util.Set;

final class MetaInfVersionsInfo
{
    static final MetaInfVersionsInfo NONE;
    private static final String META_INF_VERSIONS = "META-INF/versions/";
    private final int[] versions;
    private final String[] directories;
    
    private MetaInfVersionsInfo(final Set<Integer> versions) {
        this.versions = versions.stream().mapToInt(Integer::intValue).toArray();
        this.directories = versions.stream().map(version -> "META-INF/versions/" + version).toArray(String[]::new);
    }
    
    int[] versions() {
        return this.versions;
    }
    
    String[] directories() {
        return this.directories;
    }
    
    static MetaInfVersionsInfo get(final ZipContent zipContent) {
        final int size = zipContent.size();
        Objects.requireNonNull(zipContent);
        return get(size, zipContent::getEntry);
    }
    
    static MetaInfVersionsInfo get(final int size, final IntFunction<ZipContent.Entry> entries) {
        final Set<Integer> versions = new TreeSet<Integer>();
        for (int i = 0; i < size; ++i) {
            final ZipContent.Entry contentEntry = entries.apply(i);
            if (contentEntry.hasNameStartingWith("META-INF/versions/") && !contentEntry.isDirectory()) {
                final String name = contentEntry.getName();
                final int slash = name.indexOf(47, "META-INF/versions/".length());
                if (slash > -1) {
                    final String version = name.substring("META-INF/versions/".length(), slash);
                    try {
                        final int versionNumber = Integer.parseInt(version);
                        if (versionNumber >= NestedJarFile.BASE_VERSION) {
                            versions.add(versionNumber);
                        }
                    }
                    catch (final NumberFormatException ex) {}
                }
            }
        }
        return versions.isEmpty() ? MetaInfVersionsInfo.NONE : new MetaInfVersionsInfo(versions);
    }
    
    static {
        NONE = new MetaInfVersionsInfo(Collections.emptySet());
    }
}
