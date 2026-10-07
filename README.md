# ClarityWardrobe
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.x-green?style=for-the-badge&logo=minecraft)
![Paper](https://img.shields.io/badge/Server-Paper-blue?style=for-the-badge)
![Java](https://img.shields.io/badge/Java-21%20%7C%2025-orange?style=for-the-badge&logo=openjdk)
![License](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge)
![Version](https://img.shields.io/badge/Version-1.0.0-brightgreen?style=for-the-badge)

A high-performance, modern, lag-free cosmetic wardrobe plugin built exclusively for Paper 1.21+ featuring real-time visual overrides, native Minecraft 1.21 ItemDisplay support, ProtocolLib packet management, Custom Model Data validation, and dynamic GUI configuration.

## Description

**ClarityWardrobe** allows players to equip and display visual cosmetic hats, custom helmets, 3D backpacks, and wings via an intuitive dynamic GUI **without compromising their actual combat armor stats, toughness, or enchantments**.

Players can fight with full Netherite or Diamond armor defense while displaying their favorite custom resource pack models. Powered by a dual visual rendering engine (**Minecraft 1.21 ItemDisplay** for smooth F5 third-person self-view + **ProtocolLib** for remote packet manipulation), cosmetics automatically update in real-time even when players swap, break, or right-click armor from their inventory.

---

## Features

- **Zero Combat Interference**: Players retain 100% of their defense, knockback resistance, toughness, and armor enchantments. Visual cosmetics only override the visual appearance.
- **Dual Cosmetic Slots**:
  - **Helmet / Hat Slot**: Supports 3D custom model hats, head accessories, horns, and custom helmets.
  - **Backpack / Wings Slot**: Renders 3D backpacks, capes, and wings attached to the player's spine.
- **Real-Time Armor Auto-Refresh**: Equipping, shift-clicking, dragging armor in inventory (`pressing E`), or right-clicking armor from hands will **never overwrite** your cosmetics. Visuals instantly refresh on the next server tick.
- **Dual Visual Rendering Architecture**:
  - **Native Minecraft 1.21 ItemDisplay**: Lightweight entity rendering with 1-tick client interpolation. Provides smooth third-person (F5) view of cosmetics for the player themselves.
  - **ProtocolLib Packet Interception**: Overrides `ENTITY_EQUIPMENT` packets sent to nearby viewers so other players see your cosmetic rather than your combat armor.
- **Strict Custom Model Data (CMD) Validation**: Prevents players from placing vanilla items into cosmetic slots. Slots only accept items possessing valid Custom Model Data IDs configured in the whitelist.
- **Fully Data-Driven GUI (`wardrobe-menu.yml`)**: Admins can customize every slot, inventory size, item lore, custom sounds, decorative filler glass, close buttons, and unequip-all shortcuts.
- **Adventure MiniMessage Support**: Full support for hex gradients, bold tags, hover events, and click actions across all menus and system messages.
- **Forced Italics Elimination**: Automatically strips default Minecraft italic formatting (`<!italic>`) on GUI items and lore for crisp, professional UI menus.
- **Crash & Desync Resilience**: Automatically purges dangling display entities upon startup, world change, death, and player disconnection.
- **Asynchronous Storage**: Player wardrobe data is cached in-memory and saved asynchronously to `wardrobe-data.yml` with configurable auto-save intervals.

---

## Requirements

- **Minecraft Version**: 1.21+ (1.21, 1.21.1, 1.21.3, 1.21.4+)
- **Server Software**: [Paper](https://papermc.io/), Purpur, or compatible forks (requires Paper API)
- **Java Version**: 21 or higher (fully compatible with Java 25)
- **Dependencies**:
  - [ProtocolLib](https://github.com/dmulloy2/ProtocolLib) (v5.4.0+ for 1.21) — Required for packet equipment overrides.
- **Optional Resource Packs / Core Plugins**:
  - [ItemsAdder](https://itemsadder.devs.beer/) or [Oraxen](https://oraxen.com/) (for custom models and textures)
  - [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/)

---

## Installation

1. Download the latest `ClarityWardrobe-1.0.0.jar` from the [Releases](https://github.com/fitrahrh/ClarityWardrobe/releases) tab.
2. Ensure **ProtocolLib 5.4.0+** is installed in your server's `plugins` folder.
3. Place `ClarityWardrobe-1.0.0.jar` into your server's `plugins` folder.
4. Restart your server to generate default configurations.
5. Customize `config.yml`, `wardrobe-menu.yml`, and `language.yml` located in `plugins/ClarityWardrobe/`.
6. Reload configuration changes in-game at any time using `/wardrobe reload` (or `/cw reload`).

---

## Configuration

### Default `config.yml`

```yaml
# ==============================================================================
#                      ClarityWardrobe Configuration
#          High-Performance, Lag-Free Cosmetic Wardrobe Plugin for Paper 1.21+
# ==============================================================================

# Settings for cosmetic visual rendering
rendering:
  # Method to override the visual helmet:
  # - PROTOCOLLIB: Intercepts ENTITY_EQUIPMENT packets so vanilla armor stays on the player
  #                and the cosmetic hat is worn directly on the head slot (never floating).
  helmet-renderer: PROTOCOLLIB

  # Method to render backpack and wings:
  # - ITEM_DISPLAY: Uses Minecraft 1.21+ native ItemDisplay entity attached to player's back.
  #                 Extremely lightweight, zero tick lag, smooth client interpolation.
  # - CHEST_PACKET: Uses ProtocolLib to intercept chestplate equipment packets (for armor/elytra models).
  backpack-renderer: ITEM_DISPLAY

  # ItemDisplay fine-tuning settings (Only used when backpack-renderer is ITEM_DISPLAY)
  item-display:
    # Offset relative to player position [X (left/right), Y (up/down), Z (forward/backward)]
    # Z is negative for behind the player's spine/back.
    offset-x: 0.0
    offset-y: 1.15
    offset-z: -0.22

    # Scale of the cosmetic item display
    scale-x: 1.0
    scale-y: 1.0
    scale-z: 1.0

    # Rotation (in degrees) to orient the cosmetic on the player's back
    # Yaw 180 flips the item to face outwards away from the back.
    rotation-yaw: 180.0
    rotation-pitch: 0.0

    # Crouch offset adjustment when player sneaks
    crouch-offset-y: -0.25
    crouch-offset-z: -0.05
    crouch-pitch-tilt: 28.0

    # Hide backpack while swimming, gliding with elytra, or sleeping
    hide-when-gliding: true
    hide-when-swimming: true
    hide-when-sleeping: true

# Sound effects for GUI interactions
sounds:
  equip:
    enabled: true
    sound: ITEM_ARMOR_EQUIP_NETHERITE
    volume: 1.0
    pitch: 1.2
  unequip:
    enabled: true
    sound: ITEM_ARMOR_EQUIP_LEATHER
    volume: 1.0
    pitch: 0.8
  error:
    enabled: true
    sound: ENTITY_VILLAGER_NO
    volume: 1.0
    pitch: 1.0
  menu-click:
    enabled: true
    sound: UI_BUTTON_CLICK
    volume: 0.8
    pitch: 1.0
  menu-open:
    enabled: true
    sound: ITEM_ARMOR_EQUIP_GENERIC
    volume: 0.7
    pitch: 1.0

# Storage settings
storage:
  # Data file stored inside plugins/ClarityWardrobe/
  file: wardrobe-data.yml
  # Auto-save interval in minutes for dirty player data
  auto-save-minutes: 5
```

---

### Default `wardrobe-menu.yml`

```yaml
title: "<gradient:#38ef7d:#11998e><bold>Clarity Wardrobe</bold></gradient> <dark_gray>| Cosmetics</dark_gray>"
rows: 6

slots:
  # HELMET COSMETIC SLOT
  helmet:
    slot: 20
    validation:
      require-custom-model-data: true
      # Allowed Custom Model Data IDs ("*" allows any custom model)
      allowed-cmd:
        - "*"
      allowed-materials:
        - "*"
    placeholder:
      material: LEATHER_HELMET
      custom-model-data: 0
      name: "<yellow><bold>Hat / Helmet Cosmetic Slot</bold></yellow>"
      lore:
        - "<dark_gray>Slot: Cosmetic Head</dark_gray>"
        - ""
        - "<gray>Status: <red>No cosmetic equipped</red>"
        - ""
        - "<green>▶ Click with a custom cosmetic hat to equip!</green>"
        - "<gray>Your real combat helmet stats are preserved.</gray>"
        - ""
        - "<dark_aqua>✦ Requires Custom Model Data</dark_aqua>"
    equipped-lore-append:
      - ""
      - "<green>✔ Status: Currently Equipped</green>"
      - "<red>▶ Click to unequip and retrieve your cosmetic item.</red>"

  # BACKPACK / WINGS COSMETIC SLOT
  backpack:
    slot: 24
    validation:
      require-custom-model-data: true
      allowed-cmd:
        - "*"
      allowed-materials:
        - "*"
    placeholder:
      material: ELYTRA
      custom-model-data: 0
      name: "<aqua><bold>Backpack / Wings Slot</bold></aqua>"
      lore:
        - "<dark_gray>Slot: Cosmetic Back/Wings</dark_gray>"
        - ""
        - "<gray>Status: <red>No cosmetic equipped</red>"
        - ""
        - "<green>▶ Click with a custom backpack or wings to equip!</green>"
        - "<gray>Rendered visually on your back without altering armor.</gray>"
        - ""
        - "<dark_aqua>✦ Requires Custom Model Data</dark_aqua>"
    equipped-lore-append:
      - ""
      - "<green>✔ Status: Currently Equipped</green>"
      - "<red>▶ Click to unequip and retrieve your cosmetic item.</red>"

# ACTION BUTTONS & DECORATIONS
buttons:
  unequip-all:
    slot: 40
    material: BARRIER
    custom-model-data: 0
    name: "<red><bold>Unequip All Cosmetics</bold></red>"
    lore:
      - "<gray>Quickly remove both cosmetic items and</gray>"
      - "<gray>return them directly to your inventory.</gray>"
      - ""
      - "<yellow>▶ Click to unequip all</yellow>"

  close:
    slot: 49
    material: FEATHER
    custom-model-data: 0
    name: "<red><bold>Close Menu</bold></red>"
    lore:
      - "<gray>Click to exit the wardrobe.</gray>"

background:
  material: GRAY_STAINED_GLASS_PANE
  custom-model-data: 0
  name: " "
```

---

### Default `language.yml`

```yaml
prefix: "<gradient:#4facfe:#00f2fe><b>ClarityWardrobe</b></gradient> <dark_gray>»</dark_gray> "

messages:
  gui-opened: "<gray>Opening your cosmetic wardrobe...</gray>"
  
  helmet-equipped: "<green>Equipped <white><item></white> as your hat cosmetic!</green>"
  helmet-unequipped: "<yellow>Helmet cosmetic unequipped and returned to inventory.</yellow>"
  
  backpack-equipped: "<green>Equipped <white><item></white> as your backpack cosmetic!</green>"
  backpack-unequipped: "<yellow>Backpack cosmetic unequipped and returned to inventory.</yellow>"
  
  all-unequipped: "<gold>All active cosmetics have been unequipped and returned.</gold>"
  no-cosmetics-equipped: "<gray>You currently have no cosmetics equipped.</gray>"
  
  # Error messages
  error-no-cmd: "<red>This item is not a cosmetic! Items must have <gold>Custom Model Data</gold> to be placed here.</red>"
  error-unapproved-cmd: "<red>This item is not an approved cosmetic for this slot (CMD: <gold><cmd></gold>).</red>"
  error-unapproved-material: "<red>This item type (<gold><material></gold>) is not allowed in this slot.</red>"
  error-inventory-full: "<red>Your inventory is full! The unequipped cosmetic was dropped at your feet.</red>"
  error-no-permission: "<red>You do not have permission to use this command.</red>"
  
  # Admin messages
  reload-success: "<green>All configuration files, language, and wardrobe menus have been reloaded!</green>"
  player-only: "<red>This command can only be executed by players.</red>"
  unknown-subcommand: "<red>Unknown subcommand! Usage: <gray>/wardrobe [reload]</gray></red>"
```

---

## 🎨 Dynamic GUI & Cosmetic Slots

The wardrobe GUI is accessible via `/wardrobe` (or `/cw`, `/cosmetics`):

| Slot Component | Default Slot Index | Description |
| :--- | :--- | :--- |
| **Helmet Cosmetic Slot** | `Slot 20` | Click with a custom hat to equip. Click again with empty cursor to unequip and retrieve the item. |
| **Backpack / Wings Slot** | `Slot 24` | Click with a custom backpack or wings to equip. Rendered on the player's spine. |
| **Unequip All Button** | `Slot 40` | Instantly unequips both cosmetics and returns them into player inventory. |
| **Close Menu Button** | `Slot 49` | Safely closes the wardrobe menu. |
| **Filler Panes** | All remaining slots | Customizable background glass or custom texture panels. |

### Whitelisting Custom Model Data IDs

To restrict slots to specific cosmetic items from your resource pack, specify exact CMD IDs in `wardrobe-menu.yml`:

```yaml
validation:
  require-custom-model-data: true
  allowed-cmd:
    - 10001 # Samurai Helmet
    - 10002 # Cat Ears
    - 10003 # Witch Hat
  allowed-materials:
    - "LEATHER_HELMET"
    - "CARVED_PUMPKIN"
```

---

## ⚔️ Real-Time Armor & Visual Rendering System

A common issue in Minecraft cosmetic plugins is that equipping real armor (e.g. Iron Helmet) in the inventory (`pressing E`) or right-clicking armor overwrites the cosmetic visual. **ClarityWardrobe solves this permanently:**

1. **PlayerArmorListener**: Listens to Paper's native `PlayerArmorChangeEvent`, `PlayerInteractEvent`, `InventoryClickEvent`, and `PlayerItemBreakEvent`.
2. **Instant Visual Reapplication**: When a player puts on, swaps, or breaks real combat armor, ClarityWardrobe schedules a 1-tick delay visual refresh that immediately re-anchors the cosmetic over the real armor.
3. **Full Combat Defense Retention**: The player's combat armor slot is never removed or swapped out, ensuring full protection, enchantments (Protection IV, Respiration, etc.), and toughness remain 100% active at all times.
4. **Equipment Packet Head Rendering**: Cosmetic hats and custom model helmets are worn directly on the player's head slot via ProtocolLib equipment packets (never floating in the air), matching vanilla armor equip visuals perfectly.

---

## Commands

### Player Commands

| Command | Aliases | Description | Permission |
| :--- | :--- | :--- | :--- |
| `/wardrobe` | `/cw`, `/cosmetics` | Open the dynamic wardrobe cosmetic GUI | `claritywardrobe.use` |

### Admin Commands

| Command | Arguments | Description | Permission |
| :--- | :--- | :--- | :--- |
| `/wardrobe reload` | `reload` | Reload `config.yml`, `wardrobe-menu.yml`, and `language.yml` | `claritywardrobe.admin` |

---

## Permissions

| Permission | Description | Default |
| :--- | :--- | :--- |
| `claritywardrobe.use` | Allows opening and interacting with the wardrobe GUI | `true` (Everyone) |
| `claritywardrobe.admin` | Allows reloading configurations and managing cosmetics | `op` (Operators) |

---

## Building from Source

### Prerequisites

- **Java Development Kit (JDK)**: Version 21 or higher (JDK 21–25)
- **Apache Maven**: Version 3.9.0 or higher

### Build Steps

```bash
# Clone the repository
git clone https://github.com/fitrahrh/ClarityWardrobe.git
cd ClarityWardrobe

# Build with Maven
mvn clean package

# Compiled JAR will be located at:
# target/ClarityWardrobe-1.0.0.jar
```

---

## Support

For bug reports, feature requests, or technical questions:
- Check existing [GitHub Issues](https://github.com/fitrahrh/ClarityWardrobe/issues).
- Open a new issue with complete server version, Java version, and console logs.

---

## License

This project is licensed under the [MIT License](LICENSE).
