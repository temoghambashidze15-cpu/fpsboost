package dev.fpsboost.mixin;

import dev.fpsboost.FpsBoostConfig;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    /** x/y/z are the camera position. Players and projectiles are never culled (PvP-relevant). */
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void fpsboost$distanceCull(E entity, Frustum frustum, double x, double y, double z,
                                                          CallbackInfoReturnable<Boolean> cir) {
        if (!FpsBoostConfig.masterEnabled || !FpsBoostConfig.entityCull) return;
        if (entity instanceof PlayerEntity || entity instanceof ProjectileEntity) return;
        if (entity.squaredDistanceTo(x, y, z) > FpsBoostConfig.entityMaxDistanceSq) {
            cir.setReturnValue(false);
        }
    }
}
