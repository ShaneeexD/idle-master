package com.idlemaster;

import lombok.Data;

/**
 * Data class for tracking salvage-related information for the Idle Master overlay.
 */
@Data
public class SalvageInfo {
    
    public SalvageInfo() {
    }
    
    public SalvageInfo(SalvageInfo other) {
        this.boatHealth = other.boatHealth;
        this.maxBoatHealth = other.maxBoatHealth;
        this.inventoryUsedSlots = other.inventoryUsedSlots;
        this.cargoCount = other.cargoCount;
        this.maxCargoCount = other.maxCargoCount;
        this.playerSalvaging = other.playerSalvaging;
        this.crewSalvaging = other.crewSalvaging;
        this.crewCount = other.crewCount;
        this.monsterAttacking = other.monsterAttacking;
        this.characterName = other.characterName;
    }
    
    // Boat health
    private int boatHealth = 100;
    private int maxBoatHealth = 100;
    
    // Inventory
    private int inventoryUsedSlots = 0;
    private static final int MAX_INVENTORY_SLOTS = 28;
    
    // Cargo
    private int cargoCount = 0;
    private int maxCargoCount = 60; // Default cargo capacity, may vary by boat
    
    // Animation states
    private boolean playerSalvaging = false;
    private boolean crewSalvaging = false;
    private int crewCount = 0;
    private int crewActivelySalvaging = 0;
    
    // Monster alert
    private boolean monsterAttacking = false;
    private String monsterName = "";
    
    // Character name
    private String characterName = "";
    
    // Getters for display text
    
    public int getBoatHealthPercentage() {
        if (maxBoatHealth == 0) return 0;
        return (boatHealth * 100) / maxBoatHealth;
    }
    
    public String getBoatHealthText() {
        return String.format("%d/%d (%d%%)", boatHealth, maxBoatHealth, getBoatHealthPercentage());
    }
    
    public String getInventoryText() {
        int usagePercentage = (inventoryUsedSlots * 100) / MAX_INVENTORY_SLOTS;
        return String.format("%d/28 (%d%%)", inventoryUsedSlots, usagePercentage);
    }
    
    public int getCargoPercentage() {
        if (maxCargoCount == 0) return 0;
        return (cargoCount * 100) / maxCargoCount;
    }
    
    public String getCargoText() {
        return String.format("%d/%d (%d%%)", cargoCount, maxCargoCount, getCargoPercentage());
    }
    
    public String getPlayerStatusText() {
        return playerSalvaging ? "SALVAGING" : "IDLE";
    }
    
    public String getCrewStatusText() {
        if (crewCount == 0) {
            return "No Crew";
        }
        return String.format("%d/%d Active", crewActivelySalvaging, crewCount);
    }
    
    public String getMonsterAlertText() {
        if (monsterAttacking) {
            return monsterName.isEmpty() ? "UNDER ATTACK!" : monsterName + " ATTACKING!";
        }
        return "Safe";
    }
}
