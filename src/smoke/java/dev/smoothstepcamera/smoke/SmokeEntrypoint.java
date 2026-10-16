package dev.smoothstepcamera.smoke;

import dev.smoothstepcamera.SmoothStepCameraClient;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

import java.lang.reflect.Method;
import java.util.Arrays;

/** Verify required mixin selectors under the real Fabric transformer, without graphics. */
public final class SmokeEntrypoint implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        try {
            ClassLoader loader = getClass().getClassLoader();
            Class<?> camera = Class.forName("net.minecraft.client.Camera", false, loader);
            requireMethod(camera, "smoothstepcamera$smoothAnchor");
            Class<?> player = Class.forName("net.minecraft.client.player.LocalPlayer", false, loader);
            requireMethod(player, "smoothstepcamera$beforeTick");
            requireMethod(player, "smoothstepcamera$afterTick");
            Class<?> access = Class.forName("dev.smoothstepcamera.CameraSmoothingAccess", false, loader);
            if (!access.isAssignableFrom(player)) {
                throw new AssertionError("LocalPlayer is missing CameraSmoothingAccess");
            }

            boolean shoulderSurfing = FabricLoader.getInstance().isModLoaded("shouldersurfing");
            if (shoulderSurfing) {
                Class<?> shoulder = Class.forName(
                        "com.github.exopandora.shouldersurfing.client.ShoulderSurfingCamera", false, loader);
                requireMethod(shoulder, "smoothstepcamera$smoothCollisionOrigin");
                requireMethod(camera, "setupPosition");
            }
            new SmoothStepCameraClient().onInitializeClient();
            System.out.println("SMOOTH_STEP_SMOKE_OK shouldersurfing=" + shoulderSurfing);
            System.exit(0);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Mixin transformation failed", exception);
        }
    }

    private static void requireMethod(Class<?> type, String name) {
        boolean present = Arrays.stream(type.getDeclaredMethods())
                .map(Method::getName).anyMatch(method -> method.contains(name));
        if (!present) {
            throw new AssertionError(type.getName() + " is missing " + name);
        }
    }
}
