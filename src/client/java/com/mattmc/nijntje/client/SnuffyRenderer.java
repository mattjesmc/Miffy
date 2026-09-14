package com.mattmc.nijntje.client;

import com.mattmc.nijntje.Nijntje;
import com.mattmc.nijntje.entity.SnuffyEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public class SnuffyRenderer extends MobRenderer<SnuffyEntity, SnuffyRenderState, SnuffyModel> {
    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(Nijntje.MOD_ID, "textures/entity/snuffy.png");

    public SnuffyRenderer(final EntityRendererProvider.Context context) {
        super(context, new SnuffyModel(context.bakeLayer(NijntjeClient.SNUFFY_LAYER)), 0.25F);
    }

    @Override
    public Identifier getTextureLocation(final SnuffyRenderState state) {
        return TEXTURE;
    }

    @Override
    public SnuffyRenderState createRenderState() {
        return new SnuffyRenderState();
    }
}
