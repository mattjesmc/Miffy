package com.mattmc.nijntje;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.resources.Identifier;

/**
 * The files this repository ships, addressed the way the game addresses them.
 *
 * <p>The tests read {@code src/main/resources} directly rather than the built jar, so a failing
 * test points at the file you edit, and so {@code gradlew test} needs no jar (and never races a
 * running dev game for one).
 */
public final class Shipped {
    public static final Path ROOT = Path.of("").toAbsolutePath();
    public static final Path RESOURCES = ROOT.resolve("src").resolve("main").resolve("resources");
    /** The client source set: not on this classpath by construction, but its files can be read. */
    public static final Path CLIENT_JAVA = ROOT.resolve("src").resolve("client").resolve("java");

    private Shipped() {
    }

    /** {@code assets/<ns>/<path>} for an id such as {@code nijntje:textures/entity/nijntje.png}. */
    public static Path asset(final Identifier id) {
        return RESOURCES.resolve("assets").resolve(id.getNamespace()).resolve(id.getPath());
    }

    /** {@code data/<ns>/<path>}. */
    public static Path data(final Identifier id) {
        return RESOURCES.resolve("data").resolve(id.getNamespace()).resolve(id.getPath());
    }

    /** A texture id the way a model names it ({@code nijntje:item/bunny_barding}) to its png. */
    public static Path texture(final String id) {
        final Identifier tex = Identifier.parse(id);
        return asset(Identifier.fromNamespaceAndPath(tex.getNamespace(), "textures/" + tex.getPath() + ".png"));
    }

    /** A model id the way an item definition names it ({@code nijntje:item/bunny_barding}) to its json. */
    public static Path model(final String id) {
        final Identifier model = Identifier.parse(id);
        return asset(Identifier.fromNamespaceAndPath(model.getNamespace(), "models/" + model.getPath() + ".json"));
    }

    public static JsonObject json(final Path file) {
        try {
            return JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (final IOException e) {
            throw new UncheckedIOException(file.toString(), e);
        }
    }

    /** Every file under a directory with the given suffix, keyed by its path relative to it. */
    public static Map<String, Path> filesUnder(final Path dir, final String suffix) {
        final Map<String, Path> out = new LinkedHashMap<>();
        if (!Files.isDirectory(dir)) {
            return out;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().endsWith(suffix))
                .sorted()
                .forEach(p -> out.put(dir.relativize(p).toString().replace('\\', '/'), p));
        } catch (final IOException e) {
            throw new UncheckedIOException(dir.toString(), e);
        }
        return out;
    }

    /** The mod's English lang file as a flat map. */
    public static Map<String, String> lang() {
        final JsonObject lang = json(asset(Identifier.fromNamespaceAndPath(Nijntje.MOD_ID, "lang/en_us.json")));
        final Map<String, String> out = new LinkedHashMap<>();
        for (final Map.Entry<String, JsonElement> e : lang.entrySet()) {
            out.put(e.getKey(), e.getValue().getAsString());
        }
        return out;
    }
}
