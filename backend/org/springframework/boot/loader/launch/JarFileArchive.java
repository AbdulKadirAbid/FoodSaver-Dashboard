// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.launch;

import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.FileSystem;
import java.io.InputStream;
import java.nio.file.StandardCopyOption;
import java.nio.file.CopyOption;
import java.util.zip.ZipEntry;
import java.util.UUID;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.io.UncheckedIOException;
import org.springframework.boot.loader.net.protocol.jar.JarUrl;
import java.util.stream.Collector;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.LinkedHashSet;
import java.util.jar.JarEntry;
import java.util.function.Function;
import java.net.URL;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.Manifest;
import java.io.IOException;
import java.util.jar.JarFile;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;

class JarFileArchive implements Archive
{
    private static final String UNPACK_MARKER = "UNPACK:";
    private static final FileAttribute<?>[] NO_FILE_ATTRIBUTES;
    private static final FileAttribute<?>[] DIRECTORY_PERMISSION_ATTRIBUTES;
    private static final FileAttribute<?>[] FILE_PERMISSION_ATTRIBUTES;
    private static final Path TEMP;
    private final File file;
    private final JarFile jarFile;
    private volatile Path tempUnpackDirectory;
    
    JarFileArchive(final File file) throws IOException {
        this(file, new JarFile(file));
    }
    
    private JarFileArchive(final File file, final JarFile jarFile) {
        this.file = file;
        this.jarFile = jarFile;
    }
    
    @Override
    public Manifest getManifest() throws IOException {
        return this.jarFile.getManifest();
    }
    
    @Override
    public Set<URL> getClassPathUrls(final Predicate<Entry> includeFilter, final Predicate<Entry> directorySearchFilter) throws IOException {
        return this.jarFile.stream().map((Function<? super JarEntry, ?>)JarArchiveEntry::new).filter((Predicate<? super Object>)includeFilter).map((Function<? super Object, ?>)this::getNestedJarUrl).collect((Collector<? super Object, ?, Set<URL>>)Collectors.toCollection((Supplier<R>)LinkedHashSet::new));
    }
    
    private URL getNestedJarUrl(final JarArchiveEntry archiveEntry) {
        try {
            final JarEntry jarEntry = archiveEntry.jarEntry();
            final String comment = jarEntry.getComment();
            if (comment != null && comment.startsWith("UNPACK:")) {
                return this.getUnpackedNestedJarUrl(jarEntry);
            }
            return JarUrl.create(this.file, jarEntry);
        }
        catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
    
    private URL getUnpackedNestedJarUrl(final JarEntry jarEntry) throws IOException {
        String name = jarEntry.getName();
        if (name.lastIndexOf(47) != -1) {
            name = name.substring(name.lastIndexOf(47) + 1);
        }
        final Path path = this.getTempUnpackDirectory().resolve(name);
        if (!Files.exists(path, new LinkOption[0]) || Files.size(path) != jarEntry.getSize()) {
            this.unpack(jarEntry, path);
        }
        return path.toUri().toURL();
    }
    
    private Path getTempUnpackDirectory() {
        Path tempUnpackDirectory = this.tempUnpackDirectory;
        if (tempUnpackDirectory != null) {
            return tempUnpackDirectory;
        }
        synchronized (JarFileArchive.TEMP) {
            tempUnpackDirectory = this.tempUnpackDirectory;
            if (tempUnpackDirectory == null) {
                tempUnpackDirectory = this.createUnpackDirectory(JarFileArchive.TEMP);
                this.tempUnpackDirectory = tempUnpackDirectory;
            }
        }
        return tempUnpackDirectory;
    }
    
    private Path createUnpackDirectory(final Path parent) {
        int attempts = 0;
        final String fileName = Paths.get(this.jarFile.getName(), new String[0]).getFileName().toString();
        while (attempts++ < 100) {
            final Path unpackDirectory = parent.resolve(fileName + "-spring-boot-libs-" + UUID.randomUUID());
            try {
                this.createDirectory(unpackDirectory);
                return unpackDirectory;
            }
            catch (final IOException ex) {
                continue;
            }
            break;
        }
        throw new IllegalStateException("Failed to create unpack directory in directory '" + parent);
    }
    
    private void createDirectory(final Path path) throws IOException {
        Files.createDirectory(path, this.getFileAttributes(path, JarFileArchive.DIRECTORY_PERMISSION_ATTRIBUTES));
    }
    
    private void unpack(final JarEntry entry, final Path path) throws IOException {
        this.createFile(path);
        path.toFile().deleteOnExit();
        try (final InputStream in = this.jarFile.getInputStream(entry)) {
            Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }
    
    private void createFile(final Path path) throws IOException {
        Files.createFile(path, this.getFileAttributes(path, JarFileArchive.FILE_PERMISSION_ATTRIBUTES));
    }
    
    private FileAttribute<?>[] getFileAttributes(final Path path, final FileAttribute<?>[] permissionAttributes) {
        return this.supportsPosix(path.getFileSystem()) ? permissionAttributes : JarFileArchive.NO_FILE_ATTRIBUTES;
    }
    
    private boolean supportsPosix(final FileSystem fileSystem) {
        return fileSystem.supportedFileAttributeViews().contains("posix");
    }
    
    @Override
    public void close() throws IOException {
        this.jarFile.close();
    }
    
    @Override
    public String toString() {
        return this.file.toString();
    }
    
    private static FileAttribute<?>[] asFileAttributes(final PosixFilePermission... permissions) {
        return new FileAttribute[] { PosixFilePermissions.asFileAttribute(Set.of(permissions)) };
    }
    
    static {
        NO_FILE_ATTRIBUTES = new FileAttribute[0];
        DIRECTORY_PERMISSION_ATTRIBUTES = asFileAttributes(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE);
        FILE_PERMISSION_ATTRIBUTES = asFileAttributes(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
        TEMP = Paths.get(System.getProperty("java.io.tmpdir"), new String[0]);
    }
    
    record JarArchiveEntry(JarEntry jarEntry) implements Entry {
        @Override
        public String name() {
            return this.jarEntry.getName();
        }
        
        @Override
        public boolean isDirectory() {
            return this.jarEntry.isDirectory();
        }
    }
}
