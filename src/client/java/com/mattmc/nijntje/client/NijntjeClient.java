package com.mattmc.nijntje.client;

import com.mattmc.nijntje.Nijntje;
import com.mattmc.nijntje.menu.OpenNijntjeMenuPayload;
import com.mattmc.nijntje.registry.ModEntities;
import com.mattmc.nijntje.registry.ModMenus;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public class NijntjeClient implements ClientModInitializer {
    public static final ModelLayerLocation NIJNTJE_LAYER =
        new ModelLayerLocation(Identifier.fromNamespaceAndPath(Nijntje.MOD_ID, "nijntje"), "main");
    public static final ModelLayerLocation SNUFFY_LAYER =
        new ModelLayerLocation(Identifier.fromNamespaceAndPath(Nijntje.MOD_ID, "snuffy"), "main");

    @Override
    public void onInitializeClient() {
        ModelLayerRegistry.registerModelLayer(NIJNTJE_LAYER, NijntjeModel::createBodyLayer);
        EntityRendererRegistry.register(ModEntities.NIJNTJE, NijntjeRenderer::new);
        ModelLayerRegistry.registerModelLayer(SNUFFY_LAYER, SnuffyModel::createBodyLayer);
        EntityRendererRegistry.register(ModEntities.SNUFFY, SnuffyRenderer::new);

        // The open-payload names the bunny so the client menu factory can resolve it.
        ClientPlayNetworking.registerGlobalReceiver(OpenNijntjeMenuPayload.TYPE,
            (payload, context) -> context.client().execute(() -> ModMenus.pendingMountId = payload.entityId()));
        MenuScreens.register(ModMenus.NIJNTJE_MENU, NijntjeScreen::new);

        Nijntje.LOGGER.info("[Nijntje] Client renderers + screen registered.");
    }
}
