package com.chenjicheng.chestcountoverlay.overlay;

import net.minecraft.item.ItemStack;

public record CountedItem(ItemStack stack, long totalCount, int firstSlot) {
}
