package com.idlemaster;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.OverheadTextChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.Hooks;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import javax.swing.SwingUtilities;
import java.awt.Image;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@PluginDescriptor(
    name = "Idle Master",
    description = "Detachable overlay for AFK activities - Salvaging, thieving and more",
    tags = {"sailing", "salvage", "afk", "overlay", "idle", "thieving"}
)
public class IdleMasterPlugin extends Plugin {

    // Salvaging animation IDs (varies by side/position)
    private static final int SALVAGING_ANIMATION_1 = 13576;
    private static final int SALVAGING_ANIMATION_2 = 13577;
    private static final int SALVAGING_ANIMATION_3 = 13584;
    private static final int SORTING_SALVAGE_ANIMATION = 13599;
    
    // Shipwreck IDs - Salvageable (Active)
    private static final Set<Integer> SHIPWRECK_SALVAGEABLE_IDS = Set.of(
        60464, 60466, 60468, 60470, 60472, 60474, 60476, 60478
    );
    
    // Shipwreck IDs - Depleted (Inactive)
    private static final Set<Integer> SHIPWRECK_DEPLETED_IDS = Set.of(
        60465, 60467, 60469, 60471, 60473, 60475, 60477, 60479
    );
    
    private static final int SALVAGE_RANGE = 7;
    private static final int SHIPWRECK_SIZE = 2;
    
    // Cargo hold inventory IDs
    private static final Set<Integer> CARGO_INVENTORY_IDS = Set.of(
        InventoryID.SAILING_BOAT_1_CARGOHOLD,
        InventoryID.SAILING_BOAT_2_CARGOHOLD,
        InventoryID.SAILING_BOAT_3_CARGOHOLD,
        InventoryID.SAILING_BOAT_4_CARGOHOLD,
        InventoryID.SAILING_BOAT_5_CARGOHOLD
    );
    
    // Monster NPC IDs that attack during salvaging
    private static final Set<Integer> SALVAGE_MONSTER_IDS = Set.of(15210, 15196, 15207, 15206, 15208, 15209, 15212, 15200, 15201, 15198, 15199);
    
    // Widget IDs for boat info (from widget inspector)
    // Boat health widget ID: 61407235 (group 937, child 3)
    private static final int BOAT_HEALTH_WIDGET_ID = 61407235;
    // Cargo widgets - SailingBoatCargohold
    private static final int CARGO_OCCUPIED_WIDGET_ID = 61800452; // OCCUPIEDSLOTS - current cargo count
    private static final int CARGO_CAPACITY_WIDGET_ID = 61800453; // CAPACITY - max cargo capacity
    
    // Crew overhead text when they store salvage
    private static final String CREW_SALVAGE_OVERHEAD = "Managed to hook some salvage! I'll put it in the cargo hold.";
    
    // Chat message when sorting salvage is complete
    private static final String SORTING_DONE_MESSAGE = "You have no more salvage to sort.";
    
    // Sound effect ID for alerts
    private static final int SOUND_ID = 3817;

    @Inject
    private Client client;
    
    @Inject
    private Hooks hooks;

    @Inject
    private IdleMasterConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private IdleMasterOverlay overlay;

    @Inject
    private ConfigManager configManager;

    private FloatingOverlayWindow floatingWindow;
    private SalvageInfo salvageInfo;
    private SalvageInfo previousSalvageInfo;
    
    private Instant lastActive = Instant.now();
    private final Set<GameObject> activeShipwrecks = new HashSet<>();
    private boolean inSalvageRange = false;
    
    // Track which alerts have been played (only play once per condition)
    private boolean alertedLowBoatHealth = false;
    private boolean alertedInventoryFull = false;
    private boolean alertedCargoFull = false;
    private boolean alertedPlayerIdle = false;
    private boolean alertedCrewIdle = false;
    private boolean alertedMonsterAttack = false;
    
    // Cargo tracking
    private int cargoCount = 0;
    private int maxCargoCapacity = 0;
    
    // Track previous boat health to detect damage (monster attack)
    private int previousBoatHealth = -1;
    
    // Hide boats draw listener
    private final Hooks.RenderableDrawListener drawListener = this::shouldDraw;

    @Override
    protected void startUp() throws Exception {
        log.info("Idle Master plugin started!");
        
        salvageInfo = new SalvageInfo();
        loadCargoCount(); // Load saved cargo count
        createAndShowWindow();
        overlayManager.add(overlay);
        hooks.registerRenderableDrawListener(drawListener);
    }
    
    private void loadCargoCount() {
        String saved = configManager.getConfiguration("idlemaster", "savedCargoCount");
        if (saved != null) {
            try {
                cargoCount = Integer.parseInt(saved);
                salvageInfo.setCargoCount(cargoCount);
                log.debug("Loaded saved cargo count: {}", cargoCount);
            } catch (NumberFormatException e) {
                cargoCount = 0;
            }
        }
    }
    
    private void saveCargoCount() {
        configManager.setConfiguration("idlemaster", "savedCargoCount", String.valueOf(cargoCount));
    }

    private void createAndShowWindow() {
        SwingUtilities.invokeLater(() -> {
            floatingWindow = new FloatingOverlayWindow(salvageInfo, config, configManager);
            
            try {
                Image icon = ImageUtil.loadImageResource(getClass(), "/icon.png");
                if (icon != null) {
                    floatingWindow.setIconImage(icon);
                    floatingWindow.setIconImages(java.util.Arrays.asList(icon));
                }
            } catch (IllegalArgumentException e) {
                // Silently fall back to default icon
            }
            
            floatingWindow.setVisible(inSalvageRange);
        });
    }

    @Override
    protected void shutDown() throws Exception {
        log.info("Idle Master plugin stopped!");
        
        hooks.unregisterRenderableDrawListener(drawListener);
        overlayManager.remove(overlay);
        activeShipwrecks.clear();
        
        if (floatingWindow != null) {
            SwingUtilities.invokeLater(() -> {
                floatingWindow.savePositionAndSize();
                floatingWindow.dispose();
                floatingWindow = null;
            });
        }
    }
    
    /**
     * Hides other players' boats when in salvage range.
     */
    private boolean shouldDraw(Renderable renderable, boolean drawingUI) {
        if (!config.hideOtherBoats()) {
            return true;
        }
        
        if (renderable instanceof Scene) {
            Scene scene = (Scene) renderable;
            
            if (client.getTopLevelWorldView() == null) {
                return true;
            }
            
            WorldEntity we = client.getTopLevelWorldView().worldEntities().byIndex(scene.getWorldViewId());
            if (we == null) {
                return true;
            }
            
            // Hide other players' boats when we're in salvage range
            if (we.getOwnerType() == WorldEntity.OWNER_TYPE_OTHER_PLAYER && inSalvageRange) {
                return false;
            }
        }
        
        return true;
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        updateSalvageInfo();
        checkThresholdsAndPlaySounds();
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        if (event.getGameState() == GameState.LOGGED_IN) {
            updateSalvageInfo();
        } else if (event.getGameState() == GameState.LOADING) {
            activeShipwrecks.clear();
        }
    }
    
    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event) {
        GameObject gameObject = event.getGameObject();
        int id = gameObject.getId();
        if (SHIPWRECK_SALVAGEABLE_IDS.contains(id) || SHIPWRECK_DEPLETED_IDS.contains(id)) {
            activeShipwrecks.add(gameObject);
            log.debug("Shipwreck spawned: ID={}, Location={}", id, gameObject.getWorldLocation());
        }
    }
    
    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned event) {
        activeShipwrecks.remove(event.getGameObject());
    }
    
    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        // Track cargo hold changes
        // Mask the container ID as sailing-main does: containerId & 0x4FFF
        int containerId = event.getContainerId() & 0x4FFF;
        if (CARGO_INVENTORY_IDS.contains(containerId)) {
            ItemContainer container = event.getItemContainer();
            if (container != null) {
                Item[] items = container.getItems();
                int count = 0;
                for (Item item : items) {
                    if (item != null && item.getId() != -1) {
                        count++;
                    }
                }
                cargoCount = count;
                log.debug("Cargo updated via ItemContainerChanged: {} items (containerId={})", cargoCount, containerId);
            }
        }
    }
    
    @Subscribe
    public void onOverheadTextChanged(OverheadTextChanged event) {
        // Track crew storing salvage via their overhead text
        Actor actor = event.getActor();
        if (!(actor instanceof NPC)) {
            return;
        }
        
        NPC npc = (NPC) actor;
        
        // Check if this NPC is on OUR boat (same WorldView as local player)
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null || npc.getWorldView() != localPlayer.getWorldView()) {
            return; // NPC is on a different boat
        }
        
        String npcName = npc.getName();
        
        // Check if this is one of our crew members
        if (npcName != null && isCrewMember(npcName)) {
            String text = event.getOverheadText();
            if (text != null && text.equals(CREW_SALVAGE_OVERHEAD)) {
                // Crew member stored salvage - increment cargo count
                cargoCount++;
                salvageInfo.setCargoCount(cargoCount);
                saveCargoCount();
                log.debug("Crew {} stored salvage (overhead), cargo now: {}", npcName, cargoCount);
            }
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage event) {
        // Check for sorting salvage complete message
        if (event.getType() == ChatMessageType.SPAM || event.getType() == ChatMessageType.GAMEMESSAGE) {
            String message = event.getMessage();
            if (message != null && message.equals(SORTING_DONE_MESSAGE)) {
                // Player finished sorting salvage - play alert sound
                if (config.playSortingDoneSound()) {
                    playSoundEffect();
                }
                // Update player status to idle
                salvageInfo.setPlayerSortingSalvage(false);
                salvageInfo.setPlayerSalvaging(false);
                log.debug("Sorting salvage complete");
            }
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (event.getGroup().equals("idlemaster")) {
            if (event.getKey().equals("showOverlay") && config.showOverlay()) {
                if (floatingWindow == null) {
                    createAndShowWindow();
                } else if (!floatingWindow.isVisible()) {
                    floatingWindow.setVisible(true);
                }
                configManager.setConfiguration("idlemaster", "showOverlay", false);
            }
            
            if (floatingWindow != null) {
                previousSalvageInfo = null;
                SwingUtilities.invokeLater(() -> floatingWindow.updateConfig());
            }
        }
    }

    private void updateSalvageInfo() {
        if (client.getGameState() != GameState.LOGGED_IN) {
            return;
        }

        Player player = client.getLocalPlayer();
        if (player == null) {
            return;
        }
        
        // Check if we're in salvage range
        boolean wasInRange = inSalvageRange;
        inSalvageRange = isPlayerInSalvageRange();
        
        // Show/hide overlay based on salvage range
        if (floatingWindow != null && wasInRange != inSalvageRange) {
            SwingUtilities.invokeLater(() -> floatingWindow.setVisible(inSalvageRange));
        }
        
        // Only update info if in salvage range
        if (!inSalvageRange) {
            return;
        }

        // Update character name
        String name = player.getName();
        if (name != null && !name.equals(salvageInfo.getCharacterName())) {
            salvageInfo.setCharacterName(name);
        }

        // Update boat health (using varbits - needs actual varbit IDs)
        updateBoatHealth();

        // Update inventory
        updateInventoryUsage();

        // Update cargo count
        updateCargoCount();

        // Update player salvaging status
        updatePlayerSalvagingStatus(player);

        // Update crew status
        updateCrewStatus();

        // Check for monster attacks
        updateMonsterAlert();

        // Update the floating window
        if (floatingWindow != null && (previousSalvageInfo == null || !previousSalvageInfo.equals(salvageInfo))) {
            previousSalvageInfo = new SalvageInfo(salvageInfo);
            SwingUtilities.invokeLater(() -> {
                floatingWindow.updateDisplay();
                floatingWindow.updateCharacterName(salvageInfo.getCharacterName());
            });
        }
    }
    
    /**
     * Checks if the player's boat is within salvage range of any active shipwreck.
     */
    private boolean isPlayerInSalvageRange() {
        WorldPoint boatLocation = getPlayerBoatLocation();
        if (boatLocation == null) {
            return false;
        }
        
        for (GameObject shipwreck : activeShipwrecks) {
            // Check both active AND depleted shipwrecks for overlay visibility
            // (we want to show overlay even at depleted wrecks)
            WorldPoint shipwreckLocation = shipwreck.getWorldLocation();
            
            // Check if boat is within the salvage range
            int minX = shipwreckLocation.getX() - SALVAGE_RANGE;
            int maxX = shipwreckLocation.getX() + SHIPWRECK_SIZE - 1 + SALVAGE_RANGE;
            int minY = shipwreckLocation.getY() - SALVAGE_RANGE;
            int maxY = shipwreckLocation.getY() + SHIPWRECK_SIZE - 1 + SALVAGE_RANGE;
            
            if (boatLocation.getPlane() == shipwreckLocation.getPlane() &&
                boatLocation.getX() >= minX && boatLocation.getX() <= maxX &&
                boatLocation.getY() >= minY && boatLocation.getY() <= maxY) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Gets the world location of the player's boat.
     */
    private WorldPoint getPlayerBoatLocation() {
        if (client.getTopLevelWorldView() == null) {
            return null;
        }
        
        for (WorldEntity we : client.getTopLevelWorldView().worldEntities()) {
            // Our boat is OWNER_TYPE_SELF_PLAYER
            if (we != null && we.getOwnerType() == WorldEntity.OWNER_TYPE_SELF_PLAYER) {
                LocalPoint localPoint = we.getLocalLocation();
                if (localPoint != null) {
                    return WorldPoint.fromLocal(client, localPoint);
                }
            }
        }
        
        // Fallback to player location if not on a boat
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer != null) {
            return localPlayer.getWorldLocation();
        }
        
        return null;
    }

    private void updateBoatHealth() {
        // Read boat health from widget - the text is in a child widget
        // Widget 61407235 is the container, we need to find the child with text
        try {
            Widget healthWidget = client.getWidget(BOAT_HEALTH_WIDGET_ID);
            
            if (healthWidget == null) {
                return;
            }
            
            // The text might be in a child widget, not the parent
            String text = healthWidget.getText();
            
            // If parent has no text, check children
            if (text == null || text.isEmpty()) {
                Widget[] children = healthWidget.getChildren();
                if (children != null) {
                    for (Widget child : children) {
                        if (child != null && child.getText() != null && child.getText().contains("/")) {
                            text = child.getText();
                            break;
                        }
                    }
                }
                
                // Also check dynamic children
                if ((text == null || text.isEmpty()) && healthWidget.getDynamicChildren() != null) {
                    for (Widget child : healthWidget.getDynamicChildren()) {
                        if (child != null && child.getText() != null && child.getText().contains("/")) {
                            text = child.getText();
                            break;
                        }
                    }
                }
            }
            
            if (text != null && text.contains("/")) {
                String[] parts = text.split("/");
                if (parts.length == 2) {
                    int health = Integer.parseInt(parts[0].trim());
                    int maxHealth = Integer.parseInt(parts[1].trim());
                    salvageInfo.setBoatHealth(health);
                    salvageInfo.setMaxBoatHealth(maxHealth);
                }
            }
        } catch (Exception e) {
            log.debug("Error reading boat health: {}", e.getMessage());
        }
    }

    private void updateInventoryUsage() {
        try {
            ItemContainer inventory = client.getItemContainer(InventoryID.INV);
            if (inventory != null && inventory.getItems() != null) {
                Item[] items = inventory.getItems();
                int usedSlots = 0;
                for (Item item : items) {
                    if (item != null && item.getId() != -1) {
                        usedSlots++;
                    }
                }
                salvageInfo.setInventoryUsedSlots(usedSlots);
            } else {
                salvageInfo.setInventoryUsedSlots(0);
            }
        } catch (Exception e) {
            salvageInfo.setInventoryUsedSlots(0);
        }
    }

    private void updateCargoCount() {
        // Read cargo from widgets (same approach as boat health)
        try {
            int previousCargoCount = cargoCount;
            
            // Read occupied slots from widget
            Widget occupiedWidget = client.getWidget(CARGO_OCCUPIED_WIDGET_ID);
            if (occupiedWidget != null && !occupiedWidget.isHidden() && occupiedWidget.getText() != null) {
                String text = occupiedWidget.getText().trim();
                if (!text.isEmpty()) {
                    cargoCount = Integer.parseInt(text);
                }
            }
            
            // Read max capacity from widget
            Widget capacityWidget = client.getWidget(CARGO_CAPACITY_WIDGET_ID);
            if (capacityWidget != null && !capacityWidget.isHidden() && capacityWidget.getText() != null) {
                String text = capacityWidget.getText().trim();
                if (!text.isEmpty()) {
                    maxCargoCapacity = Integer.parseInt(text);
                }
            }
            
            salvageInfo.setCargoCount(cargoCount);
            salvageInfo.setMaxCargoCount(maxCargoCapacity > 0 ? maxCargoCapacity : 60);
            
            // Save if cargo count changed
            if (cargoCount != previousCargoCount) {
                saveCargoCount();
            }
        } catch (Exception e) {
            log.debug("Error reading cargo: {}", e.getMessage());
        }
    }

    private void updatePlayerSalvagingStatus(Player player) {
        int animation = player.getAnimation();
        
        // Check if currently doing salvaging animation
        boolean isSalvaging = (animation == SALVAGING_ANIMATION_1 || animation == SALVAGING_ANIMATION_2 || 
                               animation == SALVAGING_ANIMATION_3);
        boolean isSortingSalvage = (animation == SORTING_SALVAGE_ANIMATION);
        
        Instant now = Instant.now();
        if (isSalvaging || isSortingSalvage) {
            lastActive = now;
            salvageInfo.setPlayerSalvaging(isSalvaging);
            salvageInfo.setPlayerSortingSalvage(isSortingSalvage);
        } else {
            long millisSinceActive = Duration.between(lastActive, now).toMillis();
            int idleThreshold = config.idleThresholdMs();
            boolean stillActive = millisSinceActive < idleThreshold;
            salvageInfo.setPlayerSalvaging(stillActive && !salvageInfo.isPlayerSortingSalvage());
            salvageInfo.setPlayerSortingSalvage(stillActive && salvageInfo.isPlayerSortingSalvage());
            if (!stillActive) {
                salvageInfo.setPlayerSortingSalvage(false);
            }
        }
    }

    private void updateCrewStatus() {
        // Check for crew NPCs on the boat and their animation states
        // Only track NPCs on our own boat (same WorldView as player)
        // Note: Crew can only salvage, not sort salvage
        try {
            int crewCount = 0;
            int activeCrew = 0;
            
            Player player = client.getLocalPlayer();
            if (player == null) {
                return;
            }
            
            // Get the player's WorldView - crew on our boat share the same WorldView
            WorldView playerWorldView = player.getWorldView();
            if (playerWorldView == null) {
                return;
            }
            
            // Get NPCs from the player's WorldView (our boat only)
            for (NPC npc : playerWorldView.npcs()) {
                if (npc == null) continue;
                
                // Check if this NPC is a crew member by name
                String npcName = npc.getName();
                if (npcName != null && isCrewMember(npcName)) {
                    crewCount++;
                    
                    // Check if crew is actively salvaging
                    int npcAnimation = npc.getAnimation();
                    if (npcAnimation == SALVAGING_ANIMATION_1 || npcAnimation == SALVAGING_ANIMATION_2 || 
                        npcAnimation == SALVAGING_ANIMATION_3) {
                        activeCrew++;
                    }
                }
            }
            
            salvageInfo.setCrewCount(crewCount);
            salvageInfo.setCrewActivelySalvaging(activeCrew);
            salvageInfo.setCrewSalvaging(activeCrew > 0);
        } catch (Exception e) {
            log.debug("Error checking crew status: {}", e.getMessage());
        }
    }

    // Crewmate names from the wiki
    private static final Set<String> CREWMATE_NAMES = Set.of(
        "Jobless Jim",
        "Ex-Captain Siad",
        "Adventurer Ada",
        "Cabin Boy Jenkins",
        "Oarswoman Olga",
        "Jittery Jim",
        "Bosun Zarah",
        "Jolly Jim",
        "Spotter Virginia",
        "Sailor Jakob"
    );
    
    private boolean isCrewMember(String npcName) {
        return CREWMATE_NAMES.contains(npcName);
    }

    private void updateMonsterAlert() {
        // Detect monster attack by:
        // 1. Checking if specific salvage monsters (IDs 15210, 15196) are in range
        // 2. Detecting if boat health is going down (under attack)
        try {
            boolean monsterAttacking = false;
            String monsterName = "";
            
            int currentHealth = salvageInfo.getBoatHealth();
            
            // Method 1: Check if boat health is decreasing (being attacked)
            if (previousBoatHealth > 0 && currentHealth < previousBoatHealth) {
                monsterAttacking = true;
                monsterName = "Under Attack!";
            }
            previousBoatHealth = currentHealth;
            
            // Method 2: Check for specific salvage monster NPCs in range
            // Monsters are on the TOP LEVEL world view (the sea), not on our boat
            if (!monsterAttacking) {
                WorldPoint boatLocation = getPlayerBoatLocation();
                if (boatLocation != null && client.getTopLevelWorldView() != null) {
                    int alertRange = config.monsterAlertRange();
                    
                    // Check NPCs on the top level world view (where monsters spawn)
                    for (NPC npc : client.getTopLevelWorldView().npcs()) {
                        if (npc == null) continue;
                        
                        int npcId = npc.getId();
                        if (SALVAGE_MONSTER_IDS.contains(npcId)) {
                            WorldPoint npcLocation = npc.getWorldLocation();
                            if (npcLocation != null && npcLocation.getPlane() == boatLocation.getPlane()) {
                                // Use same distance calculation as salvage range
                                int minX = boatLocation.getX() - alertRange;
                                int maxX = boatLocation.getX() + alertRange;
                                int minY = boatLocation.getY() - alertRange;
                                int maxY = boatLocation.getY() + alertRange;
                                
                                if (npcLocation.getX() >= minX && npcLocation.getX() <= maxX &&
                                    npcLocation.getY() >= minY && npcLocation.getY() <= maxY) {
                                    monsterAttacking = true;
                                    monsterName = npc.getName() != null ? npc.getName() : "Monster";
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            
            salvageInfo.setMonsterAttacking(monsterAttacking);
            salvageInfo.setMonsterName(monsterName);
        } catch (Exception e) {
            log.debug("Error checking monster attacks: {}", e.getMessage());
        }
    }

    private void checkThresholdsAndPlaySounds() {
        if (client.getGameState() != GameState.LOGGED_IN) {
            return;
        }

        boolean shouldPlaySound = false;

        // Check boat health - only alert once when it drops below threshold
        boolean lowBoatHealth = salvageInfo.getBoatHealthPercentage() <= config.lowBoatHealthThreshold();
        if (config.playBoatHealthSound() && lowBoatHealth && !alertedLowBoatHealth) {
            shouldPlaySound = true;
            alertedLowBoatHealth = true;
        } else if (!lowBoatHealth) {
            alertedLowBoatHealth = false; // Reset when health recovers
        }

        // Check inventory full - only alert once when it becomes full AND player is salvaging
        // This prevents alerts when taking items out of cargo to clean
        boolean inventoryFull = salvageInfo.getInventoryUsedSlots() >= 28;
        boolean playerIsSalvaging = salvageInfo.isPlayerSalvaging();
        if (config.playInventorySound() && inventoryFull && playerIsSalvaging && !alertedInventoryFull) {
            shouldPlaySound = true;
            alertedInventoryFull = true;
        } else if (!inventoryFull) {
            alertedInventoryFull = false; // Reset when inventory has space
        }

        // Check cargo full - only alert once when it becomes full
        boolean cargoFull = salvageInfo.getCargoCount() >= salvageInfo.getMaxCargoCount() && salvageInfo.getMaxCargoCount() > 0;
        if (config.playCargoSound() && cargoFull && !alertedCargoFull) {
            shouldPlaySound = true;
            alertedCargoFull = true;
        } else if (!cargoFull) {
            alertedCargoFull = false; // Reset when cargo has space
        }

        // Check player idle - only alert once when player becomes idle
        // Don't alert while sorting (sorting done is handled via chat message)
        boolean playerIdle = !salvageInfo.isPlayerSalvaging() && !salvageInfo.isPlayerSortingSalvage();
        boolean playerSorting = salvageInfo.isPlayerSortingSalvage();
        
        if (config.playPlayerIdleSound() && playerIdle && !alertedPlayerIdle && !playerSorting) {
            shouldPlaySound = true;
            alertedPlayerIdle = true;
        } else if (!playerIdle) {
            alertedPlayerIdle = false; // Reset when player starts salvaging or sorting
        }

        // Check crew idle - only alert once when crew becomes idle
        boolean crewIdle = salvageInfo.getCrewCount() > 0 && !salvageInfo.isCrewSalvaging();
        if (config.playCrewIdleSound() && crewIdle && !alertedCrewIdle) {
            shouldPlaySound = true;
            alertedCrewIdle = true;
        } else if (!crewIdle) {
            alertedCrewIdle = false; // Reset when crew starts salvaging
        }

        // Check monster attack - only alert once per attack
        boolean monsterAttacking = salvageInfo.isMonsterAttacking();
        if (config.playMonsterAlertSound() && monsterAttacking && !alertedMonsterAttack) {
            shouldPlaySound = true;
            alertedMonsterAttack = true;
        } else if (!monsterAttacking) {
            alertedMonsterAttack = false; // Reset when monster stops attacking
        }

        if (shouldPlaySound) {
            playSoundEffect();
        }
    }
    
    private void playSoundEffect() {
        Preferences preferences = client.getPreferences();
        int previousVolume = preferences.getSoundEffectVolume();
        preferences.setSoundEffectVolume(config.soundVolume());
        client.playSoundEffect(SOUND_ID, config.soundVolume());
        preferences.setSoundEffectVolume(previousVolume);
    }

    @Provides
    IdleMasterConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(IdleMasterConfig.class);
    }
}
