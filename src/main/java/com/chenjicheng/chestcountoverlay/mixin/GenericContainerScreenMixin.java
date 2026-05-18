package com.chenjicheng.chestcountoverlay.mixin;

import com.chenjicheng.chestcountoverlay.overlay.ChestCountOverlayRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GenericContainerScreen.class)
public abstract class GenericContainerScreenMixin extends HandledScreen<GenericContainerScreenHandler> {
    private GenericContainerScreenMixin(GenericContainerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void chest_count_overlay$render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ChestCountOverlayRenderer.render(
                context,
                this.handler,
                this.handler.getRows() * 9,
                this.x,
                this.y,
                this.backgroundWidth,
                this.backgroundHeight,
                this.width,
                this.height,
                mouseX,
                mouseY
        );
    }

}
