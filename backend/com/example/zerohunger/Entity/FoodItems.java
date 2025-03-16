// 
// Decompiled by Procyon v0.6.0
// 

package com.example.zerohunger.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.LocalDate;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.GenerationType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Entity;

@Entity
@Table(name = "food")
public class FoodItems
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long foodID;
    @ManyToOne
    @JoinColumn(name = "userID")
    private Users user;
    private String foodName;
    private LocalDate expiration;
    @Enumerated(EnumType.STRING)
    private FoodStatus status;
    @Column(nullable = true)
    private LocalDate statusChanged;
    
    public Users getUser() {
        return this.user;
    }
    
    public void setUser(final Users user) {
        this.user = user;
    }
    
    public Long getFoodID() {
        return this.foodID;
    }
    
    public void setFoodID(final Long foodID) {
        this.foodID = foodID;
    }
    
    public String getFoodName() {
        return this.foodName;
    }
    
    public void setFoodName(final String foodName) {
        this.foodName = foodName;
    }
    
    public LocalDate getExp() {
        return this.expiration;
    }
    
    public void setExp(final LocalDate expiration) {
        this.expiration = expiration;
    }
    
    public FoodStatus getStatus() {
        return this.status;
    }
    
    public void setStatus(final FoodStatus status) {
        this.status = status;
    }
    
    public LocalDate getDateChanged() {
        return this.statusChanged;
    }
    
    public void setDateChanged(final LocalDate statusChanged) {
        this.statusChanged = statusChanged;
    }
}
