package com.chenjicheng.chestcountoverlay;

import com.chenjicheng.chestcountoverlay.config.ChestCountOverlayConfig;
import com.chenjicheng.chestcountoverlay.input.ChestCountOverlayKeyBindings;
import net.fabricmc.api.ClientModInitializer;

public final class ChestCountOverlayClient implements ClientModInitializer {
    public static final String MOD_ID = "chest_count_overlay";

    @Override
    public void onInitializeClient() {
        ChestCountOverlayConfig.load();
        ChestCountOverlayKeyBindings.register();
    }
}
