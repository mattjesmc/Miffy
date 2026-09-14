package com.mattmc.nijntje.client;

import com.mattmc.nijntje.Nijntje;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * Re-renders the model with a dress-only mask texture, tinted by the Nijntje's dye colour —
 * the same trick vanilla uses for the dyeable wolf collar.
 */
@Environment(EnvType.CLIENT)
public class NijntjeDressLayer extends RenderLayer<NijntjeRenderState, NijntjeModel> {
    private static final Identifier DRESS_TEXTURE =
        Identifier.fromNamespaceAndPath(Nijntje.MOD_ID, "textures/entity/nijntje_dress.png");

    public NijntjeDressLayer(final RenderLayerParent<NijntjeRenderState, NijntjeModel> renderer) {
        super(renderer);
    }

    @Override
    public void submit(final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector,
                       final int lightCoords, final NijntjeRenderState state, final float yRot, final float xRot) {
        if (state.dressColor != null && !state.isInvisible) {
            int color = state.dressColor.getTextureDiffuseColor();
            submitNodeCollector.order(1)
                .submitModel(
                    this.getParentModel(),
                    state,
                    poseStack,
                    RenderTypes.entityCutout(DRESS_TEXTURE),
                    lightCoords,
                    OverlayTexture.NO_OVERLAY,
                    color,
                    null,
                    state.outlineColor,
                    null
                );
        }
    }
}
