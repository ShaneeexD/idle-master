package com.idlemaster;

import net.runelite.client.config.*;

@ConfigGroup("idlemaster")
public interface IdleMasterConfig extends Config {

    // ==================== SAILING - SALVAGING ====================
    @ConfigSection(
        name = "Sailing - Salvaging",
        description = "Settings for salvaging activities.",
        position = 0,
        closedByDefault = false
    )
    String salvagingSection = "salvagingSection";

    // --- Hide Other Boats ---
    @ConfigItem(
        keyName = "hideOtherBoats",
        name = "Hide Other Boats",
        description = "Hide other players' boats when in salvage range.",
        section = salvagingSection,
        position = 0
    )
    default boolean hideOtherBoats() { return true; }

    // --- Boat Health Section ---
    @ConfigSection(
        name = "Sailing - Boat Health",
        description = "Settings for boat health display and alerts.",
        position = 10,
        closedByDefault = true
    )
    String boatHealthSection = "boatHealthSection";

    @ConfigItem(
        keyName = "showBoatHealth",
        name = "Show Boat Health",
        description = "Display boat health bar in the overlay.",
        section = boatHealthSection,
        position = 1
    )
    default boolean showBoatHealth() { return true; }

    @ConfigItem(
        keyName = "lowBoatHealthThreshold",
        name = "Low health threshold (%)",
        description = "Play sound when boat health is at or below this percentage.",
        section = boatHealthSection,
        position = 2
    )
    @Range(min = 1, max = 100)
    default int lowBoatHealthThreshold() { return 30; }

    @ConfigItem(
        keyName = "playBoatHealthSound",
        name = "Play sound when low",
        description = "Play a sound when boat health is low.",
        section = boatHealthSection,
        position = 3
    )
    default boolean playBoatHealthSound() { return true; }

    // --- Inventory Section ---
    @ConfigSection(
        name = "Sailing - Inventory",
        description = "Settings for inventory display.",
        position = 20,
        closedByDefault = true
    )
    String inventorySection = "inventorySection";

    @ConfigItem(
        keyName = "showInventory",
        name = "Show Inventory",
        description = "Display inventory usage in the overlay.",
        section = inventorySection,
        position = 1
    )
    default boolean showInventory() { return true; }

    @ConfigItem(
        keyName = "playInventorySound",
        name = "Play sound when full",
        description = "Play a sound when inventory is full.",
        section = inventorySection,
        position = 2
    )
    default boolean playInventorySound() { return false; }

    // --- Cargo Section ---
    @ConfigSection(
        name = "Sailing - Cargo",
        description = "Settings for cargo display.",
        position = 30,
        closedByDefault = true
    )
    String cargoSection = "cargoSection";

    @ConfigItem(
        keyName = "showCargo",
        name = "Show Cargo",
        description = "Display cargo count in the overlay.",
        section = cargoSection,
        position = 1
    )
    default boolean showCargo() { return true; }

    @ConfigItem(
        keyName = "playCargoSound",
        name = "Play sound when full",
        description = "Play a sound when cargo is full.",
        section = cargoSection,
        position = 2
    )
    default boolean playCargoSound() { return true; }

    @ConfigItem(
        keyName = "showSalvageSpots",
        name = "Show Salvage Spots",
        description = "Display count of active salvage spots in range (e.g., 2/2 for double spot).",
        section = salvagingSection,
        position = 1
    )
    default boolean showSalvageSpots() { return true; }

    @ConfigItem(
        keyName = "playSalvageSpotSound",
        name = "Play sound when spot respawns",
        description = "Play a sound when a salvage spot becomes active after being depleted.",
        section = salvagingSection,
        position = 2
    )
    default boolean playSalvageSpotSound() { return true; }

    // --- Player Status Section ---
    @ConfigSection(
        name = "Sailing - Player Status",
        description = "Settings for player salvaging status.",
        position = 40,
        closedByDefault = true
    )
    String playerStatusSection = "playerStatusSection";

    @ConfigItem(
        keyName = "showPlayerStatus",
        name = "Show Player Status",
        description = "Display whether player is actively salvaging.",
        section = playerStatusSection,
        position = 1
    )
    default boolean showPlayerStatus() { return true; }

    @ConfigItem(
        keyName = "idleThresholdMs",
        name = "Idle threshold (ms)",
        description = "Time in milliseconds before player is considered idle.",
        section = playerStatusSection,
        position = 2
    )
    @Range(min = 500, max = 10000)
    default int idleThresholdMs() { return 1200; }

    @ConfigItem(
        keyName = "playPlayerIdleSound",
        name = "Play sound when shipwreck depleted",
        description = "Play a sound when the shipwreck is fully salvaged and reclaimed by the sea.",
        section = playerStatusSection,
        position = 3
    )
    default boolean playPlayerIdleSound() { return true; }

    @ConfigItem(
        keyName = "playSortingDoneSound",
        name = "Play sound when sorting done",
        description = "Play a sound when player finishes sorting salvage.",
        section = playerStatusSection,
        position = 4
    )
    default boolean playSortingDoneSound() { return true; }

    // --- Crew Status Section ---
    @ConfigSection(
        name = "Sailing - Crew Status",
        description = "Settings for crew salvaging status.",
        position = 50,
        closedByDefault = true
    )
    String crewStatusSection = "crewStatusSection";

    @ConfigItem(
        keyName = "showCrewStatus",
        name = "Show Crew Status",
        description = "Display crew salvaging status.",
        section = crewStatusSection,
        position = 1
    )
    default boolean showCrewStatus() { return true; }

    @ConfigItem(
        keyName = "playCrewIdleSound",
        name = "Play sound when crew idle",
        description = "Play a sound when crew stops salvaging.",
        section = crewStatusSection,
        position = 2
    )
    default boolean playCrewIdleSound() { return false; }

    // --- Monster Alert Section ---
    @ConfigSection(
        name = "Sailing - Monster Alert",
        description = "Settings for monster attack alerts.",
        position = 60,
        closedByDefault = true
    )
    String monsterAlertSection = "monsterAlertSection";

    @ConfigItem(
        keyName = "showMonsterAlert",
        name = "Show Monster Alert",
        description = "Display alert when monsters are attacking. Background flashes red.",
        section = monsterAlertSection,
        position = 1
    )
    default boolean showMonsterAlert() { return true; }

    @ConfigItem(
        keyName = "monsterAlertRange",
        name = "Alert range (tiles)",
        description = "Range in tiles to check for monster attacks.",
        section = monsterAlertSection,
        position = 2
    )
    @Range(min = 1, max = 20)
    default int monsterAlertRange() { return 15; }

    @ConfigItem(
        keyName = "playMonsterAlertSound",
        name = "Play sound on attack",
        description = "Play a sound when monsters are attacking.",
        section = monsterAlertSection,
        position = 3
    )
    default boolean playMonsterAlertSound() { return true; }

    // --- XP Progress Section ---
    @ConfigSection(
        name = "Sailing - XP Progress",
        description = "Settings for sailing XP progress display.",
        position = 65,
        closedByDefault = true
    )
    String xpProgressSection = "xpProgressSection";

    @ConfigItem(
        keyName = "showXpBar",
        name = "Show XP Bar",
        description = "Display sailing XP progress bar at the bottom of the overlay.",
        section = xpProgressSection,
        position = 1
    )
    default boolean showXpBar() { return true; }

    // --- Window Settings Section ---
    @ConfigSection(
        name = "General Settings",
        description = "Configure general overlay settings.",
        position = 70
    )
    String windowSection = "windowSection";

    @ConfigItem(
        keyName = "opacity",
        name = "Opacity",
        description = "Set the opacity level of the overlay (0 = transparent, 255 = opaque)",
        section = windowSection,
        position = 1
    )
    @Range(min = 0, max = 255)
    default int opacity() { return 200; }

    @ConfigItem(
        keyName = "showCloseButton",
        name = "Show Close Button",
        description = "Show a close button on the overlay.",
        section = windowSection,
        position = 2
    )
    default boolean showCloseButton() { return true; }

    @ConfigItem(
        keyName = "showMinimizeButton",
        name = "Show Minimize Button",
        description = "Show a minimize button on the overlay.",
        section = windowSection,
        position = 3
    )
    default boolean showMinimizeButton() { return true; }

    @ConfigItem(
        keyName = "showCharacterName",
        name = "Show Character Name",
        description = "Show the character name in the overlay.",
        section = windowSection,
        position = 4
    )
    default boolean showCharacterName() { return true; }

    @ConfigItem(
        keyName = "showWindowBorder",
        name = "Show Window Border",
        description = "Show the window border.",
        section = windowSection,
        position = 5
    )
    default boolean showWindowBorder() { return true; }

    @ConfigItem(
        keyName = "soundVolume",
        name = "Sound Volume",
        description = "Volume for alert sounds.",
        section = windowSection,
        position = 6
    )
    @Range(min = 0, max = 100)
    default int soundVolume() { return 50; }

    @ConfigItem(
        keyName = "resetPosition",
        name = "Reset Position",
        description = "Reset the overlay window position to default.",
        section = windowSection,
        position = 7
    )
    default boolean resetPosition() { return false; }

    @ConfigItem(
        keyName = "showOverlay",
        name = "Restore Overlay",
        description = "Restore the overlay window if it was closed.",
        section = windowSection,
        position = 8
    )
    default boolean showOverlay() { return false; }

    // ==================== THIEVING ====================
    @ConfigSection(
        name = "Thieving - Wealthy Citizens",
        description = "Settings for Wealthy Citizens pickpocketing overlay.",
        position = 100,
        closedByDefault = true
    )
    String thievingSection = "thievingSection";

    @ConfigItem(
        keyName = "enableThievingOverlay",
        name = "Enable Thieving Overlay",
        description = "Show overlay when near Wealthy Citizens NPCs for pickpocketing.",
        section = thievingSection,
        position = 0
    )
    default boolean enableThievingOverlay() { return true; }

    @ConfigItem(
        keyName = "showThievingPlayerStatus",
        name = "Show Player Status",
        description = "Display whether player is actively pickpocketing.",
        section = thievingSection,
        position = 1
    )
    default boolean showThievingPlayerStatus() { return true; }

    @ConfigItem(
        keyName = "showThievingXpBar",
        name = "Show XP Bar",
        description = "Display thieving XP progress bar.",
        section = thievingSection,
        position = 2
    )
    default boolean showThievingXpBar() { return true; }

    @ConfigItem(
        keyName = "showThievingDistractionStatus",
        name = "Show Distraction Status",
        description = "Display whether a Wealthy Citizen is currently distracted.",
        section = thievingSection,
        position = 3
    )
    default boolean showThievingDistractionStatus() { return true; }

    @ConfigItem(
        keyName = "showThievingCoinPouches",
        name = "Show Coin Pouches",
        description = "Display coin pouch count (limit based on Ardougne diary).",
        section = thievingSection,
        position = 4
    )
    default boolean showThievingCoinPouches() { return true; }

    @ConfigItem(
        keyName = "playThievingDistractionStartSound",
        name = "Sound: Distraction Start",
        description = "Play a sound when a Wealthy Citizen becomes distracted.",
        section = thievingSection,
        position = 5
    )
    default boolean playThievingDistractionStartSound() { return true; }

    @ConfigItem(
        keyName = "playThievingDistractionEndSound",
        name = "Sound: Distraction End",
        description = "Play a sound when a Wealthy Citizen is no longer distracted.",
        section = thievingSection,
        position = 6
    )
    default boolean playThievingDistractionEndSound() { return true; }

    @ConfigItem(
        keyName = "playThievingPouchFullSound",
        name = "Sound: Pouches Full",
        description = "Play a sound when coin pouches reach the limit.",
        section = thievingSection,
        position = 7
    )
    default boolean playThievingPouchFullSound() { return true; }

    @ConfigItem(
        keyName = "playThievingIdleSound",
        name = "Sound: Idle Alert",
        description = "Play a sound when idle for too long while thieving.",
        section = thievingSection,
        position = 8
    )
    default boolean playThievingIdleSound() { return true; }

    @ConfigItem(
        keyName = "thievingIdleThreshold",
        name = "Idle Threshold (seconds)",
        description = "Seconds of inactivity before playing idle sound.",
        section = thievingSection,
        position = 9
    )
    @Range(min = 1, max = 60)
    default int thievingIdleThreshold() { return 5; }

    @ConfigItem(
        keyName = "hideNpcsDuringDistraction",
        name = "Hide NPCs During Distraction",
        description = "Hide all NPCs except the distracted citizen when in thieving area.",
        section = thievingSection,
        position = 10
    )
    default boolean hideNpcsDuringDistraction() { return false; }

    @ConfigItem(
        keyName = "highlightDistractedCitizen",
        name = "Highlight Distracted Citizen",
        description = "Highlight the distracted wealthy citizen with a green outline.",
        section = thievingSection,
        position = 11
    )
    default boolean highlightDistractedCitizen() { return true; }
}
