package com.chenjicheng.chestcountoverlay.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.chenjicheng.chestcountoverlay.ChestCountOverlayClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ChestCountOverlayConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve(ChestCountOverlayClient.MOD_ID + ".json");

    private static ChestCountOverlayConfig instance = new ChestCountOverlayConfig();

    private boolean enabled = true;
    private Placement placement = Placement.LEFT;
    private boolean showWhenEmpty = false;
    private boolean countNestedContainerContents = true;

    public static ChestCountOverlayConfig get() {
        return instance;
    }

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            ChestCountOverlayConfig loaded = GSON.fromJson(reader, ChestCountOverlayConfig.class);
            if (loaded == null) {
                throw new IllegalStateException("Config file is empty: " + CONFIG_PATH);
            }

            loaded.normalize();
            instance = loaded;
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Failed to load " + CONFIG_PATH, exception);
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(instance, writer);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to save " + CONFIG_PATH, exception);
        }
    }

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void toggleEnabled() {
        enabled = !enabled;
        save();
    }

    public Placement placement() {
        return placement;
    }

    public void setPlacement(Placement placement) {
        this.placement = placement == null ? Placement.LEFT : placement;
    }

    public void cyclePlacement() {
        placement = placement.next();
        save();
    }

    public boolean showWhenEmpty() {
        return showWhenEmpty;
    }

    public void setShowWhenEmpty(boolean showWhenEmpty) {
        this.showWhenEmpty = showWhenEmpty;
    }

    public void toggleShowWhenEmpty() {
        showWhenEmpty = !showWhenEmpty;
        save();
    }

    public boolean countNestedContainerContents() {
        return countNestedContainerContents;
    }

    public void setCountNestedContainerContents(boolean countNestedContainerContents) {
        this.countNestedContainerContents = countNestedContainerContents;
    }

    public void toggleCountNestedContainerContents() {
        countNestedContainerContents = !countNestedContainerContents;
        save();
    }

    private void normalize() {
        if (placement == null) {
            placement = Placement.LEFT;
        }
    }

    public enum Placement {
        LEFT,
        RIGHT,
        AUTO;

        private Placement next() {
            return switch (this) {
                case LEFT -> RIGHT;
                case RIGHT -> AUTO;
                case AUTO -> LEFT;
            };
        }
    }
}
