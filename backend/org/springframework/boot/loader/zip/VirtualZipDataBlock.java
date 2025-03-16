// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.zip;

import java.nio.ByteBuffer;
import java.io.IOException;
import java.util.List;
import java.util.Collection;
import java.util.ArrayList;

class VirtualZipDataBlock extends VirtualDataBlock implements CloseableDataBlock
{
    private final CloseableDataBlock data;
    
    VirtualZipDataBlock(final CloseableDataBlock data, final NameOffsetLookups nameOffsetLookups, final ZipCentralDirectoryFileHeaderRecord[] centralRecords, final long[] centralRecordPositions) throws IOException {
        this.data = data;
        final List<DataBlock> parts = new ArrayList<DataBlock>();
        final List<DataBlock> centralParts = new ArrayList<DataBlock>();
        long offset = 0L;
        long sizeOfCentralDirectory = 0L;
        for (int i = 0; i < centralRecords.length; ++i) {
            final ZipCentralDirectoryFileHeaderRecord centralRecord = centralRecords[i];
            final int nameOffset = nameOffsetLookups.get(i);
            final long centralRecordPos = centralRecordPositions[i];
            final DataBlock name = new DataPart(centralRecordPos + 46L + nameOffset, Short.toUnsignedLong(centralRecord.fileNameLength()) - nameOffset);
            final long localRecordPos = Integer.toUnsignedLong(centralRecord.offsetToLocalHeader());
            final ZipLocalFileHeaderRecord localRecord = ZipLocalFileHeaderRecord.load(this.data, localRecordPos);
            final DataBlock content = new DataPart(localRecordPos + localRecord.size(), centralRecord.compressedSize());
            final boolean hasDescriptorRecord = ZipDataDescriptorRecord.isPresentBasedOnFlag(centralRecord);
            final ZipDataDescriptorRecord dataDescriptorRecord = hasDescriptorRecord ? ZipDataDescriptorRecord.load(data, localRecordPos + localRecord.size() + content.size()) : null;
            sizeOfCentralDirectory += this.addToCentral(centralParts, centralRecord, centralRecordPos, name, (int)offset);
            offset += this.addToLocal(parts, centralRecord, localRecord, dataDescriptorRecord, name, content);
        }
        parts.addAll(centralParts);
        final ZipEndOfCentralDirectoryRecord eocd = new ZipEndOfCentralDirectoryRecord((short)centralRecords.length, (int)sizeOfCentralDirectory, (int)offset);
        parts.add(new ByteArrayDataBlock(eocd.asByteArray()));
        this.setParts(parts);
    }
    
    private long addToCentral(final List<DataBlock> parts, final ZipCentralDirectoryFileHeaderRecord originalRecord, final long originalRecordPos, final DataBlock name, final int offsetToLocalHeader) throws IOException {
        final ZipCentralDirectoryFileHeaderRecord record = originalRecord.withFileNameLength((short)(name.size() & 0xFFFFL)).withOffsetToLocalHeader(offsetToLocalHeader);
        final int originalExtraFieldLength = Short.toUnsignedInt(originalRecord.extraFieldLength());
        final int originalFileCommentLength = Short.toUnsignedInt(originalRecord.fileCommentLength());
        final int extraFieldAndCommentSize = originalExtraFieldLength + originalFileCommentLength;
        parts.add(new ByteArrayDataBlock(record.asByteArray()));
        parts.add(name);
        if (extraFieldAndCommentSize > 0) {
            parts.add(new DataPart(originalRecordPos + originalRecord.size() - extraFieldAndCommentSize, extraFieldAndCommentSize));
        }
        return record.size();
    }
    
    private long addToLocal(final List<DataBlock> parts, final ZipCentralDirectoryFileHeaderRecord centralRecord, final ZipLocalFileHeaderRecord originalRecord, final ZipDataDescriptorRecord dataDescriptorRecord, final DataBlock name, final DataBlock content) throws IOException {
        final ZipLocalFileHeaderRecord record = originalRecord.withFileNameLength((short)(name.size() & 0xFFFFL));
        final long originalRecordPos = Integer.toUnsignedLong(centralRecord.offsetToLocalHeader());
        final int extraFieldLength = Short.toUnsignedInt(originalRecord.extraFieldLength());
        parts.add(new ByteArrayDataBlock(record.asByteArray()));
        parts.add(name);
        if (extraFieldLength > 0) {
            parts.add(new DataPart(originalRecordPos + originalRecord.size() - extraFieldLength, extraFieldLength));
        }
        parts.add(content);
        if (dataDescriptorRecord != null) {
            parts.add(new ByteArrayDataBlock(dataDescriptorRecord.asByteArray()));
        }
        return record.size() + content.size() + ((dataDescriptorRecord != null) ? dataDescriptorRecord.size() : 0L);
    }
    
    @Override
    public void close() throws IOException {
        this.data.close();
    }
    
    final class DataPart implements DataBlock
    {
        private final long offset;
        private final long size;
        
        DataPart(final long offset, final long size) {
            this.offset = offset;
            this.size = size;
        }
        
        @Override
        public long size() throws IOException {
            return this.size;
        }
        
        @Override
        public int read(final ByteBuffer dst, final long pos) throws IOException {
            final int remaining = (int)(this.size - pos);
            if (remaining <= 0) {
                return -1;
            }
            int originalLimit = -1;
            if (dst.remaining() > remaining) {
                originalLimit = dst.limit();
                dst.limit(dst.position() + remaining);
            }
            final int result = VirtualZipDataBlock.this.data.read(dst, this.offset + pos);
            if (originalLimit != -1) {
                dst.limit(originalLimit);
            }
            return result;
        }
    }
}
