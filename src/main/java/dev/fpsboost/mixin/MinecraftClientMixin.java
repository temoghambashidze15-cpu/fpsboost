package dev.fpsboost.mixin;

import dev.fpsboost.FpsBoostConfig;
import dev.fpsboost.FrameStats;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    /** Frame interval = time between consecutive render() entries. Diagnostics only; runs even when masterEnabled=false. */
    @Inject(method = "render(Z)V", at = @At("HEAD"))
    private void fpsboost$frame(boolean tick, CallbackInfo ci) {
        if (FpsBoostConfig.frameRecording) FrameStats.onFrame();
    }
}
