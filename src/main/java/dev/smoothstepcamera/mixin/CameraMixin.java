package dev.smoothstepcamera.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.smoothstepcamera.CameraSmoothingAccess;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Camera.class)
abstract class CameraMixin {
    @Shadow private Entity entity;

    // Modify the shared anchor before vanilla/Shoulder Surfing computes camera
    // distance and wall collision. Never apply a second offset at method TAIL.
    @ModifyExpressionValue(method = "alignWithEntity", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/Mth;lerp(DDD)D", ordinal = 1))
    private double smoothstepcamera$smoothAnchor(double y, float partialTick) {
        return entity instanceof CameraSmoothingAccess smoothing
                ? y + smoothing.smoothstepcamera$offset(partialTick) : y;
    }
}
