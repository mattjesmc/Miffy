package com.mattmc.nijntje.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.mattmc.nijntje.GameBootstrap;
import com.mattmc.nijntje.Nijntje;
import com.mattmc.nijntje.Shipped;
import com.mattmc.nijntje.registry.ModItems;
import com.mojang.serialization.JsonOps;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Every recipe this repository ships, read through the game's own recipe codec and then actually
 * crafted. A recipe is how the barding is reached in survival, and vanilla's loader steps over a
 * malformed one with a log line: the game boots, the table just never lights up.
 */
class ShippedRecipeTest {
    private static RegistryOps<JsonElement> ops;

    @BeforeAll
    static void boot() {
        GameBootstrap.content();
        // Ingredients and results resolve item ids against the built-in registries; nothing in
        // this mod's recipes names a tag, so no datapack needs loading to bind one.
        ops = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)
            .createSerializationContext(JsonOps.INSTANCE);
    }

    private static Map<String, Path> recipes() {
        return Shipped.filesUnder(Shipped.data(Identifier.fromNamespaceAndPath(Nijntje.MOD_ID, "recipe")), ".json");
    }

    private static Recipe<?> decode(final Path file) {
        return Recipe.CODEC.parse(ops, Shipped.json(file))
            .getOrThrow(message -> new AssertionError(file.getFileName() + ": " + message));
    }

    @Test
    void everyRecipeDecodesAndRoundTrips() {
        final Map<String, Path> files = recipes();
        assertFalse(files.isEmpty(), "this repository ships recipes; none were found under data/nijntje/recipe");
        final List<String> broken = new ArrayList<>();
        files.forEach((name, file) -> {
            final Recipe<?> recipe = decode(file);
            // And back out: a field the codec drops on encode would survive a parse-only check.
            final JsonElement again = Recipe.CODEC.encodeStart(ops, recipe)
                .getOrThrow(message -> new AssertionError(name + " cannot be written back: " + message));
            final JsonElement third = Recipe.CODEC.encodeStart(ops, decode(file))
                .getOrThrow(message -> new AssertionError(name + ": " + message));
            if (!again.equals(third)) {
                broken.add(name + " writes differently every time");
            }
        });
        assertTrue(broken.isEmpty(), String.join("\n", broken));
    }

    @Test
    void bunnyBardingIsCraftedFromIronAroundLeather() {
        final Recipe<?> recipe = decode(recipes().get("bunny_barding.json"));
        final ShapedRecipe shaped = assertInstanceOf(ShapedRecipe.class, recipe);
        assertEquals(3, shaped.getWidth());
        assertEquals(3, shaped.getHeight());

        final ItemStack iron = new ItemStack(Items.IRON_INGOT);
        final ItemStack leather = new ItemStack(Items.LEATHER);
        final CraftingInput grid = CraftingInput.of(3, 3, List.of(
            iron, ItemStack.EMPTY, iron,
            iron, leather, iron,
            iron, iron, iron));
        assertTrue(shaped.matches(grid, null), "the shipped pattern does not match I I / ILI / III");

        final ItemStack result = shaped.assemble(grid);
        assertSame(ModItems.BUNNY_BARDING, result.getItem());
        assertEquals(1, result.getCount());

        // Leather in the wrong place is not a barding.
        final CraftingInput wrong = CraftingInput.of(3, 3, List.of(
            iron, leather, iron,
            iron, ItemStack.EMPTY, iron,
            iron, iron, iron));
        assertFalse(shaped.matches(wrong, null));
    }
}
