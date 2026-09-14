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

/** Snuffy the puppy — quadruped with a simple bounding leg-walk. Exported from Blockbench. */
@Environment(EnvType.CLIENT)
public class SnuffyModel extends EntityModel<SnuffyRenderState> {
    private final ModelPart head;
    private final ModelPart frontLegs;
    private final ModelPart backLegs;

    public SnuffyModel(final ModelPart root) {
        super(root);
        ModelPart body = root.getChild("body");
        this.head = body.getChild("head");
        this.frontLegs = body.getChild("front_legs");
        this.backLegs = body.getChild("back_legs");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild(
            "body",
            CubeListBuilder.create()
                .texOffs(0, 13).addBox(-2.5F, -4.0F, -3.0F, 5.0F, 4.0F, 7.0F)
                .texOffs(26, 13).addBox(-0.5F, -7.0F, 3.5F, 1.0F, 3.0F, 1.0F),
            PartPose.offset(0.0F, 21.0F, 0.0F)
        );

        body.addOrReplaceChild(
            "head",
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.0F, -4.0F, -5.0F, 6.0F, 6.0F, 6.0F)
                .texOffs(26, 0).addBox(-1.5F, -1.0F, -7.0F, 3.0F, 3.0F, 2.0F)
                .texOffs(31, 13).addBox(-4.0F, -4.0F, -4.0F, 1.0F, 5.0F, 2.0F)
                .texOffs(38, 13).addBox(3.0F, -4.0F, -4.0F, 1.0F, 5.0F, 2.0F),
            PartPose.offset(0.0F, -4.0F, -3.0F)
        );

        body.addOrReplaceChild(
            "front_legs",
            CubeListBuilder.create()
                .texOffs(0, 25).addBox(-2.5F, 0.0F, -1.0F, 1.5F, 3.0F, 1.5F)
                .texOffs(8, 25).addBox(1.0F, 0.0F, -1.0F, 1.5F, 3.0F, 1.5F),
            PartPose.offset(0.0F, 0.0F, -2.0F)
        );

        body.addOrReplaceChild(
            "back_legs",
            CubeListBuilder.create()
                .texOffs(16, 25).addBox(-2.5F, 0.0F, -1.0F, 1.5F, 3.0F, 1.5F)
                .texOffs(24, 25).addBox(1.0F, 0.0F, -1.0F, 1.5F, 3.0F, 1.5F),
            PartPose.offset(0.0F, 0.0F, 3.0F)
        );

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(final SnuffyRenderState state) {
        super.setupAnim(state);
        this.head.yRot = state.yRot * (float) (Math.PI / 180.0);
        this.head.xRot = state.xRot * (float) (Math.PI / 180.0);

        float speed = Math.min(state.walkAnimationSpeed, 1.0F);
        float pos = state.walkAnimationPos * 0.8F;
        this.frontLegs.xRot = Mth.cos(pos) * 1.0F * speed;
        this.backLegs.xRot = Mth.cos(pos + (float) Math.PI) * 1.0F * speed;
    }
}
