package com.idlemaster;

import net.runelite.client.config.*;
import java.awt.Color;

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
        description = "Display boat health in the overlay.",
        section = boatHealthSection,
        position = 1
    )
    default boolean showBoatHealth() { return true; }

    @ConfigItem(
        keyName = "highlightBoatHealthBackground",
        name = "Highlight when low",
        description = "Highlight background when boat health is low.",
        section = boatHealthSection,
        position = 2
    )
    default boolean highlightBoatHealthBackground() { return false; }

    @ConfigItem(
        keyName = "lowBoatHealthThreshold",
        name = "Low health threshold",
        description = "Highlight when boat health is at or below this percentage.",
        section = boatHealthSection,
        position = 3
    )
    @Range(min = 1, max = 100)
    default int lowBoatHealthThreshold() { return 30; }

    @Alpha
    @ConfigItem(
        keyName = "lowBoatHealthColor",
        name = "Low health color",
        description = "Background color when boat health is low.",
        section = boatHealthSection,
        position = 4
    )
    default Color lowBoatHealthColor() { return new Color(255, 50, 50, 180); }

    @ConfigItem(
        keyName = "playBoatHealthSound",
        name = "Play sound when low",
        description = "Play a sound when boat health is low.",
        section = boatHealthSection,
        position = 5
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
        keyName = "highlightInventoryBackground",
        name = "Highlight when full",
        description = "Highlight background when inventory is full.",
        section = inventorySection,
        position = 2
    )
    default boolean highlightInventoryBackground() { return false; }

    @Alpha
    @ConfigItem(
        keyName = "fullInventoryColor",
        name = "Full inventory color",
        description = "Background color when inventory is full.",
        section = inventorySection,
        position = 3
    )
    default Color fullInventoryColor() { return new Color(255, 180, 50, 180); }

    @ConfigItem(
        keyName = "playInventorySound",
        name = "Play sound when full",
        description = "Play a sound when inventory is full.",
        section = inventorySection,
        position = 4
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
        keyName = "highlightCargoBackground",
        name = "Highlight when full",
        description = "Highlight background when cargo is full.",
        section = cargoSection,
        position = 2
    )
    default boolean highlightCargoBackground() { return false; }

    @Alpha
    @ConfigItem(
        keyName = "fullCargoColor",
        name = "Full cargo color",
        description = "Background color when cargo is full.",
        section = cargoSection,
        position = 3
    )
    default Color fullCargoColor() { return new Color(50, 255, 50, 180); }

    @ConfigItem(
        keyName = "playCargoSound",
        name = "Play sound when full",
        description = "Play a sound when cargo is full.",
        section = cargoSection,
        position = 4
    )
    default boolean playCargoSound() { return true; }

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
        keyName = "highlightPlayerIdleBackground",
        name = "Highlight when idle",
        description = "Highlight background when player is not salvaging.",
        section = playerStatusSection,
        position = 2
    )
    default boolean highlightPlayerIdleBackground() { return false; }

    @Alpha
    @ConfigItem(
        keyName = "playerIdleColor",
        name = "Idle color",
        description = "Background color when player is idle.",
        section = playerStatusSection,
        position = 3
    )
    default Color playerIdleColor() { return new Color(255, 200, 100, 180); }

    @ConfigItem(
        keyName = "idleThresholdMs",
        name = "Idle threshold (ms)",
        description = "Time in milliseconds before player is considered idle.",
        section = playerStatusSection,
        position = 4
    )
    @Range(min = 500, max = 10000)
    default int idleThresholdMs() { return 1200; }

    @ConfigItem(
        keyName = "playPlayerIdleSound",
        name = "Play sound when idle",
        description = "Play a sound when player stops salvaging.",
        section = playerStatusSection,
        position = 5
    )
    default boolean playPlayerIdleSound() { return true; }

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
        keyName = "highlightCrewIdleBackground",
        name = "Highlight when crew idle",
        description = "Highlight background when crew members are not salvaging.",
        section = crewStatusSection,
        position = 2
    )
    default boolean highlightCrewIdleBackground() { return false; }

    @Alpha
    @ConfigItem(
        keyName = "crewIdleColor",
        name = "Crew idle color",
        description = "Background color when crew is idle.",
        section = crewStatusSection,
        position = 3
    )
    default Color crewIdleColor() { return new Color(200, 150, 100, 180); }

    @ConfigItem(
        keyName = "playCrewIdleSound",
        name = "Play sound when crew idle",
        description = "Play a sound when crew stops salvaging.",
        section = crewStatusSection,
        position = 4
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
        description = "Display alert when monsters are attacking nearby players.",
        section = monsterAlertSection,
        position = 1
    )
    default boolean showMonsterAlert() { return true; }

    @ConfigItem(
        keyName = "highlightMonsterAlertBackground",
        name = "Highlight when attacked",
        description = "Highlight background when monsters are attacking.",
        section = monsterAlertSection,
        position = 2
    )
    default boolean highlightMonsterAlertBackground() { return false; }

    @Alpha
    @ConfigItem(
        keyName = "monsterAlertColor",
        name = "Monster alert color",
        description = "Background color when monsters are attacking.",
        section = monsterAlertSection,
        position = 3
    )
    default Color monsterAlertColor() { return new Color(255, 0, 0, 200); }

    @ConfigItem(
        keyName = "monsterAlertRange",
        name = "Alert range (tiles)",
        description = "Range in tiles to check for monster attacks on nearby players.",
        section = monsterAlertSection,
        position = 4
    )
    @Range(min = 1, max = 20)
    default int monsterAlertRange() { return 10; }

    @ConfigItem(
        keyName = "playMonsterAlertSound",
        name = "Play sound on attack",
        description = "Play a sound when monsters are attacking.",
        section = monsterAlertSection,
        position = 5
    )
    default boolean playMonsterAlertSound() { return true; }

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
}
