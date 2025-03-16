// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.zip;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.ByteBuffer;
import org.springframework.boot.loader.log.DebugLogger;

record ZipLocalFileHeaderRecord(short versionNeededToExtract, short generalPurposeBitFlag, short compressionMethod, short lastModFileTime, short lastModFileDate, int crc32, int compressedSize, int uncompressedSize, short fileNameLength, short extraFieldLength) {
    private static final DebugLogger debug;
    private static final int SIGNATURE = 67324752;
    private static final int MINIMUM_SIZE = 30;
    
    long size() {
        return 30 + this.fileNameLength() + this.extraFieldLength();
    }
    
    ZipLocalFileHeaderRecord withExtraFieldLength(final short extraFieldLength) {
        return new ZipLocalFileHeaderRecord(this.versionNeededToExtract, this.generalPurposeBitFlag, this.compressionMethod, this.lastModFileTime, this.lastModFileDate, this.crc32, this.compressedSize, this.uncompressedSize, this.fileNameLength, extraFieldLength);
    }
    
    ZipLocalFileHeaderRecord withFileNameLength(final short fileNameLength) {
        return new ZipLocalFileHeaderRecord(this.versionNeededToExtract, this.generalPurposeBitFlag, this.compressionMethod, this.lastModFileTime, this.lastModFileDate, this.crc32, this.compressedSize, this.uncompressedSize, fileNameLength, this.extraFieldLength);
    }
    
    byte[] asByteArray() {
        final ByteBuffer buffer = ByteBuffer.allocate(30);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(67324752);
        buffer.putShort(this.versionNeededToExtract);
        buffer.putShort(this.generalPurposeBitFlag);
        buffer.putShort(this.compressionMethod);
        buffer.putShort(this.lastModFileTime);
        buffer.putShort(this.lastModFileDate);
        buffer.putInt(this.crc32);
        buffer.putInt(this.compressedSize);
        buffer.putInt(this.uncompressedSize);
        buffer.putShort(this.fileNameLength);
        buffer.putShort(this.extraFieldLength);
        return buffer.array();
    }
    
    static ZipLocalFileHeaderRecord load(final DataBlock dataBlock, final long pos) throws IOException {
        ZipLocalFileHeaderRecord.debug.log("Loading LocalFileHeaderRecord from position %s", pos);
        final ByteBuffer buffer = ByteBuffer.allocate(30);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        dataBlock.readFully(buffer, pos);
        buffer.rewind();
        if (buffer.getInt() != 67324752) {
            throw new IOException("Zip 'Local File Header Record' not found at position " + pos);
        }
        return new ZipLocalFileHeaderRecord(buffer.getShort(), buffer.getShort(), buffer.getShort(), buffer.getShort(), buffer.getShort(), buffer.getInt(), buffer.getInt(), buffer.getInt(), buffer.getShort(), buffer.getShort());
    }
    
    static {
        debug = DebugLogger.get(ZipLocalFileHeaderRecord.class);
    }
}
