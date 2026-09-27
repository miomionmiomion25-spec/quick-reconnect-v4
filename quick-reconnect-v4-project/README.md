# Quick Reconnect v4

Fabric client mod for Minecraft 1.21.11.

## Requirements

- Minecraft 1.21.11
- Fabric Loader 0.19.3 or newer
- Fabric API 0.141.6+1.21.11 or newer
- Java 21

## Behaviour

- Default key: **R**
- The key is a normal Minecraft key binding and can be changed under **Options -> Controls -> Key Binds**.
- Pressing the key while connected to a multiplayer server saves the current server, disconnects immediately, waits exactly 1000 ms, and performs one reconnect attempt.
- There is no reconnect loop.
- Leaving a server normally through the Esc menu does not schedule a reconnect.
- If the reconnect attempt fails, the mod does not retry automatically.

## Build

From this directory, with Java 21 and Gradle 8.14+ installed:

```text
gradle build
```

The remapped jar is created in `build/libs/quick-reconnect-v4-4.0.0.jar`.

Put that jar into `.minecraft/mods`.
