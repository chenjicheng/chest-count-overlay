package com.chenjicheng.chestcountoverlay.overlay;

import com.chenjicheng.chestcountoverlay.config.ChestCountOverlayConfig;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ContainerItemCounter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerItemCounter.class);
    private static final int CHEST_COLUMNS = 9;
    private static final int MAX_NESTED_CONTAINER_DEPTH = 8;
    private static final int MAX_NESTED_STACK_VISITS = 16_384;
    private static final BigInteger K_THRESHOLD = BigInteger.valueOf(100_000L);
    private static final BigInteger ONE_THOUSAND = BigInteger.valueOf(1_000L);
    private static boolean loggedNestedVisitLimit;

    private ContainerItemCounter() {
    }

    public static List<CountedItem> count(GenericContainerScreenHandler handler, int rows) {
        return count(handler, Math.max(0, rows * CHEST_COLUMNS));
    }

    public static List<CountedItem> count(ScreenHandler handler, int containerSlotCount) {
        int slotLimit = Math.min(Math.max(0, containerSlotCount), handler.slots.size());
        Map<StackKey, MutableCountedItem> countedItems = new LinkedHashMap<>();
        TraversalState traversalState = new TraversalState();

        for (int slotIndex = 0; slotIndex < slotLimit; slotIndex++) {
            Slot slot = handler.slots.get(slotIndex);
            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) {
                continue;
            }

            addStackAndNestedContents(countedItems, traversalState, stack, BigInteger.ONE, slotIndex, 0);
        }

        List<MutableCountedItem> sortedItems = new ArrayList<>(countedItems.values());
        sortedItems.sort(
                Comparator.comparing((MutableCountedItem item) -> item.totalCount)
                        .reversed()
                        .thenComparingInt(item -> item.firstSeenOrder)
        );

        List<CountedItem> result = new ArrayList<>(sortedItems.size());
        for (MutableCountedItem item : sortedItems) {
            result.add(new CountedItem(item.stack, item.totalCount, item.firstSlot));
        }
        return result;
    }

    public static String formatCount(BigInteger count) {
        if (count.compareTo(K_THRESHOLD) >= 0) {
            return count.divide(ONE_THOUSAND) + "k";
        }
        return count.toString();
    }

    private static void addStackAndNestedContents(
            Map<StackKey, MutableCountedItem> countedItems,
            TraversalState traversalState,
            ItemStack stack,
            BigInteger parentMultiplier,
            int firstSlot,
            int depth
    ) {
        if (stack.isEmpty() || parentMultiplier.signum() <= 0) {
            return;
        }

        BigInteger totalCount = BigInteger.valueOf(stack.getCount()).multiply(parentMultiplier);
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
                if (!traversalState.tryVisitNestedStack()) {
                    logNestedVisitLimitReached();
                    return;
                }
                addStackAndNestedContents(countedItems, traversalState, nestedStack, totalCount, firstSlot, depth + 1);
            }
        }

        BundleContentsComponent bundleContents = stack.get(DataComponentTypes.BUNDLE_CONTENTS);
        if (bundleContents != null && !bundleContents.isEmpty()) {
            for (ItemStack nestedStack : bundleContents.iterateCopy()) {
                if (!traversalState.tryVisitNestedStack()) {
                    logNestedVisitLimitReached();
                    return;
                }
                addStackAndNestedContents(countedItems, traversalState, nestedStack, totalCount, firstSlot, depth + 1);
            }
        }
    }

    private static void addStack(
            Map<StackKey, MutableCountedItem> countedItems,
            ItemStack stack,
            BigInteger totalCount,
            int firstSlot
    ) {
        StackKey key = StackKey.of(stack);
        MutableCountedItem existing = countedItems.get(key);
        if (existing == null) {
            ItemStack representative = stack.copy();
            representative.setCount(1);
            countedItems.put(key, new MutableCountedItem(representative, totalCount, firstSlot, countedItems.size()));
        } else {
            existing.totalCount = existing.totalCount.add(totalCount);
            existing.firstSlot = Math.min(existing.firstSlot, firstSlot);
        }
    }

    private static void logNestedVisitLimitReached() {
        if (!loggedNestedVisitLimit) {
            loggedNestedVisitLimit = true;
            LOGGER.warn(
                    "Nested container counting reached the per-pass visit cap of {}; additional nested stacks were not counted.",
                    MAX_NESTED_STACK_VISITS
            );
        }
    }

    private record StackKey(ItemStack stack) {
        private static StackKey of(ItemStack stack) {
            ItemStack keyStack = stack.copy();
            keyStack.setCount(1);
            return new StackKey(keyStack);
        }

        @Override
        public boolean equals(Object object) {
            return this == object
                    || object instanceof StackKey other
                    && ItemStack.areItemsAndComponentsEqual(this.stack, other.stack);
        }

        @Override
        public int hashCode() {
            /*
             * Keep this shallow. Component hash codes can recursively walk nested
             * ItemStacks before the nested traversal visit cap has a chance to run.
             * Exact merging is still enforced by equals().
             */
            return this.stack.getItem().hashCode();
        }
    }

    private static final class MutableCountedItem {
        private final ItemStack stack;
        private BigInteger totalCount;
        private int firstSlot;
        private final int firstSeenOrder;

        private MutableCountedItem(ItemStack stack, BigInteger totalCount, int firstSlot, int firstSeenOrder) {
            this.stack = stack;
            this.totalCount = totalCount;
            this.firstSlot = firstSlot;
            this.firstSeenOrder = firstSeenOrder;
        }
    }

    private static final class TraversalState {
        private int nestedStackVisits;

        private boolean tryVisitNestedStack() {
            if (nestedStackVisits >= MAX_NESTED_STACK_VISITS) {
                return false;
            }

            nestedStackVisits++;
            return true;
        }
    }
}
