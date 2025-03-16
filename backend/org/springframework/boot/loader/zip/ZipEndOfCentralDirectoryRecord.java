// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.zip;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.ByteBuffer;
import org.springframework.boot.loader.log.DebugLogger;

record ZipEndOfCentralDirectoryRecord(short numberOfThisDisk, short diskWhereCentralDirectoryStarts, short numberOfCentralDirectoryEntriesOnThisDisk, short totalNumberOfCentralDirectoryEntries, int sizeOfCentralDirectory, int offsetToStartOfCentralDirectory, short commentLength) {
    private static final DebugLogger debug;
    private static final int SIGNATURE = 101010256;
    private static final int MAXIMUM_COMMENT_LENGTH = 65535;
    private static final int MINIMUM_SIZE = 22;
    private static final int MAXIMUM_SIZE = 65557;
    static final int BUFFER_SIZE = 256;
    static final int COMMENT_OFFSET = 22;
    
    ZipEndOfCentralDirectoryRecord(final short totalNumberOfCentralDirectoryEntries, final int sizeOfCentralDirectory, final int offsetToStartOfCentralDirectory) {
        this((short)0, (short)0, totalNumberOfCentralDirectoryEntries, totalNumberOfCentralDirectoryEntries, sizeOfCentralDirectory, offsetToStartOfCentralDirectory, (short)0);
    }
    
    long size() {
        return 22 + this.commentLength;
    }
    
    byte[] asByteArray() {
        final ByteBuffer buffer = ByteBuffer.allocate(22);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(101010256);
        buffer.putShort(this.numberOfThisDisk);
        buffer.putShort(this.diskWhereCentralDirectoryStarts);
        buffer.putShort(this.numberOfCentralDirectoryEntriesOnThisDisk);
        buffer.putShort(this.totalNumberOfCentralDirectoryEntries);
        buffer.putInt(this.sizeOfCentralDirectory);
        buffer.putInt(this.offsetToStartOfCentralDirectory);
        buffer.putShort(this.commentLength);
        return buffer.array();
    }
    
    static Located load(final DataBlock dataBlock) throws IOException {
        final ByteBuffer buffer = ByteBuffer.allocate(256);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        final long pos = locate(dataBlock, buffer);
        return new Located(pos, new ZipEndOfCentralDirectoryRecord(buffer.getShort(), buffer.getShort(), buffer.getShort(), buffer.getShort(), buffer.getInt(), buffer.getInt(), buffer.getShort()));
    }
    
    private static long locate(final DataBlock dataBlock, final ByteBuffer buffer) throws IOException {
        long endPos = dataBlock.size();
        ZipEndOfCentralDirectoryRecord.debug.log("Finding EndOfCentralDirectoryRecord starting at end position %s", endPos);
        while (endPos > 0L) {
            buffer.clear();
            final long totalRead = dataBlock.size() - endPos;
            if (totalRead > 65557L) {
                throw new IOException("Zip 'End Of Central Directory Record' not found after reading " + totalRead + " bytes");
            }
            long startPos = endPos - buffer.limit();
            if (startPos < 0L) {
                buffer.limit((int)startPos + buffer.limit());
                startPos = 0L;
            }
            ZipEndOfCentralDirectoryRecord.debug.log("Finding EndOfCentralDirectoryRecord from %s with limit %s", startPos, buffer.limit());
            dataBlock.readFully(buffer, startPos);
            final int offset = findInBuffer(buffer);
            if (offset >= 0) {
                ZipEndOfCentralDirectoryRecord.debug.log("Found EndOfCentralDirectoryRecord at %s + %s", startPos, offset);
                return startPos + offset;
            }
            endPos = endPos - 256L + 22L;
        }
        throw new IOException("Zip 'End Of Central Directory Record' not found after reading entire data block");
    }
    
    private static int findInBuffer(final ByteBuffer buffer) {
        for (int pos = buffer.limit() - 4; pos >= 0; --pos) {
            buffer.position(pos);
            if (buffer.getInt() == 101010256) {
                return pos;
            }
        }
        return -1;
    }
    
    static {
        debug = DebugLogger.get(ZipEndOfCentralDirectoryRecord.class);
    }
    
    record Located(long pos, ZipEndOfCentralDirectoryRecord endOfCentralDirectoryRecord) {}
}
