package com.chenjicheng.chestcountoverlay.overlay;

import net.minecraft.item.ItemStack;

import java.math.BigInteger;

public record CountedItem(ItemStack stack, BigInteger totalCount, int firstSlot) {
}
