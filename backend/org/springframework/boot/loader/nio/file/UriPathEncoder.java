// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.nio.file;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

final class UriPathEncoder
{
    private static final char[] ALLOWED;
    
    private UriPathEncoder() {
    }
    
    static String encode(final String path) {
        final byte[] bytes2;
        final byte[] bytes = bytes2 = path.getBytes(StandardCharsets.UTF_8);
        for (final byte b : bytes2) {
            if (!isAllowed(b)) {
                return encode(bytes);
            }
        }
        return path;
    }
    
    private static String encode(final byte[] bytes) {
        final ByteArrayOutputStream result = new ByteArrayOutputStream(bytes.length);
        for (final byte b : bytes) {
            if (isAllowed(b)) {
                result.write(b);
            }
            else {
                result.write(37);
                result.write(Character.toUpperCase(Character.forDigit(b >> 4 & 0xF, 16)));
                result.write(Character.toUpperCase(Character.forDigit(b & 0xF, 16)));
            }
        }
        return result.toString(StandardCharsets.UTF_8);
    }
    
    private static boolean isAllowed(final int ch) {
        for (final char allowed : UriPathEncoder.ALLOWED) {
            if (ch == allowed) {
                return true;
            }
        }
        return isAlpha(ch) || isDigit(ch);
    }
    
    private static boolean isAlpha(final int ch) {
        return (ch >= 97 && ch <= 122) || (ch >= 65 && ch <= 90);
    }
    
    private static boolean isDigit(final int ch) {
        return ch >= 48 && ch <= 57;
    }
    
    static {
        ALLOWED = "/:@-._~!$&'()*+,;=".toCharArray();
    }
}
