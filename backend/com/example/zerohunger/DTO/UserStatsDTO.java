// 
// Decompiled by Procyon v0.6.0
// 

package com.example.zerohunger.DTO;

public class UserStatsDTO
{
    private final int totalWasted;
    private final int totalSaved;
    private final int wastedLastWeek;
    private final int wastedLastMonth;
    
    public UserStatsDTO(final int totalWasted, final int totalSaved, final int wastedLastWeek, final int wastedLastMonth) {
        this.totalWasted = totalWasted;
        this.totalSaved = totalSaved;
        this.wastedLastWeek = wastedLastWeek;
        this.wastedLastMonth = wastedLastMonth;
    }
    
    public int getTotalWasted() {
        return this.totalWasted;
    }
    
    public int getTotalSaved() {
        return this.totalSaved;
    }
    
    public int getWastedLastWeek() {
        return this.wastedLastWeek;
    }
    
    public int getWastedLastMonth() {
        return this.wastedLastMonth;
    }
}
