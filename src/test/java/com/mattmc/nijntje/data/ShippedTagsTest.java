package com.mattmc.nijntje.data;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mattmc.nijntje.GameBootstrap;
import com.mattmc.nijntje.Shipped;
import com.mattmc.nijntje.registry.ModEntities;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.EntityTypeTags;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The tag files this repository ships. A tag whose member does not exist is not a smaller tag:
 * vanilla drops the whole tag with a log line, and for {@code can_equip_saddle} that would mean
 * every vanilla mount silently loses its saddle slot in a world with this mod installed.
 */
class ShippedTagsTest {
    @BeforeAll
    static void boot() {
        GameBootstrap.content();
    }

    @Test
    void everyEntityTypeTagMemberExists() {
        final Map<String, Path> files = Shipped.filesUnder(
            Shipped.RESOURCES.resolve("data").resolve("minecraft").resolve("tags").resolve("entity_type"), ".json");
        assertFalse(files.isEmpty(), "expected data/minecraft/tags/entity_type/*.json");
        final List<String> missing = new ArrayList<>();
        files.forEach((name, file) -> {
            final JsonObject tag = Shipped.json(file);
            for (final JsonElement value : tag.getAsJsonArray("values")) {
                final String entry = value.isJsonPrimitive() ? value.getAsString() : value.getAsJsonObject().get("id").getAsString();
                if (entry.startsWith("#")) {
                    continue; // a nested tag: bound at datapack load, not a registry entry
                }
                if (!BuiltInRegistries.ENTITY_TYPE.containsKey(Identifier.parse(entry))) {
                    missing.add(name + " names " + entry + ", which is not a registered entity type");
                }
            }
        });
        assertTrue(missing.isEmpty(), String.join("\n", missing));
    }

    @Test
    void nijntjeIsAddedToCanEquipSaddleWithoutReplacingIt() {
        // The saddle slot is what makes the bunny rideable; `replace: true` here would strip it
        // from every vanilla mount.
        final Path file = Shipped.data(Identifier.withDefaultNamespace("tags/entity_type/"
            + EntityTypeTags.CAN_EQUIP_SADDLE.location().getPath() + ".json"));
        final JsonObject tag = Shipped.json(file);
        assertFalse(tag.has("replace") && tag.get("replace").getAsBoolean(),
            "can_equip_saddle.json must not replace the vanilla tag");
        boolean found = false;
        for (final JsonElement value : tag.getAsJsonArray("values")) {
            found |= value.getAsString().equals(ModEntities.NIJNTJE_ID.toString());
        }
        assertTrue(found, "can_equip_saddle.json does not list " + ModEntities.NIJNTJE_ID);
    }
}
