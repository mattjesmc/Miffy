package com.mattmc.nijntje.registry;

import com.mattmc.nijntje.Nijntje;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.equipment.Equippable;

public final class ModItems {
    public static final Identifier BUNNY_BARDING_ID =
        Identifier.fromNamespaceAndPath(Nijntje.MOD_ID, "bunny_barding");
    private static final Identifier BARDING_ARMOR_MOD_ID =
        Identifier.fromNamespaceAndPath(Nijntje.MOD_ID, "bunny_barding_armor");
    private static final Identifier BARDING_TOUGHNESS_MOD_ID =
        Identifier.fromNamespaceAndPath(Nijntje.MOD_ID, "bunny_barding_toughness");

    public static final Identifier NIJNTJE_SPAWN_EGG_ID =
        Identifier.fromNamespaceAndPath(Nijntje.MOD_ID, "nijntje_spawn_egg");

    public static Item BUNNY_BARDING;
    public static Item NIJNTJE_SPAWN_EGG;

    private ModItems() {}

    public static void register() {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, BUNNY_BARDING_ID);

        // Body armor equippable only on Nijntje; right-clicking the bunny with it puts it on.
        Equippable equippable = Equippable.builder(EquipmentSlot.BODY)
            .setAllowedEntities(ModEntities.NIJNTJE)
            .setEquipOnInteract(true)
            .setDamageOnHurt(false)
            .build();

        // Grants armor + toughness while worn in the BODY slot.
        ItemAttributeModifiers attributes = ItemAttributeModifiers.builder()
            .add(Attributes.ARMOR,
                new AttributeModifier(BARDING_ARMOR_MOD_ID, 6.0, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.BODY)
            .add(Attributes.ARMOR_TOUGHNESS,
                new AttributeModifier(BARDING_TOUGHNESS_MOD_ID, 2.0, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.BODY)
            .build();

        BUNNY_BARDING = Registry.register(
            BuiltInRegistries.ITEM, key,
            new Item(new Item.Properties()
                .stacksTo(1)
                .component(DataComponents.EQUIPPABLE, equippable)
                .attributes(attributes)
                .setId(key)));

        ResourceKey<Item> eggKey = ResourceKey.create(Registries.ITEM, NIJNTJE_SPAWN_EGG_ID);
        NIJNTJE_SPAWN_EGG = Registry.register(
            BuiltInRegistries.ITEM, eggKey,
            new SpawnEggItem(new Item.Properties()
                .component(DataComponents.ENTITY_DATA, TypedEntityData.of(ModEntities.NIJNTJE, new CompoundTag()))
                .setId(eggKey)));

        Nijntje.LOGGER.info("[Nijntje] Registered items.");
    }
}
