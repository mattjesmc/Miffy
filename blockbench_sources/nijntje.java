// Made with Blockbench 5.1.4
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


public class nijntje<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("modid", "nijntje"), "main");
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart saddle;
	private final ModelPart legs;
	private final ModelPart barding;

	public nijntje(ModelPart root) {
		this.body = root.getChild("body");
		this.head = this.body.getChild("head");
		this.saddle = this.head.getChild("saddle");
		this.legs = this.body.getChild("legs");
		this.barding = this.body.getChild("barding");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(34, 0).addBox(-2.5F, 2.0F, -2.0F, 5.0F, 5.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(24, 18).addBox(-1.5F, 4.0F, 2.0F, 3.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 15.0F, 0.0F));

		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, -7.0F, -4.5F, 9.0F, 9.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(34, 18).addBox(0.5F, -15.0F, -0.5F, 2.0F, 8.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(42, 18).addBox(-2.5F, -15.0F, -0.5F, 2.0F, 8.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, -0.5F));

		PartDefinition saddle = head.addOrReplaceChild("saddle", CubeListBuilder.create().texOffs(0, 30).addBox(-1.5F, -2.0F, -2.0F, 3.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(16, 30).addBox(-0.5F, -4.0F, -2.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -7.0F, 0.0F));

		PartDefinition legs = body.addOrReplaceChild("legs", CubeListBuilder.create().texOffs(12, 18).addBox(-2.5F, 0.0F, -4.0F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(0, 18).addBox(0.5F, 0.0F, -4.0F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 7.0F, 0.5F));

		PartDefinition barding = body.addOrReplaceChild("barding", CubeListBuilder.create().texOffs(22, 36).addBox(-3.0F, -2.0F, -2.5F, 6.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 4.0F, 0.0F));

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