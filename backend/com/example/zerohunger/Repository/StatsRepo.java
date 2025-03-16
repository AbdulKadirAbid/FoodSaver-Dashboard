// 
// Decompiled by Procyon v0.6.0
// 

package com.example.zerohunger.Repository;

import com.example.zerohunger.DTO.UserStatsDTO;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.zerohunger.Entity.userStats;
import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface StatsRepo extends JpaRepository<userStats, Long>
{
    @Modifying
    @Query("UPDATE userStats u SET u.wastedLastWeek = :week, u.wastedLastMonth = :month WHERE userID = :Id")
    void routineUpdateStats(@Param("week") final int week, @Param("month") final int month, @Param("Id") final Long Id);
    
    @Query("SELECT new com.example.zerohunger.DTO.UserStatsDTO(u.totalWasted, u.totalSaved, u.wastedLastWeek, u.wastedLastMonth) FROM userStats u WHERE u.userID = ?1")
    UserStatsDTO findStatsByUserId(final Long userId);
    
    @Query("SELECT u.wastedLastMonth FROM userStats u WHERE u.userID = ?1")
    int getFoodWastedLastMonth(final Long userId);
    
    @Query("SELECT u.wastedLastWeek FROM userStats u WHERE u.userID = ?1")
    int getFoodWastedLastWeek(final Long userId);
    
    @Query("SELECT new com.example.zerohunger.DTO.UserStatsDTO(u.totalWasted, u.totalSaved, u.wastedLastWeek, u.wastedLastMonth) FROM userStats u WHERE u.userID = ?1")
    UserStatsDTO findDonationStatsByUserId(final Long userId);
    
    @Query("SELECT u.wastedLastMonth FROM userStats u WHERE u.userID = ?1")
    int getDonationsLastMonth(final Long userId);
}
