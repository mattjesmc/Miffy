package com.mattmc.nijntje.client;

import com.mattmc.nijntje.Nijntje;
import com.mattmc.nijntje.entity.NijntjeEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;

@Environment(EnvType.CLIENT)
public class NijntjeRenderer extends MobRenderer<NijntjeEntity, NijntjeRenderState, NijntjeModel> {
    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(Nijntje.MOD_ID, "textures/entity/nijntje.png");

    public NijntjeRenderer(final EntityRendererProvider.Context context) {
        super(context, new NijntjeModel(context.bakeLayer(NijntjeClient.NIJNTJE_LAYER)), 0.3F);
        this.addLayer(new NijntjeDressLayer(this));
    }

    @Override
    public Identifier getTextureLocation(final NijntjeRenderState state) {
        return TEXTURE;
    }

    @Override
    public NijntjeRenderState createRenderState() {
        return new NijntjeRenderState();
    }

    // Growth scale is applied through NijntjeEntity.getScale(), which the base renderer
    // reads into state.scale — so no per-frame scaling work is needed for size.
    @Override
    public void extractRenderState(final NijntjeEntity entity, final NijntjeRenderState state, final float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.saddled = entity.isSaddled();
        state.sitting = entity.isInSittingPose();
        state.showBarding = !entity.getItemBySlot(EquipmentSlot.BODY).isEmpty();
        state.dressColor = entity.getDressColor();
    }
}
