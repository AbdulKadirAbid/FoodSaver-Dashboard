// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.zip;

import java.io.EOFException;
import java.nio.charset.StandardCharsets;
import java.nio.ByteOrder;
import java.io.UncheckedIOException;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.springframework.boot.loader.log.DebugLogger;

final class ZipString
{
    private static final DebugLogger debug;
    static final int BUFFER_SIZE = 256;
    private static final int[] INITIAL_BYTE_BITMASK;
    private static final int SUBSEQUENT_BYTE_BITMASK = 63;
    private static final int EMPTY_HASH;
    private static final int EMPTY_SLASH_HASH;
    
    private ZipString() {
    }
    
    static int hash(final CharSequence charSequence, final boolean addEndSlash) {
        return hash(0, charSequence, addEndSlash);
    }
    
    static int hash(final int initialHash, final CharSequence charSequence, final boolean addEndSlash) {
        if (charSequence == null || charSequence.isEmpty()) {
            return addEndSlash ? ZipString.EMPTY_SLASH_HASH : ZipString.EMPTY_HASH;
        }
        final boolean endsWithSlash = charSequence.charAt(charSequence.length() - 1) == '/';
        int hash = initialHash;
        if (charSequence instanceof String && initialHash == 0) {
            hash = charSequence.hashCode();
        }
        else {
            for (int i = 0; i < charSequence.length(); ++i) {
                final char ch = charSequence.charAt(i);
                hash = 31 * hash + ch;
            }
        }
        hash = ((addEndSlash && !endsWithSlash) ? (31 * hash + 47) : hash);
        ZipString.debug.log("%s calculated for charsequence '%s' (addEndSlash=%s)", hash, charSequence, endsWithSlash);
        return hash;
    }
    
    static int hash(ByteBuffer buffer, final DataBlock dataBlock, long pos, int len, final boolean addEndSlash) throws IOException {
        if (len == 0) {
            return addEndSlash ? ZipString.EMPTY_SLASH_HASH : ZipString.EMPTY_HASH;
        }
        buffer = ((buffer != null) ? buffer : ByteBuffer.allocate(256));
        final byte[] bytes = buffer.array();
        int hash = 0;
        char lastChar = '\0';
        int codePointSize = 1;
        while (len > 0) {
            for (int count = readInBuffer(dataBlock, pos, buffer, len, codePointSize), byteIndex = 0; byteIndex < count; byteIndex += codePointSize, pos += codePointSize, len -= codePointSize, codePointSize = 1) {
                codePointSize = getCodePointSize(bytes, byteIndex);
                if (!hasEnoughBytes(byteIndex, codePointSize, count)) {
                    break;
                }
                final int codePoint = getCodePoint(bytes, byteIndex, codePointSize);
                if (codePoint <= 65535) {
                    lastChar = (char)(codePoint & 0xFFFF);
                    hash = 31 * hash + lastChar;
                }
                else {
                    lastChar = '\0';
                    hash = 31 * hash + Character.highSurrogate(codePoint);
                    hash = 31 * hash + Character.lowSurrogate(codePoint);
                }
            }
        }
        hash = ((addEndSlash && lastChar != '/') ? (31 * hash + 47) : hash);
        ZipString.debug.log("%08X calculated for datablock position %s size %s (addEndSlash=%s)", hash, pos, len, addEndSlash);
        return hash;
    }
    
    static boolean matches(ByteBuffer buffer, final DataBlock dataBlock, final long pos, final int len, final CharSequence charSequence, final boolean addSlash) {
        if (charSequence.isEmpty()) {
            return true;
        }
        buffer = ((buffer != null) ? buffer : ByteBuffer.allocate(256));
        try {
            return compare(buffer, dataBlock, pos, len, charSequence, addSlash ? CompareType.MATCHES_ADDING_SLASH : CompareType.MATCHES) != -1;
        }
        catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
    
    static int startsWith(ByteBuffer buffer, final DataBlock dataBlock, final long pos, final int len, final CharSequence charSequence) {
        if (charSequence.isEmpty()) {
            return 0;
        }
        buffer = ((buffer != null) ? buffer : ByteBuffer.allocate(256));
        try {
            return compare(buffer, dataBlock, pos, len, charSequence, CompareType.STARTS_WITH);
        }
        catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
    
    private static int compare(final ByteBuffer buffer, final DataBlock dataBlock, long pos, int len, final CharSequence charSequence, final CompareType compareType) throws IOException {
        if (charSequence.isEmpty()) {
            return 0;
        }
        final boolean addSlash = compareType == CompareType.MATCHES_ADDING_SLASH && !endsWith(charSequence, '/');
        int charSequenceIndex = 0;
        final int maxCharSequenceLength = addSlash ? (charSequence.length() + 1) : charSequence.length();
        int result = 0;
        final byte[] bytes = buffer.array();
        int codePointSize = 1;
        while (len > 0) {
            final int count = readInBuffer(dataBlock, pos, buffer, len, codePointSize);
            int byteIndex = 0;
            while (byteIndex < count) {
                codePointSize = getCodePointSize(bytes, byteIndex);
                if (!hasEnoughBytes(byteIndex, codePointSize, count)) {
                    break;
                }
                final int codePoint = getCodePoint(bytes, byteIndex, codePointSize);
                if (codePoint <= 65535) {
                    final char ch = (char)(codePoint & 0xFFFF);
                    if (charSequenceIndex >= maxCharSequenceLength || getChar(charSequence, charSequenceIndex++) != ch) {
                        return -1;
                    }
                }
                else {
                    char ch = Character.highSurrogate(codePoint);
                    if (charSequenceIndex >= maxCharSequenceLength || getChar(charSequence, charSequenceIndex++) != ch) {
                        return -1;
                    }
                    ch = Character.lowSurrogate(codePoint);
                    if (charSequenceIndex >= charSequence.length() || getChar(charSequence, charSequenceIndex++) != ch) {
                        return -1;
                    }
                }
                byteIndex += codePointSize;
                pos += codePointSize;
                len -= codePointSize;
                result += codePointSize;
                codePointSize = 1;
                if (compareType == CompareType.STARTS_WITH && charSequenceIndex >= charSequence.length()) {
                    return result;
                }
            }
        }
        return (charSequenceIndex >= charSequence.length()) ? result : -1;
    }
    
    private static boolean hasEnoughBytes(final int byteIndex, final int codePointSize, final int count) {
        return byteIndex + codePointSize - 1 < count;
    }
    
    private static boolean endsWith(final CharSequence charSequence, final char ch) {
        return !charSequence.isEmpty() && charSequence.charAt(charSequence.length() - 1) == ch;
    }
    
    private static char getChar(final CharSequence charSequence, final int index) {
        return (index != charSequence.length()) ? charSequence.charAt(index) : '/';
    }
    
    static String readString(final DataBlock data, final long pos, final long len) {
        try {
            if (len > 2147483647L) {
                throw new IllegalStateException("String is too long to read");
            }
            final ByteBuffer buffer = ByteBuffer.allocate((int)len);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            data.readFully(buffer, pos);
            return new String(buffer.array(), StandardCharsets.UTF_8);
        }
        catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
    
    private static int readInBuffer(final DataBlock dataBlock, long pos, final ByteBuffer buffer, final int maxLen, final int minLen) throws IOException {
        buffer.clear();
        if (buffer.remaining() > maxLen) {
            buffer.limit(maxLen);
        }
        int result;
        int count;
        for (result = 0; result < minLen; result += count, pos += count) {
            count = dataBlock.read(buffer, pos);
            if (count <= 0) {
                throw new EOFException();
            }
        }
        return result;
    }
    
    private static int getCodePointSize(final byte[] bytes, final int i) {
        final int b = Byte.toUnsignedInt(bytes[i]);
        if ((b & 0x80) == 0x0) {
            return 1;
        }
        if ((b & 0xE0) == 0xC0) {
            return 2;
        }
        if ((b & 0xF0) == 0xE0) {
            return 3;
        }
        return 4;
    }
    
    private static int getCodePoint(final byte[] bytes, final int i, final int codePointSize) {
        int codePoint = Byte.toUnsignedInt(bytes[i]);
        codePoint &= ZipString.INITIAL_BYTE_BITMASK[codePointSize - 1];
        for (int j = 1; j < codePointSize; ++j) {
            codePoint = (codePoint << 6) + (bytes[i + j] & 0x3F);
        }
        return codePoint;
    }
    
    static {
        debug = DebugLogger.get(ZipString.class);
        INITIAL_BYTE_BITMASK = new int[] { 127, 31, 15, 7 };
        EMPTY_HASH = "".hashCode();
        EMPTY_SLASH_HASH = "/".hashCode();
    }
    
    private enum CompareType
    {
        MATCHES, 
        MATCHES_ADDING_SLASH, 
        STARTS_WITH;
    }
}
