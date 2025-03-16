// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.log;

public abstract sealed class DebugLogger permits DisabledDebugLogger, SystemErrDebugLogger
{
    private static final String ENABLED_PROPERTY = "loader.debug";
    private static final DebugLogger disabled;
    
    public abstract void log(final String message);
    
    public abstract void log(final String message, final Object arg1);
    
    public abstract void log(final String message, final Object arg1, final Object arg2);
    
    public abstract void log(final String message, final Object arg1, final Object arg2, final Object arg3);
    
    public abstract void log(final String message, final Object arg1, final Object arg2, final Object arg3, final Object arg4);
    
    public static DebugLogger get(final Class<?> sourceClass) {
        return (DebugLogger.disabled != null) ? DebugLogger.disabled : new SystemErrDebugLogger(sourceClass);
    }
    
    static {
        disabled = (Boolean.getBoolean("loader.debug") ? null : new DisabledDebugLogger());
    }
    
    private static final class DisabledDebugLogger extends DebugLogger
    {
        @Override
        public void log(final String message) {
        }
        
        @Override
        public void log(final String message, final Object arg1) {
        }
        
        @Override
        public void log(final String message, final Object arg1, final Object arg2) {
        }
        
        @Override
        public void log(final String message, final Object arg1, final Object arg2, final Object arg3) {
        }
        
        @Override
        public void log(final String message, final Object arg1, final Object arg2, final Object arg3, final Object arg4) {
        }
    }
    
    private static final class SystemErrDebugLogger extends DebugLogger
    {
        private final String prefix;
        
        SystemErrDebugLogger(final Class<?> sourceClass) {
            this.prefix = "LOADER: " + sourceClass + " : ";
        }
        
        @Override
        public void log(final String message) {
            this.print(message);
        }
        
        @Override
        public void log(final String message, final Object arg1) {
            this.print(message.formatted(arg1));
        }
        
        @Override
        public void log(final String message, final Object arg1, final Object arg2) {
            this.print(message.formatted(arg1, arg2));
        }
        
        @Override
        public void log(final String message, final Object arg1, final Object arg2, final Object arg3) {
            this.print(message.formatted(arg1, arg2, arg3));
        }
        
        @Override
        public void log(final String message, final Object arg1, final Object arg2, final Object arg3, final Object arg4) {
            this.print(message.formatted(arg1, arg2, arg3, arg4));
        }
        
        private void print(final String message) {
            System.err.println(this.prefix + message);
        }
    }
}
