package com.chenjicheng.chestcountoverlay.config;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.ButtonOption;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.text.Text;

import java.util.Locale;

public final class ChestCountOverlayConfigScreen {
    private ChestCountOverlayConfigScreen() {
    }

    public static Screen create(Screen parent) {
        ChestCountOverlayConfig config = ChestCountOverlayConfig.get();

        return YetAnotherConfigLib.createBuilder()
                .title(Text.translatable("config.chest_count_overlay.title"))
                .category(ConfigCategory.createBuilder()
                        .name(Text.translatable("config.chest_count_overlay.category.general"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.translatable("config.chest_count_overlay.enabled"))
                                .description(OptionDescription.of(Text.translatable("config.chest_count_overlay.enabled.description")))
                                .binding(true, config::enabled, config::setEnabled)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<ChestCountOverlayConfig.Placement>createBuilder()
                                .name(Text.translatable("config.chest_count_overlay.placement"))
                                .description(OptionDescription.of(Text.translatable("config.chest_count_overlay.placement.description")))
                                .binding(ChestCountOverlayConfig.Placement.LEFT, config::placement, config::setPlacement)
                                .controller(option -> EnumControllerBuilder.create(option)
                                        .enumClass(ChestCountOverlayConfig.Placement.class)
                                        .valueFormatter(ChestCountOverlayConfigScreen::placementText))
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.translatable("config.chest_count_overlay.show_when_empty"))
                                .description(OptionDescription.of(Text.translatable("config.chest_count_overlay.show_when_empty.description")))
                                .binding(false, config::showWhenEmpty, config::setShowWhenEmpty)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.translatable("config.chest_count_overlay.count_nested"))
                                .description(OptionDescription.of(Text.translatable("config.chest_count_overlay.count_nested.description")))
                                .binding(true, config::countNestedContainerContents, config::setCountNestedContainerContents)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(ButtonOption.createBuilder()
                                .name(Text.translatable("config.chest_count_overlay.keybinds"))
                                .text(Text.translatable("config.chest_count_overlay.keybinds.open"))
                                .description(OptionDescription.of(Text.translatable("config.chest_count_overlay.keybinds.description")))
                                .action((screen, option) -> {
                                    MinecraftClient client = MinecraftClient.getInstance();
                                    client.setScreen(new KeybindsScreen(screen, client.options));
                                })
                                .build())
                        .build())
                .save(ChestCountOverlayConfig::save)
                .build()
                .generateScreen(parent);
    }

    private static Text placementText(ChestCountOverlayConfig.Placement placement) {
        return Text.translatable("config.chest_count_overlay.placement." + placement.name().toLowerCase(Locale.ROOT));
    }
}
