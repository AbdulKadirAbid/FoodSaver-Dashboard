// 
// Decompiled by Procyon v0.6.0
// 

package com.example.zerohunger.Entity;

import java.time.LocalDateTime;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.GenerationType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Entity;

@Entity
@Table(name = "sessions")
public class Sessions
{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String sessionID;
    @OneToOne
    @JoinColumn(name = "userID", referencedColumnName = "userID")
    private Users userID;
    private LocalDateTime expiresAt;
    
    public String getSeshID() {
        return this.sessionID;
    }
    
    public void setSeshID(final String ID) {
        this.sessionID = ID;
    }
    
    public Users getUserID() {
        return this.userID;
    }
    
    public void setUserID(final Users ID) {
        this.userID = ID;
    }
    
    public LocalDateTime getExpAt() {
        return this.expiresAt;
    }
    
    public void setExpAt(final LocalDateTime exp) {
        this.expiresAt = exp;
    }
}
