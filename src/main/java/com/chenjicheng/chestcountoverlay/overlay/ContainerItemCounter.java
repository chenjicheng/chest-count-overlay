package com.chenjicheng.chestcountoverlay.overlay;

import com.chenjicheng.chestcountoverlay.config.ChestCountOverlayConfig;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ContainerItemCounter {
    private static final int CHEST_COLUMNS = 9;
    private static final int MAX_NESTED_CONTAINER_DEPTH = 8;
    private static final long K_THRESHOLD = 100_000L;

    private ContainerItemCounter() {
    }

    public static List<CountedItem> count(GenericContainerScreenHandler handler, int rows) {
        return count(handler, Math.max(0, rows * CHEST_COLUMNS));
    }

    public static List<CountedItem> count(ScreenHandler handler, int containerSlotCount) {
        int slotLimit = Math.min(Math.max(0, containerSlotCount), handler.slots.size());
        List<MutableCountedItem> countedItems = new ArrayList<>();

        for (int slotIndex = 0; slotIndex < slotLimit; slotIndex++) {
            Slot slot = handler.slots.get(slotIndex);
            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) {
                continue;
            }

            addStackAndNestedContents(countedItems, stack, 1L, slotIndex, 0);
        }

        countedItems.sort(
                Comparator.comparingLong((MutableCountedItem item) -> item.totalCount)
                        .reversed()
                        .thenComparingInt(item -> item.firstSlot)
        );

        List<CountedItem> result = new ArrayList<>(countedItems.size());
        for (MutableCountedItem item : countedItems) {
            result.add(new CountedItem(item.stack, item.totalCount, item.firstSlot));
        }
        return result;
    }

    public static String formatCount(long count) {
        if (count >= K_THRESHOLD) {
            return (count / 1_000L) + "k";
        }
        return Long.toString(count);
    }

    private static void addStackAndNestedContents(
            List<MutableCountedItem> countedItems,
            ItemStack stack,
            long parentMultiplier,
            int firstSlot,
            int depth
    ) {
        if (stack.isEmpty() || parentMultiplier <= 0L) {
            return;
        }

        long totalCount = Math.multiplyExact((long) stack.getCount(), parentMultiplier);
        addStack(countedItems, stack, totalCount, firstSlot);

        if (!ChestCountOverlayConfig.get().countNestedContainerContents() || depth >= MAX_NESTED_CONTAINER_DEPTH) {
            return;
        }

        /*
         * Modern vanilla stores shulker-box-like item contents on the stack's
         * CONTAINER component. This stays client-only because the component is
         * read from the already synchronized ItemStack.
         */
        ContainerComponent container = stack.get(DataComponentTypes.CONTAINER);
        if (container != null) {
            for (ItemStack nestedStack : container.iterateNonEmptyCopy()) {
                addStackAndNestedContents(countedItems, nestedStack, totalCount, firstSlot, depth + 1);
            }
        }

        BundleContentsComponent bundleContents = stack.get(DataComponentTypes.BUNDLE_CONTENTS);
        if (bundleContents != null && !bundleContents.isEmpty()) {
            for (ItemStack nestedStack : bundleContents.iterateCopy()) {
                addStackAndNestedContents(countedItems, nestedStack, totalCount, firstSlot, depth + 1);
            }
        }
    }

    private static void addStack(List<MutableCountedItem> countedItems, ItemStack stack, long totalCount, int firstSlot) {
        MutableCountedItem existing = findMatching(countedItems, stack);
        if (existing == null) {
            ItemStack representative = stack.copy();
            representative.setCount(1);
            countedItems.add(new MutableCountedItem(representative, totalCount, firstSlot));
        } else {
            existing.totalCount = Math.addExact(existing.totalCount, totalCount);
            existing.firstSlot = Math.min(existing.firstSlot, firstSlot);
        }
    }

    private static MutableCountedItem findMatching(List<MutableCountedItem> countedItems, ItemStack stack) {
        for (MutableCountedItem countedItem : countedItems) {
            // This helper compares item identity and data components in modern Minecraft.
            if (ItemStack.areItemsAndComponentsEqual(countedItem.stack, stack)) {
                return countedItem;
            }
        }
        return null;
    }

    private static final class MutableCountedItem {
        private final ItemStack stack;
        private long totalCount;
        private int firstSlot;

        private MutableCountedItem(ItemStack stack, long totalCount, int firstSlot) {
            this.stack = stack;
            this.totalCount = totalCount;
            this.firstSlot = firstSlot;
        }
    }
}
