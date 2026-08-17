package com.animania.extra.client.screen;

import com.animania.extra.ExtraHamsterWheelMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Compact native screen for the wheel's single food slot. */
public final class ExtraHamsterWheelScreen extends AbstractContainerScreen<ExtraHamsterWheelMenu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("minecraft", "textures/gui/container/hopper.png");

    public ExtraHamsterWheelScreen(ExtraHamsterWheelMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageHeight = 133;
        inventoryLabelY = 39;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        // The hopper texture has exactly the same 176x133 player-inventory
        // layout used by this menu. Cover its four unused storage slots and
        // expose only the real central wheel slot.
        graphics.fill(leftPos + 43, topPos + 19, leftPos + 133, topPos + 38, 0xFFC6C6C6);
        graphics.fill(leftPos + 79, topPos + 19, leftPos + 97, topPos + 37, 0xFF373737);
        graphics.fill(leftPos + 80, topPos + 20, leftPos + 96, topPos + 36, 0xFF8B8B8B);
    }
}
