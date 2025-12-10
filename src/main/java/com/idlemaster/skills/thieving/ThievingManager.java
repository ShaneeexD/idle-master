package com.idlemaster.skills.thieving;

import com.idlemaster.IdleMasterConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.config.ConfigManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

/**
 * Manages thieving detection and overlay for pickpocketing activities.
 */
@Slf4j
@Singleton
public class ThievingManager {
    
    // Wealthy Citizens NPC IDs for pickpocketing
    private static final Set<Integer> WC_NPC_IDS = Set.of(13302, 13303, 13304, 13305);
    
    // Wealthy Citizen name for distraction detection
    private static final String WEALTHY_CITIZEN_NAME = "Wealthy citizen";
    
    // Detection range for thieving area
    private static final int THIEVING_RANGE = 10;
    
    // Pickpocketing animation ID
    private static final int PICKPOCKET_ANIMATION = 881;
    
    // Sound effect ID for alerts
    private static final int SOUND_ID = 3817;
    
    // Coin pouch item ID (there are multiple variants, we'll count by name)
    private static final String COIN_POUCH_NAME = "Coin pouch";
    
    // Ardougne diary varbits (Easy doesn't affect coin pouch limit)
    private static final int DIARY_ARDOUGNE_MEDIUM = 4459;
    private static final int DIARY_ARDOUGNE_HARD = 4460;
    private static final int DIARY_ARDOUGNE_ELITE = 4461;
    
    // Coin pouch limits based on diary completion
    private static final int BASE_POUCH_LIMIT = 28;
    private static final int MEDIUM_POUCH_LIMIT = 56;
    private static final int HARD_POUCH_LIMIT = 84;
    private static final int ELITE_POUCH_LIMIT = 140;
    
    private final Client client;
    private final IdleMasterConfig config;
    private final ConfigManager configManager;
    
    @Getter
    private final ThievingInfo thievingInfo = new ThievingInfo();
    private ThievingInfo previousThievingInfo;
    
    @Getter
    private ThievingOverlayWindow overlayWindow;
    
    private Instant lastActiveTime = Instant.now();
    private Instant distractionStartTime = null;
    private Instant lastDistractionEndTime = null;
    private boolean wasInArea = false;
    private boolean wasDistracted = false;
    private boolean alertedDistractionStart = false;
    private boolean alertedDistractionEnd = false;
    private boolean alertedPouchFull = false;
    private int previousPouchCount = 0;
    
    // Track the distracted NPC for entity hiding
    @Getter
    private NPC distractedNpc = null;
    
    @Inject
    public ThievingManager(Client client, IdleMasterConfig config, ConfigManager configManager) {
        this.client = client;
        this.config = config;
        this.configManager = configManager;
    }
    
    public void startUp() {
        if (overlayWindow == null) {
            SwingUtilities.invokeLater(() -> {
                overlayWindow = new ThievingOverlayWindow(thievingInfo, config, configManager);
                overlayWindow.setVisible(false);
            });
        }
    }
    
    public void shutDown() {
        if (overlayWindow != null) {
            SwingUtilities.invokeLater(() -> {
                overlayWindow.savePositionAndSize();
                overlayWindow.setVisible(false);
                overlayWindow.dispose();
                overlayWindow = null;
            });
        }
    }
    
    public void onGameTick() {
        if (client.getGameState() != GameState.LOGGED_IN) {
            return;
        }
        
        Player player = client.getLocalPlayer();
        if (player == null) {
            return;
        }
        
        // Check if player is in thieving area
        boolean inArea = isPlayerInThievingArea(player);
        thievingInfo.setInThievingArea(inArea);
        
        // Show/hide overlay based on area
        if (wasInArea != inArea && overlayWindow != null) {
            SwingUtilities.invokeLater(() -> overlayWindow.setVisible(inArea));
        }
        wasInArea = inArea;
        
        if (!inArea) {
            return;
        }
        
        // Update character name
        String name = player.getName();
        if (name != null && !name.equals(thievingInfo.getCharacterName())) {
            thievingInfo.setCharacterName(name);
        }
        
        // Update player thieving status
        updatePlayerThievingStatus(player);
        
        // Update distraction status
        updateDistractionStatus(player);
        
        // Update coin pouch count
        updateCoinPouchCount();
        
        // Update thieving XP
        updateThievingXp();
        
        // Update overlay
        if (overlayWindow != null && (previousThievingInfo == null || !previousThievingInfo.equals(thievingInfo))) {
            previousThievingInfo = new ThievingInfo(thievingInfo);
            SwingUtilities.invokeLater(() -> {
                overlayWindow.updateDisplay();
                overlayWindow.updateCharacterName(thievingInfo.getCharacterName());
            });
        }
    }
    
    /**
     * Checks if the player is within range of any Wealthy Citizens NPCs.
     */
    private boolean isPlayerInThievingArea(Player player) {
        WorldPoint playerLocation = player.getWorldLocation();
        if (playerLocation == null) {
            return false;
        }
        
        // Use the player's WorldView to get NPCs
        WorldView worldView = player.getWorldView();
        if (worldView == null) {
            return false;
        }
        
        for (NPC npc : worldView.npcs()) {
            if (npc == null) continue;
            
            int npcId = npc.getId();
            if (WC_NPC_IDS.contains(npcId)) {
                WorldPoint npcLocation = npc.getWorldLocation();
                if (npcLocation != null && npcLocation.getPlane() == playerLocation.getPlane()) {
                    int distance = playerLocation.distanceTo(npcLocation);
                    if (distance <= THIEVING_RANGE) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    private void updatePlayerThievingStatus(Player player) {
        int animation = player.getAnimation();
        boolean isThieving = (animation == PICKPOCKET_ANIMATION);
        
        if (isThieving) {
            lastActiveTime = Instant.now();
            thievingInfo.setIdleTimeSeconds(0);
        } else {
            long idleSeconds = Duration.between(lastActiveTime, Instant.now()).getSeconds();
            thievingInfo.setIdleTimeSeconds((int) idleSeconds);
        }
        
        thievingInfo.setPlayerThieving(isThieving);
    }
    
    /**
     * Checks if any Wealthy Citizen is distracted (interacting with a child NPC).
     * A citizen is distracted when they are interacting with an NPC that has combat level 0.
     */
    private void updateDistractionStatus(Player player) {
        WorldView worldView = player.getWorldView();
        if (worldView == null) {
            return;
        }
        
        boolean foundDistracted = false;
        NPC foundDistractedNpc = null;
        
        for (NPC npc : worldView.npcs()) {
            if (npc == null) continue;
            
            String npcName = npc.getName();
            if (npcName != null && npcName.equals(WEALTHY_CITIZEN_NAME)) {
                // Check if the citizen is interacting with someone
                if (npc.isInteracting()) {
                    Actor interactingWith = npc.getInteracting();
                    // If interacting with a child (combat level 0), they're distracted
                    if (interactingWith != null && interactingWith.getCombatLevel() == 0) {
                        foundDistracted = true;
                        foundDistractedNpc = npc;
                        break;
                    }
                }
            }
        }
        
        // Update tracked distracted NPC
        distractedNpc = foundDistractedNpc;
        
        // Update distraction state
        if (foundDistracted) {
            if (!wasDistracted) {
                // Distraction just started
                distractionStartTime = Instant.now();
                alertedDistractionStart = false;
                alertedDistractionEnd = false; // Reset end alert for next cycle
            }
            
            // Calculate how long distracted
            if (distractionStartTime != null) {
                long distractionSeconds = Duration.between(distractionStartTime, Instant.now()).getSeconds();
                thievingInfo.setDistractionTimeSeconds((int) distractionSeconds);
            }
            
            thievingInfo.setCitizenDistracted(true);
            thievingInfo.setTimeSinceLastDistraction(0);
            
            // Play alert sound when distraction starts
            if (!alertedDistractionStart && config.playThievingDistractionStartSound()) {
                playSoundEffect();
                alertedDistractionStart = true;
                log.debug("Wealthy citizen distracted - playing start alert");
            }
        } else {
            if (wasDistracted) {
                // Distraction just ended
                lastDistractionEndTime = Instant.now();
                
                // Play alert sound when distraction ends
                if (!alertedDistractionEnd && config.playThievingDistractionEndSound()) {
                    playSoundEffect();
                    alertedDistractionEnd = true;
                    log.debug("Wealthy citizen no longer distracted - playing end alert");
                }
            }
            
            thievingInfo.setCitizenDistracted(false);
            thievingInfo.setDistractionTimeSeconds(0);
            
            // Calculate time since last distraction
            if (lastDistractionEndTime != null) {
                long secondsSince = Duration.between(lastDistractionEndTime, Instant.now()).getSeconds();
                thievingInfo.setTimeSinceLastDistraction((int) secondsSince);
            }
            
            // Reset start alert flag when not distracted
            alertedDistractionStart = false;
        }
        
        wasDistracted = foundDistracted;
    }
    
    private void playSoundEffect() {
        try {
            Preferences preferences = client.getPreferences();
            int previousVolume = preferences.getSoundEffectVolume();
            preferences.setSoundEffectVolume(config.soundVolume());
            client.playSoundEffect(SOUND_ID, config.soundVolume());
            preferences.setSoundEffectVolume(previousVolume);
        } catch (Exception e) {
            log.debug("Error playing sound effect: {}", e.getMessage());
        }
    }
    
    private void updateCoinPouchCount() {
        try {
            // Update max pouches based on Ardougne diary completion
            int maxPouches = getMaxCoinPouches();
            thievingInfo.setMaxCoinPouches(maxPouches);
            
            // Count coin pouches in inventory
            ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
            if (inventory == null) {
                thievingInfo.setCoinPouchCount(0);
                previousPouchCount = 0;
                return;
            }
            
            int pouchCount = 0;
            for (Item item : inventory.getItems()) {
                if (item == null || item.getId() == -1) continue;
                
                String itemName = client.getItemDefinition(item.getId()).getName();
                if (itemName != null && itemName.equals(COIN_POUCH_NAME)) {
                    pouchCount += item.getQuantity();
                }
            }
            
            thievingInfo.setCoinPouchCount(pouchCount);
            
            // Play alert when pouches become full (only once per fill)
            if (pouchCount >= maxPouches && previousPouchCount < maxPouches) {
                if (!alertedPouchFull && config.playThievingPouchFullSound()) {
                    playSoundEffect();
                    alertedPouchFull = true;
                    log.debug("Coin pouches full - playing alert");
                }
            } else if (pouchCount < maxPouches) {
                // Reset alert when pouches are no longer full
                alertedPouchFull = false;
            }
            
            previousPouchCount = pouchCount;
        } catch (Exception e) {
            log.debug("Error updating coin pouch count: {}", e.getMessage());
        }
    }
    
    /**
     * Determines the maximum coin pouch limit based on Ardougne diary completion.
     * Elite requires Hard, Hard requires Medium, Medium requires Easy.
     */
    private int getMaxCoinPouches() {
        try {
            // Check diary completion in order: Elite > Hard > Medium > Easy
            // Diary varbit value of 1 means completed
            int eliteComplete = client.getVarbitValue(DIARY_ARDOUGNE_ELITE);
            int hardComplete = client.getVarbitValue(DIARY_ARDOUGNE_HARD);
            int mediumComplete = client.getVarbitValue(DIARY_ARDOUGNE_MEDIUM);
            
            // Elite diary gives 140 pouches (requires all lower tiers)
            if (eliteComplete == 1 && hardComplete == 1 && mediumComplete == 1) {
                return ELITE_POUCH_LIMIT;
            }
            // Hard diary gives 84 pouches (requires medium)
            if (hardComplete == 1 && mediumComplete == 1) {
                return HARD_POUCH_LIMIT;
            }
            // Medium diary gives 56 pouches
            if (mediumComplete == 1) {
                return MEDIUM_POUCH_LIMIT;
            }
            
            return BASE_POUCH_LIMIT;
        } catch (Exception e) {
            log.debug("Error checking Ardougne diary: {}", e.getMessage());
            return BASE_POUCH_LIMIT;
        }
    }
    
    private void updateThievingXp() {
        try {
            int currentXp = client.getSkillExperience(Skill.THIEVING);
            int currentLevel = client.getRealSkillLevel(Skill.THIEVING);
            
            thievingInfo.setThievingXp(currentXp);
            thievingInfo.setThievingLevel(currentLevel);
            
            if (currentLevel >= 99) {
                thievingInfo.setXpToNextLevel(0);
                thievingInfo.setXpInCurrentLevel(0);
                thievingInfo.setXpForCurrentLevel(0);
            } else {
                int xpForCurrentLevel = Experience.getXpForLevel(currentLevel);
                int xpForNextLevel = Experience.getXpForLevel(currentLevel + 1);
                int xpInCurrentLevel = currentXp - xpForCurrentLevel;
                int xpNeededForLevel = xpForNextLevel - xpForCurrentLevel;
                int xpToNextLevel = xpForNextLevel - currentXp;
                
                thievingInfo.setXpToNextLevel(xpToNextLevel);
                thievingInfo.setXpInCurrentLevel(xpInCurrentLevel);
                thievingInfo.setXpForCurrentLevel(xpNeededForLevel);
            }
        } catch (Exception e) {
            log.debug("Error updating thieving XP: {}", e.getMessage());
        }
    }
    
    public void onConfigChanged() {
        if (overlayWindow != null) {
            SwingUtilities.invokeLater(() -> overlayWindow.updateConfig());
        }
    }
    
    public void onStatChanged(Skill skill) {
        if (skill == Skill.THIEVING && thievingInfo.isInThievingArea()) {
            updateThievingXp();
        }
    }
    
    /**
     * Checks if an NPC should be hidden during distraction.
     * Hides all NPCs except the distracted wealthy citizen.
     */
    public boolean shouldHideNpc(NPC npc) {
        if (!config.hideNpcsDuringDistraction()) {
            return false;
        }
        
        // Only hide when in thieving area and citizen is distracted
        if (!thievingInfo.isInThievingArea() || !thievingInfo.isCitizenDistracted()) {
            return false;
        }
        
        // Don't hide the distracted NPC
        if (distractedNpc != null && npc.getIndex() == distractedNpc.getIndex()) {
            return false;
        }
        
        // Hide all other NPCs
        return true;
    }
}
