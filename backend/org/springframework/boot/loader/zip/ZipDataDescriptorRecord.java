// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.zip;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.ByteBuffer;
import org.springframework.boot.loader.log.DebugLogger;

record ZipDataDescriptorRecord(boolean includeSignature, int crc32, int compressedSize, int uncompressedSize) {
    private static final DebugLogger debug;
    private static final int SIGNATURE = 134695760;
    private static final int DATA_SIZE = 12;
    private static final int SIGNATURE_SIZE = 4;
    
    long size() {
        return this.includeSignature() ? 16L : 12L;
    }
    
    byte[] asByteArray() {
        final ByteBuffer buffer = ByteBuffer.allocate((int)this.size());
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        if (this.includeSignature) {
            buffer.putInt(134695760);
        }
        buffer.putInt(this.crc32);
        buffer.putInt(this.compressedSize);
        buffer.putInt(this.uncompressedSize);
        return buffer.array();
    }
    
    static ZipDataDescriptorRecord load(final DataBlock dataBlock, final long pos) throws IOException {
        ZipDataDescriptorRecord.debug.log("Loading ZipDataDescriptorRecord from position %s", pos);
        final ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.limit(4);
        dataBlock.readFully(buffer, pos);
        buffer.rewind();
        final int signatureOrCrc = buffer.getInt();
        final boolean hasSignature = signatureOrCrc == 134695760;
        buffer.rewind();
        buffer.limit(hasSignature ? 12 : 8);
        dataBlock.readFully(buffer, pos + 4L);
        buffer.rewind();
        return new ZipDataDescriptorRecord(hasSignature, hasSignature ? buffer.getInt() : signatureOrCrc, buffer.getInt(), buffer.getInt());
    }
    
    static boolean isPresentBasedOnFlag(final ZipLocalFileHeaderRecord localRecord) {
        return isPresentBasedOnFlag(localRecord.generalPurposeBitFlag());
    }
    
    static boolean isPresentBasedOnFlag(final ZipCentralDirectoryFileHeaderRecord centralRecord) {
        return isPresentBasedOnFlag(centralRecord.generalPurposeBitFlag());
    }
    
    static boolean isPresentBasedOnFlag(final int generalPurposeBitFlag) {
        return (generalPurposeBitFlag & 0x8) != 0x0;
    }
    
    static {
        debug = DebugLogger.get(ZipDataDescriptorRecord.class);
    }
}
