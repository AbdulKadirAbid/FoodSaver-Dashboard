// 
// Decompiled by Procyon v0.6.0
// 

package com.example.zerohunger.Repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import java.time.LocalDate;
import com.example.zerohunger.Entity.FoodStatus;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.zerohunger.Entity.FoodItems;
import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface FoodRepo extends JpaRepository<FoodItems, Long>
{
    @Modifying
    @Query("UPDATE FoodItems f SET f.status = :status, f.statusChanged = :date WHERE f.foodID = :id")
    void updateFoodStatus(@Param("id") final Long id, @Param("status") final FoodStatus status, @Param("date") final LocalDate date);
    
    @Transactional
    @Modifying
    @Query("DELETE FROM FoodItems f WHERE f.statusChanged < :date")
    void removeOldFood(@Param("date") final LocalDate date);
    
    @Query("SELECT COUNT(f) FROM FoodItems f WHERE f.statusChanged > :date AND f.user.userID = :Id")
    int getFoodCountSince(@Param("date") final LocalDate date, @Param("Id") final Long Id);
}
