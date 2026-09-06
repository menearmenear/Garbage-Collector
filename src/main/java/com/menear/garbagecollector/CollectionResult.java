package com.menear.garbagecollector;

public class CollectionResult {
    public final boolean success;
    public final int moneyAdd;
    public final String garbageType;
    public final int value;
    public final boolean luckGained;
    public final String message;

    public CollectionResult(boolean success, int moneyAdd, String garbageType, int value, boolean luckGained, String message) {
        this.success = success;
        this.moneyAdd = moneyAdd;
        this.garbageType = garbageType;
        this.value = value;
        this.luckGained = luckGained;
        this.message = message;
    }

    public static CollectionResult fail(String message) {
        return new CollectionResult(false, 0, null, 0, false, message);
    }
}