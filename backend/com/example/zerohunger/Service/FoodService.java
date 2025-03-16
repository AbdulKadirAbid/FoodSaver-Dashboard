// 
// Decompiled by Procyon v0.6.0
// 

package com.example.zerohunger.Service;

import java.util.function.Supplier;
import jakarta.transaction.Transactional;
import com.example.zerohunger.Entity.FoodStatus;
import com.example.zerohunger.Entity.FoodItems;
import java.time.LocalDate;
import com.example.zerohunger.Entity.Users;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.zerohunger.Repository.FoodRepo;
import org.springframework.stereotype.Service;

@Service
public class FoodService
{
    private final FoodRepo foodRepo;
    private final StatsService statsService;
    
    @Autowired
    public FoodService(final FoodRepo foodRepo, final StatsService statsService) {
        this.foodRepo = foodRepo;
        this.statsService = statsService;
    }
    
    @Transactional
    public void AddFood(final String foodName, final Users userID, final LocalDate exp) {
        final FoodItems foodItem = new FoodItems();
        foodItem.setFoodName(foodName);
        foodItem.setExp(exp);
        foodItem.setStatus(FoodStatus.UNCONFIRMED);
        foodItem.setDateChanged(null);
        foodItem.setUser(userID);
        this.foodRepo.save((Object)foodItem);
    }
    
    @Transactional
    public void MarkFood(final Long foodID, final FoodStatus status) {
        final FoodItems foodItem = this.foodRepo.findById((Object)foodID).orElseThrow(FoodService::lambda$MarkFood$0);
        final Long userID = foodItem.getUser().getUserID();
        final LocalDate date = LocalDate.now();
        this.foodRepo.updateFoodStatus(foodID, status, date);
        switch (FoodService$1.$SwitchMap$com$example$zerohunger$Entity$FoodStatus[status.ordinal()]) {
            case 1: {
                this.statsService.IncrementTotalSaved(userID);
                break;
            }
            case 2: {
                this.statsService.IncrementTotalWasted(userID);
                break;
            }
        }
    }
    
    public void removeOldFood() {
        final LocalDate date = LocalDate.now();
        final LocalDate old = date.minusMonths(1L);
        this.foodRepo.removeOldFood(old);
    }
    
    public void CountFoodSinceDate(final Long userID) {
        final LocalDate date = LocalDate.now();
        final LocalDate week = date.minusWeeks(1L);
        final LocalDate month = date.minusMonths(1L);
        final int weekCount = this.foodRepo.getFoodCountSince(week, userID);
        final int monthCount = this.foodRepo.getFoodCountSince(month, userID);
        this.statsService.routineUpdateStats(weekCount, monthCount, userID);
    }
}
