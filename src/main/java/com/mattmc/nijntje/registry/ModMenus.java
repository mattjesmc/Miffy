package com.mattmc.nijntje.registry;

import com.mattmc.nijntje.Nijntje;
import com.mattmc.nijntje.entity.NijntjeEntity;
import com.mattmc.nijntje.menu.NijntjeMenu;
import com.mattmc.nijntje.menu.OpenNijntjeMenuPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {
    public static MenuType<NijntjeMenu> NIJNTJE_MENU;

    /** Set client-side by the open-payload receiver; read by the client menu factory. */
    public static int pendingMountId = -1;

    private ModMenus() {}

    public static void register() {
        NIJNTJE_MENU = Registry.register(
            BuiltInRegistries.MENU,
            Identifier.fromNamespaceAndPath(Nijntje.MOD_ID, "nijntje"),
            new MenuType<>(ModMenus::createClientMenu, FeatureFlags.VANILLA_SET));

        PayloadTypeRegistry.clientboundPlay().register(OpenNijntjeMenuPayload.TYPE, OpenNijntjeMenuPayload.STREAM_CODEC);
        Nijntje.LOGGER.info("[Nijntje] Registered menu + networking.");
    }

    // Client-side: resolve the bunny the payload named, then build the menu.
    private static NijntjeMenu createClientMenu(final int containerId, final Inventory inventory) {
        Entity entity = inventory.player.level().getEntity(pendingMountId);
        NijntjeEntity mount = entity instanceof NijntjeEntity nijntje ? nijntje : null;
        return new NijntjeMenu(containerId, inventory, mount);
    }
}
