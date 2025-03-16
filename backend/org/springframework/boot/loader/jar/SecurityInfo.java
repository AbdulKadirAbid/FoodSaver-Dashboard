// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.jar;

import java.io.InputStream;
import java.util.jar.JarEntry;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.springframework.boot.loader.zip.ZipContent;
import java.security.CodeSigner;
import java.security.cert.Certificate;

final class SecurityInfo
{
    static final SecurityInfo NONE;
    private final Certificate[][] certificateLookups;
    private final CodeSigner[][] codeSignerLookups;
    
    private SecurityInfo(final Certificate[][] entryCertificates, final CodeSigner[][] entryCodeSigners) {
        this.certificateLookups = entryCertificates;
        this.codeSignerLookups = entryCodeSigners;
    }
    
    Certificate[] getCertificates(final ZipContent.Entry contentEntry) {
        return (Certificate[])((this.certificateLookups != null) ? ((Certificate[])this.clone(this.certificateLookups[contentEntry.getLookupIndex()])) : null);
    }
    
    CodeSigner[] getCodeSigners(final ZipContent.Entry contentEntry) {
        return (CodeSigner[])((this.codeSignerLookups != null) ? ((CodeSigner[])this.clone(this.codeSignerLookups[contentEntry.getLookupIndex()])) : null);
    }
    
    private <T> T[] clone(final T[] array) {
        return (T[])((array != null) ? ((T[])array.clone()) : null);
    }
    
    static SecurityInfo get(final ZipContent content) {
        if (!content.hasJarSignatureFile()) {
            return SecurityInfo.NONE;
        }
        try {
            return load(content);
        }
        catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
    
    private static SecurityInfo load(final ZipContent content) throws IOException {
        final int size = content.size();
        boolean hasSecurityInfo = false;
        final Certificate[][] entryCertificates = new Certificate[size][];
        final CodeSigner[][] entryCodeSigners = new CodeSigner[size][];
        try (final JarEntriesStream entries = new JarEntriesStream(content.openRawZipData().asInputStream())) {
            for (JarEntry entry = entries.getNextEntry(); entry != null; entry = entries.getNextEntry()) {
                final ZipContent.Entry relatedEntry = content.getEntry(entry.getName());
                if (relatedEntry != null && entries.matches(relatedEntry.isDirectory(), relatedEntry.getUncompressedSize(), relatedEntry.getCompressionMethod(), () -> relatedEntry.openContent().asInputStream())) {
                    final Certificate[] certificates = entry.getCertificates();
                    final CodeSigner[] codeSigners = entry.getCodeSigners();
                    if (certificates != null || codeSigners != null) {
                        hasSecurityInfo = true;
                        entryCertificates[relatedEntry.getLookupIndex()] = certificates;
                        entryCodeSigners[relatedEntry.getLookupIndex()] = codeSigners;
                    }
                }
            }
        }
        return hasSecurityInfo ? new SecurityInfo(entryCertificates, entryCodeSigners) : SecurityInfo.NONE;
    }
    
    static {
        NONE = new SecurityInfo(null, null);
    }
}
