# Public Ender Chest (Fabric 26.3)

Inspired by ArkoSammy's public ender chest mod.

A server-side Fabric mod prototype for a shared 54-slot Ender Chest inventory.

## Included features

- Shift + right-click a placed Ender Chest to open the public inventory.
- Shift + right-click while holding an Ender Chest to open it without placing it.
- One 54-slot inventory shared across players, stored in the overworld's saved data.
- Uses the vanilla six-row chest menu, so clients do not need this mod installed.
- Inventory menu synchronizes through vanilla container packets, including concurrent viewers.
- Shows name of the player and time something was put into it's inventory
- Has a log file for moderators to keep track of who is taking what (Located in world folder of the server)

## Build requirements

- Java 25
- Gradle 9.7.1 or newer
- Minecraft 26.3 / Fabric Loader 0.19.5

The project uses Fabric Loom 1.18-SNAPSHOT and Fabric API 0.165.0+26.3.
