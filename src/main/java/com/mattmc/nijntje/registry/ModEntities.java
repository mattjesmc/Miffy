package com.mattmc.nijntje.registry;

import com.mattmc.nijntje.Nijntje;
import com.mattmc.nijntje.entity.NijntjeEntity;
import com.mattmc.nijntje.entity.SnuffyEntity;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.levelgen.Heightmap;

public final class ModEntities {
    public static final Identifier NIJNTJE_ID =
        Identifier.fromNamespaceAndPath(Nijntje.MOD_ID, "nijntje");

    public static final ResourceKey<EntityType<?>> NIJNTJE_KEY =
        ResourceKey.create(Registries.ENTITY_TYPE, NIJNTJE_ID);

    // Adult footprint; per-stage sizes are handled via getDefaultDimensions()/scale.
    public static final EntityType<NijntjeEntity> NIJNTJE =
        EntityType.Builder.of(NijntjeEntity::new, MobCategory.CREATURE)
            .sized(0.5F, 0.7F)
            .eyeHeight(0.55F)
            .passengerAttachments(1.0F) // seat on top of the head, between the ears (scales with growth)
            .clientTrackingRange(10)
            .build(NIJNTJE_KEY);

    public static final Identifier SNUFFY_ID =
        Identifier.fromNamespaceAndPath(Nijntje.MOD_ID, "snuffy");
    public static final ResourceKey<EntityType<?>> SNUFFY_KEY =
        ResourceKey.create(Registries.ENTITY_TYPE, SNUFFY_ID);

    public static final EntityType<SnuffyEntity> SNUFFY =
        EntityType.Builder.of(SnuffyEntity::new, MobCategory.CREATURE)
            .sized(0.5F, 0.5F)
            .eyeHeight(0.4F)
            .clientTrackingRange(10)
            .build(SNUFFY_KEY);

    private ModEntities() {}

    public static void register() {
        Registry.register(BuiltInRegistries.ENTITY_TYPE, NIJNTJE_KEY, NIJNTJE);
        Registry.register(BuiltInRegistries.ENTITY_TYPE, SNUFFY_KEY, SNUFFY);
        FabricDefaultAttributeRegistry.register(NIJNTJE, NijntjeEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(SNUFFY, SnuffyEntity.createAttributes());

        // Wild Nijntje spawn on grassy ground in daylight (checkAnimalSpawnRules), in small
        // groups across overworld biomes.
        SpawnPlacements.register(NIJNTJE, SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Animal::checkAnimalSpawnRules);
        BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(),
            MobCategory.CREATURE, NIJNTJE, 8, 1, 3);

        Nijntje.LOGGER.info("[Nijntje] Registered entity type + spawns.");
    }
}
