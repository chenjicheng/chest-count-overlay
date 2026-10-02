package com.chenjicheng.chestcountoverlay;

import com.chenjicheng.chestcountoverlay.config.ChestCountOverlayConfig;
import com.chenjicheng.chestcountoverlay.config.ChestCountOverlayConfigScreen;
import com.chenjicheng.chestcountoverlay.overlay.ChestCountOverlayRenderer;
import com.chenjicheng.chestcountoverlay.overlay.ContainerItemCounter;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/** Runs the production render/input hooks against real synchronized vanilla menus. */
public final class DocumentationScreenshots implements FabricClientGameTest {
    private final Path output = FabricLoader.getInstance().getGameDir().resolve("documentation-screenshots");

    @Override
    public void runTest(ClientGameTestContext context) {
        try {
            generate(context);
        } catch (Exception exception) {
            throw new AssertionError("Documentation screenshot run failed", exception);
        }
    }

    private void generate(ClientGameTestContext context) throws Exception {
        Files.createDirectories(output);
        Files.deleteIfExists(output.resolve("completed.json"));
        context.runOnClient(client -> {
            // Keep software-rendered CI startup inexpensive; capture uses full resolution below.
            client.options.getViewDistance().setValue(2);
            ChestCountOverlayConfig.get().setEnabled(true);
            ChestCountOverlayConfig.get().setPlacement(ChestCountOverlayConfig.Placement.LEFT);
            ChestCountOverlayConfig.get().setShowWhenEmpty(false);
            ChestCountOverlayConfig.get().setCountNestedContainerContents(true);
        });
        try (var world = context.worldBuilder().create()) {
            world.getClientWorld().waitForChunksRender();
            context.getInput().resizeWindow(1440, 900);
            context.runOnClient(client -> {
                client.options.getGuiScale().setValue(3);
                client.onResolutionChanged();
            });
            openChest(context, world, 3, "Chest", inventory -> {
                inventory.setStack(0, new ItemStack(Items.STONE, 64));
                inventory.setStack(1, new ItemStack(Items.STONE, 32));
                inventory.setStack(2, new ItemStack(Items.TORCH, 64));
                inventory.setStack(3, new ItemStack(Items.TORCH, 64));
                inventory.setStack(4, new ItemStack(Items.OAK_LOG, 32));
                inventory.setStack(5, new ItemStack(Items.DIAMOND, 16));
                inventory.setStack(6, new ItemStack(Items.BREAD, 12));
                inventory.setStack(7, new ItemStack(Items.IRON_INGOT, 8));
            });
            assertCount(context, Items.STONE, 96, 27);
            assertCount(context, Items.TORCH, 128, 27);
            context.getInput().pressKey(GLFW.GLFW_KEY_GRAVE_ACCENT);
            context.runOnClient(client -> {
                if (!expanded()) throw new AssertionError("Registered shortcut did not expand the overlay");
            });
            capture(context, "cco-chest-counts");

            openChest(context, world, 6, "Large Chest", inventory -> {
                ItemStack shulker = new ItemStack(Items.BLUE_SHULKER_BOX);
                shulker.set(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(List.of(
                        new ItemStack(Items.STONE, 64), new ItemStack(Items.DIAMOND, 16))));
                inventory.setStack(0, shulker);
                inventory.setStack(1, shulker.copy());
                inventory.setStack(2, new ItemStack(Items.DIAMOND, 8));
                ItemStack bundle = new ItemStack(Items.BUNDLE);
                bundle.set(DataComponentTypes.BUNDLE_CONTENTS, new BundleContentsComponent(List.of(
                        new ItemStack(Items.CARROT, 24), new ItemStack(Items.GOLD_INGOT, 4))));
                inventory.setStack(3, bundle);
                inventory.setStack(4, new ItemStack(Items.IRON_INGOT, 32));
                inventory.setStack(5, new ItemStack(Items.STONE, 32));
            });
            assertCount(context, Items.STONE, 160, 54);
            assertCount(context, Items.DIAMOND, 40, 54);
            assertCount(context, Items.BLUE_SHULKER_BOX, 2, 54);
            context.runOnClient(client -> ChestCountOverlayConfig.get().setCountNestedContainerContents(false));
            assertCount(context, Items.DIAMOND, 8, 54);
            context.runOnClient(client -> ChestCountOverlayConfig.get().setCountNestedContainerContents(true));
            capture(context, "cco-nested-containers");

            world.getServer().runOnServer(server -> {
                var player = server.getPlayerManager().getPlayerList().getFirst();
                var inventory = new SimpleInventory(27);
                inventory.setStack(0, new ItemStack(Items.COBBLESTONE, 64));
                inventory.setStack(1, new ItemStack(Items.COBBLESTONE, 64));
                inventory.setStack(2, new ItemStack(Items.GLASS, 48));
                inventory.setStack(3, new ItemStack(Items.OAK_PLANKS, 32));
                player.closeHandledScreen();
                player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                        (id, inv, owner) -> new ShulkerBoxScreenHandler(id, inv, inventory), Text.literal("Shulker Box")));
            });
            context.waitForScreen(ShulkerBoxScreen.class);
            assertCount(context, Items.COBBLESTONE, 128, 27);
            capture(context, "cco-shulker-box");
            context.getInput().pressKey(GLFW.GLFW_KEY_GRAVE_ACCENT);
            context.runOnClient(client -> {
                if (expanded()) throw new AssertionError("Registered shortcut did not collapse the overlay");
            });
            capture(context, "cco-collapsed");
            context.getInput().pressKey(GLFW.GLFW_KEY_GRAVE_ACCENT);

            openChest(context, world, 6, "Storage Chest", inventory -> {
                Item[] items = {Items.STONE, Items.COBBLESTONE, Items.DIRT, Items.GRASS_BLOCK,
                        Items.OAK_LOG, Items.SPRUCE_LOG, Items.BIRCH_LOG, Items.OAK_PLANKS,
                        Items.GLASS, Items.SAND, Items.GRAVEL, Items.IRON_INGOT, Items.GOLD_INGOT,
                        Items.DIAMOND, Items.EMERALD, Items.REDSTONE, Items.COAL, Items.TORCH,
                        Items.BREAD, Items.CARROT, Items.APPLE, Items.POTATO, Items.WHEAT, Items.PAPER};
                for (int index = 0; index < items.length; index++) {
                    inventory.setStack(index, new ItemStack(items[index], 64 - index));
                }
            });
            context.waitTicks(10);
            // Coordinates follow this fixed 480x300 GUI and the production rail layout.
            context.getInput().setCursorPos(85 * 3, 95 * 3);
            context.getInput().scroll(-1);
            context.runOnClient(client -> {
                var field = ChestCountOverlayRenderer.class.getDeclaredField("SCROLL_STATE");
                field.setAccessible(true);
                var scroll = (com.chenjicheng.chestcountoverlay.overlay.OverlayScrollState) field.get(null);
                if (scroll.firstVisibleIndex() == 0) throw new AssertionError("Container sidebar did not scroll");
            });
            capture(context, "cco-scrolling");

            world.getServer().runOnServer(server -> server.getPlayerManager().getPlayerList().getFirst().closeHandledScreen());
            context.waitForScreen(null);
            context.setScreen(() -> ChestCountOverlayConfigScreen.create(null));
            capture(context, "cco-settings-en");
            context.runOnClient(client -> {
                client.getLanguageManager().setLanguage("zh_cn");
                client.options.language = "zh_cn";
                client.reloadResources();
            });
            context.waitFor(client -> client.getOverlay() == null
                    && Text.translatable("config.chest_count_overlay.title").getString().contains("统计"));
            context.setScreen(() -> ChestCountOverlayConfigScreen.create(null));
            capture(context, "cco-settings-zh");
        }
        Files.writeString(output.resolve("completed.json"), """
                {"completed":true,"scenes":["cco-chest-counts","cco-nested-containers","cco-shulker-box",
                "cco-collapsed","cco-scrolling","cco-settings-en","cco-settings-zh"]}
                """);
    }

    private void openChest(ClientGameTestContext context, TestSingleplayerContext world,
                           int rows, String title, Consumer<SimpleInventory> populate) {
        world.getServer().runOnServer(server -> {
            var player = server.getPlayerManager().getPlayerList().getFirst();
            player.getInventory().clear();
            player.getInventory().setStack(0, new ItemStack(Items.STONE, 64));
            var inventory = new SimpleInventory(rows * 9);
            populate.accept(inventory);
            player.closeHandledScreen();
            player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                    (id, inv, owner) -> rows == 3
                            ? GenericContainerScreenHandler.createGeneric9x3(id, inv, inventory)
                            : GenericContainerScreenHandler.createGeneric9x6(id, inv, inventory), Text.literal(title)));
        });
        context.waitFor(client -> client.currentScreen instanceof GenericContainerScreen
                && client.currentScreen.getTitle().getString().equals(title));
    }

    private void assertCount(ClientGameTestContext context, Item item, int expected, int slots) {
        context.waitFor(client -> ContainerItemCounter.count(client.player.currentScreenHandler, slots).stream()
                .anyMatch(counted -> counted.stack().isOf(item) && counted.totalCount().equals(BigInteger.valueOf(expected))));
    }

    private void capture(ClientGameTestContext context, String name) {
        if (name.startsWith("cco-settings")) {
            context.waitFor(client -> client.currentScreen != null && client.currentScreen.getTitle().getString()
                    .equals(Text.translatable("config.chest_count_overlay.title").getString()));
        } else {
            context.waitFor(client -> animationSettled());
        }
        context.getInput().setCursorPos(0, 0);
        if (name.startsWith("cco-settings")) context.getInput().setCursorPos(240, 305);
        context.waitTicks(10);
        context.runOnClient(client -> client.getToastManager().clear());
        context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix().withSize(1440, 900)
                .withDestinationDir(output));
    }

    private static boolean expanded() throws ReflectiveOperationException {
        var field = ChestCountOverlayRenderer.class.getDeclaredField("targetExpanded");
        field.setAccessible(true);
        return field.getBoolean(null);
    }

    private static boolean animationSettled() {
        try {
            var field = ChestCountOverlayRenderer.class.getDeclaredField("animationProgress");
            field.setAccessible(true);
            float progress = field.getFloat(null);
            return expanded() ? progress >= 0.999F : progress <= 0.001F;
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Unable to verify the captured overlay animation", exception);
        }
    }
}
