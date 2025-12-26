# SBPCLifestealItems

SBPCLifestealItems adds a set of unlockable lifesteal-themed items to SBPC servers, giving players new progression rewards without bypassing the base game balance. The plugin targets Spigot/Paper 1.21+ and depends on SBPC for custom item unlocks.【F:src/plugin.yml†L1-L7】【F:pom.xml†L18-L33】

## Requirements & Installation
- **Server:** Spigot/Paper 1.21+ (Java 17).【F:pom.xml†L18-L54】
- **Dependency:** [SBPC](https://github.com/) must be installed and loaded before this plugin (declared as a hard dependency).【F:src/plugin.yml†L5-L7】
- **Build:** `mvn package` produces the plugin JAR; drop it into your `plugins/` folder alongside SBPC and restart the server.【F:pom.xml†L18-L57】

## Custom Items & How to Use Them
All four items are SBPC unlockables—once a player unlocks the item, its crafting recipe is discovered for them in-game. Display names and lore are configurable; the summaries below use the defaults from `config.yml`.

### Tracking Compass
- **What it does:** Right-click to lock onto a random other player for 15 seconds; the compass ticks every 3 seconds and then shatters when time is up.【F:src/config.yml†L8-L69】
- **Example:** Craft the compass (after unlocking), right-click while exploring, and follow the target indicator until it breaks; useful for hunting rival teams during lifesteal events.

### Armor Padding
- **What it does:** Craft padding items and combine them with armor pieces to add temporary padding. Each padding gives +2 units, up to 5 per piece; works on all vanilla armor types listed in the config.【F:src/config.yml†L13-L46】【F:src/config.yml†L71-L76】
- **Example:** Add padding to a diamond chestplate before a raid to soak up a few extra hearts of incoming damage; padding is consumed as it absorbs hits.

### Enchanted Branch
- **What it does:** Swap the branch with another inventory item to roll a random enchantment the player has unlocked; feedback messages tell the player if the enchant sticks or no valid enchant is found.【F:src/config.yml†L48-L85】
- **Example:** Drag the branch onto an iron sword to try for Sharpness I/II without needing an enchanting table—great for mid-game gear upgrades.

### Heart Meds
- **What it does:** A consumable that restores +1 maximum heart (once per player) if the player has lost max health; blocked if the player already used one or is at full hearts.【F:src/config.yml†L51-L95】
- **Example:** After dying and losing a heart, right-click the Heart Meds to regain that lost max health and keep competing in a lifesteal season.

## Configuration Highlights
`config.yml` exposes tuning knobs so you can fit the items to your server’s pacing:
- Tracking Compass duration and tick interval.【F:src/config.yml†L8-L11】
- Armor Padding strength, cap per piece, and allowed armor materials.【F:src/config.yml†L13-L46】
- Toggle for Enchanted Branch (future use).【F:src/config.yml†L48-L49】
- Heart Meds max-health restoration amount.【F:src/config.yml†L51-L53】
- All item names, lore, and player-facing messages for localization/branding.【F:src/config.yml†L55-L95】

## Tips for Players & Admins
- Remind players they must unlock items through SBPC progression before recipes appear; once unlocked, recipes are auto-discovered on join or unlock events.【F:src/plugin.yml†L1-L7】
- Encourage strategic use: pair Armor Padding with Heart Meds to mitigate big hits and recover max health, while the Tracking Compass helps locate opponents to trigger lifesteal combat.
