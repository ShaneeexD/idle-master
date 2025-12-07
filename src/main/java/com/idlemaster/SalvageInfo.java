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
        this.playerSortingSalvage = other.playerSortingSalvage;
        this.crewSalvaging = other.crewSalvaging;
        this.crewCount = other.crewCount;
        this.monsterAttacking = other.monsterAttacking;
        this.boatUnderAttack = other.boatUnderAttack;
        this.monsterAlertText = other.monsterAlertText;
        this.characterName = other.characterName;
        this.idleTimeSeconds = other.idleTimeSeconds;
        this.sailingXp = other.sailingXp;
        this.sailingLevel = other.sailingLevel;
        this.xpToNextLevel = other.xpToNextLevel;
        this.xpInCurrentLevel = other.xpInCurrentLevel;
        this.xpForCurrentLevel = other.xpForCurrentLevel;
        this.activeSalvageSpots = other.activeSalvageSpots;
        this.totalSalvageSpots = other.totalSalvageSpots;
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
    private boolean playerSortingSalvage = false;
    private boolean crewSalvaging = false;
    private int crewCount = 0;
    private int crewActivelySalvaging = 0;
    
    // Monster alert
    private boolean monsterAttacking = false;
    private String monsterName = "";
    
    // Boat under attack (HP decreasing)
    private boolean boatUnderAttack = false;
    private String monsterAlertText = "Safe";
    
    // Character name
    private String characterName = "";
    
    // Idle timer
    private int idleTimeSeconds = 0;
    
    // Sailing XP tracking
    private int sailingXp = 0;
    private int sailingLevel = 1;
    private int xpToNextLevel = 0;
    private int xpInCurrentLevel = 0;
    private int xpForCurrentLevel = 0;
    
    // Salvage spot tracking
    private int activeSalvageSpots = 0;
    private int totalSalvageSpots = 0;
    
    // Getters for display text
    
    public int getBoatHealthPercentage() {
        if (maxBoatHealth == 0) return 0;
        return (boatHealth * 100) / maxBoatHealth;
    }
    
    public String getBoatHealthText() {
        return String.format("%d/%d (%d%%)", boatHealth, maxBoatHealth, getBoatHealthPercentage());
    }
    
    public int getInventoryPercentage() {
        return (inventoryUsedSlots * 100) / MAX_INVENTORY_SLOTS;
    }
    
    public String getInventoryText() {
        return String.format("%d/28 (%d%%)", inventoryUsedSlots, getInventoryPercentage());
    }
    
    public int getCargoPercentage() {
        if (maxCargoCount == 0) return 0;
        return (cargoCount * 100) / maxCargoCount;
    }
    
    public String getCargoText() {
        return String.format("%d/%d (%d%%)", cargoCount, maxCargoCount, getCargoPercentage());
    }
    
    public String getSalvageSpotsText() {
        if (totalSalvageSpots == 0) {
            return "No spots";
        }
        return String.format("%d/%d", activeSalvageSpots, totalSalvageSpots);
    }
    
    public String getPlayerStatusText() {
        if (playerSortingSalvage) {
            return "SORTING SALVAGE";
        }
        return playerSalvaging ? "SALVAGING" : "IDLE";
    }
    
    public String getIdleTimeText() {
        if (idleTimeSeconds <= 0) {
            return "";
        }
        int minutes = idleTimeSeconds / 60;
        int seconds = idleTimeSeconds % 60;
        if (minutes > 0) {
            return String.format(" (%dm %ds)", minutes, seconds);
        }
        return String.format(" (%ds)", seconds);
    }
    
    public String getCrewStatusText() {
        if (crewCount == 0) {
            return "No Crew";
        }
        return String.format("%d/%d Active", crewActivelySalvaging, crewCount);
    }
    
    // Override Lombok getter to use custom logic
    public String getMonsterAlertText() {
        if (boatUnderAttack) {
            return monsterAlertText;
        }
        if (monsterAttacking) {
            return monsterName.isEmpty() ? "UNDER ATTACK!" : monsterName + " ATTACKING!";
        }
        return "Safe";
    }
    
    // Check if any attack is happening (monster or boat damage)
    public boolean isMonsterAttacking() {
        return monsterAttacking || boatUnderAttack;
    }
    
    // XP progress percentage within current level
    public int getXpProgressPercentage() {
        if (xpForCurrentLevel == 0) return 0;
        return (xpInCurrentLevel * 100) / xpForCurrentLevel;
    }
    
    // Format XP remaining text
    public String getXpRemainingText() {
        if (xpToNextLevel <= 0) {
            return "Max Level";
        }
        return formatNumber(xpToNextLevel) + " left";
    }
    
    // Format large numbers with K/M suffix
    private String formatNumber(int num) {
        if (num >= 1_000_000) {
            return String.format("%.1fM", num / 1_000_000.0);
        } else if (num >= 1_000) {
            return String.format("%.1fK", num / 1_000.0);
        }
        return String.valueOf(num);
    }
}
