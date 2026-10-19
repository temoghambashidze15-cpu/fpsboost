package dev.fpsboost;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/** Plain static fields so the hot-path mixins read them with no lookups. Loaded once at startup. */
public final class FpsBoostConfig {
    public static boolean masterEnabled = true;
    public static String preset = "none";

    public static boolean particleLimit = true;
    public static int particleMaxCount = 2500;
    public static int particleSpawnPercent = 100;
    public static double particleMaxDistanceSq = 0;
    public static boolean ambientBlockParticles = true;

    public static boolean entityCull = true;
    public static double entityMaxDistanceSq = 56 * 56;

    public static boolean blockEntityCull = true;
    public static double blockEntityMaxDistanceSq = 48 * 48;

    public static boolean lowerServerThreadPriority = false;

    public static boolean frameRecording = true;
    public static int frameBuffer = 20000;
    public static boolean gcTracking = true;
    public static boolean writeCsv = true;

    private FpsBoostConfig() {}

    public static void load() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("fpsboost.properties");
        if (!Files.exists(file)) {
            try (InputStream in = FpsBoostConfig.class.getResourceAsStream("/fpsboost-default.properties")) {
                if (in != null) {
                    Files.createDirectories(file.getParent());
                    Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                FpsBoostMod.LOGGER.warn("Could not write default config: {}", e.toString());
            }
        }

        Properties p = new Properties();
        if (Files.exists(file)) {
            try (Reader r = Files.newBufferedReader(file)) {
                p.load(r);
            } catch (IOException e) {
                FpsBoostMod.LOGGER.warn("Could not read config, using defaults: {}", e.toString());
            }
        }

        masterEnabled = bool(p, "masterEnabled", true);
        preset = str(p, "preset", "none").toLowerCase();

        particleLimit = bool(p, "particles.limitEnabled", true);
        particleMaxCount = num(p, "particles.maxCount", 2500, 50, 16384);
        particleSpawnPercent = num(p, "particles.spawnPercent", 100, 1, 100);
        int pd = num(p, "particles.maxDistance", 0, 0, 256);
        ambientBlockParticles = bool(p, "particles.ambientBlockParticles", true);

        entityCull = bool(p, "entities.cullEnabled", true);
        int ed = num(p, "entities.maxDistance", 56, 8, 512);
        blockEntityCull = bool(p, "blockEntities.cullEnabled", true);
        int bd = num(p, "blockEntities.maxDistance", 48, 8, 512);

        lowerServerThreadPriority = bool(p, "server.lowerThreadPriority", false);

        frameRecording = bool(p, "diagnostics.frameRecording", true);
        frameBuffer = num(p, "diagnostics.frameBuffer", 20000, 600, 200000);
        gcTracking = bool(p, "diagnostics.gcTracking", true);
        writeCsv = bool(p, "diagnostics.writeCsv", true);

        if (preset.equals("pvp")) {
            particleLimit = true;
            particleMaxCount = 600;
            particleSpawnPercent = 35;
            pd = 20;
            ambientBlockParticles = false;
            ed = 32;
            bd = 24;
        }

        particleMaxDistanceSq = pd * (double) pd; // 0 stays 0 = disabled
        entityMaxDistanceSq = ed * (double) ed;
        blockEntityMaxDistanceSq = bd * (double) bd;
    }

    public static String summary() {
        return "masterEnabled=" + masterEnabled + ", preset=" + preset
                + ", particles(limit=" + particleLimit + ", max=" + particleMaxCount + ", pct=" + particleSpawnPercent
                + ", dist=" + (int) Math.sqrt(particleMaxDistanceSq) + ", ambient=" + ambientBlockParticles + ")"
                + ", entityCull=" + entityCull + "@" + (int) Math.sqrt(entityMaxDistanceSq)
                + ", blockEntityCull=" + blockEntityCull + "@" + (int) Math.sqrt(blockEntityMaxDistanceSq)
                + ", lowerServerPriority=" + lowerServerThreadPriority;
    }

    private static String str(Properties p, String key, String def) {
        String v = p.getProperty(key);
        return v == null ? def : v.trim();
    }

    private static boolean bool(Properties p, String key, boolean def) {
        String v = p.getProperty(key);
        return v == null ? def : Boolean.parseBoolean(v.trim());
    }

    private static int num(Properties p, String key, int def, int min, int max) {
        String v = p.getProperty(key);
        if (v == null) return def;
        try {
            int n = Integer.parseInt(v.trim());
            return Math.max(min, Math.min(max, n));
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
