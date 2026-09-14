package com.mattmc.nijntje.assets;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.mattmc.nijntje.GameBootstrap;
import com.mattmc.nijntje.Nijntje;
import com.mattmc.nijntje.Shipped;
import com.mattmc.nijntje.registry.ModCreativeTab;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Every name the game will ask {@code en_us.json} for has an answer, and every answer is asked
 * for. A missing key renders as the raw {@code item.nijntje.bunny_barding}; an orphaned one is a
 * rename that was only half done.
 */
class LangCoverageTest {
    @BeforeAll
    static void boot() {
        GameBootstrap.content();
    }

    /** The keys the registered content will look up, derived from the registries, not typed. */
    private static Set<String> expectedKeys() {
        final Set<String> keys = new LinkedHashSet<>();
        for (final Map.Entry<net.minecraft.resources.ResourceKey<Item>, Item> e : BuiltInRegistries.ITEM.entrySet()) {
            if (e.getKey().identifier().getNamespace().equals(Nijntje.MOD_ID)) {
                keys.add(e.getValue().getDescriptionId());
            }
        }
        for (final Map.Entry<net.minecraft.resources.ResourceKey<EntityType<?>>, EntityType<?>> e : BuiltInRegistries.ENTITY_TYPE.entrySet()) {
            if (e.getKey().identifier().getNamespace().equals(Nijntje.MOD_ID)) {
                keys.add(e.getValue().getDescriptionId());
            }
        }
        if (BuiltInRegistries.CREATIVE_MODE_TAB.getValue(ModCreativeTab.NIJNTJE_TAB_KEY).getDisplayName()
                .getContents() instanceof TranslatableContents title) {
            keys.add(title.getKey());
        }
        // Subtitles come from sounds.json, the one place a sound names its caption.
        final JsonObject sounds = Shipped.json(Shipped.asset(Identifier.fromNamespaceAndPath(Nijntje.MOD_ID, "sounds.json")));
        for (final String event : sounds.keySet()) {
            final JsonObject def = sounds.getAsJsonObject(event);
            if (def.has("subtitle")) {
                keys.add(def.get("subtitle").getAsString());
            }
        }
        return keys;
    }

    @Test
    void everyRegisteredNameHasAnEnglishEntry() {
        final Map<String, String> lang = Shipped.lang();
        final List<String> missing = new ArrayList<>();
        for (final String key : expectedKeys()) {
            if (!lang.containsKey(key)) {
                missing.add(key);
            } else if (lang.get(key).isBlank()) {
                missing.add(key + " (blank)");
            }
        }
        assertTrue(missing.isEmpty(), "en_us.json lacks: " + missing);
    }

    @Test
    void everyEnglishEntryIsAskedFor() {
        final Set<String> expected = expectedKeys();
        final List<String> orphans = new ArrayList<>();
        for (final String key : Shipped.lang().keySet()) {
            if (!expected.contains(key)) {
                orphans.add(key);
            }
        }
        assertTrue(orphans.isEmpty(), "en_us.json has keys nothing registered asks for (a half-done rename?): " + orphans);
    }
}
