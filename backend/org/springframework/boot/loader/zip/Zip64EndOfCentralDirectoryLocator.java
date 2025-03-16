// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.zip;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.ByteBuffer;
import org.springframework.boot.loader.log.DebugLogger;

record Zip64EndOfCentralDirectoryLocator(long pos, int numberOfThisDisk, long offsetToZip64EndOfCentralDirectoryRecord, int totalNumberOfDisks) {
    private static final DebugLogger debug;
    private static final int SIGNATURE = 117853008;
    static final int SIZE = 20;
    
    static Zip64EndOfCentralDirectoryLocator find(final DataBlock dataBlock, final long endOfCentralDirectoryPos) throws IOException {
        Zip64EndOfCentralDirectoryLocator.debug.log("Finding Zip64EndOfCentralDirectoryLocator from EOCD at %s", endOfCentralDirectoryPos);
        final long pos = endOfCentralDirectoryPos - 20L;
        if (pos < 0L) {
            Zip64EndOfCentralDirectoryLocator.debug.log("No Zip64EndOfCentralDirectoryLocator due to negative position %s", pos);
            return null;
        }
        final ByteBuffer buffer = ByteBuffer.allocate(20);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        dataBlock.read(buffer, pos);
        buffer.rewind();
        final int signature = buffer.getInt();
        if (signature != 117853008) {
            Zip64EndOfCentralDirectoryLocator.debug.log("Found incorrect Zip64EndOfCentralDirectoryLocator signature %s at position %s", signature, pos);
            return null;
        }
        Zip64EndOfCentralDirectoryLocator.debug.log("Found Zip64EndOfCentralDirectoryLocator at position %s", pos);
        return new Zip64EndOfCentralDirectoryLocator(pos, buffer.getInt(), buffer.getLong(), buffer.getInt());
    }
    
    static {
        debug = DebugLogger.get(Zip64EndOfCentralDirectoryLocator.class);
    }
}
