package com.chenjicheng.chestcountoverlay.overlay;

import com.chenjicheng.chestcountoverlay.config.ChestCountOverlayConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.List;

public final class ChestCountOverlayRenderer {
    private static final int PANEL_WIDTH = 74;
    private static final int COLLAPSED_TAB_WIDTH = 16;
    private static final int PANEL_GAP = 2;
    private static final int HEADER_HEIGHT = 14;
    private static final int LIST_PADDING_X = 4;
    private static final int ROW_HEIGHT = 18;
    private static final int ICON_SIZE = 16;
    private static final int SCROLLBAR_WIDTH = 2;
    private static final int SCROLLBAR_GAP = 2;
    private static final int SCROLL_ROWS_PER_WHEEL = 3;
    private static final float ANIMATION_SECONDS = 0.12F;

    private static final int PANEL_BACKGROUND = 0x18000000;
    private static final int HEADER_BACKGROUND = 0x52000000;
    private static final int HEADER_HOVERED = 0x70000000;
    private static final int PANEL_BORDER = 0x26FFFFFF;
    private static final int PANEL_EDGE_SHADOW = 0x44000000;
    private static final int ROW_BACKGROUND_EVEN = 0x64000000;
    private static final int ROW_BACKGROUND_ODD = 0x52000000;
    private static final int ROW_BACKGROUND_HOVERED = 0x8C000000;
    private static final int ROW_REMAINDER_BACKGROUND = 0x36000000;
    private static final int ROW_SEPARATOR = 0x20FFFFFF;
    private static final int ROW_DIVIDER = 0x16FFFFFF;
    private static final int TEXT_COLOR = 0xFFF2F2F2;
    private static final int SCROLL_TRACK = 0x20000000;
    private static final int SCROLL_THUMB = 0x88FFFFFF;
    private static final int BUTTON_ICON = 0xFFEDEDED;

    private static final OverlayScrollState SCROLL_STATE = new OverlayScrollState();
    private static ScreenHandler activeHandler;
    private static boolean targetExpanded;
    private static float animationProgress;
    private static long lastAnimationNanos;

    private ChestCountOverlayRenderer() {
    }

    public static void render(
            DrawContext context,
            ScreenHandler handler,
            int containerSlotCount,
            int containerX,
            int containerY,
            int containerWidth,
            int visualContainerHeight,
            int screenWidth,
            int screenHeight,
            int mouseX,
            int mouseY
    ) {
        if (!ChestCountOverlayConfig.get().enabled()) {
            SCROLL_STATE.reset();
            return;
        }

        ensureActiveHandler(handler);

        List<CountedItem> items = ContainerItemCounter.count(handler, containerSlotCount);
        if (items.isEmpty() && !ChestCountOverlayConfig.get().showWhenEmpty()) {
            SCROLL_STATE.reset();
            return;
        }

        Layout layout = createLayout(containerX, containerY, containerWidth, visualContainerHeight, screenWidth, screenHeight);
        int maxFirstVisibleIndex = maxFirstVisibleIndex(items.size(), layout.visibleRows());
        SCROLL_STATE.clampTo(maxFirstVisibleIndex);
        float progress = updateAnimationProgress();

        drawOverlay(context, layout, items, maxFirstVisibleIndex, progress, mouseX, mouseY);

        if (progress >= 0.98F) {
            drawTooltip(context, layout, items, mouseX, mouseY);
        }
    }

    public static boolean mouseClicked(
            ScreenHandler handler,
            int containerSlotCount,
            int containerX,
            int containerY,
            int containerWidth,
            int visualContainerHeight,
            int screenWidth,
            int screenHeight,
            double mouseX,
            double mouseY,
            int button
    ) {
        if (!ChestCountOverlayConfig.get().enabled()) {
            return false;
        }

        if (button != 0) {
            return false;
        }

        if (ContainerItemCounter.count(handler, containerSlotCount).isEmpty() && !ChestCountOverlayConfig.get().showWhenEmpty()) {
            return false;
        }

        ensureActiveHandler(handler);
        Layout layout = createLayout(containerX, containerY, containerWidth, visualContainerHeight, screenWidth, screenHeight);
        if (buttonLayout(layout).contains(mouseX, mouseY)) {
            toggleExpanded();
            return true;
        }

        return targetExpanded && layout.containsPanel(mouseX, mouseY);
    }

    public static void toggleExpanded() {
        targetExpanded = !targetExpanded;
        lastAnimationNanos = System.nanoTime();
    }

    public static boolean mouseScrolled(
            ScreenHandler handler,
            int containerSlotCount,
            int containerX,
            int containerY,
            int containerWidth,
            int visualContainerHeight,
            int screenWidth,
            int screenHeight,
            double mouseX,
            double mouseY,
            double verticalAmount
    ) {
        if (!ChestCountOverlayConfig.get().enabled()) {
            return false;
        }

        if (verticalAmount == 0.0D) {
            return false;
        }

        List<CountedItem> items = ContainerItemCounter.count(handler, containerSlotCount);
        if (items.isEmpty()) {
            return false;
        }

        Layout layout = createLayout(containerX, containerY, containerWidth, visualContainerHeight, screenWidth, screenHeight);
        if (!targetExpanded || animationProgress < 0.98F || layout.visibleRows() <= 0 || !layout.containsRows(mouseX, mouseY)) {
            return false;
        }

        int maxFirstVisibleIndex = maxFirstVisibleIndex(items.size(), layout.visibleRows());
        if (maxFirstVisibleIndex <= 0) {
            return false;
        }

        ensureActiveHandler(handler);

        int rowDelta = verticalAmount > 0 ? -SCROLL_ROWS_PER_WHEEL : SCROLL_ROWS_PER_WHEEL;
        SCROLL_STATE.scrollRows(rowDelta, maxFirstVisibleIndex);
        return true;
    }

    private static void ensureActiveHandler(ScreenHandler handler) {
        if (activeHandler != handler) {
            activeHandler = handler;
            animationProgress = targetExpanded ? 1.0F : 0.0F;
            lastAnimationNanos = System.nanoTime();
            SCROLL_STATE.reset();
        }
    }

    private static float updateAnimationProgress() {
        long now = System.nanoTime();
        long elapsedNanos = lastAnimationNanos == 0L ? 0L : now - lastAnimationNanos;
        lastAnimationNanos = now;

        float delta = elapsedNanos / 1_000_000_000.0F / ANIMATION_SECONDS;
        if (targetExpanded) {
            animationProgress = Math.min(1.0F, animationProgress + delta);
        } else {
            animationProgress = Math.max(0.0F, animationProgress - delta);
        }
        return smoothStep(animationProgress);
    }

    private static float smoothStep(float value) {
        return value * value * (3.0F - 2.0F * value);
    }

    private static void drawOverlay(
            DrawContext context,
            Layout layout,
            List<CountedItem> items,
            int maxFirstVisibleIndex,
            float progress,
            int mouseX,
            int mouseY
    ) {
        int visibleWidth = Math.max(COLLAPSED_TAB_WIDTH, Math.round(PANEL_WIDTH * progress));
        if (progress <= 0.001F) {
            drawCollapsedTab(context, layout, mouseX, mouseY);
            return;
        }

        int clipLeft = layout.side() == Side.RIGHT ? layout.x() : layout.right() - visibleWidth;
        int clipRight = layout.side() == Side.RIGHT ? layout.x() + visibleWidth : layout.right();

        context.enableScissor(clipLeft, layout.y(), clipRight, layout.bottom());
        drawPanel(context, layout);
        drawHeader(context, layout, mouseX, mouseY);
        if (visibleWidth > COLLAPSED_TAB_WIDTH) {
            drawRows(context, layout, items, mouseX, mouseY);
            drawScrollbar(context, layout, items.size(), maxFirstVisibleIndex);
        }
        context.disableScissor();
    }

    private static void drawCollapsedTab(DrawContext context, Layout layout, int mouseX, int mouseY) {
        ButtonLayout button = buttonLayout(layout);
        boolean hovered = button.contains(mouseX, mouseY);

        context.fill(button.x(), button.y(), button.right(), button.bottom(), hovered ? HEADER_HOVERED : HEADER_BACKGROUND);
        context.fill(button.x(), button.y(), button.right(), button.y() + 1, PANEL_BORDER);
        context.fill(button.x(), button.y(), button.x() + 1, button.bottom(), PANEL_BORDER);
        context.fill(button.x(), button.bottom() - 1, button.right(), button.bottom(), PANEL_EDGE_SHADOW);
        context.fill(button.right() - 1, button.y(), button.right(), button.bottom(), PANEL_EDGE_SHADOW);
        drawButtonChevron(context, button, chevronPointsRight(layout.side()));
    }

    private static void drawHeader(DrawContext context, Layout layout, int mouseX, int mouseY) {
        ButtonLayout button = buttonLayout(layout);
        boolean hovered = button.contains(mouseX, mouseY);

        context.fill(layout.x(), layout.y(), layout.right(), layout.y() + HEADER_HEIGHT, HEADER_BACKGROUND);
        if (hovered) {
            context.fill(button.x(), button.y(), button.right(), button.bottom(), HEADER_HOVERED);
        }
        context.fill(layout.x() + 1, layout.y() + HEADER_HEIGHT - 1, layout.right() - 1, layout.y() + HEADER_HEIGHT, ROW_SEPARATOR);
        drawButtonChevron(context, button, chevronPointsRight(layout.side()));
    }

    private static boolean chevronPointsRight(Side side) {
        return side == Side.RIGHT ? !targetExpanded : targetExpanded;
    }

    private static void drawButtonChevron(DrawContext context, ButtonLayout button, boolean pointsRight) {
        int centerX = button.x() + button.width() / 2;
        int centerY = button.y() + button.height() / 2;
        for (int dy = -3; dy <= 3; dy++) {
            int length = 4 - Math.abs(dy);
            int y = centerY + dy;
            if (pointsRight) {
                context.fill(centerX - 1, y, centerX - 1 + length, y + 1, BUTTON_ICON);
            } else {
                context.fill(centerX + 1 - length, y, centerX + 1, y + 1, BUTTON_ICON);
            }
        }
    }

    private static ButtonLayout buttonLayout(Layout layout) {
        int buttonX = layout.side() == Side.RIGHT
                ? layout.x()
                : layout.right() - COLLAPSED_TAB_WIDTH;
        return new ButtonLayout(buttonX, layout.y(), COLLAPSED_TAB_WIDTH, HEADER_HEIGHT);
    }

    private static void drawPanel(DrawContext context, Layout layout) {
        int x = layout.x();
        int y = layout.y();
        int right = layout.right();
        int bottom = layout.bottom();

        context.fill(x, y, right, bottom, PANEL_BACKGROUND);
        context.fill(x, y, right, y + 1, PANEL_BORDER);
        context.fill(x, bottom - 1, right, bottom, PANEL_EDGE_SHADOW);
        context.fill(x, y, x + 1, bottom, PANEL_BORDER);
        context.fill(right - 1, y, right, bottom, PANEL_EDGE_SHADOW);
    }

    private static void drawRows(DrawContext context, Layout layout, List<CountedItem> items, int mouseX, int mouseY) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;
        int firstVisibleIndex = SCROLL_STATE.firstVisibleIndex();
        int lastVisibleIndex = Math.min(items.size(), firstVisibleIndex + layout.visibleRows());
        int scrollbarSpace = needsScrollbar(items.size(), layout.visibleRows()) ? SCROLLBAR_WIDTH + SCROLLBAR_GAP : 0;
        int countRight = layout.right() - LIST_PADDING_X - scrollbarSpace;
        int hoveredIndex = hoveredIndex(layout, items.size(), mouseX, mouseY);
        int renderedRows = lastVisibleIndex - firstVisibleIndex;

        for (int itemIndex = firstVisibleIndex; itemIndex < lastVisibleIndex; itemIndex++) {
            CountedItem item = items.get(itemIndex);
            int row = itemIndex - firstVisibleIndex;
            int rowY = layout.contentY() + row * ROW_HEIGHT;
            int rowBottom = rowY + ROW_HEIGHT;
            int iconX = layout.x() + LIST_PADDING_X;
            int iconY = rowY + (ROW_HEIGHT - ICON_SIZE) / 2;
            int rowLeft = layout.x() + 1;
            int rowRight = layout.right() - 1;
            int countLeft = layout.x() + 27;
            int rowBackground = itemIndex == hoveredIndex
                    ? ROW_BACKGROUND_HOVERED
                    : row % 2 == 0 ? ROW_BACKGROUND_EVEN : ROW_BACKGROUND_ODD;

            context.fill(rowLeft, rowY, rowRight, rowBottom, rowBackground);
            if (row > 0) {
                context.fill(rowLeft, rowY, rowRight, rowY + 1, ROW_SEPARATOR);
            }

            context.fill(countLeft - 3, rowY + 3, countLeft - 2, rowY + ROW_HEIGHT - 3, ROW_DIVIDER);
            context.drawItem(item.stack(), iconX, iconY);

            String countText = ContainerItemCounter.formatCount(item.totalCount());
            int textX = countRight - textRenderer.getWidth(countText);
            if (textX < countLeft) {
                textX = countLeft;
            }
            int textY = rowY + (ROW_HEIGHT - textRenderer.fontHeight) / 2;
            context.drawText(textRenderer, countText, textX, textY, TEXT_COLOR, true);
        }

        drawContentRemainder(context, layout, renderedRows);
    }

    private static void drawContentRemainder(DrawContext context, Layout layout, int renderedRows) {
        if (renderedRows < layout.visibleRows()) {
            return;
        }

        int remainderTop = layout.contentY() + layout.visibleRows() * ROW_HEIGHT;
        if (remainderTop >= layout.bottom()) {
            return;
        }

        context.fill(layout.x() + 1, remainderTop, layout.right() - 1, layout.bottom() - 1, ROW_REMAINDER_BACKGROUND);
        context.fill(layout.x() + 1, remainderTop, layout.right() - 1, remainderTop + 1, ROW_SEPARATOR);
    }

    private static void drawScrollbar(DrawContext context, Layout layout, int itemCount, int maxFirstVisibleIndex) {
        if (!needsScrollbar(itemCount, layout.visibleRows())) {
            return;
        }

        int trackX = layout.right() - 2 - SCROLLBAR_WIDTH;
        int trackY = layout.contentY() + 1;
        int trackHeight = layout.contentHeight() - 2;
        int thumbHeight = Math.max(8, trackHeight * layout.visibleRows() / itemCount);
        int thumbTravel = Math.max(0, trackHeight - thumbHeight);
        int thumbY = trackY + (maxFirstVisibleIndex == 0 ? 0 : thumbTravel * SCROLL_STATE.firstVisibleIndex() / maxFirstVisibleIndex);

        context.fill(trackX, trackY, trackX + SCROLLBAR_WIDTH, trackY + trackHeight, SCROLL_TRACK);
        context.fill(trackX, thumbY, trackX + SCROLLBAR_WIDTH, thumbY + thumbHeight, SCROLL_THUMB);
    }

    private static void drawTooltip(DrawContext context, Layout layout, List<CountedItem> items, int mouseX, int mouseY) {
        int hoveredIndex = hoveredIndex(layout, items.size(), mouseX, mouseY);
        if (hoveredIndex < 0) {
            return;
        }

        CountedItem item = items.get(hoveredIndex);
        MinecraftClient client = MinecraftClient.getInstance();
        context.drawTooltip(
                client.textRenderer,
                List.of(item.stack().getName(), Text.literal("Count: " + item.totalCount())),
                mouseX,
                mouseY
        );
    }

    private static int hoveredIndex(Layout layout, int itemCount, int mouseX, int mouseY) {
        if (!layout.containsRows(mouseX, mouseY) || layout.visibleRows() <= 0) {
            return -1;
        }

        int relativeY = mouseY - layout.contentY();
        if (relativeY < 0 || relativeY >= layout.visibleRows() * ROW_HEIGHT) {
            return -1;
        }

        int hoveredIndex = SCROLL_STATE.firstVisibleIndex() + (relativeY / ROW_HEIGHT);
        return hoveredIndex < itemCount ? hoveredIndex : -1;
    }

    private static Layout createLayout(
            int containerX,
            int containerY,
            int containerWidth,
            int visualContainerHeight,
            int screenWidth,
            int screenHeight
    ) {
        int panelWidth = Math.min(PANEL_WIDTH, Math.max(1, screenWidth));
        int panelHeight = Math.min(Math.max(1, visualContainerHeight), Math.max(1, screenHeight));
        int contentHeight = Math.max(0, panelHeight - HEADER_HEIGHT);
        int visibleRows = contentHeight / ROW_HEIGHT;

        int rightX = containerX + containerWidth + PANEL_GAP;
        int leftX = containerX - panelWidth - PANEL_GAP;
        int panelX;
        boolean leftFits = leftX >= 0;
        boolean rightFits = rightX + panelWidth <= screenWidth;
        ChestCountOverlayConfig.Placement placement = ChestCountOverlayConfig.get().placement();
        panelX = switch (placement) {
            case RIGHT -> rightFits ? rightX : leftFits ? leftX : MathHelper.clamp(rightX, 0, Math.max(0, screenWidth - panelWidth));
            case LEFT, AUTO -> leftFits ? leftX : rightFits ? rightX : MathHelper.clamp(leftX, 0, Math.max(0, screenWidth - panelWidth));
        };

        int panelY = MathHelper.clamp(containerY, 0, Math.max(0, screenHeight - panelHeight));
        Side side = panelX < containerX ? Side.LEFT : Side.RIGHT;
        return new Layout(panelX, panelY, panelWidth, panelHeight, visibleRows, side);
    }

    private static int maxFirstVisibleIndex(int itemCount, int visibleRows) {
        return Math.max(0, itemCount - visibleRows);
    }

    private static boolean needsScrollbar(int itemCount, int visibleRows) {
        return visibleRows > 0 && itemCount > visibleRows;
    }

    private enum Side {
        LEFT,
        RIGHT
    }

    private record Layout(int x, int y, int width, int height, int visibleRows, Side side) {
        private int right() {
            return x + width;
        }

        private int bottom() {
            return y + height;
        }

        private int contentY() {
            return y + HEADER_HEIGHT;
        }

        private int contentHeight() {
            return Math.max(0, bottom() - contentY());
        }

        private boolean containsRows(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < right() && mouseY >= contentY() && mouseY < bottom();
        }

        private boolean containsPanel(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < right() && mouseY >= y && mouseY < bottom();
        }
    }

    private record ButtonLayout(int x, int y, int width, int height) {
        private int right() {
            return x + width;
        }

        private int bottom() {
            return y + height;
        }

        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < right() && mouseY >= y && mouseY < bottom();
        }
    }
}
