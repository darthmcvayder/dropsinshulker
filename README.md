# Offhand Shulker Pickup (Fabric, Minecraft 26.3)

Hold a shulker box in your offhand and items you pick up go straight into it.
Server-side only: players don't need to install anything. No Fabric API required.

## Behavior
- Only a single shulker box (stack of 1) in the offhand counts.
- Fills existing matching stacks first, then empty slots; respects stack sizes.
- Shulker boxes and other items vanilla forbids inside containers are never inserted.
- Anything that doesn't fit goes to your normal inventory as usual.
- Pickup stats, sound and animation work as normal.

## Build
**Easiest (no setup):** push this folder to a GitHub repo. The included workflow
builds it; download the jar from the run's *Artifacts* section.

**Locally:** install JDK 25 and Gradle 9.6, then run `gradle build`.
The jar lands in `build/libs/` (use the one without `-sources`).

## Install
Drop the jar into your Fabric 26.3 server's `mods/` folder and restart.
