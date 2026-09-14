package com.mattmc.nijntje;

import com.mattmc.nijntje.registry.ModCreativeTab;
import com.mattmc.nijntje.registry.ModEntities;
import com.mattmc.nijntje.registry.ModItems;
import com.mattmc.nijntje.registry.ModMenus;
import com.mattmc.nijntje.registry.ModSounds;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;

/**
 * Brings up as much of the game as a registry test needs, once per test JVM.
 *
 * <p>Two layers have to be there before this mod's registry classes can even load. {@code
 * fabric-loader-junit} (a test dependency in build.gradle) runs the tests under Knot with the
 * mixins applied, which is what lets Fabric's registry API reach {@code BuiltInRegistries}.
 * {@link Bootstrap} then fills the built-in registries themselves - vanilla refuses to register
 * into them before it has been called, with "Not bootstrapped".
 *
 * <p>Neither is a game: no world, no server, no client. Anything that needs a {@code Level} (a
 * living Nijntje, its AI, its inventory) is not testable here; that half is what the running dev
 * game and the toolkit's {@code checkAssets} gate are for.
 */
public final class GameBootstrap {
    private static boolean done;
    private static boolean content;

    private GameBootstrap() {
    }

    /** Idempotent: {@link Bootstrap#bootStrap()} is itself a no-op the second time, this is cheaper. */
    public static synchronized void once() {
        if (done) {
            return;
        }
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        done = true;
    }

    /**
     * Everything this mod puts into a built-in registry, in {@link Nijntje#onInitialize()}'s own
     * order and for its reasons: the spawn egg and the barding's {@code Equippable} both name
     * {@link ModEntities#NIJNTJE}, and the creative tab's icon is the spawn egg.
     *
     * <p>The flag is not an optimisation: {@link net.minecraft.core.Registry#register} throws on a
     * second registration of the same id, so this must run once per JVM however many tests ask.
     */
    public static synchronized void content() {
        if (content) {
            return;
        }
        once();
        ModSounds.register();
        ModEntities.register();
        ModItems.register();
        ModCreativeTab.register();
        ModMenus.register();
        bakeItemComponents();
        content = true;
    }

    /**
     * Since 26.x an item's components are not fixed at registration: they are BAKED afterwards
     * from a registry provider ({@link DataComponentInitializers}), because a default may point
     * into a datapack registry. Until that bake every item holder - vanilla's included - answers
     * {@code Components not bound yet}, and {@code new ItemStack(...)} throws saying so. The game
     * runs it at server start with the full datapack; this mod's defaults only name built-in
     * registries (attributes, entity types), so vanilla's static lookup is enough here.
     *
     * <p>After {@link #content()}'s registrations so that this mod's items are in the list.
     */
    private static void bakeItemComponents() {
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS
            .build(VanillaRegistries.createLookup())
            .forEach(DataComponentInitializers.PendingComponents::apply);
    }
}
