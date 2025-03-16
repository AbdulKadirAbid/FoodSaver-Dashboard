// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.zip;

import java.util.function.BiFunction;
import java.util.zip.ZipEntry;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.io.UncheckedIOException;
import java.nio.channels.ClosedChannelException;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.Map;
import org.springframework.boot.loader.log.DebugLogger;
import java.io.Closeable;

public final class ZipContent implements Closeable
{
    private static final String META_INF = "META-INF/";
    private static final byte[] SIGNATURE_SUFFIX;
    private static final DebugLogger debug;
    private static final Map<Source, ZipContent> cache;
    private final Source source;
    private final Kind kind;
    private final FileDataBlock data;
    private final long centralDirectoryPos;
    private final long commentPos;
    private final long commentLength;
    private final int[] lookupIndexes;
    private final int[] nameHashLookups;
    private final int[] relativeCentralDirectoryOffsetLookups;
    private final NameOffsetLookups nameOffsetLookups;
    private final boolean hasJarSignatureFile;
    private SoftReference<CloseableDataBlock> virtualData;
    private SoftReference<Map<Class<?>, Object>> info;
    
    private ZipContent(final Source source, final Kind kind, final FileDataBlock data, final long centralDirectoryPos, final long commentPos, final long commentLength, final int[] lookupIndexes, final int[] nameHashLookups, final int[] relativeCentralDirectoryOffsetLookups, final NameOffsetLookups nameOffsetLookups, final boolean hasJarSignatureFile) {
        this.source = source;
        this.kind = kind;
        this.data = data;
        this.centralDirectoryPos = centralDirectoryPos;
        this.commentPos = commentPos;
        this.commentLength = commentLength;
        this.lookupIndexes = lookupIndexes;
        this.nameHashLookups = nameHashLookups;
        this.relativeCentralDirectoryOffsetLookups = relativeCentralDirectoryOffsetLookups;
        this.nameOffsetLookups = nameOffsetLookups;
        this.hasJarSignatureFile = hasJarSignatureFile;
    }
    
    public Kind getKind() {
        return this.kind;
    }
    
    public CloseableDataBlock openRawZipData() throws IOException {
        this.data.open();
        return this.nameOffsetLookups.hasAnyEnabled() ? this.getVirtualData() : this.data;
    }
    
    private CloseableDataBlock getVirtualData() throws IOException {
        CloseableDataBlock virtualData = (this.virtualData != null) ? this.virtualData.get() : null;
        if (virtualData != null) {
            return virtualData;
        }
        virtualData = this.createVirtualData();
        this.virtualData = new SoftReference<CloseableDataBlock>(virtualData);
        return virtualData;
    }
    
    private CloseableDataBlock createVirtualData() throws IOException {
        final int size = this.size();
        final NameOffsetLookups nameOffsetLookups = this.nameOffsetLookups.emptyCopy();
        final ZipCentralDirectoryFileHeaderRecord[] centralRecords = new ZipCentralDirectoryFileHeaderRecord[size];
        final long[] centralRecordPositions = new long[size];
        for (int i = 0; i < size; ++i) {
            final int lookupIndex = this.lookupIndexes[i];
            final long pos = this.getCentralDirectoryFileHeaderRecordPos(lookupIndex);
            nameOffsetLookups.enable(i, this.nameOffsetLookups.isEnabled(lookupIndex));
            centralRecords[i] = ZipCentralDirectoryFileHeaderRecord.load(this.data, pos);
            centralRecordPositions[i] = pos;
        }
        return new VirtualZipDataBlock(this.data, nameOffsetLookups, centralRecords, centralRecordPositions);
    }
    
    public int size() {
        return this.lookupIndexes.length;
    }
    
    public String getComment() {
        try {
            return ZipString.readString(this.data, this.commentPos, this.commentLength);
        }
        catch (final UncheckedIOException ex) {
            if (ex.getCause() instanceof ClosedChannelException) {
                throw new IllegalStateException("Zip content closed", ex);
            }
            throw ex;
        }
    }
    
    public Entry getEntry(final CharSequence name) {
        return this.getEntry(null, name);
    }
    
    public Entry getEntry(final CharSequence namePrefix, final CharSequence name) {
        for (int nameHash = this.nameHash(namePrefix, name), lookupIndex = this.getFirstLookupIndex(nameHash), size = this.size(); lookupIndex >= 0 && lookupIndex < size && this.nameHashLookups[lookupIndex] == nameHash; ++lookupIndex) {
            final long pos = this.getCentralDirectoryFileHeaderRecordPos(lookupIndex);
            final ZipCentralDirectoryFileHeaderRecord centralRecord = this.loadZipCentralDirectoryFileHeaderRecord(pos);
            if (this.hasName(lookupIndex, centralRecord, pos, namePrefix, name)) {
                return new Entry(lookupIndex, centralRecord);
            }
        }
        return null;
    }
    
    public boolean hasEntry(final CharSequence namePrefix, final CharSequence name) {
        for (int nameHash = this.nameHash(namePrefix, name), lookupIndex = this.getFirstLookupIndex(nameHash), size = this.size(); lookupIndex >= 0 && lookupIndex < size && this.nameHashLookups[lookupIndex] == nameHash; ++lookupIndex) {
            final long pos = this.getCentralDirectoryFileHeaderRecordPos(lookupIndex);
            final ZipCentralDirectoryFileHeaderRecord centralRecord = this.loadZipCentralDirectoryFileHeaderRecord(pos);
            if (this.hasName(lookupIndex, centralRecord, pos, namePrefix, name)) {
                return true;
            }
        }
        return false;
    }
    
    public Entry getEntry(final int index) {
        final int lookupIndex = this.lookupIndexes[index];
        final long pos = this.getCentralDirectoryFileHeaderRecordPos(lookupIndex);
        final ZipCentralDirectoryFileHeaderRecord centralRecord = this.loadZipCentralDirectoryFileHeaderRecord(pos);
        return new Entry(lookupIndex, centralRecord);
    }
    
    private ZipCentralDirectoryFileHeaderRecord loadZipCentralDirectoryFileHeaderRecord(final long pos) {
        try {
            return ZipCentralDirectoryFileHeaderRecord.load(this.data, pos);
        }
        catch (final IOException ex) {
            if (ex instanceof ClosedChannelException) {
                throw new IllegalStateException("Zip content closed", ex);
            }
            throw new UncheckedIOException(ex);
        }
    }
    
    private int nameHash(final CharSequence namePrefix, final CharSequence name) {
        int nameHash = 0;
        nameHash = ((namePrefix != null) ? ZipString.hash(nameHash, namePrefix, false) : nameHash);
        nameHash = ZipString.hash(nameHash, name, true);
        return nameHash;
    }
    
    private int getFirstLookupIndex(final int nameHash) {
        int lookupIndex = Arrays.binarySearch(this.nameHashLookups, 0, this.nameHashLookups.length, nameHash);
        if (lookupIndex < 0) {
            return -1;
        }
        while (lookupIndex > 0 && this.nameHashLookups[lookupIndex - 1] == nameHash) {
            --lookupIndex;
        }
        return lookupIndex;
    }
    
    private long getCentralDirectoryFileHeaderRecordPos(final int lookupIndex) {
        return this.centralDirectoryPos + this.relativeCentralDirectoryOffsetLookups[lookupIndex];
    }
    
    private boolean hasName(final int lookupIndex, final ZipCentralDirectoryFileHeaderRecord centralRecord, long pos, final CharSequence namePrefix, final CharSequence name) {
        final int offset = this.nameOffsetLookups.get(lookupIndex);
        pos += 46 + offset;
        int len = centralRecord.fileNameLength() - offset;
        final ByteBuffer buffer = ByteBuffer.allocate(256);
        if (namePrefix != null) {
            final int startsWithNamePrefix = ZipString.startsWith(buffer, this.data, pos, len, namePrefix);
            if (startsWithNamePrefix == -1) {
                return false;
            }
            pos += startsWithNamePrefix;
            len -= startsWithNamePrefix;
        }
        return ZipString.matches(buffer, this.data, pos, len, name, true);
    }
    
    public <I> I getInfo(final Class<I> type, final Function<ZipContent, I> function) {
        Map<Class<?>, Object> info = (this.info != null) ? this.info.get() : null;
        if (info == null) {
            info = new ConcurrentHashMap<Class<?>, Object>();
            this.info = new SoftReference<Map<Class<?>, Object>>(info);
        }
        return (I)info.computeIfAbsent(type, key -> {
            ZipContent.debug.log("Getting %s info from zip '%s'", type.getName(), this);
            return function.apply(this);
        });
    }
    
    public boolean hasJarSignatureFile() {
        return this.hasJarSignatureFile;
    }
    
    @Override
    public void close() throws IOException {
        this.data.close();
    }
    
    @Override
    public String toString() {
        return this.source.toString();
    }
    
    public static ZipContent open(final Path path) throws IOException {
        return open(new Source(path.toAbsolutePath(), null));
    }
    
    public static ZipContent open(final Path path, final String nestedEntryName) throws IOException {
        return open(new Source(path.toAbsolutePath(), nestedEntryName));
    }
    
    private static ZipContent open(final Source source) throws IOException {
        ZipContent zipContent = ZipContent.cache.get(source);
        if (zipContent != null) {
            ZipContent.debug.log("Opening existing cached zip content for %s", zipContent);
            zipContent.data.open();
            return zipContent;
        }
        ZipContent.debug.log("Loading zip content from %s", source);
        zipContent = Loader.load(source);
        final ZipContent previouslyCached = ZipContent.cache.putIfAbsent(source, zipContent);
        if (previouslyCached != null) {
            ZipContent.debug.log("Closing zip content from %s since cache was populated from another thread", source);
            zipContent.close();
            previouslyCached.data.open();
            return previouslyCached;
        }
        return zipContent;
    }
    
    static {
        SIGNATURE_SUFFIX = ".DSA".getBytes(StandardCharsets.UTF_8);
        debug = DebugLogger.get(ZipContent.class);
        cache = new ConcurrentHashMap<Source, ZipContent>();
    }
    
    public enum Kind
    {
        ZIP, 
        NESTED_ZIP, 
        NESTED_DIRECTORY;
    }
    
    record Source(Path path, String nestedEntryName) {
        boolean isNested() {
            return this.nestedEntryName != null;
        }
        
        @Override
        public String toString() {
            return this.isNested() ? (this.path() + "[" + this.nestedEntryName()) : this.path().toString();
        }
    }
    
    private static final class Loader
    {
        private final ByteBuffer buffer;
        private final Source source;
        private final FileDataBlock data;
        private final long centralDirectoryPos;
        private final int[] index;
        private int[] nameHashLookups;
        private int[] relativeCentralDirectoryOffsetLookups;
        private final NameOffsetLookups nameOffsetLookups;
        private int cursor;
        
        private Loader(final Source source, final Entry directoryEntry, final FileDataBlock data, final long centralDirectoryPos, final int maxSize) {
            this.buffer = ByteBuffer.allocate(256);
            this.source = source;
            this.data = data;
            this.centralDirectoryPos = centralDirectoryPos;
            this.index = new int[maxSize];
            this.nameHashLookups = new int[maxSize];
            this.relativeCentralDirectoryOffsetLookups = new int[maxSize];
            this.nameOffsetLookups = ((directoryEntry != null) ? new NameOffsetLookups(directoryEntry.getName().length(), maxSize) : NameOffsetLookups.NONE);
        }
        
        private void add(final ZipCentralDirectoryFileHeaderRecord centralRecord, final long pos, final boolean enableNameOffset) throws IOException {
            final int nameOffset = this.nameOffsetLookups.enable(this.cursor, enableNameOffset);
            final int hash = ZipString.hash(this.buffer, this.data, pos + 46L + nameOffset, centralRecord.fileNameLength() - nameOffset, true);
            this.nameHashLookups[this.cursor] = hash;
            this.relativeCentralDirectoryOffsetLookups[this.cursor] = (int)(pos - this.centralDirectoryPos);
            this.index[this.cursor] = this.cursor;
            ++this.cursor;
        }
        
        private ZipContent finish(final Kind kind, final long commentPos, final long commentLength, final boolean hasJarSignatureFile) {
            if (this.cursor != this.nameHashLookups.length) {
                this.nameHashLookups = Arrays.copyOf(this.nameHashLookups, this.cursor);
                this.relativeCentralDirectoryOffsetLookups = Arrays.copyOf(this.relativeCentralDirectoryOffsetLookups, this.cursor);
            }
            final int size = this.nameHashLookups.length;
            this.sort(0, size - 1);
            final int[] lookupIndexes = new int[size];
            for (int i = 0; i < size; ++i) {
                lookupIndexes[this.index[i]] = i;
            }
            return new ZipContent(this.source, kind, this.data, this.centralDirectoryPos, commentPos, commentLength, lookupIndexes, this.nameHashLookups, this.relativeCentralDirectoryOffsetLookups, this.nameOffsetLookups, hasJarSignatureFile);
        }
        
        private void sort(final int left, final int right) {
            if (left < right) {
                final int pivot = this.nameHashLookups[left + (right - left) / 2];
                int i;
                int j;
                for (i = left, j = right; i <= j; ++i, --j) {
                    while (this.nameHashLookups[i] < pivot) {
                        ++i;
                    }
                    while (this.nameHashLookups[j] > pivot) {
                        --j;
                    }
                    if (i <= j) {
                        this.swap(i, j);
                    }
                }
                if (left < j) {
                    this.sort(left, j);
                }
                if (right > i) {
                    this.sort(i, right);
                }
            }
        }
        
        private void swap(final int i, final int j) {
            swap(this.index, i, j);
            swap(this.nameHashLookups, i, j);
            swap(this.relativeCentralDirectoryOffsetLookups, i, j);
            this.nameOffsetLookups.swap(i, j);
        }
        
        private static void swap(final int[] array, final int i, final int j) {
            final int temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }
        
        static ZipContent load(final Source source) throws IOException {
            if (!source.isNested()) {
                return loadNonNested(source);
            }
            try (final ZipContent zip = ZipContent.open(source.path())) {
                final Entry entry = zip.getEntry(source.nestedEntryName());
                if (entry == null) {
                    throw new IOException("Nested entry '%s' not found in container zip '%s'".formatted(source.nestedEntryName(), source.path()));
                }
                return entry.isDirectory() ? loadNestedDirectory(source, zip, entry) : loadNestedZip(source, entry);
            }
        }
        
        private static ZipContent loadNonNested(final Source source) throws IOException {
            ZipContent.debug.log("Loading non-nested zip '%s'", source.path());
            return openAndLoad(source, Kind.ZIP, new FileDataBlock(source.path()));
        }
        
        private static ZipContent loadNestedZip(final Source source, final Entry entry) throws IOException {
            if (entry.centralRecord.compressionMethod() != 0) {
                throw new IOException("Nested entry '%s' in container zip '%s' must not be compressed".formatted(source.nestedEntryName(), source.path()));
            }
            ZipContent.debug.log("Loading nested zip entry '%s' from '%s'", source.nestedEntryName(), source.path());
            return openAndLoad(source, Kind.NESTED_ZIP, entry.getContent());
        }
        
        private static ZipContent openAndLoad(final Source source, final Kind kind, final FileDataBlock data) throws IOException {
            try {
                data.open();
                return loadContent(source, kind, data);
            }
            catch (final IOException | RuntimeException ex) {
                data.close();
                throw ex;
            }
        }
        
        private static ZipContent loadContent(final Source source, final Kind kind, FileDataBlock data) throws IOException {
            final ZipEndOfCentralDirectoryRecord.Located locatedEocd = ZipEndOfCentralDirectoryRecord.load(data);
            final ZipEndOfCentralDirectoryRecord eocd = locatedEocd.endOfCentralDirectoryRecord();
            final long eocdPos = locatedEocd.pos();
            final Zip64EndOfCentralDirectoryLocator zip64Locator = Zip64EndOfCentralDirectoryLocator.find(data, eocdPos);
            final Zip64EndOfCentralDirectoryRecord zip64Eocd = Zip64EndOfCentralDirectoryRecord.load(data, zip64Locator);
            data = data.slice(getStartOfZipContent(data, eocd, zip64Eocd));
            final long centralDirectoryPos = (zip64Eocd != null) ? zip64Eocd.offsetToStartOfCentralDirectory() : Integer.toUnsignedLong(eocd.offsetToStartOfCentralDirectory());
            final long numberOfEntries = (zip64Eocd != null) ? zip64Eocd.totalNumberOfCentralDirectoryEntries() : Short.toUnsignedInt(eocd.totalNumberOfCentralDirectoryEntries());
            if (numberOfEntries < 0L) {
                throw new IllegalStateException("Invalid number of zip entries in " + source);
            }
            if (numberOfEntries > 2147483647L) {
                throw new IllegalStateException("Too many zip entries in " + source);
            }
            final Loader loader = new Loader(source, null, data, centralDirectoryPos, (int)numberOfEntries);
            final ByteBuffer signatureNameSuffixBuffer = ByteBuffer.allocate(ZipContent.SIGNATURE_SUFFIX.length);
            boolean hasJarSignatureFile = false;
            long pos = centralDirectoryPos;
            for (int i = 0; i < numberOfEntries; ++i) {
                final ZipCentralDirectoryFileHeaderRecord centralRecord = ZipCentralDirectoryFileHeaderRecord.load(data, pos);
                if (!hasJarSignatureFile) {
                    final long filenamePos = pos + 46L;
                    if (centralRecord.fileNameLength() > ZipContent.SIGNATURE_SUFFIX.length && ZipString.startsWith(loader.buffer, data, filenamePos, centralRecord.fileNameLength(), "META-INF/") >= 0) {
                        signatureNameSuffixBuffer.clear();
                        data.readFully(signatureNameSuffixBuffer, filenamePos + centralRecord.fileNameLength() - ZipContent.SIGNATURE_SUFFIX.length);
                        hasJarSignatureFile = Arrays.equals(ZipContent.SIGNATURE_SUFFIX, signatureNameSuffixBuffer.array());
                    }
                }
                loader.add(centralRecord, pos, false);
                pos += centralRecord.size();
            }
            final long commentPos = locatedEocd.pos() + 22L;
            return loader.finish(kind, commentPos, eocd.commentLength(), hasJarSignatureFile);
        }
        
        private static long getStartOfZipContent(final FileDataBlock data, final ZipEndOfCentralDirectoryRecord eocd, final Zip64EndOfCentralDirectoryRecord zip64Eocd) throws IOException {
            final long specifiedOffsetToStartOfCentralDirectory = (zip64Eocd != null) ? zip64Eocd.offsetToStartOfCentralDirectory() : Integer.toUnsignedLong(eocd.offsetToStartOfCentralDirectory());
            final long sizeOfCentralDirectoryAndEndRecords = getSizeOfCentralDirectoryAndEndRecords(eocd, zip64Eocd);
            final long actualOffsetToStartOfCentralDirectory = data.size() - sizeOfCentralDirectoryAndEndRecords;
            return actualOffsetToStartOfCentralDirectory - specifiedOffsetToStartOfCentralDirectory;
        }
        
        private static long getSizeOfCentralDirectoryAndEndRecords(final ZipEndOfCentralDirectoryRecord eocd, final Zip64EndOfCentralDirectoryRecord zip64Eocd) {
            long result = 0L;
            result += eocd.size();
            if (zip64Eocd != null) {
                result += 20L;
                result += zip64Eocd.size();
            }
            result += ((zip64Eocd != null) ? zip64Eocd.sizeOfCentralDirectory() : Integer.toUnsignedLong(eocd.sizeOfCentralDirectory()));
            return result;
        }
        
        private static ZipContent loadNestedDirectory(final Source source, final ZipContent zip, final Entry directoryEntry) throws IOException {
            ZipContent.debug.log("Loading nested directory entry '%s' from '%s'", source.nestedEntryName(), source.path());
            if (!source.nestedEntryName().endsWith("/")) {
                throw new IllegalArgumentException("Nested entry name must end with '/'");
            }
            final String directoryName = directoryEntry.getName();
            zip.data.open();
            try {
                final Loader loader = new Loader(source, directoryEntry, zip.data, zip.centralDirectoryPos, zip.size());
                for (int cursor = 0; cursor < zip.size(); ++cursor) {
                    final int index = zip.lookupIndexes[cursor];
                    if (index != directoryEntry.getLookupIndex()) {
                        final long pos = zip.getCentralDirectoryFileHeaderRecordPos(index);
                        final ZipCentralDirectoryFileHeaderRecord centralRecord = ZipCentralDirectoryFileHeaderRecord.load(zip.data, pos);
                        final long namePos = pos + 46L;
                        final short nameLen = centralRecord.fileNameLength();
                        if (ZipString.startsWith(loader.buffer, zip.data, namePos, nameLen, directoryName) != -1) {
                            loader.add(centralRecord, pos, true);
                        }
                    }
                }
                return loader.finish(Kind.NESTED_DIRECTORY, zip.commentPos, zip.commentLength, zip.hasJarSignatureFile);
            }
            catch (final IOException | RuntimeException ex) {
                zip.data.close();
                throw ex;
            }
        }
    }
    
    public class Entry
    {
        private final int lookupIndex;
        private final ZipCentralDirectoryFileHeaderRecord centralRecord;
        private volatile String name;
        private volatile FileDataBlock content;
        
        Entry(final int lookupIndex, final ZipCentralDirectoryFileHeaderRecord centralRecord) {
            this.lookupIndex = lookupIndex;
            this.centralRecord = centralRecord;
        }
        
        public int getLookupIndex() {
            return this.lookupIndex;
        }
        
        public boolean isDirectory() {
            return this.getName().endsWith("/");
        }
        
        public boolean hasNameStartingWith(final CharSequence prefix) {
            final String name = this.name;
            if (name != null) {
                return name.startsWith(prefix.toString());
            }
            final long pos = ZipContent.this.getCentralDirectoryFileHeaderRecordPos(this.lookupIndex) + 46L;
            return ZipString.startsWith(null, ZipContent.this.data, pos, this.centralRecord.fileNameLength(), prefix) != -1;
        }
        
        public String getName() {
            String name = this.name;
            if (name == null) {
                final int offset = ZipContent.this.nameOffsetLookups.get(this.lookupIndex);
                final long pos = ZipContent.this.getCentralDirectoryFileHeaderRecordPos(this.lookupIndex) + 46L + offset;
                name = ZipString.readString(ZipContent.this.data, pos, this.centralRecord.fileNameLength() - offset);
                this.name = name;
            }
            return name;
        }
        
        public int getCompressionMethod() {
            return this.centralRecord.compressionMethod();
        }
        
        public int getUncompressedSize() {
            return this.centralRecord.uncompressedSize();
        }
        
        public CloseableDataBlock openContent() throws IOException {
            final FileDataBlock content = this.getContent();
            content.open();
            return content;
        }
        
        private FileDataBlock getContent() throws IOException {
            FileDataBlock content = this.content;
            if (content == null) {
                final long pos = Integer.toUnsignedLong(this.centralRecord.offsetToLocalHeader());
                this.checkNotZip64Extended(pos);
                final ZipLocalFileHeaderRecord localHeader = ZipLocalFileHeaderRecord.load(ZipContent.this.data, pos);
                final long size = Integer.toUnsignedLong(this.centralRecord.compressedSize());
                this.checkNotZip64Extended(size);
                content = ZipContent.this.data.slice(pos + localHeader.size(), size);
                this.content = content;
            }
            return content;
        }
        
        private void checkNotZip64Extended(final long value) throws IOException {
            if (value == -1L) {
                throw new IOException("Zip64 extended information extra fields are not supported");
            }
        }
        
        public <E extends ZipEntry> E as(final Function<String, E> factory) {
            return this.as((entry, name) -> factory.apply(name));
        }
        
        public <E extends ZipEntry> E as(final BiFunction<Entry, String, E> factory) {
            try {
                final E result = factory.apply(this, this.getName());
                final long pos = ZipContent.this.getCentralDirectoryFileHeaderRecordPos(this.lookupIndex);
                this.centralRecord.copyTo(ZipContent.this.data, pos, result);
                return result;
            }
            catch (final IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }
    }
}
