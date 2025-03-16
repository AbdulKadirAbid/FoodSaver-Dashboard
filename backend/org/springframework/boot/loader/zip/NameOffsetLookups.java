// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.zip;

import java.util.BitSet;

class NameOffsetLookups
{
    public static final NameOffsetLookups NONE;
    private final int offset;
    private final BitSet enabled;
    
    NameOffsetLookups(final int offset, final int size) {
        this.offset = offset;
        this.enabled = ((size != 0) ? new BitSet(size) : null);
    }
    
    void swap(final int i, final int j) {
        if (this.enabled != null) {
            final boolean temp = this.enabled.get(i);
            this.enabled.set(i, this.enabled.get(j));
            this.enabled.set(j, temp);
        }
    }
    
    int get(final int index) {
        return this.isEnabled(index) ? this.offset : 0;
    }
    
    int enable(final int index, final boolean enable) {
        if (this.enabled != null) {
            this.enabled.set(index, enable);
        }
        return enable ? this.offset : 0;
    }
    
    boolean isEnabled(final int index) {
        return this.enabled != null && this.enabled.get(index);
    }
    
    boolean hasAnyEnabled() {
        return this.enabled != null && this.enabled.cardinality() > 0;
    }
    
    NameOffsetLookups emptyCopy() {
        return new NameOffsetLookups(this.offset, this.enabled.size());
    }
    
    static {
        NONE = new NameOffsetLookups(0, 0);
    }
}
