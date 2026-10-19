package dev.fpsboost;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.LiteralText;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

public final class FpsBoostMod implements ClientModInitializer {
    public static final Logger LOGGER = LogManager.getLogger("fpsboost");

    private static int rng = 0x2545F491;

    /** Cheap xorshift32 returning 0..99; avoids ThreadLocalRandom lookups in the particle hot path. */
    public static int nextPercent() {
        int x = rng;
        x ^= x << 13;
        x ^= x >>> 17;
        x ^= x << 5;
        rng = x;
        return (x >>> 1) % 100;
    }

    @Override
    public void onInitializeClient() {
        FpsBoostConfig.load();
        FrameStats.init(FpsBoostConfig.frameBuffer);
        if (FpsBoostConfig.gcTracking) GcWatcher.init();
        LOGGER.info("FPS Boost loaded: {}", FpsBoostConfig.summary());

        KeyBinding report = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.fpsboost.report", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F9, "category.fpsboost"));
        KeyBinding reset = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.fpsboost.reset", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F10, "category.fpsboost"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (report.wasPressed()) PerfReport.dump(client);
            while (reset.wasPressed()) {
                FrameStats.reset();
                GcWatcher.reset();
                if (client.inGameHud != null) {
                    client.inGameHud.getChatHud().addMessage(new LiteralText("[FPSBoost] Recording restarted."));
                }
            }
        });

        if (FpsBoostConfig.masterEnabled && FpsBoostConfig.lowerServerThreadPriority) {
            ServerLifecycleEvents.SERVER_STARTED.register(server -> {
                Thread t = Thread.currentThread();
                if (!server.isDedicated() && "Server thread".equals(t.getName())) {
                    t.setPriority(Math.max(Thread.MIN_PRIORITY, Thread.NORM_PRIORITY - 1));
                    LOGGER.info("Integrated server thread priority lowered to {}", t.getPriority());
                }
            });
        }
    }
}
