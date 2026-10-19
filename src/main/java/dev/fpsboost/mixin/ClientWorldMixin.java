package dev.fpsboost.mixin;

import dev.fpsboost.FpsBoostConfig;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin {
    /** Vanilla scans ~667 random blocks around the player every tick purely to spawn ambient particles. */
    @Inject(method = "doRandomBlockDisplayTicks", at = @At("HEAD"), cancellable = true)
    private void fpsboost$skipAmbient(int x, int y, int z, CallbackInfo ci) {
        if (FpsBoostConfig.masterEnabled && !FpsBoostConfig.ambientBlockParticles) ci.cancel();
    }
}
