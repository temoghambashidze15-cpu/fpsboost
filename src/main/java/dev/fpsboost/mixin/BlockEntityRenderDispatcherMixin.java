package dev.fpsboost.mixin;

import dev.fpsboost.FpsBoostConfig;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {
    @Inject(method = "render(Lnet/minecraft/block/entity/BlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V",
            at = @At("HEAD"), cancellable = true)
    private void fpsboost$distanceCull(BlockEntity be, float tickDelta, MatrixStack matrices,
                                       VertexConsumerProvider consumers, CallbackInfo ci) {
        if (!FpsBoostConfig.masterEnabled || !FpsBoostConfig.blockEntityCull) return;
        if (be instanceof BeaconBlockEntity) return;
        Vec3d c = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();
        BlockPos p = be.getPos();
        double dx = p.getX() + 0.5 - c.x, dy = p.getY() + 0.5 - c.y, dz = p.getZ() + 0.5 - c.z;
        if (dx * dx + dy * dy + dz * dz > FpsBoostConfig.blockEntityMaxDistanceSq) ci.cancel();
    }
}
