package dev.smoothstepcamera.mixin;

import dev.smoothstepcamera.CameraSmoothingAccess;
import dev.smoothstepcamera.SmoothStepCameraClient;
import dev.smoothstepcamera.StepSmoother;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
abstract class LocalPlayerMixin implements CameraSmoothingAccess {
    @Unique private final StepSmoother smoothstepcamera$smoother = new StepSmoother();
    @Unique private Level smoothstepcamera$level;
    @Unique private Vec3 smoothstepcamera$lastPosition;
    @Unique private double smoothstepcamera$beforeX;
    @Unique private double smoothstepcamera$beforeY;
    @Unique private double smoothstepcamera$beforeZ;
    @Unique private double smoothstepcamera$beforeVelocityY;
    @Unique private boolean smoothstepcamera$wasGrounded;

    @Inject(method = "tick", at = @At("HEAD"))
    private void smoothstepcamera$beforeTick(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (smoothstepcamera$level != player.level()
                || (smoothstepcamera$lastPosition != null
                && player.position().distanceToSqr(smoothstepcamera$lastPosition) > 1.0e-8)) {
            smoothstepcamera$smoother.reset(player.getY());
            smoothstepcamera$level = player.level();
        }
        smoothstepcamera$beforeX = player.getX();
        smoothstepcamera$beforeY = player.getY();
        smoothstepcamera$beforeZ = player.getZ();
        smoothstepcamera$beforeVelocityY = player.getDeltaMovement().y;
        smoothstepcamera$wasGrounded = player.onGround();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void smoothstepcamera$afterTick(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        double dx = player.getX() - smoothstepcamera$beforeX;
        double dz = player.getZ() - smoothstepcamera$beforeZ;
        smoothstepcamera$lastPosition = player.position();
        if (!smoothstepcamera$canUse(player) || dx * dx + dz * dz > 16.0) {
            smoothstepcamera$smoother.reset(player.getY());
            return;
        }

        boolean walking = smoothstepcamera$beforeVelocityY <= 1.0e-5
                && player.getDeltaMovement().y <= 1.0e-5 && !player.isJumping();
        double gap = Double.POSITIVE_INFINITY;
        if (walking && player.getY() <= smoothstepcamera$beforeY
                && (smoothstepcamera$wasGrounded || smoothstepcamera$smoother.isDescending())) {
            // Vanilla's own collision query includes block and entity shapes.
            gap = player.onGround() ? 0.0
                    : player.getAvailableSpaceBelow(StepSmoother.MAX_STEP_HEIGHT + 0.01);
        }
        smoothstepcamera$smoother.tick(smoothstepcamera$beforeY, player.getY(),
                smoothstepcamera$wasGrounded, player.onGround(), walking, gap,
                SmoothStepCameraClient.smoothingSeconds());
    }

    @Unique
    private static boolean smoothstepcamera$canUse(LocalPlayer player) {
        return SmoothStepCameraClient.isEnabled() && player.isAlive() && !player.isSpectator()
                && !player.noPhysics && !player.isPassenger() && !player.isSleeping()
                && !player.getAbilities().flying && !player.isFallFlying()
                && !player.isSwimming() && !player.isInWater() && !player.isInLava()
                && !player.onClimbable();
    }

    @Override
    public double smoothstepcamera$offset(float partialTick) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (smoothstepcamera$level != player.level() || !smoothstepcamera$canUse(player)
                || smoothstepcamera$lastPosition == null
                || player.position().distanceToSqr(smoothstepcamera$lastPosition) > 1.0e-8) {
            return 0.0;
        }
        return smoothstepcamera$smoother.offset(partialTick);
    }
}
