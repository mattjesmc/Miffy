package com.mattmc.nijntje;

import com.mattmc.nijntje.registry.ModCreativeTab;
import com.mattmc.nijntje.registry.ModEntities;
import com.mattmc.nijntje.registry.ModItems;
import com.mattmc.nijntje.registry.ModMenus;
import com.mattmc.nijntje.registry.ModSounds;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Nijntje implements ModInitializer {
    public static final String MOD_ID = "nijntje";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[Nijntje] Initializing...");
        ModSounds.register();
        ModEntities.register();
        ModItems.register();
        ModCreativeTab.register();
        ModMenus.register();
        LOGGER.info("[Nijntje] Initialized.");
    }
}
