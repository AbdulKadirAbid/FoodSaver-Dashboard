// 
// Decompiled by Procyon v0.6.0
// 

package com.example.zerohunger.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import com.example.zerohunger.Entity.Users;
import org.springframework.stereotype.Repository;
import com.example.zerohunger.Entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface RefreshTokenRepo extends JpaRepository<RefreshToken, Long>
{
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.userID = :userID")
    void deleteByUserID(@Param("userID") final Users userID);
}
