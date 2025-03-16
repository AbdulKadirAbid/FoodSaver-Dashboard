// 
// Decompiled by Procyon v0.6.0
// 

package com.example.zerohunger.Service;

import java.util.Map;
import java.time.LocalDate;
import java.util.function.Supplier;
import com.example.zerohunger.Entity.userStats;
import com.example.zerohunger.DTO.UserStatsDTO;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.zerohunger.Repository.FoodRepo;
import com.example.zerohunger.Repository.StatsRepo;
import org.springframework.stereotype.Service;

@Service
public class StatsService
{
    private final StatsRepo statsRepo;
    private final FoodRepo foodRepo;
    
    @Autowired
    public StatsService(final StatsRepo statsRepo, final FoodRepo foodRepo) {
        this.statsRepo = statsRepo;
        this.foodRepo = foodRepo;
    }
    
    public UserStatsDTO getUserStats(final Long UserID) {
        final userStats statsEntry = this.statsRepo.findById((Object)UserID).orElse(null);
        if (statsEntry != null) {
            final UserStatsDTO stats = new UserStatsDTO(statsEntry.getTotalWasted(), statsEntry.getTotalSaved(), statsEntry.getWastedLastWeek(), statsEntry.getWastedLastMonth());
            return stats;
        }
        return null;
    }
    
    public void IncrementTotalWasted(final Long userID) {
        final userStats user = this.statsRepo.findById((Object)userID).orElseThrow(StatsService::lambda$IncrementTotalWasted$0);
        user.addTotalWasted();
    }
    
    public void IncrementTotalSaved(final Long userID) {
        final userStats user = this.statsRepo.findById((Object)userID).orElseThrow(StatsService::lambda$IncrementTotalSaved$1);
        user.addTotalSaved();
    }
    
    public void routineUpdateStats(final int week, final int month, final Long userID) {
        this.statsRepo.routineUpdateStats(week, month, userID);
    }
    
    public int getTotalWasted(final Long userId) {
        return this.foodRepo.getFoodCountSince(LocalDate.MIN, userId);
    }
    
    public int getTotalSaved(final Long userId) {
        return this.foodRepo.getFoodCountSince(LocalDate.MIN, userId);
    }
    
    public Map<String, Integer> getFoodWasteStats(final Long userId) {
        final LocalDate now = LocalDate.now();
        final LocalDate oneWeekAgo = now.minusWeeks(1L);
        final LocalDate oneMonthAgo = now.minusMonths(1L);
        final int wastedLastWeek = this.foodRepo.getFoodCountSince(oneWeekAgo, userId);
        final int wastedLastMonth = this.foodRepo.getFoodCountSince(oneMonthAgo, userId);
        return Map.of("wastedLastWeek", wastedLastWeek, "wastedLastMonth", wastedLastMonth);
    }
    
    public int getFoodWastedLastMonth(final Long userId) {
        return this.statsRepo.getFoodWastedLastMonth(userId);
    }
    
    public int getFoodWastedLastWeek(final Long userId) {
        return this.statsRepo.getFoodWastedLastWeek(userId);
    }
    
    public UserStatsDTO getDonationStats(final Long userId) {
        return this.statsRepo.findDonationStatsByUserId(userId);
    }
    
    public int getDonationsLastMonth(final Long userId) {
        return this.statsRepo.getDonationsLastMonth(userId);
    }
}
