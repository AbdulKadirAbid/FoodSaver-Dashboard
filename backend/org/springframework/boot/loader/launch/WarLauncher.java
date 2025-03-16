// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.launch;

public class WarLauncher extends ExecutableArchiveLauncher
{
    public WarLauncher() throws Exception {
    }
    
    protected WarLauncher(final Archive archive) throws Exception {
        super(archive);
    }
    
    @Override
    protected String getEntryPathPrefix() {
        return "WEB-INF/";
    }
    
    @Override
    protected boolean isLibraryFileOrClassesDirectory(final Archive.Entry entry) {
        final String name = entry.name();
        if (entry.isDirectory()) {
            return name.equals("WEB-INF/classes/");
        }
        return name.startsWith("WEB-INF/lib/") || name.startsWith("WEB-INF/lib-provided/");
    }
    
    public static void main(final String[] args) throws Exception {
        new WarLauncher().launch(args);
    }
}
