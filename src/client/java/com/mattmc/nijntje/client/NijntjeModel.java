package com.mattmc.nijntje.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Nijntje (Miffy) model — authored in Blockbench, exported via the modded_entity codec.
 * Hierarchy: root -> body (body/tail) -> {head -> saddle, legs}. The saddle shows when
 * saddled; the legs swing forward and the body settles back for the sitting pose.
 */
@Environment(EnvType.CLIENT)
public class NijntjeModel extends EntityModel<NijntjeRenderState> {
    private static final float BODY_Y = 15.0F;

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart saddle;
    private final ModelPart legs;
    private final ModelPart barding;

    public NijntjeModel(final ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.saddle = this.head.getChild("saddle");
        this.legs = this.body.getChild("legs");
        this.barding = this.body.getChild("barding");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild(
            "body",
            CubeListBuilder.create()
                .texOffs(34, 0).addBox(-2.5F, 2.0F, -2.0F, 5.0F, 5.0F, 4.0F)
                .texOffs(24, 18).addBox(-1.5F, 4.0F, 2.0F, 3.0F, 2.0F, 1.0F),
            PartPose.offset(0.0F, BODY_Y, 0.0F)
        );

        PartDefinition head = body.addOrReplaceChild(
            "head",
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.5F, -7.0F, -4.5F, 9.0F, 9.0F, 8.0F)
                .texOffs(34, 18).addBox(0.5F, -15.0F, -0.5F, 2.0F, 8.0F, 2.0F)
                .texOffs(42, 18).addBox(-2.5F, -15.0F, -0.5F, 2.0F, 8.0F, 2.0F),
            PartPose.offset(0.0F, 0.0F, -0.5F)
        );

        head.addOrReplaceChild(
            "saddle",
            CubeListBuilder.create()
                .texOffs(0, 30).addBox(-1.5F, -2.0F, -2.0F, 3.0F, 2.0F, 4.0F)
                .texOffs(16, 30).addBox(-0.5F, -4.0F, -2.0F, 1.0F, 2.0F, 1.0F),
            PartPose.offset(0.0F, -7.0F, 0.0F)
        );

        body.addOrReplaceChild(
            "legs",
            CubeListBuilder.create()
                .texOffs(12, 18).addBox(-2.5F, 0.0F, -4.0F, 2.0F, 2.0F, 4.0F)
                .texOffs(0, 18).addBox(0.5F, 0.0F, -4.0F, 2.0F, 2.0F, 4.0F),
            PartPose.offset(0.0F, 7.0F, 0.5F)
        );

        body.addOrReplaceChild(
            "barding",
            CubeListBuilder.create()
                .texOffs(22, 36).addBox(-3.0F, -2.0F, -2.5F, 6.0F, 5.0F, 5.0F),
            PartPose.offset(0.0F, 4.0F, 0.0F)
        );

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(final NijntjeRenderState state) {
        super.setupAnim(state);
        this.head.yRot = state.yRot * (float) (Math.PI / 180.0);
        this.head.xRot = state.xRot * (float) (Math.PI / 180.0);
        this.saddle.visible = state.saddled;
        this.barding.visible = state.showBarding;

        // Sitting: settle back onto the rear with the legs out in front. Set absolutely
        // each frame (not +=) so the pose can't accumulate.
        if (state.sitting) {
            this.body.xRot = -0.35F;
            this.body.y = BODY_Y + 3.0F;
            this.legs.xRot = -0.6F;
        } else {
            this.body.xRot = 0.0F;
            // Hop gait: the legs kick out and the whole body lifts on each bound, scaled
            // by how fast it's actually moving.
            float speed = Math.min(state.walkAnimationSpeed, 1.0F);
            float phase = state.walkAnimationPos * 0.45F;
            float lift = Math.max(0.0F, Mth.sin(phase - 0.4F));
            this.legs.xRot = Mth.sin(phase) * 1.1F * speed;
            this.body.y = BODY_Y - lift * 2.5F * speed;
        }
    }
}
