package com.chenjicheng.chestcountoverlay.input;

import com.chenjicheng.chestcountoverlay.ChestCountOverlayClient;
import com.chenjicheng.chestcountoverlay.config.ChestCountOverlayConfig;
import com.chenjicheng.chestcountoverlay.overlay.ChestCountOverlayRenderer;
import com.chenjicheng.chestcountoverlay.overlay.ContainerItemCounter;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class ChestCountOverlayKeyBindings {
    private static final int SHULKER_BOX_SLOT_COUNT = 27;
    private static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(
            Identifier.of(ChestCountOverlayClient.MOD_ID, "controls")
    );

    private static KeyBinding toggleOverlay;

    private ChestCountOverlayKeyBindings() {
    }

    public static void register() {
        toggleOverlay = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.chest_count_overlay.toggle_overlay",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_GRAVE_ACCENT,
                CATEGORY
        ));
    }

    public static boolean handleScreenKeyPressed(Screen screen, KeyInput input) {
        if (toggleOverlay == null || !toggleOverlay.matchesKey(input) || !canToggleOverlay(screen)) {
            return false;
        }

        ChestCountOverlayRenderer.toggleExpanded();
        return true;
    }

    private static boolean canToggleOverlay(Screen screen) {
        if (!ChestCountOverlayConfig.get().enabled()) {
            return false;
        }

        if (!(screen instanceof HandledScreen<?> handledScreen)) {
            return false;
        }

        ScreenHandler handler = handledScreen.getScreenHandler();
        int containerSlotCount = supportedContainerSlotCount(screen, handler);
        return containerSlotCount > 0
                && (ChestCountOverlayConfig.get().showWhenEmpty() || !ContainerItemCounter.count(handler, containerSlotCount).isEmpty());
    }

    private static int supportedContainerSlotCount(Screen screen, ScreenHandler handler) {
        if (screen instanceof GenericContainerScreen && handler instanceof GenericContainerScreenHandler genericHandler) {
            return genericHandler.getRows() * 9;
        }

        if (screen instanceof ShulkerBoxScreen && handler instanceof ShulkerBoxScreenHandler) {
            return SHULKER_BOX_SLOT_COUNT;
        }

        return 0;
    }
}
