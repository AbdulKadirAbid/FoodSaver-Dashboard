// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.jar;

import java.io.FilterInputStream;
import java.util.zip.Inflater;
import java.nio.ByteBuffer;
import org.springframework.boot.loader.zip.CloseableDataBlock;
import java.util.function.Consumer;
import java.util.Spliterators;
import java.util.NoSuchElementException;
import java.security.CodeSigner;
import java.security.cert.Certificate;
import java.util.jar.Attributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.util.zip.ZipException;
import java.util.zip.ZipEntry;
import java.util.Spliterator;
import java.util.stream.StreamSupport;
import java.util.function.Predicate;
import java.util.Objects;
import org.springframework.boot.loader.zip.ZipContent;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.jar.JarEntry;
import java.util.Enumeration;
import java.io.UncheckedIOException;
import java.util.jar.Manifest;
import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import org.springframework.boot.loader.ref.Cleaner;
import org.springframework.boot.loader.log.DebugLogger;
import java.util.jar.JarFile;

public class NestedJarFile extends JarFile
{
    private static final int DECIMAL = 10;
    private static final String META_INF = "META-INF/";
    static final String META_INF_VERSIONS = "META-INF/versions/";
    static final int BASE_VERSION;
    private static final DebugLogger debug;
    private final Cleaner cleaner;
    private final NestedJarFileResources resources;
    private final java.lang.ref.Cleaner.Cleanable cleanup;
    private final String name;
    private final int version;
    private volatile NestedJarEntry lastEntry;
    private volatile boolean closed;
    private volatile ManifestInfo manifestInfo;
    private volatile MetaInfVersionsInfo metaInfVersionsInfo;
    
    NestedJarFile(final File file) throws IOException {
        this(file, null, null, false, Cleaner.instance);
    }
    
    public NestedJarFile(final File file, final String nestedEntryName) throws IOException {
        this(file, nestedEntryName, null, true, Cleaner.instance);
    }
    
    public NestedJarFile(final File file, final String nestedEntryName, final Runtime.Version version) throws IOException {
        this(file, nestedEntryName, version, true, Cleaner.instance);
    }
    
    NestedJarFile(final File file, final String nestedEntryName, final Runtime.Version version, final boolean onlyNestedJars, final Cleaner cleaner) throws IOException {
        super(file);
        if (onlyNestedJars && (nestedEntryName == null || nestedEntryName.isEmpty())) {
            throw new IllegalArgumentException("nestedEntryName must not be empty");
        }
        NestedJarFile.debug.log("Created nested jar file (%s, %s, %s)", file, nestedEntryName, version);
        this.cleaner = cleaner;
        this.resources = new NestedJarFileResources(file, nestedEntryName);
        this.cleanup = cleaner.register(this, this.resources);
        this.name = file.getPath() + ((nestedEntryName != null) ? ("!/" + nestedEntryName) : "");
        this.version = ((version != null) ? version.feature() : JarFile.baseVersion().feature());
    }
    
    public InputStream getRawZipDataInputStream() throws IOException {
        final RawZipDataInputStream inputStream = new RawZipDataInputStream(this.resources.zipContent().openRawZipData().asInputStream());
        this.resources.addInputStream(inputStream);
        return inputStream;
    }
    
    @Override
    public Manifest getManifest() throws IOException {
        try {
            return this.resources.zipContentForManifest().getInfo(ManifestInfo.class, this::getManifestInfo).getManifest();
        }
        catch (final UncheckedIOException ex) {
            throw ex.getCause();
        }
    }
    
    @Override
    public Enumeration<JarEntry> entries() {
        synchronized (this) {
            this.ensureOpen();
            return new JarEntriesEnumeration(this.resources.zipContent());
        }
    }
    
    @Override
    public Stream<JarEntry> stream() {
        synchronized (this) {
            this.ensureOpen();
            return this.streamContentEntries().map(x$0 -> new NestedJarEntry(x$0));
        }
    }
    
    @Override
    public Stream<JarEntry> versionedStream() {
        synchronized (this) {
            this.ensureOpen();
            return this.streamContentEntries().map((Function<? super ZipContent.Entry, ?>)this::getBaseName).filter(Objects::nonNull).distinct().map((Function<? super Object, ? extends JarEntry>)this::getJarEntry).filter(Objects::nonNull);
        }
    }
    
    private Stream<ZipContent.Entry> streamContentEntries() {
        final ZipContentEntriesSpliterator spliterator = new ZipContentEntriesSpliterator(this.resources.zipContent());
        return StreamSupport.stream((Spliterator<ZipContent.Entry>)spliterator, false);
    }
    
    private String getBaseName(final ZipContent.Entry contentEntry) {
        final String name = contentEntry.getName();
        if (!name.startsWith("META-INF/versions/")) {
            return name;
        }
        final int versionNumberStartIndex = "META-INF/versions/".length();
        final int versionNumberEndIndex = (versionNumberStartIndex != -1) ? name.indexOf(47, versionNumberStartIndex) : -1;
        if (versionNumberEndIndex == -1 || versionNumberEndIndex == name.length() - 1) {
            return null;
        }
        try {
            final int versionNumber = Integer.parseInt(name, versionNumberStartIndex, versionNumberEndIndex, 10);
            if (versionNumber > this.version) {
                return null;
            }
        }
        catch (final NumberFormatException ex) {
            return null;
        }
        return name.substring(versionNumberEndIndex + 1);
    }
    
    @Override
    public JarEntry getJarEntry(final String name) {
        return this.getNestedJarEntry(name);
    }
    
    @Override
    public JarEntry getEntry(final String name) {
        return this.getNestedJarEntry(name);
    }
    
    public boolean hasEntry(final String name) {
        final NestedJarEntry lastEntry = this.lastEntry;
        if (lastEntry != null && name.equals(lastEntry.getName())) {
            return true;
        }
        final ZipContent.Entry entry = this.getVersionedContentEntry(name);
        if (entry != null) {
            return true;
        }
        synchronized (this) {
            this.ensureOpen();
            return this.resources.zipContent().hasEntry(null, name);
        }
    }
    
    private NestedJarEntry getNestedJarEntry(final String name) {
        Objects.requireNonNull(name, "name");
        final NestedJarEntry lastEntry = this.lastEntry;
        if (lastEntry != null && name.equals(lastEntry.getName())) {
            return lastEntry;
        }
        ZipContent.Entry entry = this.getVersionedContentEntry(name);
        entry = ((entry != null) ? entry : this.getContentEntry(null, name));
        if (entry == null) {
            return null;
        }
        final NestedJarEntry nestedJarEntry = new NestedJarEntry(entry, name);
        return this.lastEntry = nestedJarEntry;
    }
    
    private ZipContent.Entry getVersionedContentEntry(final String name) {
        if (NestedJarFile.BASE_VERSION >= this.version || name.startsWith("META-INF/") || !this.getManifestInfo().isMultiRelease()) {
            return null;
        }
        final MetaInfVersionsInfo metaInfVersionsInfo = this.getMetaInfVersionsInfo();
        final int[] versions = metaInfVersionsInfo.versions();
        final String[] directories = metaInfVersionsInfo.directories();
        for (int i = versions.length - 1; i >= 0; --i) {
            if (versions[i] <= this.version) {
                final ZipContent.Entry entry = this.getContentEntry(directories[i], name);
                if (entry != null) {
                    return entry;
                }
            }
        }
        return null;
    }
    
    private ZipContent.Entry getContentEntry(final String namePrefix, final String name) {
        synchronized (this) {
            this.ensureOpen();
            return this.resources.zipContent().getEntry(namePrefix, name);
        }
    }
    
    private ManifestInfo getManifestInfo() {
        ManifestInfo manifestInfo = this.manifestInfo;
        if (manifestInfo != null) {
            return manifestInfo;
        }
        synchronized (this) {
            this.ensureOpen();
            manifestInfo = this.resources.zipContent().getInfo(ManifestInfo.class, this::getManifestInfo);
        }
        return this.manifestInfo = manifestInfo;
    }
    
    private ManifestInfo getManifestInfo(final ZipContent zipContent) {
        final ZipContent.Entry contentEntry = zipContent.getEntry("META-INF/MANIFEST.MF");
        if (contentEntry == null) {
            return ManifestInfo.NONE;
        }
        try (final InputStream inputStream = this.getInputStream(contentEntry)) {
            final Manifest manifest = new Manifest(inputStream);
            return new ManifestInfo(manifest);
        }
        catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
    
    private MetaInfVersionsInfo getMetaInfVersionsInfo() {
        MetaInfVersionsInfo metaInfVersionsInfo = this.metaInfVersionsInfo;
        if (metaInfVersionsInfo != null) {
            return metaInfVersionsInfo;
        }
        synchronized (this) {
            this.ensureOpen();
            metaInfVersionsInfo = this.resources.zipContent().getInfo(MetaInfVersionsInfo.class, MetaInfVersionsInfo::get);
        }
        return this.metaInfVersionsInfo = metaInfVersionsInfo;
    }
    
    @Override
    public InputStream getInputStream(final ZipEntry entry) throws IOException {
        Objects.requireNonNull(entry, "entry");
        if (entry instanceof final NestedJarEntry nestedJarEntry) {
            if (nestedJarEntry.isOwnedBy(this)) {
                return this.getInputStream(nestedJarEntry.contentEntry());
            }
        }
        return this.getInputStream(this.getNestedJarEntry(entry.getName()).contentEntry());
    }
    
    private InputStream getInputStream(final ZipContent.Entry contentEntry) throws IOException {
        final int compression = contentEntry.getCompressionMethod();
        if (compression != 0 && compression != 8) {
            throw new ZipException("invalid compression method");
        }
        synchronized (this) {
            this.ensureOpen();
            InputStream inputStream = new JarEntryInputStream(contentEntry);
            try {
                if (compression == 8) {
                    inputStream = new JarEntryInflaterInputStream((JarEntryInputStream)inputStream, this.resources);
                }
                this.resources.addInputStream(inputStream);
                return inputStream;
            }
            catch (final RuntimeException ex) {
                inputStream.close();
                throw ex;
            }
        }
    }
    
    @Override
    public String getComment() {
        synchronized (this) {
            this.ensureOpen();
            return this.resources.zipContent().getComment();
        }
    }
    
    @Override
    public int size() {
        synchronized (this) {
            this.ensureOpen();
            return this.resources.zipContent().size();
        }
    }
    
    @Override
    public void close() throws IOException {
        super.close();
        if (this.closed) {
            return;
        }
        this.closed = true;
        synchronized (this) {
            try {
                this.cleanup.clean();
            }
            catch (final UncheckedIOException ex) {
                throw ex.getCause();
            }
        }
    }
    
    @Override
    public String getName() {
        return this.name;
    }
    
    private void ensureOpen() {
        if (this.closed) {
            throw new IllegalStateException("Zip file closed");
        }
        if (this.resources.zipContent() == null) {
            throw new IllegalStateException("The object is not initialized.");
        }
    }
    
    public void clearCache() {
        synchronized (this) {
            this.lastEntry = null;
        }
    }
    
    static {
        BASE_VERSION = JarFile.baseVersion().feature();
        debug = DebugLogger.get(NestedJarFile.class);
    }
    
    private class NestedJarEntry extends JarEntry
    {
        private static final IllegalStateException CANNOT_BE_MODIFIED_EXCEPTION;
        private final ZipContent.Entry contentEntry;
        private final String name;
        private volatile boolean populated;
        
        NestedJarEntry(final NestedJarFile this$0, final ZipContent.Entry contentEntry) {
            this(contentEntry, contentEntry.getName());
        }
        
        NestedJarEntry(final ZipContent.Entry contentEntry, final String name) {
            super(contentEntry.getName());
            this.contentEntry = contentEntry;
            this.name = name;
        }
        
        @Override
        public long getTime() {
            this.populate();
            return super.getTime();
        }
        
        @Override
        public LocalDateTime getTimeLocal() {
            this.populate();
            return super.getTimeLocal();
        }
        
        @Override
        public void setTime(final long time) {
            throw NestedJarEntry.CANNOT_BE_MODIFIED_EXCEPTION;
        }
        
        @Override
        public void setTimeLocal(final LocalDateTime time) {
            throw NestedJarEntry.CANNOT_BE_MODIFIED_EXCEPTION;
        }
        
        @Override
        public FileTime getLastModifiedTime() {
            this.populate();
            return super.getLastModifiedTime();
        }
        
        @Override
        public ZipEntry setLastModifiedTime(final FileTime time) {
            throw NestedJarEntry.CANNOT_BE_MODIFIED_EXCEPTION;
        }
        
        @Override
        public FileTime getLastAccessTime() {
            this.populate();
            return super.getLastAccessTime();
        }
        
        @Override
        public ZipEntry setLastAccessTime(final FileTime time) {
            throw NestedJarEntry.CANNOT_BE_MODIFIED_EXCEPTION;
        }
        
        @Override
        public FileTime getCreationTime() {
            this.populate();
            return super.getCreationTime();
        }
        
        @Override
        public ZipEntry setCreationTime(final FileTime time) {
            throw NestedJarEntry.CANNOT_BE_MODIFIED_EXCEPTION;
        }
        
        @Override
        public long getSize() {
            return (long)this.contentEntry.getUncompressedSize() & 0xFFFFFFFFL;
        }
        
        @Override
        public void setSize(final long size) {
            throw NestedJarEntry.CANNOT_BE_MODIFIED_EXCEPTION;
        }
        
        @Override
        public long getCompressedSize() {
            this.populate();
            return super.getCompressedSize();
        }
        
        @Override
        public void setCompressedSize(final long csize) {
            throw NestedJarEntry.CANNOT_BE_MODIFIED_EXCEPTION;
        }
        
        @Override
        public long getCrc() {
            this.populate();
            return super.getCrc();
        }
        
        @Override
        public void setCrc(final long crc) {
            throw NestedJarEntry.CANNOT_BE_MODIFIED_EXCEPTION;
        }
        
        @Override
        public int getMethod() {
            this.populate();
            return super.getMethod();
        }
        
        @Override
        public void setMethod(final int method) {
            throw NestedJarEntry.CANNOT_BE_MODIFIED_EXCEPTION;
        }
        
        @Override
        public byte[] getExtra() {
            this.populate();
            return super.getExtra();
        }
        
        @Override
        public void setExtra(final byte[] extra) {
            throw NestedJarEntry.CANNOT_BE_MODIFIED_EXCEPTION;
        }
        
        @Override
        public String getComment() {
            this.populate();
            return super.getComment();
        }
        
        @Override
        public void setComment(final String comment) {
            throw NestedJarEntry.CANNOT_BE_MODIFIED_EXCEPTION;
        }
        
        boolean isOwnedBy(final NestedJarFile nestedJarFile) {
            return NestedJarFile.this == nestedJarFile;
        }
        
        @Override
        public String getRealName() {
            return super.getName();
        }
        
        @Override
        public String getName() {
            return this.name;
        }
        
        @Override
        public Attributes getAttributes() throws IOException {
            final Manifest manifest = NestedJarFile.this.getManifest();
            return (manifest != null) ? manifest.getAttributes(this.getName()) : null;
        }
        
        @Override
        public Certificate[] getCertificates() {
            return this.getSecurityInfo().getCertificates(this.contentEntry());
        }
        
        @Override
        public CodeSigner[] getCodeSigners() {
            return this.getSecurityInfo().getCodeSigners(this.contentEntry());
        }
        
        private SecurityInfo getSecurityInfo() {
            return NestedJarFile.this.resources.zipContent().getInfo(SecurityInfo.class, SecurityInfo::get);
        }
        
        ZipContent.Entry contentEntry() {
            return this.contentEntry;
        }
        
        private void populate() {
            final boolean populated = this.populated;
            if (!populated) {
                final ZipEntry entry = this.contentEntry.as(ZipEntry::new);
                super.setMethod(entry.getMethod());
                super.setTime(entry.getTime());
                super.setCrc(entry.getCrc());
                super.setCompressedSize(entry.getCompressedSize());
                super.setSize(entry.getSize());
                super.setExtra(entry.getExtra());
                super.setComment(entry.getComment());
                this.populated = true;
            }
        }
        
        static {
            CANNOT_BE_MODIFIED_EXCEPTION = new IllegalStateException("Neste jar entries cannot be modified");
        }
    }
    
    private class JarEntriesEnumeration implements Enumeration<JarEntry>
    {
        private final ZipContent zipContent;
        private int cursor;
        
        JarEntriesEnumeration(final ZipContent zipContent) {
            this.zipContent = zipContent;
        }
        
        @Override
        public boolean hasMoreElements() {
            return this.cursor < this.zipContent.size();
        }
        
        @Override
        public NestedJarEntry nextElement() {
            if (!this.hasMoreElements()) {
                throw new NoSuchElementException();
            }
            synchronized (NestedJarFile.this) {
                NestedJarFile.this.ensureOpen();
                return new NestedJarEntry(this.zipContent.getEntry(this.cursor++));
            }
        }
    }
    
    private class ZipContentEntriesSpliterator extends Spliterators.AbstractSpliterator<ZipContent.Entry>
    {
        private static final int ADDITIONAL_CHARACTERISTICS = 1297;
        private final ZipContent zipContent;
        private int cursor;
        
        ZipContentEntriesSpliterator(final ZipContent zipContent) {
            super(zipContent.size(), 1297);
            this.zipContent = zipContent;
        }
        
        @Override
        public boolean tryAdvance(final Consumer<? super ZipContent.Entry> action) {
            if (this.cursor < this.zipContent.size()) {
                synchronized (NestedJarFile.this) {
                    NestedJarFile.this.ensureOpen();
                    action.accept(this.zipContent.getEntry(this.cursor++));
                }
                return true;
            }
            return false;
        }
    }
    
    private class JarEntryInputStream extends InputStream
    {
        private final int uncompressedSize;
        private final CloseableDataBlock content;
        private long pos;
        private long remaining;
        private volatile boolean closed;
        
        JarEntryInputStream(final ZipContent.Entry entry) throws IOException {
            this.uncompressedSize = entry.getUncompressedSize();
            this.content = entry.openContent();
        }
        
        @Override
        public int read() throws IOException {
            final byte[] b = { 0 };
            return (this.read(b, 0, 1) == 1) ? (b[0] & 0xFF) : -1;
        }
        
        @Override
        public int read(final byte[] b, final int off, final int len) throws IOException {
            final int result;
            synchronized (NestedJarFile.this) {
                this.ensureOpen();
                final ByteBuffer dst = ByteBuffer.wrap(b, off, len);
                final int count = this.content.read(dst, this.pos);
                if (count > 0) {
                    this.pos += count;
                    this.remaining -= count;
                }
                result = count;
            }
            if (this.remaining == 0L) {
                this.close();
            }
            return result;
        }
        
        @Override
        public long skip(final long n) throws IOException {
            final long result;
            synchronized (NestedJarFile.this) {
                result = ((n > 0L) ? this.maxForwardSkip(n) : this.maxBackwardSkip(n));
                this.pos += result;
                this.remaining -= result;
            }
            if (this.remaining == 0L) {
                this.close();
            }
            return result;
        }
        
        private long maxForwardSkip(final long n) {
            final boolean willCauseOverflow = this.pos + n < 0L;
            return (willCauseOverflow || n > this.remaining) ? this.remaining : n;
        }
        
        private long maxBackwardSkip(final long n) {
            return Math.max(-this.pos, n);
        }
        
        @Override
        public int available() {
            return (this.remaining < 2147483647L) ? ((int)this.remaining) : Integer.MAX_VALUE;
        }
        
        private void ensureOpen() throws ZipException {
            if (NestedJarFile.this.closed || this.closed) {
                throw new ZipException("ZipFile closed");
            }
        }
        
        @Override
        public void close() throws IOException {
            if (this.closed) {
                return;
            }
            this.closed = true;
            this.content.close();
            NestedJarFile.this.resources.removeInputStream(this);
        }
        
        int getUncompressedSize() {
            return this.uncompressedSize;
        }
    }
    
    private class JarEntryInflaterInputStream extends ZipInflaterInputStream
    {
        private final java.lang.ref.Cleaner.Cleanable cleanup;
        private volatile boolean closed;
        
        JarEntryInflaterInputStream(final NestedJarFile this$0, final JarEntryInputStream inputStream, final NestedJarFileResources resources) {
            this(inputStream, resources, resources.getOrCreateInflater());
        }
        
        private JarEntryInflaterInputStream(final JarEntryInputStream inputStream, final NestedJarFileResources resources, final Inflater inflater) {
            super(inputStream, inflater, inputStream.getUncompressedSize());
            this.cleanup = NestedJarFile.this.cleaner.register(this, resources.createInflatorCleanupAction(inflater));
        }
        
        @Override
        public void close() throws IOException {
            if (this.closed) {
                return;
            }
            this.closed = true;
            super.close();
            NestedJarFile.this.resources.removeInputStream(this);
            this.cleanup.clean();
        }
    }
    
    private class RawZipDataInputStream extends FilterInputStream
    {
        private volatile boolean closed;
        
        RawZipDataInputStream(final InputStream in) {
            super(in);
        }
        
        @Override
        public void close() throws IOException {
            if (this.closed) {
                return;
            }
            this.closed = true;
            super.close();
            NestedJarFile.this.resources.removeInputStream(this);
        }
    }
}
