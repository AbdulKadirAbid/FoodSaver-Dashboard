// 
// Decompiled by Procyon v0.6.0
// 

package com.example.zerohunger.Repository;

import org.springframework.data.jpa.repository.Query;
import java.util.List;
import org.springframework.stereotype.Repository;
import com.example.zerohunger.Entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface UsersRepo extends JpaRepository<Users, Long>
{
    @Query("SELECT x.userID FROM Users x")
    List<Long> listUserIDs();
    
    Users findByEmail(final String email);
}
