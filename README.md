# FPS Boost (client) - Minecraft 1.16.5, Fabric

STATUS: source only. It has NOT been compiled or run (the build sandbox had no network access, and Fabric Loom must download Minecraft and mappings).
Mixin targets use Yarn 1.16.5+build.10 names written from knowledge of those mappings. If one is wrong, the game will refuse to start and the log names the exact mixin and target. Fix that one line and rebuild.

## Versions
| Component | Version |
|---|---|
| Minecraft | 1.16.5 |
| Fabric Loader | 0.14.9 (>= 0.11.0 works) |
| Fabric API | 0.42.0+1.16 |
| Yarn | 1.16.5+build.10 |
| Loom | 0.10-SNAPSHOT (needs Gradle 7.2+) |
| Java to run | 8 or newer |
| Java to build | JDK 8-17 |
| Sodium (optional) | 0.2.0 (the 1.16.5 build; confirmed against the Iris install guide). No newer Sodium exists for 1.16.5. |

This mod has no compile-time dependency on Sodium and does not touch chunk meshing, so it cannot duplicate Sodium's work.

## Build
    gradle wrapper --gradle-version 7.3.3
    ./gradlew build        (Windows: gradlew.bat build)
Output: build/libs/fpsboost-1.0.0+mc1.16.5.jar (not the -sources jar).

## Install
1. Install Fabric Loader for 1.16.5, then put in .minecraft/mods: fabric-api-0.42.0+1.16, sodium-fabric-mc1.16.5-0.2.0, and fpsboost jar.
2. Start the game once. config/fpsboost.properties is created.
3. Set JVM arguments (see below), then restart.

## What is implemented
- Particle cap, spawn percentage and distance limit. Refused particles are never constructed.
- Optional removal of the per-tick ambient-particle scan (torch flames, drips, redstone sparks, etc.).
- Distance culling of non-player, non-projectile entities.
- Distance culling of block entities (beacons exempt).
- PvP preset (preset=pvp): fewer particles, no ambient particles, shorter entity/block-entity distance.
- Diagnostics: allocation-free frame-time ring buffer, GC event tracking, on-demand report.
  F9 = write report + CSV to fpsboost-reports/. F10 = restart recording window.
- Optional experimental: lower the integrated server thread priority (off by default).

## What is deliberately NOT implemented
- Chunk rebuild scheduling, culling, mesh work: Sodium 0.2.0 owns this; touching it risks breakage.
- Forced or repeated System.gc(): never.
- Custom multithreading: none.
- Allocation "optimizations" in vanilla code: none are included, because I could not profile them and will not ship placebo.
- Anything touching reach, attack speed, hit registration or server logic.

## Known limitations
- Culled entities/block entities beyond the configured distance disappear (e.g. far NPCs on some servers).
- Block-break particles and emitters use a different spawn path and are not capped.
- GC durations are what the collector reports; for concurrent collectors that is not always a pure pause.
- Frame times measure the gap between render() calls, so vsync / FPS-limit waits are included.
- Effect size is unmeasured. Expect it to matter most in particle-heavy PvP, many-entity scenes and signs/chests-heavy bases.

## Test protocol (A/B on the same install)
1. Same world, same render distance, same route/scene, same time of day, game freshly started, 1 minute warm-up.
2. Scenarios: (a) flying a fixed route through unloaded chunks, (b) standing in a large mob/player fight area, (c) a base with many chests/signs.
3. Press F10 at the start, F9 after 2 minutes. Repeat 3 runs per scenario.
4. Run once with masterEnabled=false, then masterEnabled=true (and again with preset=pvp). Restart between config changes.
5. Compare avg FPS, 1% low, frames >50 ms, worst frame, GC count/max pause. Plot frames-*.csv to see whether spikes line up with GC events.
Treat differences smaller than run-to-run variation as no difference.

## JVM arguments (matter more than any mod for stutter on 4 GB RAM)
Do not give Minecraft more than about 1.5-2 GB total on a 4 GB machine; Windows itself needs the rest and paging causes huge freezes.
Start with:
    -Xms1536M -Xmx1536M -XX:+UseG1GC -XX:MaxGCPauseMillis=40 -XX:+ParallelRefProcEnabled
If the report shows frequent long GC pauses, try instead:
    -Xms1536M -Xmx1536M -XX:+UseParallelGC
Keep whichever gives the better 1% low and fewer >50 ms frames in your own report.

## Vanilla/Sodium settings that usually matter most
Render distance 6-8 (in 1.16.5 it also sets how many chunks the integrated server keeps loaded), Biome Blend low, Clouds off, Particles Decreased/Minimal, Smooth Lighting off, Entity Distance 50-75%. Check Task Manager / HWiNFO for CPU clock and temperature during a drop: a sustained 70 -> 40 -> 20 slide with no GC events is the typical sign of thermal or power throttling, which no mod can fix.
