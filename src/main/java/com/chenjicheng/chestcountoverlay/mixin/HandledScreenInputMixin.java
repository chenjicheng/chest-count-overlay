package com.chenjicheng.chestcountoverlay.mixin;

import com.chenjicheng.chestcountoverlay.input.ChestCountOverlayKeyBindings;
import com.chenjicheng.chestcountoverlay.overlay.ChestCountOverlayRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public abstract class HandledScreenInputMixin<T extends ScreenHandler> extends Screen {
    private static final int SHULKER_BOX_SLOT_COUNT = 27;

    @Shadow
    @Final
    protected T handler;

    @Shadow
    protected int x;

    @Shadow
    protected int y;

    @Shadow
    protected int backgroundWidth;

    @Shadow
    protected int backgroundHeight;

    private HandledScreenInputMixin(Text title) {
        super(title);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void chest_count_overlay$keyPressed(KeyInput input, CallbackInfoReturnable<Boolean> cir) {
        if (ChestCountOverlayKeyBindings.handleScreenKeyPressed(this, input)) {
            cir.setReturnValue(true);
        }
    }

    // GenericContainerScreen inherits input handling from HandledScreen in Yarn 1.21.11.
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void chest_count_overlay$mouseClicked(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        int containerSlotCount = supportedContainerSlotCount();
        if (containerSlotCount <= 0) {
            return;
        }

        if (ChestCountOverlayRenderer.mouseClicked(
                this.handler,
                containerSlotCount,
                this.x,
                this.y,
                this.backgroundWidth,
                this.backgroundHeight,
                this.width,
                this.height,
                click.x(),
                click.y(),
                click.button()
        )) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void chest_count_overlay$mouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        int containerSlotCount = supportedContainerSlotCount();
        if (containerSlotCount <= 0) {
            return;
        }

        boolean consumed = ChestCountOverlayRenderer.mouseScrolled(
                this.handler,
                containerSlotCount,
                this.x,
                this.y,
                this.backgroundWidth,
                this.backgroundHeight,
                this.width,
                this.height,
                mouseX,
                mouseY,
                verticalAmount
        );

        if (consumed) {
            cir.setReturnValue(true);
        }
    }

    private int supportedContainerSlotCount() {
        if ((Object) this instanceof GenericContainerScreen && this.handler instanceof GenericContainerScreenHandler genericHandler) {
            return genericHandler.getRows() * 9;
        }

        if ((Object) this instanceof ShulkerBoxScreen && this.handler instanceof ShulkerBoxScreenHandler) {
            return SHULKER_BOX_SLOT_COUNT;
        }

        return 0;
    }
}
