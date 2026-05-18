package com.chenjicheng.chestcountoverlay.overlay;

public final class OverlayScrollState {
    private int firstVisibleIndex;

    public int firstVisibleIndex() {
        return firstVisibleIndex;
    }

    public void reset() {
        firstVisibleIndex = 0;
    }

    public void scrollRows(int rowDelta, int maxFirstVisibleIndex) {
        firstVisibleIndex = clamp(firstVisibleIndex + rowDelta, maxFirstVisibleIndex);
    }

    public void clampTo(int maxFirstVisibleIndex) {
        firstVisibleIndex = clamp(firstVisibleIndex, maxFirstVisibleIndex);
    }

    private static int clamp(int value, int maxFirstVisibleIndex) {
        if (value < 0) {
            return 0;
        }
        return Math.min(value, Math.max(0, maxFirstVisibleIndex));
    }
}
