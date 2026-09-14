package com.mattmc.nijntje.menu;

import com.mattmc.nijntje.entity.NijntjeEntity;
import com.mattmc.nijntje.registry.ModMenus;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ArmorSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Miffy's inventory: a saddle slot, a barding (body-armor) slot, a 3x3 saddlebag, and the
 * player inventory. The screen draws hunger/size/progress on top of these slots.
 */
public class NijntjeMenu extends AbstractContainerMenu {
    private static final Identifier SADDLE_SPRITE = Identifier.withDefaultNamespace("container/slot/saddle");
    private static final Identifier ARMOR_SPRITE = Identifier.withDefaultNamespace("container/slot/horse_armor");
    public static final int STORAGE_SIZE = 9;
    private static final int CONTAINER_SLOTS = 2 + STORAGE_SIZE; // saddle + armor + storage

    public final NijntjeEntity mount;

    public NijntjeMenu(final int containerId, final Inventory playerInventory, final NijntjeEntity mount) {
        super(ModMenus.NIJNTJE_MENU, containerId);
        this.mount = mount;

        Container saddleContainer = mount.createEquipmentSlotContainer(EquipmentSlot.SADDLE);
        Container bodyContainer = mount.createEquipmentSlotContainer(EquipmentSlot.BODY);
        Container storage = mount.getInventory();

        this.addSlot(new ArmorSlot(saddleContainer, mount, EquipmentSlot.SADDLE, 0, 8, 18, SADDLE_SPRITE));
        this.addSlot(new ArmorSlot(bodyContainer, mount, EquipmentSlot.BODY, 0, 8, 36, ARMOR_SPRITE));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new Slot(storage, col + row * 3, 98 + col * 18, 18 + row * 18));
            }
        }

        this.addStandardInventorySlots(playerInventory, 8, 134);
    }

    @Override
    public boolean stillValid(final Player player) {
        return this.mount != null && this.mount.isAlive()
            && this.mount.distanceToSqr(player) < 64.0 && this.mount.isOwnedBy(player);
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < CONTAINER_SLOTS) {
                if (!this.moveItemStackTo(stack, CONTAINER_SLOTS, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, 0, CONTAINER_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }
}
