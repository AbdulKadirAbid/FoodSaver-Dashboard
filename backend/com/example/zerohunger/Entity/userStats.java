// 
// Decompiled by Procyon v0.6.0
// 

package com.example.zerohunger.Entity;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Entity;

@Entity
@Table(name = "stats")
public class userStats
{
    @Id
    private Long userID;
    @OneToOne
    @JoinColumn(name = "userID", referencedColumnName = "userID")
    private Users user;
    private int totalWasted;
    private int totalSaved;
    private int wastedLastWeek;
    private int wastedLastMonth;
    
    public Long getUserID() {
        return this.userID;
    }
    
    public void setUserID(final Users userID) {
        this.user = userID;
    }
    
    public int getTotalWasted() {
        return this.totalWasted;
    }
    
    public void addTotalWasted() {
        ++this.totalWasted;
    }
    
    public int getTotalSaved() {
        return this.totalSaved;
    }
    
    public void addTotalSaved() {
        ++this.totalSaved;
    }
    
    public int getWastedLastWeek() {
        return this.wastedLastWeek;
    }
    
    public void setWastedLastWeek(final int waste) {
        this.wastedLastWeek = waste;
    }
    
    public int getWastedLastMonth() {
        return this.wastedLastMonth;
    }
    
    public void setWastedLastMonth(final int waste) {
        this.wastedLastMonth = waste;
    }
    
    public void setTotalSaved(final int i) {
        this.totalSaved = i;
    }
    
    public void setTotalWasted(final int i) {
        this.totalWasted = i;
    }
}
