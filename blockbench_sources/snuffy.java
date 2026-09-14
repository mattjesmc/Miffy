// Made with Blockbench 5.1.4
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


public class snuffy<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("modid", "snuffy"), "main");
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart front_legs;
	private final ModelPart back_legs;

	public snuffy(ModelPart root) {
		this.body = root.getChild("body");
		this.head = this.body.getChild("head");
		this.front_legs = this.body.getChild("front_legs");
		this.back_legs = this.body.getChild("back_legs");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 13).addBox(-2.5F, -4.0F, -3.0F, 5.0F, 4.0F, 7.0F, new CubeDeformation(0.0F))
		.texOffs(26, 13).addBox(-0.5F, -7.0F, 3.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 21.0F, 0.0F));

		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -4.0F, -5.0F, 6.0F, 6.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(26, 0).addBox(-1.5F, -1.0F, -7.0F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(31, 13).addBox(-4.0F, -4.0F, -4.0F, 1.0F, 5.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(38, 13).addBox(3.0F, -4.0F, -4.0F, 1.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -4.0F, -3.0F));

		PartDefinition front_legs = body.addOrReplaceChild("front_legs", CubeListBuilder.create().texOffs(0, 25).addBox(-2.5F, 0.0F, -1.0F, 1.5F, 3.0F, 1.5F, new CubeDeformation(0.0F))
		.texOffs(8, 25).addBox(1.0F, 0.0F, -1.0F, 1.5F, 3.0F, 1.5F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, -2.0F));

		PartDefinition back_legs = body.addOrReplaceChild("back_legs", CubeListBuilder.create().texOffs(16, 25).addBox(-2.5F, 0.0F, -1.0F, 1.5F, 3.0F, 1.5F, new CubeDeformation(0.0F))
		.texOffs(24, 25).addBox(1.0F, 0.0F, -1.0F, 1.5F, 3.0F, 1.5F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 3.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		body.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}