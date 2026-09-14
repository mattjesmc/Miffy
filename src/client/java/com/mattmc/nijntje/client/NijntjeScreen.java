package com.mattmc.nijntje.client;

import com.mattmc.nijntje.entity.NijntjeEntity;
import com.mattmc.nijntje.menu.NijntjeMenu;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

@Environment(EnvType.CLIENT)
public class NijntjeScreen extends AbstractContainerScreen<NijntjeMenu> {
    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");

    public NijntjeScreen(final NijntjeMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title, 176, 216);
        this.inventoryLabelY = 124;
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partial) {
        super.extractBackground(graphics, mouseX, mouseY, partial);
        int x = this.leftPos;
        int y = this.topPos;

        // Panel.
        graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + this.imageWidth - 1, y + this.imageHeight - 1, 0xFFC6C6C6);

        // Slot backgrounds for every slot in the menu.
        for (Slot slot : this.menu.slots) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, x + slot.x - 1, y + slot.y - 1, 18, 18);
        }

        NijntjeEntity mount = this.menu.mount;
        if (mount == null) {
            return;
        }

        // Live bunny preview — doubles as the size/age indicator (it renders at its grown scale).
        InventoryScreen.extractEntityInInventoryFollowsMouse(
            graphics, x + 27, y + 16, x + 71, y + 78, 18, 0.25F, (float) mouseX, (float) mouseY, mount);

        // Fullness bar (how recently fed).
        graphics.text(this.font, Component.literal("Fullness"), x + 8, y + 86, 0xFF303030, false);
        drawBar(graphics, x + 8, y + 94, 160, 6, mount.getFullness() / 100.0F, 0xFFE0A030, 0xFF3A2A10);

        // Growth progress bar toward the next size.
        int need = mount.getCareNeeded();
        float progress = need <= 0 ? 1.0F : Mth.clamp(mount.getSyncedCareProgress() / (float) need, 0.0F, 1.0F);
        graphics.text(this.font, Component.literal(need <= 0 ? "Fully grown" : "Growth"), x + 8, y + 104, 0xFF303030, false);
        drawBar(graphics, x + 8, y + 112, 160, 6, progress, 0xFF57C24A, 0xFF16330F);
    }

    private void drawBar(final GuiGraphicsExtractor graphics, final int x, final int y, final int w, final int h,
                         final float frac, final int fg, final int bg) {
        graphics.fill(x, y, x + w, y + h, 0xFF202020);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, bg);
        int filled = (int) ((w - 2) * Mth.clamp(frac, 0.0F, 1.0F));
        if (filled > 0) {
            graphics.fill(x + 1, y + 1, x + 1 + filled, y + h - 1, fg);
        }
    }

    private static String stageName(final NijntjeEntity.GrowthStage stage) {
        return switch (stage) {
            case BABY -> "Baby";
            case YOUNG -> "Young";
            case ADULT -> "Adult";
            case GROWN -> "Grown";
        };
    }

    @Override
    protected void extractLabels(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        NijntjeEntity mount = this.menu.mount;
        Component titleText = mount == null
            ? this.title
            : Component.literal(mount.getName().getString() + " (" + stageName(mount.getGrowthStage()) + ")");
        graphics.text(this.font, titleText, this.titleLabelX, this.titleLabelY, 0xFF404040, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF404040, false);
    }
}
