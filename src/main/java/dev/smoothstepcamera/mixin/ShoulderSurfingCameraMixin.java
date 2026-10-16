package dev.smoothstepcamera.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.smoothstepcamera.CameraSmoothingAccess;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

// @Pseudo keeps Shoulder Surfing optional: no classes from its jar are linked.
// https://github.com/LlamaLad7/MixinExtras/wiki/ModifyExpressionValue
@Pseudo
@Mixin(targets = "com.github.exopandora.shouldersurfing.client.ShoulderSurfingCamera", remap = false)
abstract class ShoulderSurfingCameraMixin {
    // 26.3 / SSR 5.2.0 uses a separate eye origin for its eight collision rays.
    // https://github.com/Exopandora/ShoulderSurfing/blob/master/common/src/main/java/com/github/exopandora/shouldersurfing/client/ShoulderSurfingCamera.java
    @ModifyExpressionValue(method = "maxZoom", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;getEyePosition(F)Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 smoothstepcamera$smoothCollisionOrigin(Vec3 original, Camera camera,
            Entity cameraEntity, BlockGetter level, Vec3 cameraOffset, float partialTick) {
        if (cameraEntity instanceof CameraSmoothingAccess smoothing) {
            return original.add(0.0, smoothing.smoothstepcamera$offset(partialTick), 0.0);
        }
        return original;
    }
}
