// 
// Decompiled by Procyon v0.6.0
// 

package com.example.zerohunger.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import com.example.zerohunger.Entity.Users;
import org.springframework.stereotype.Repository;
import com.example.zerohunger.Entity.Sessions;
import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface SessionRepo extends JpaRepository<Sessions, Long>
{
    @Modifying
    @Query("DELETE FROM Sessions s WHERE s.userID = :userID")
    void deleteByUserID(@Param("userID") final Users userID);
}
