// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.net.util;

import java.nio.charset.CoderResult;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.CharBuffer;
import java.nio.ByteBuffer;

public final class UrlDecoder
{
    private UrlDecoder() {
    }
    
    public static String decode(final String string) {
        final int length = string.length();
        if (length == 0 || string.indexOf(37) < 0) {
            return string;
        }
        final StringBuilder result = new StringBuilder(length);
        final ByteBuffer byteBuffer = ByteBuffer.allocate(length);
        final CharBuffer charBuffer = CharBuffer.allocate(length);
        final CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
        int index = 0;
        while (index < length) {
            final char ch = string.charAt(index);
            if (ch != '%') {
                result.append(ch);
                if (index + 1 >= length) {
                    return result.toString();
                }
                ++index;
            }
            else {
                index = fillByteBuffer(byteBuffer, string, index, length);
                decodeToCharBuffer(byteBuffer, charBuffer, decoder);
                result.append(charBuffer.flip());
            }
        }
        return result.toString();
    }
    
    private static int fillByteBuffer(final ByteBuffer byteBuffer, final String string, int index, final int length) {
        byteBuffer.clear();
        do {
            byteBuffer.put(unescape(string, index));
            index += 3;
        } while (index < length && string.charAt(index) == '%');
        byteBuffer.flip();
        return index;
    }
    
    private static byte unescape(final String string, final int index) {
        try {
            return (byte)Integer.parseInt(string, index + 1, index + 3, 16);
        }
        catch (final NumberFormatException ex) {
            throw new IllegalArgumentException();
        }
    }
    
    private static void decodeToCharBuffer(final ByteBuffer byteBuffer, final CharBuffer charBuffer, final CharsetDecoder decoder) {
        decoder.reset();
        charBuffer.clear();
        assertNoError(decoder.decode(byteBuffer, charBuffer, true));
        assertNoError(decoder.flush(charBuffer));
    }
    
    private static void assertNoError(final CoderResult result) {
        if (result.isError()) {
            throw new IllegalArgumentException("Error decoding percent encoded characters");
        }
    }
}
