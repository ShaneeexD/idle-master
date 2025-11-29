# Idle Master

A RuneLite plugin that provides a detachable overlay for AFK sailing activities in Old School RuneScape.

## Features

### Salvaging Mode
- **Boat Health** - Monitor your boat's hull integrity with color-coded warnings
- **Inventory** - Track your personal inventory usage
- **Cargo Count** - See how much cargo you've collected
- **Player Status** - Know when you stop salvaging (idle detection)
- **Crew Status** - Monitor if your crew members are actively working
- **Monster Alert** - Get warned when monsters attack nearby players

### Overlay Features
- Detachable floating window (works on single monitor setups)
- Resizable and draggable
- Configurable opacity and colors
- Sound alerts for various thresholds
- Click-through to RuneLite window

## Configuration

All features can be toggled and customized in the plugin settings:
- Enable/disable individual display elements
- Set threshold values for alerts
- Customize colors for each alert type
- Adjust sound volume and enable/disable sounds

## Building

```bash
./gradlew build
```

## Installation

1. Build the plugin
2. Copy the JAR to your RuneLite plugins folder
3. Enable "Idle Master" in RuneLite plugin configuration

## Notes

- Widget IDs and Varbits for sailing are placeholders and need to be discovered using RuneLite developer tools
- Crew member detection uses name matching - update `isCrewMember()` with actual crew NPC names
- Animation IDs for salvaging need verification in-game

## Future Plans

- Support for other sailing activities (voyages, combat, etc.)
- More detailed crew management
- Integration with other sailing plugins