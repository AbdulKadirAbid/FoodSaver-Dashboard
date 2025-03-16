// 
// Decompiled by Procyon v0.6.0
// 

package com.example.zerohunger.Entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Column;
import java.time.LocalDate;
import jakarta.persistence.GenerationType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Entity;

@Entity
@Table(name = "users")
public class Users
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userID;
    private String firstName;
    private String lastName;
    private LocalDate DOB;
    @Column(nullable = false, unique = true)
    private String email;
    private String password;
    @OneToOne(mappedBy = "user", cascade = { CascadeType.ALL })
    private userStats stats;
    
    public userStats getStats() {
        return this.stats;
    }
    
    public void setStats(final userStats stats) {
        this.stats = stats;
    }
    
    public Long getUserID() {
        return this.userID;
    }
    
    public void setUserID(final Long userID) {
        this.userID = userID;
    }
    
    public String getFirstName() {
        return this.firstName;
    }
    
    public void setFirstName(final String name) {
        this.firstName = name;
    }
    
    public String getLastName() {
        return this.lastName;
    }
    
    public void setLastName(final String name) {
        this.lastName = name;
    }
    
    public String getEmail() {
        return this.email;
    }
    
    public void setEmail(final String email) {
        this.email = email;
    }
    
    public LocalDate getDOB() {
        return this.DOB;
    }
    
    public void setDOB(final LocalDate dob) {
        this.DOB = dob;
    }
    
    public String getPassword() {
        return this.password;
    }
    
    public void setPassword(final String password) {
        this.password = password;
    }
}
