// 
// Decompiled by Procyon v0.6.0
// 

package com.example.zerohunger.Entity;

public enum FoodStatus
{
    public static final enum FoodStatus UNCONFIRMED;
    public static final enum FoodStatus USED;
    public static final enum FoodStatus WASTED;
    
    public static FoodStatus valueOf(final String name) {
        return Enum.valueOf(FoodStatus.class, name);
    }
    
    static {
        FoodStatus.UNCONFIRMED = new FoodStatus("UNCONFIRMED", 0);
        FoodStatus.USED = new FoodStatus("USED", 1);
        FoodStatus.WASTED = new FoodStatus("WASTED", 2);
        FoodStatus.$VALUES = $values();
    }
}
