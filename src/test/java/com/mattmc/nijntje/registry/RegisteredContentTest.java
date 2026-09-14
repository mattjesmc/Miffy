package com.mattmc.nijntje.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mattmc.nijntje.GameBootstrap;
import com.mattmc.nijntje.entity.NijntjeEntity;
import com.mattmc.nijntje.entity.SnuffyEntity;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.Equippable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * What {@link com.mattmc.nijntje.Nijntje#onInitialize()} leaves in the built-in registries, and
 * the cross-references between those entries that the game only checks at the moment a player
 * uses them: the egg's entity, who the barding fits, what it is worth.
 */
class RegisteredContentTest {
    @BeforeAll
    static void boot() {
        GameBootstrap.content();
    }

    @Test
    void everyIdIsRegisteredUnderTheModNamespace() {
        assertSame(ModEntities.NIJNTJE, BuiltInRegistries.ENTITY_TYPE.getValue(ModEntities.NIJNTJE_ID));
        assertSame(ModEntities.SNUFFY, BuiltInRegistries.ENTITY_TYPE.getValue(ModEntities.SNUFFY_ID));
        assertSame(ModItems.BUNNY_BARDING, BuiltInRegistries.ITEM.getValue(ModItems.BUNNY_BARDING_ID));
        assertSame(ModItems.NIJNTJE_SPAWN_EGG, BuiltInRegistries.ITEM.getValue(ModItems.NIJNTJE_SPAWN_EGG_ID));
        assertSame(ModSounds.NIJNTJE_SAY, BuiltInRegistries.SOUND_EVENT.getValue(ModSounds.NIJNTJE_SAY_ID));
        assertTrue(BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(ModCreativeTab.NIJNTJE_TAB_KEY));
        assertSame(ModMenus.NIJNTJE_MENU,
            BuiltInRegistries.MENU.getValue(Identifier.fromNamespaceAndPath("nijntje", "nijntje")));
    }

    @Test
    void spawnEggSpawnsANijntje() {
        final EntityType<?> type = SpawnEggItem.getType(new ItemStack(ModItems.NIJNTJE_SPAWN_EGG));
        assertSame(ModEntities.NIJNTJE, type, "the egg's ENTITY_DATA component names the wrong entity");
    }

    @Test
    void bardingFitsOnlyANijntjeAndEquipsOnInteract() {
        final Equippable equippable = ModItems.BUNNY_BARDING.components().get(DataComponents.EQUIPPABLE);
        assertNotNull(equippable, "bunny_barding has no EQUIPPABLE component");
        assertEquals(EquipmentSlot.BODY, equippable.slot());
        assertTrue(equippable.equipOnInteract(), "right-clicking the bunny with it should put it on");
        assertTrue(equippable.allowedEntities().isPresent(), "allowed on every entity: a player could wear it");
        assertTrue(equippable.allowedEntities().get().contains(BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(ModEntities.NIJNTJE)));
        assertFalse(equippable.allowedEntities().get().contains(BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(EntityTypes.PLAYER)));
        assertFalse(equippable.allowedEntities().get().contains(BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(ModEntities.SNUFFY)));
    }

    @Test
    void bardingGrantsArmorAndToughnessInTheBodySlot() {
        final ItemAttributeModifiers modifiers = ModItems.BUNNY_BARDING.components().get(DataComponents.ATTRIBUTE_MODIFIERS);
        assertNotNull(modifiers, "bunny_barding has no ATTRIBUTE_MODIFIERS component");
        final Map<String, Double> byAttribute = new java.util.HashMap<>();
        for (final ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            assertEquals(EquipmentSlotGroup.BODY, entry.slot(), entry.attribute() + " applies outside the body slot");
            byAttribute.put(entry.attribute().getRegisteredName(), entry.modifier().amount());
        }
        assertEquals(6.0, byAttribute.get(Attributes.ARMOR.getRegisteredName()));
        assertEquals(2.0, byAttribute.get(Attributes.ARMOR_TOUGHNESS.getRegisteredName()));
        assertEquals(2, byAttribute.size(), "unexpected extra modifiers: " + byAttribute);
    }

    @Test
    void bardingDoesNotStack() {
        assertEquals(1, ModItems.BUNNY_BARDING.getDefaultMaxStackSize());
    }

    @Test
    void nijntjeAttributesCoverWhatGrowthAndRidingModify() {
        // The growth stages add modifiers to these five, and a modifier on an attribute the
        // supplier lacks is a silent no-op, not an error.
        final AttributeSupplier attributes = NijntjeEntity.createAttributes().build();
        assertTrue(attributes.hasAttribute(Attributes.MAX_HEALTH));
        assertTrue(attributes.hasAttribute(Attributes.MOVEMENT_SPEED));
        assertTrue(attributes.hasAttribute(Attributes.ATTACK_DAMAGE));
        assertTrue(attributes.hasAttribute(Attributes.ARMOR));
        assertTrue(attributes.hasAttribute(Attributes.SCALE));
        assertTrue(attributes.hasAttribute(Attributes.JUMP_STRENGTH), "PlayerRideableJumping reads JUMP_STRENGTH");
        assertEquals(10.0, attributes.getBaseValue(Attributes.MAX_HEALTH));

        final AttributeSupplier snuffy = SnuffyEntity.createAttributes().build();
        assertTrue(snuffy.hasAttribute(Attributes.MAX_HEALTH));
        assertTrue(snuffy.hasAttribute(Attributes.MOVEMENT_SPEED));
    }
}
