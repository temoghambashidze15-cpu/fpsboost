package dev.fpsboost;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.LiteralText;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;

/** Builds the on-demand report (F9). All the allocation happens here, never per frame. */
public final class PerfReport {
    private PerfReport() {}

    public static void dump(MinecraftClient mc) {
        long[] f = FrameStats.snapshot();
        int n = f.length;
        if (n < 60) {
            say(mc, "Not enough frames recorded yet (" + n + "). Play a bit longer.");
            return;
        }

        long total = 0;
        for (long v : f) total += v;
        long[] s = f.clone();
        Arrays.sort(s);

        double avgMs = total / (double) n / 1e6;
        double medMs = s[n / 2] / 1e6;
        double p99Ms = s[Math.min(n - 1, (int) Math.floor(n * 0.99))] / 1e6;
        double maxMs = s[n - 1] / 1e6;
        double low1 = lowFps(s, 0.01);
        double low01 = lowFps(s, 0.001);

        int spikes = 0, over50 = 0, over100 = 0;
        for (long v : s) {
            double ms = v / 1e6;
            if (ms > 2 * medMs) spikes++;
            if (ms > 50) over50++;
            if (ms > 100) over100++;
        }

        long[] gc = GcWatcher.snapshot();
        Runtime rt = Runtime.getRuntime();
        long usedMb = (rt.totalMemory() - rt.freeMemory()) >> 20;
        long committedMb = rt.totalMemory() >> 20;
        long maxMb = rt.maxMemory() >> 20;

        StringBuilder sb = new StringBuilder(1024);
        sb.append("FPS Boost performance report\n");
        sb.append("config: ").append(FpsBoostConfig.summary()).append('\n');
        sb.append("sodium present: ").append(FabricLoader.getInstance().isModLoaded("sodium")).append('\n');
        sb.append(String.format(Locale.ROOT, "render distance: %d, fps limit: %d, vsync: %b%n",
                mc.options.viewDistance, mc.options.maxFps, mc.options.enableVsync));
        sb.append(String.format(Locale.ROOT, "window: %d frames, %.1f s%n", n, total / 1e9));
        sb.append(String.format(Locale.ROOT, "average FPS: %.1f (avg frame %.2f ms)%n", 1000.0 / avgMs, avgMs));
        sb.append(String.format(Locale.ROOT, "median frame: %.2f ms (%.1f FPS)%n", medMs, 1000.0 / medMs));
        sb.append(String.format(Locale.ROOT, "1%% low FPS: %.1f   0.1%% low FPS: %.1f  (average of the slowest frames)%n", low1, low01));
        sb.append(String.format(Locale.ROOT, "99th percentile frame: %.2f ms, worst frame: %.2f ms%n", p99Ms, maxMs));
        sb.append(String.format(Locale.ROOT, "spikes (>2x median): %d, frames >50 ms: %d, frames >100 ms: %d%n", spikes, over50, over100));
        sb.append(String.format(Locale.ROOT, "heap: %d MB used / %d MB committed / %d MB max%n", usedMb, committedMb, maxMb));
        sb.append(String.format(Locale.ROOT, "GC events: %d, total %d ms, longest %d ms, >=50 ms: %d (since last reset)%n",
                gc[0], gc[1], gc[2], gc[3]));

        Path dir = FabricLoader.getInstance().getGameDir().resolve("fpsboost-reports");
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path txt = dir.resolve("report-" + stamp + ".txt");
        try {
            Files.createDirectories(dir);
            Files.write(txt, sb.toString().getBytes(StandardCharsets.UTF_8));
            if (FpsBoostConfig.writeCsv) {
                Path csv = dir.resolve("frames-" + stamp + ".csv");
                try (BufferedWriter w = Files.newBufferedWriter(csv, StandardCharsets.UTF_8)) {
                    w.write("frame,ms\n");
                    for (int i = 0; i < n; i++) {
                        w.write(i + "," + String.format(Locale.ROOT, "%.3f", f[i] / 1e6) + "\n");
                    }
                }
            }
        } catch (IOException e) {
            FpsBoostMod.LOGGER.warn("Could not write report: {}", e.toString());
        }

        say(mc, String.format(Locale.ROOT, "avg %.1f | 1%% low %.1f | worst %.0f ms | GC %d (max %d ms) | saved to fpsboost-reports/",
                1000.0 / avgMs, low1, maxMs, gc[0], gc[2]));
    }

    private static double lowFps(long[] sortedAsc, double fraction) {
        int n = sortedAsc.length;
        int k = Math.max(1, (int) (n * fraction));
        long sum = 0;
        for (int i = n - k; i < n; i++) sum += sortedAsc[i];
        double avgMs = sum / (double) k / 1e6;
        return 1000.0 / avgMs;
    }

    private static void say(MinecraftClient mc, String msg) {
        if (mc.inGameHud != null) {
            mc.inGameHud.getChatHud().addMessage(new LiteralText("[FPSBoost] " + msg));
        }
    }
}
