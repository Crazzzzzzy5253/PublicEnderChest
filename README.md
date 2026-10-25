# Public Ender Chest (Fabric 26.3)

A server-side Fabric mod prototype for a shared 54-slot Ender Chest inventory.

## Included features

- Shift + right-click a placed Ender Chest to open the public inventory.
- Shift + right-click while holding an Ender Chest to open it without placing it.
- `/publicenderchest` command for players; `/publicenderchest info` is operator-only (permission level 2).
- One 54-slot inventory shared across players, stored in the overworld's saved data.
- Uses the vanilla six-row chest menu, so clients do not need this mod installed.
- Inventory menu synchronizes through vanilla container packets, including concurrent viewers.

## Build requirements

- Java 25
- Gradle 9.7.1 or newer
- Minecraft 26.3 / Fabric Loader 0.19.5

The project uses Fabric Loom 1.18-SNAPSHOT and Fabric API 0.165.0+26.3.

## Build

From this folder, run:

```bash
gradle build
```

The output JAR should be in `build/libs/`. Install the JAR and Fabric API in the server's `mods` folder. Players do not need the mod.

## Important testing note

This source project was generated as a starting implementation and has not been compiled against the 26.3 mappings in this environment. Minecraft 26.3 is a recent, unobfuscated release and some inventory/NBT APIs may need small mapping-specific adjustments before it builds. Test on a copy of your world before using it on a live server.
