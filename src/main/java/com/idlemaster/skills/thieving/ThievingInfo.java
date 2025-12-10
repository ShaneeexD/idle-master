package com.idlemaster.skills.thieving;

import lombok.Getter;
import lombok.Setter;

/**
 * Holds state information for thieving activities.
 */
@Getter
@Setter
public class ThievingInfo {
    
    // Player info
    private String characterName = "";
    
    // Thieving state
    private boolean inThievingArea = false;
    private boolean playerThieving = false;
    private int idleTimeSeconds = 0;
    
    // Distraction state
    private boolean citizenDistracted = false;
    private int distractionTimeSeconds = 0;
    private int timeSinceLastDistraction = 0;
    
    // Coin pouch tracking
    private int coinPouchCount = 0;
    private int maxCoinPouches = 28; // Base limit, increases with Ardougne diary
    
    // XP tracking
    private int thievingXp = 0;
    private int thievingLevel = 1;
    private int xpToNextLevel = 0;
    private int xpInCurrentLevel = 0;
    private int xpForCurrentLevel = 0;
    
    // Copy constructor
    public ThievingInfo() {}
    
    public ThievingInfo(ThievingInfo other) {
        this.characterName = other.characterName;
        this.inThievingArea = other.inThievingArea;
        this.playerThieving = other.playerThieving;
        this.idleTimeSeconds = other.idleTimeSeconds;
        this.citizenDistracted = other.citizenDistracted;
        this.distractionTimeSeconds = other.distractionTimeSeconds;
        this.timeSinceLastDistraction = other.timeSinceLastDistraction;
        this.coinPouchCount = other.coinPouchCount;
        this.maxCoinPouches = other.maxCoinPouches;
        this.thievingXp = other.thievingXp;
        this.thievingLevel = other.thievingLevel;
        this.xpToNextLevel = other.xpToNextLevel;
        this.xpInCurrentLevel = other.xpInCurrentLevel;
        this.xpForCurrentLevel = other.xpForCurrentLevel;
    }
    
    // Display text methods
    
    public String getPlayerStatusText() {
        return playerThieving ? "PICKPOCKETING" : "IDLE";
    }
    
    public String getDistractionStatusText() {
        if (citizenDistracted) {
            return "DISTRACTED";
        }
        return "ALERT";
    }
    
    public String getDistractionTimeText() {
        if (citizenDistracted && distractionTimeSeconds > 0) {
            return String.format(" (%ds)", distractionTimeSeconds);
        }
        if (!citizenDistracted && timeSinceLastDistraction > 0) {
            return String.format(" (%ds ago)", timeSinceLastDistraction);
        }
        return "";
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
    
    public int getXpProgressPercentage() {
        if (xpForCurrentLevel == 0) return 0;
        return (xpInCurrentLevel * 100) / xpForCurrentLevel;
    }
    
    public String getXpRemainingText() {
        if (xpToNextLevel <= 0) {
            return "Max Level";
        }
        return formatNumber(xpToNextLevel) + " left";
    }
    
    public String getCoinPouchText() {
        return coinPouchCount + "/" + maxCoinPouches;
    }
    
    public int getCoinPouchPercentage() {
        if (maxCoinPouches == 0) return 0;
        return (coinPouchCount * 100) / maxCoinPouches;
    }
    
    public boolean isCoinPouchFull() {
        return coinPouchCount >= maxCoinPouches;
    }
    
    private String formatNumber(int num) {
        if (num >= 1_000_000) {
            return String.format("%.1fM", num / 1_000_000.0);
        } else if (num >= 1_000) {
            return String.format("%.1fK", num / 1_000.0);
        }
        return String.valueOf(num);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ThievingInfo other = (ThievingInfo) obj;
        return inThievingArea == other.inThievingArea &&
               playerThieving == other.playerThieving &&
               idleTimeSeconds == other.idleTimeSeconds &&
               citizenDistracted == other.citizenDistracted &&
               distractionTimeSeconds == other.distractionTimeSeconds &&
               timeSinceLastDistraction == other.timeSinceLastDistraction &&
               coinPouchCount == other.coinPouchCount &&
               maxCoinPouches == other.maxCoinPouches &&
               thievingXp == other.thievingXp &&
               thievingLevel == other.thievingLevel &&
               xpToNextLevel == other.xpToNextLevel &&
               characterName.equals(other.characterName);
    }
}
