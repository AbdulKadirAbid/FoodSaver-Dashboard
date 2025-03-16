// 
// Decompiled by Procyon v0.6.0
// 

package com.example.zerohunger.Entity;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.GenerationType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Entity;

@Entity
@Table(name = "RefreshToken")
public class RefreshToken
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long TokenID;
    @OneToOne
    @JoinColumn(name = "userID", referencedColumnName = "userID")
    private Users userID;
    @Column(nullable = false, unique = true)
    private String token;
    private LocalDateTime expiresAt;
    
    public Long getTokenID() {
        return this.TokenID;
    }
    
    public void setTokenID(final Long ID) {
        this.TokenID = ID;
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
    
    public String getToken() {
        return this.token;
    }
    
    public void setToken(final String token) {
        this.token = token;
    }
}
