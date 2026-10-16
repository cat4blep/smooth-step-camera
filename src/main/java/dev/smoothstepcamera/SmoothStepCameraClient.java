package dev.smoothstepcamera;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SmoothStepCameraClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("smoothstepcamera");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Config config = new Config();

    @Override
    public void onInitializeClient() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("smoothstepcamera.json");
        try {
            if (Files.exists(path)) {
                Config loaded = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), Config.class);
                if (loaded == null) {
                    throw new IllegalArgumentException("Expected a JSON object");
                }
                loaded.smoothingTimeMs = Math.clamp(loaded.smoothingTimeMs, 50, 500);
                config = loaded;
            } else {
                Files.createDirectories(path.getParent());
                Files.writeString(path, GSON.toJson(config) + System.lineSeparator(), StandardCharsets.UTF_8);
            }
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("Cannot read Smooth Step Camera config; using defaults", exception);
        }
        LOGGER.info("Smooth Step Camera ready ({} ms)", config.smoothingTimeMs);
    }

    public static boolean isEnabled() {
        return config.enabled;
    }

    public static double smoothingSeconds() {
        return config.smoothingTimeMs / 1000.0;
    }

    private static final class Config {
        boolean enabled = true;
        int smoothingTimeMs = 180;
    }
}
