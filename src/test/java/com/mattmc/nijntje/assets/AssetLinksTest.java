package com.mattmc.nijntje.assets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mattmc.nijntje.GameBootstrap;
import com.mattmc.nijntje.Nijntje;
import com.mattmc.nijntje.Shipped;
import com.mattmc.nijntje.registry.ModSounds;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The links between shipped files: an item to its client definition, a definition to its model,
 * a model to its textures, a sound event to its clips, and the client code's texture ids to the
 * pngs. The resource loader treats a broken link as a missing texture, not a failed load.
 *
 * <p>Overlaps the toolkit's {@code checkAssets} gate on purpose: that runs against a live game
 * and knows vanilla; this runs in {@code gradlew test} and knows THIS mod's cross-references.
 */
class AssetLinksTest {
    @BeforeAll
    static void boot() {
        GameBootstrap.content();
    }

    private static Set<String> modItemPaths() {
        final Set<String> paths = new LinkedHashSet<>();
        for (final Map.Entry<net.minecraft.resources.ResourceKey<Item>, Item> e : BuiltInRegistries.ITEM.entrySet()) {
            if (e.getKey().identifier().getNamespace().equals(Nijntje.MOD_ID)) {
                paths.add(e.getKey().identifier().getPath());
            }
        }
        return paths;
    }

    @Test
    void everyItemHasAClientDefinitionModelAndTextures() {
        final List<String> problems = new ArrayList<>();
        for (final String item : modItemPaths()) {
            final Path definition = Shipped.asset(Identifier.fromNamespaceAndPath(Nijntje.MOD_ID, "items/" + item + ".json"));
            if (!Files.isRegularFile(definition)) {
                problems.add(item + ": no assets/nijntje/items/" + item + ".json (renders as the missing-model cube)");
                continue;
            }
            final JsonObject model = Shipped.json(definition).getAsJsonObject("model");
            if (model == null || !model.has("model")) {
                problems.add(item + ": items/" + item + ".json has no model.model");
                continue;
            }
            final Path modelFile = Shipped.model(model.get("model").getAsString());
            if (!Files.isRegularFile(modelFile)) {
                problems.add(item + ": model " + model.get("model").getAsString() + " has no file at " + Shipped.RESOURCES.relativize(modelFile));
                continue;
            }
            final JsonObject textures = Shipped.json(modelFile).getAsJsonObject("textures");
            if (textures == null) {
                continue; // inherits its parent's textures wholesale; nothing of ours to check
            }
            for (final Map.Entry<String, JsonElement> tex : textures.entrySet()) {
                final String ref = tex.getValue().getAsString();
                if (ref.startsWith("#")) {
                    continue;
                }
                if (!Files.isRegularFile(Shipped.texture(ref))) {
                    problems.add(item + ": texture " + ref + " has no png");
                }
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    void soundsJsonMatchesTheRegisteredEventsAndItsClipsExist() {
        final JsonObject sounds = Shipped.json(Shipped.asset(Identifier.fromNamespaceAndPath(Nijntje.MOD_ID, "sounds.json")));

        final Set<String> registered = new LinkedHashSet<>();
        for (final Map.Entry<net.minecraft.resources.ResourceKey<net.minecraft.sounds.SoundEvent>, net.minecraft.sounds.SoundEvent> e
                : BuiltInRegistries.SOUND_EVENT.entrySet()) {
            if (e.getKey().identifier().getNamespace().equals(Nijntje.MOD_ID)) {
                registered.add(e.getKey().identifier().getPath());
            }
        }
        assertEquals(registered, sounds.keySet(),
            "sounds.json's events and the registered SoundEvents must be the same set");
        assertTrue(registered.contains(ModSounds.NIJNTJE_SAY_ID.getPath()));

        final List<String> missing = new ArrayList<>();
        for (final String event : sounds.keySet()) {
            for (final JsonElement clip : sounds.getAsJsonObject(event).getAsJsonArray("sounds")) {
                final String name = clip.isJsonPrimitive() ? clip.getAsString() : clip.getAsJsonObject().get("name").getAsString();
                final Identifier id = Identifier.parse(name);
                final Path ogg = Shipped.asset(Identifier.fromNamespaceAndPath(id.getNamespace(), "sounds/" + id.getPath() + ".ogg"));
                if (!Files.isRegularFile(ogg)) {
                    missing.add(event + " names " + name + " but there is no " + Shipped.RESOURCES.relativize(ogg));
                }
            }
        }
        assertTrue(missing.isEmpty(), String.join("\n", missing));
    }

    /**
     * The client source set is not on this classpath (splitEnvironmentSourceSets), so its texture
     * ids are read out of the source text: every {@code Identifier.fromNamespaceAndPath(MOD_ID,
     * "textures/...")} the renderers name must have a png.
     */
    @Test
    void everyTextureTheClientCodeNamesExists() throws IOException {
        final Pattern ref = Pattern.compile("fromNamespaceAndPath\\(\\s*Nijntje\\.MOD_ID\\s*,\\s*\"(textures/[^\"]+)\"");
        final List<String> named = new ArrayList<>();
        final List<String> missing = new ArrayList<>();
        for (final Path source : Shipped.filesUnder(Shipped.CLIENT_JAVA, ".java").values()) {
            final Matcher m = ref.matcher(Files.readString(source));
            while (m.find()) {
                named.add(m.group(1));
                final Path png = Shipped.asset(Identifier.fromNamespaceAndPath(Nijntje.MOD_ID, m.group(1)));
                if (!Files.isRegularFile(png)) {
                    missing.add(source.getFileName() + " names " + m.group(1) + " but there is no " + Shipped.RESOURCES.relativize(png));
                }
            }
        }
        assertFalse(named.isEmpty(), "found no texture ids in src/client - did the renderers move?");
        assertTrue(missing.isEmpty(), String.join("\n", missing));
    }

    @Test
    void fabricModJsonNamesRealEntrypointsAndIcon() throws ClassNotFoundException {
        final JsonObject mod = Shipped.json(Shipped.RESOURCES.resolve("fabric.mod.json"));
        assertEquals(Nijntje.MOD_ID, mod.get("id").getAsString());
        assertTrue(Files.isRegularFile(Shipped.RESOURCES.resolve(mod.get("icon").getAsString())), "icon file missing");

        final JsonObject entrypoints = mod.getAsJsonObject("entrypoints");
        for (final JsonElement main : entrypoints.getAsJsonArray("main")) {
            Class.forName(main.getAsString()); // on this classpath: a typo fails here, not at boot
        }
        for (final JsonElement client : entrypoints.getAsJsonArray("client")) {
            final Path source = Shipped.CLIENT_JAVA.resolve(client.getAsString().replace('.', '/') + ".java");
            assertTrue(Files.isRegularFile(source), "client entrypoint " + client.getAsString() + " has no source under src/client");
        }
    }
}
