package dev.fpsboost.mixin;

import dev.fpsboost.FpsBoostConfig;
import dev.fpsboost.FpsBoostMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.render.Camera;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Queue;

@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {
    @Unique private int fpsboost$alive;
    @Unique private int fpsboost$addedThisTick;

    /** Recount once per tick (a handful of O(1) size() calls) instead of on every spawn. */
    @Inject(method = "tick", at = @At("TAIL"))
    private void fpsboost$recount(CallbackInfo ci) {
        int n = 0;
        for (Queue<Particle> q : ((ParticleManagerAccessor) (Object) this).fpsboost$getParticles().values()) {
            n += q.size();
        }
        fpsboost$alive = n;
        fpsboost$addedThisTick = 0;
    }

    /** Refusing here means the Particle object is never constructed. */
    @Inject(method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;",
            at = @At("HEAD"), cancellable = true)
    private void fpsboost$limit(ParticleEffect effect, double x, double y, double z,
                                double vx, double vy, double vz, CallbackInfoReturnable<Particle> cir) {
        if (!FpsBoostConfig.masterEnabled || !FpsBoostConfig.particleLimit) return;

        if (fpsboost$alive + fpsboost$addedThisTick >= FpsBoostConfig.particleMaxCount) {
            cir.setReturnValue(null);
            return;
        }
        if (FpsBoostConfig.particleSpawnPercent < 100 && FpsBoostMod.nextPercent() >= FpsBoostConfig.particleSpawnPercent) {
            cir.setReturnValue(null);
            return;
        }
        if (FpsBoostConfig.particleMaxDistanceSq > 0) {
            MinecraftClient mc = MinecraftClient.getInstance();
            Camera cam = mc.gameRenderer.getCamera();
            if (cam.isReady()) {
                Vec3d c = cam.getPos();
                double dx = x - c.x, dy = y - c.y, dz = z - c.z;
                if (dx * dx + dy * dy + dz * dz > FpsBoostConfig.particleMaxDistanceSq) {
                    cir.setReturnValue(null);
                    return;
                }
            }
        }
        fpsboost$addedThisTick++;
    }
}
