package com.mattmc.nijntje.registry;

import com.mattmc.nijntje.Nijntje;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public final class ModCreativeTab {
    public static final ResourceKey<CreativeModeTab> NIJNTJE_TAB_KEY =
        ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(Nijntje.MOD_ID, "nijntje"));

    private ModCreativeTab() {}

    public static void register() {
        CreativeModeTab tab = FabricCreativeModeTab.builder()
            .title(Component.translatable("itemGroup.nijntje.nijntje"))
            .icon(() -> new ItemStack(ModItems.NIJNTJE_SPAWN_EGG))
            .displayItems((params, output) -> {
                output.accept(ModItems.NIJNTJE_SPAWN_EGG);
                output.accept(ModItems.BUNNY_BARDING);
            })
            .build();
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, NIJNTJE_TAB_KEY, tab);
        Nijntje.LOGGER.info("[Nijntje] Registered creative tab.");
    }
}
