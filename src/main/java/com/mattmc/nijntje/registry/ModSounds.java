package com.mattmc.nijntje.registry;

import com.mattmc.nijntje.Nijntje;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
    public static final Identifier NIJNTJE_SAY_ID =
        Identifier.fromNamespaceAndPath(Nijntje.MOD_ID, "nijntje_say");

    public static SoundEvent NIJNTJE_SAY;

    private ModSounds() {}

    public static void register() {
        NIJNTJE_SAY = Registry.register(
            BuiltInRegistries.SOUND_EVENT, NIJNTJE_SAY_ID,
            SoundEvent.createVariableRangeEvent(NIJNTJE_SAY_ID));
        Nijntje.LOGGER.info("[Nijntje] Registered sounds.");
    }
}
