// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.zip;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.ByteBuffer;
import org.springframework.boot.loader.log.DebugLogger;

record Zip64EndOfCentralDirectoryRecord(long size, long sizeOfZip64EndOfCentralDirectoryRecord, short versionMadeBy, short versionNeededToExtract, int numberOfThisDisk, int diskWhereCentralDirectoryStarts, long numberOfCentralDirectoryEntriesOnThisDisk, long totalNumberOfCentralDirectoryEntries, long sizeOfCentralDirectory, long offsetToStartOfCentralDirectory) {
    private static final DebugLogger debug;
    private static final int SIGNATURE = 101075792;
    private static final int MINIMUM_SIZE = 56;
    
    static Zip64EndOfCentralDirectoryRecord load(final DataBlock dataBlock, final Zip64EndOfCentralDirectoryLocator locator) throws IOException {
        if (locator == null) {
            return null;
        }
        final ByteBuffer buffer = ByteBuffer.allocate(56);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        final long size = locator.pos() - locator.offsetToZip64EndOfCentralDirectoryRecord();
        final long pos = locator.pos() - size;
        Zip64EndOfCentralDirectoryRecord.debug.log("Loading Zip64EndOfCentralDirectoryRecord from position %s size %s", pos, size);
        dataBlock.readFully(buffer, pos);
        buffer.rewind();
        final int signature = buffer.getInt();
        if (signature != 101075792) {
            Zip64EndOfCentralDirectoryRecord.debug.log("Found incorrect Zip64EndOfCentralDirectoryRecord signature %s at position %s", signature, pos);
            throw new IOException("Zip64 'End Of Central Directory Record' not found at position " + pos + ". Zip file is corrupt or includes prefixed bytes which are not supported with Zip64 files");
        }
        return new Zip64EndOfCentralDirectoryRecord(size, buffer.getLong(), buffer.getShort(), buffer.getShort(), buffer.getInt(), buffer.getInt(), buffer.getLong(), buffer.getLong(), buffer.getLong(), buffer.getLong());
    }
    
    static {
        debug = DebugLogger.get(Zip64EndOfCentralDirectoryRecord.class);
    }
}
