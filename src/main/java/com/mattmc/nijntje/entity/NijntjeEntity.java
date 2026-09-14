package com.mattmc.nijntje.entity;

import com.mattmc.nijntje.Nijntje;
import com.mattmc.nijntje.registry.ModEntities;
import com.mattmc.nijntje.registry.ModSounds;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import com.mattmc.nijntje.menu.NijntjeMenu;
import com.mattmc.nijntje.menu.OpenNijntjeMenuPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ItemBasedSteering;
import net.minecraft.world.entity.ItemSteerable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class NijntjeEntity extends TamableAnimal implements ItemSteerable, PlayerRideableJumping {
    private static final EntityDataAccessor<Integer> DATA_GROWTH_STAGE =
        SynchedEntityData.defineId(NijntjeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_BOOST_TIME =
        SynchedEntityData.defineId(NijntjeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_DRESS_COLOR =
        SynchedEntityData.defineId(NijntjeEntity.class, EntityDataSerializers.INT);
    private static final DyeColor DEFAULT_DRESS_COLOR = DyeColor.ORANGE;
    // Synced for the inventory GUI's progress + hunger bars (client-readable).
    private static final EntityDataAccessor<Integer> DATA_CARE_PROGRESS =
        SynchedEntityData.defineId(NijntjeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_FULLNESS =
        SynchedEntityData.defineId(NijntjeEntity.class, EntityDataSerializers.INT);

    private final ItemBasedSteering steering = new ItemBasedSteering(this.entityData, DATA_BOOST_TIME);
    /** The Nijntje's own "saddlebag" storage (opened as a vanilla 3x3 container). */
    private final SimpleContainer inventory = new SimpleContainer(9);

    // --- Care/growth tuning ---
    // Shipping values: with good care (a carrot roughly once a minute) a Nijntje reaches its
    // fully Grown stage in ~30 minutes. A counted meal is worth CARE_CARROT (20), one meal
    // counts per FEED_COOLDOWN_TICKS, and the three stage thresholds sum to ~600 (~30 carrots).
    private static final int DAY_TICKS = 24000;
    private static final int FEED_COOLDOWN_TICKS = 1200;      // 60s between meals that count
    private static final int CARE_CARROT = 20;
    private static final int CARE_GOLDEN_CARROT = 40;
    private static final int CARE_DANDELION = 10;
    private static final int PASSIVE_CARE_PER_DAY = 5;        // reward for simply keeping it safe & healthy
    private static final int NEGLECT_GRACE_TICKS = 3 * DAY_TICKS;
    private static final int NEGLECT_PER_DAY = 12;
    private static final int REGRESS_FLOOR = -100;            // care this low regresses a stage

    // Care points required to advance OUT of a given stage (Baby, Young, Adult; Grown is terminal).
    // Sum ~600 => ~30 counted carrots => ~30 min of good care, weighted toward the later stages.
    private static final int[] CARE_TO_ADVANCE = {120, 200, 280, Integer.MAX_VALUE};

    // Per-stage attribute bonuses, indexed by GrowthStage.ordinal().
    private static final double[] HEALTH_BONUS = {0.0, 4.0, 10.0, 20.0};
    private static final double[] ATTACK_BONUS = {0.0, 1.0, 3.0, 6.0};
    private static final double[] SPEED_BONUS = {-0.03, 0.0, 0.02, 0.05};
    private static final double[] ARMOR_BONUS = {0.0, 0.0, 1.0, 3.0};

    private static final Identifier SCALE_MOD_ID = Identifier.fromNamespaceAndPath(Nijntje.MOD_ID, "growth_scale");
    private static final Identifier HEALTH_MOD_ID = Identifier.fromNamespaceAndPath(Nijntje.MOD_ID, "growth_health");
    private static final Identifier ATTACK_MOD_ID = Identifier.fromNamespaceAndPath(Nijntje.MOD_ID, "growth_attack");
    private static final Identifier SPEED_MOD_ID = Identifier.fromNamespaceAndPath(Nijntje.MOD_ID, "growth_speed");
    private static final Identifier ARMOR_MOD_ID = Identifier.fromNamespaceAndPath(Nijntje.MOD_ID, "growth_armor");

    private int careProgress = 0;
    private long lastFedGameTime = 0L;
    private long lastProcessedDay = Long.MIN_VALUE;
    private boolean hasSnuffy = false;

    /**
     * Visual + gameplay progression, independent of vanilla baby aging.
     * Care raises this; each stage scales the model, hitbox, and attributes.
     */
    public enum GrowthStage {
        BABY(0.55F),
        YOUNG(0.85F),
        ADULT(1.2F),
        GROWN(1.6F);

        private final float scale;

        GrowthStage(final float scale) {
            this.scale = scale;
        }

        public float scale() {
            return this.scale;
        }

        public boolean isAtLeast(final GrowthStage other) {
            return this.ordinal() >= other.ordinal();
        }

        public static GrowthStage byId(final int id) {
            GrowthStage[] values = values();
            return values[Mth.clamp(id, 0, values.length - 1)];
        }
    }

    public NijntjeEntity(final EntityType<? extends NijntjeEntity> type, final Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
            .add(Attributes.MAX_HEALTH, 10.0)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.ATTACK_DAMAGE, 2.0)
            .add(Attributes.JUMP_STRENGTH, 0.5);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        // Everyone flees fire/lava/etc.; only the young flee ordinary combat (see below).
        this.goalSelector.addGoal(1, new TamableAnimalPanicGoal(1.5, DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES));
        this.goalSelector.addGoal(1, new TamableAnimalPanicGoal(2.0) {
            @Override
            public boolean canUse() {
                return !getGrowthStage().isAtLeast(GrowthStage.ADULT) && super.canUse();
            }
        });
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        // Adult+ Nijntje fight alongside their owner.
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.3, true) {
            @Override
            public boolean canUse() {
                return getGrowthStage().isAtLeast(GrowthStage.ADULT) && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return getGrowthStage().isAtLeast(GrowthStage.ADULT) && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.1, 10.0F, 2.0F));
        this.goalSelector.addGoal(5, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(6, new TemptGoal(this, 1.1, stack -> isNijntjeFood(stack), false));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
    }

    /** Only grown-up Nijntje (Adult and above) will take up a fight for their owner. */
    @Override
    public boolean wantsToAttack(final LivingEntity target, final LivingEntity owner) {
        return getGrowthStage().isAtLeast(GrowthStage.ADULT);
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_GROWTH_STAGE, GrowthStage.ADULT.ordinal());
        builder.define(DATA_BOOST_TIME, 0);
        builder.define(DATA_DRESS_COLOR, DEFAULT_DRESS_COLOR.getId());
        builder.define(DATA_CARE_PROGRESS, 0);
        builder.define(DATA_FULLNESS, 100);
    }

    // --- Client-readable stats for the inventory GUI ---

    /** Care points accumulated toward the next growth stage (synced). */
    public int getSyncedCareProgress() {
        return this.entityData.get(DATA_CARE_PROGRESS);
    }

    /** Care points needed to leave the current stage, or -1 when fully grown. */
    public int getCareNeeded() {
        int need = CARE_TO_ADVANCE[getGrowthStage().ordinal()];
        return need == Integer.MAX_VALUE ? -1 : need;
    }

    /** 0-100 "fullness"; drops toward 0 as neglect approaches, refills on feeding. */
    public int getFullness() {
        return this.entityData.get(DATA_FULLNESS);
    }

    public DyeColor getDressColor() {
        return DyeColor.byId(this.entityData.get(DATA_DRESS_COLOR));
    }

    public void setDressColor(final DyeColor color) {
        this.entityData.set(DATA_DRESS_COLOR, color.getId());
    }

    // --- Growth stage ---

    public GrowthStage getGrowthStage() {
        return GrowthStage.byId(this.entityData.get(DATA_GROWTH_STAGE));
    }

    public void setGrowthStage(final GrowthStage stage) {
        this.entityData.set(DATA_GROWTH_STAGE, stage.ordinal());
        // Apply attributes (incl. SCALE) first so the refreshed hitbox uses the new scale.
        this.applyStageAttributes();
        this.refreshDimensions();
    }


    private void applyStageAttributes() {
        int i = getGrowthStage().ordinal();
        // SCALE drives both the visual size and the hitbox (getScale() is final and reads it).
        setAttributeBonus(Attributes.SCALE, SCALE_MOD_ID, getGrowthStage().scale() - 1.0);
        setAttributeBonus(Attributes.MAX_HEALTH, HEALTH_MOD_ID, HEALTH_BONUS[i]);
        setAttributeBonus(Attributes.ATTACK_DAMAGE, ATTACK_MOD_ID, ATTACK_BONUS[i]);
        setAttributeBonus(Attributes.MOVEMENT_SPEED, SPEED_MOD_ID, SPEED_BONUS[i]);
        setAttributeBonus(Attributes.ARMOR, ARMOR_MOD_ID, ARMOR_BONUS[i]);
        if (this.getHealth() > this.getMaxHealth()) {
            this.setHealth(this.getMaxHealth());
        }
    }

    private void setAttributeBonus(final net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                                   final Identifier id, final double value) {
        AttributeInstance inst = this.getAttribute(attribute);
        if (inst == null) {
            return;
        }
        inst.removeModifier(id);
        if (value != 0.0) {
            inst.addOrUpdateTransientModifier(new AttributeModifier(id, value, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    // --- Care logic ---

    /** Adds (or, when negative, removes) care points and applies any resulting stage change. */
    public void addCare(final int amount) {
        this.careProgress += amount;
        GrowthStage stage = getGrowthStage();
        if (amount > 0) {
            int need = CARE_TO_ADVANCE[stage.ordinal()];
            if (need != Integer.MAX_VALUE && this.careProgress >= need && stage.ordinal() < GrowthStage.GROWN.ordinal()) {
                this.careProgress -= need;
                GrowthStage next = GrowthStage.byId(stage.ordinal() + 1);
                setGrowthStage(next);
                this.setHealth(this.getMaxHealth());
                celebrateGrowth();
                if (next == GrowthStage.GROWN) {
                    trySpawnSnuffy();
                }
            }
        } else if (this.careProgress <= REGRESS_FLOOR) {
            if (stage.ordinal() > GrowthStage.BABY.ordinal()) {
                GrowthStage prev = GrowthStage.byId(stage.ordinal() - 1);
                setGrowthStage(prev);
                this.careProgress = CARE_TO_ADVANCE[prev.ordinal()] / 2;
                this.level().broadcastEntityEvent(this, (byte) 6); // smoke (unhappy)
            } else {
                this.careProgress = REGRESS_FLOOR;
            }
        }
        this.entityData.set(DATA_CARE_PROGRESS, this.careProgress);
    }

    private void celebrateGrowth() {
        this.level().broadcastEntityEvent(this, (byte) 7); // hearts
        this.playSound(SoundEvents.PLAYER_LEVELUP, 0.6F, 1.4F);
    }

    /** When a well-cared-for Nijntje grows up, it gains a puppy companion, Snuffy. */
    private void trySpawnSnuffy() {
        if (this.hasSnuffy || !this.isTame() || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        SnuffyEntity snuffy = ModEntities.SNUFFY.create(serverLevel, EntitySpawnReason.MOB_SUMMONED);
        if (snuffy != null) {
            snuffy.snapTo(this.getX() + 0.6, this.getY(), this.getZ() + 0.6, this.getYRot(), 0.0F);
            snuffy.setOwner(this);
            snuffy.setTame(true, false);
            snuffy.setPersistenceRequired();
            serverLevel.addFreshEntity(snuffy);
            this.hasSnuffy = true;
            this.level().broadcastEntityEvent(this, (byte) 7);
        }
    }

    @Override
    public void customServerAiStep(final ServerLevel level) {
        super.customServerAiStep(level);
        if (!this.isTame() || this.tickCount % 40 != 0) {
            return;
        }
        long now = level.getGameTime();
        if (this.lastFedGameTime == 0L) {
            this.lastFedGameTime = now; // freshly tamed/loaded — treat as recently fed
        }
        // Keep the GUI "fullness" bar in sync (100 = just fed, 0 = neglect threshold).
        long sinceFed = now - this.lastFedGameTime;
        int fullness = (int) Mth.clamp(100L - sinceFed * 100L / NEGLECT_GRACE_TICKS, 0L, 100L);
        this.entityData.set(DATA_FULLNESS, fullness);
        long day = now / DAY_TICKS;
        if (this.lastProcessedDay == Long.MIN_VALUE) {
            this.lastProcessedDay = day;
            return;
        }
        if (day > this.lastProcessedDay) {
            this.lastProcessedDay = day;
            if (this.getHealth() >= this.getMaxHealth() * 0.75F) {
                addCare(PASSIVE_CARE_PER_DAY);
            }
            if (now - this.lastFedGameTime > NEGLECT_GRACE_TICKS) {
                addCare(-NEGLECT_PER_DAY);
            }
        }
    }

    // --- Riding (Grown only, saddle-equipped) ---

    private boolean canBeRidden() {
        return getGrowthStage() == GrowthStage.GROWN;
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        if (this.isSaddled() && canBeRidden()
            && this.getFirstPassenger() instanceof Player player
            && (this.isOwnedBy(player) || !this.isTame())) {
            return player;
        }
        return super.getControllingPassenger();
    }

    @Override
    protected Vec3 getRiddenInput(final Player controller, final Vec3 selfInput) {
        float forward = controller.zza < 0.0F ? controller.zza * 0.35F : controller.zza;
        float strafe = controller.xxa * 0.5F;
        return new Vec3(strafe, 0.0, forward);
    }

    @Override
    protected float getRiddenSpeed(final Player controller) {
        return (float) (this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.6 * this.steering.boostFactor());
    }

    @Override
    protected void tickRidden(final Player controller, final Vec3 riddenInput) {
        super.tickRidden(controller, riddenInput);
        this.setRot(controller.getYRot(), controller.getXRot() * 0.5F);
        this.yRotO = this.yBodyRot = this.yHeadRot = this.getYRot();
        this.steering.tickBoost();
    }

    @Override
    public boolean boost() {
        return this.steering.boost(this.getRandom());
    }

    // Jump while riding (press space): a charged hop, like a horse but bunny-flavoured.
    @Override
    public boolean canJump() {
        return canBeRidden() && this.isSaddled();
    }

    @Override
    public void onPlayerJump(final int jumpAmount) {
        // Charge feedback handled client-side; velocity is applied on handleStartJump.
    }

    @Override
    public void handleStartJump(final int jumpScale) {
        if (this.onGround()) {
            float scale = this.getPlayerJumpPendingScale(jumpScale);
            Vec3 d = this.getDeltaMovement();
            this.setDeltaMovement(d.x, 0.42 + 0.30 * scale, d.z);
            this.playSound(SoundEvents.RABBIT_JUMP, 1.0F,
                (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
        }
    }

    @Override
    public void handleStopJump() {
    }

    @Override
    public void onSyncedDataUpdated(final EntityDataAccessor<?> accessor) {
        if (DATA_BOOST_TIME.equals(accessor) && this.level().isClientSide()) {
            this.steering.onSynced();
        }
        super.onSyncedDataUpdated(accessor);
    }

    @Override
    public boolean canUseSlot(final EquipmentSlot slot) {
        if (slot == EquipmentSlot.SADDLE) {
            return this.isAlive() && canBeRidden();
        }
        if (slot == EquipmentSlot.BODY) {
            return this.isAlive();
        }
        return super.canUseSlot(slot);
    }

    @Override
    protected boolean canDispenserEquipIntoSlot(final EquipmentSlot slot) {
        return slot == EquipmentSlot.SADDLE || slot == EquipmentSlot.BODY || super.canDispenserEquipIntoSlot(slot);
    }

    // --- Saddlebag inventory ---

    public SimpleContainer getInventory() {
        return this.inventory;
    }

    @Override
    public void die(final DamageSource source) {
        if (!this.level().isClientSide()) {
            Containers.dropContents(this.level(), this, this.inventory);
        }
        super.die(source);
    }

    // --- Food & interaction ---

    private static boolean isNijntjeFood(final ItemStack stack) {
        return stack.is(Items.CARROT) || stack.is(Items.GOLDEN_CARROT) || stack.is(Items.DANDELION);
    }

    private static int careForFood(final ItemStack stack) {
        if (stack.is(Items.GOLDEN_CARROT)) {
            return CARE_GOLDEN_CARROT;
        }
        if (stack.is(Items.DANDELION)) {
            return CARE_DANDELION;
        }
        return CARE_CARROT;
    }

    @Override
    public boolean isFood(final ItemStack stack) {
        return isNijntjeFood(stack);
    }

    @Override
    public InteractionResult mobInteract(final Player player, final InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Level level = this.level();
        boolean food = this.isFood(stack);

        if (this.isTame()) {
            if (food) {
                int points = careForFood(stack);
                boolean hungry = this.getHealth() < this.getMaxHealth();
                if (hungry) {
                    this.feed(player, hand, stack, 2.0F, 4.0F);
                } else {
                    this.usePlayerItem(player, hand, stack);
                    this.playEatingSound();
                }
                if (!level.isClientSide()) {
                    long now = level.getGameTime();
                    if (now - this.lastFedGameTime >= FEED_COOLDOWN_TICKS) {
                        this.lastFedGameTime = now;
                        addCare(points);
                        this.level().broadcastEntityEvent(this, (byte) 7);
                    }
                }
                return InteractionResult.SUCCESS;
            }
            // Recolour the dress with any dye held by the owner.
            if (this.isOwnedBy(player)) {
                DyeColor dye = stack.get(DataComponents.DYE);
                if (dye != null && dye != this.getDressColor()) {
                    if (!level.isClientSide()) {
                        this.setDressColor(dye);
                        if (!player.getAbilities().instabuild) {
                            stack.shrink(1);
                        }
                    }
                    return InteractionResult.SUCCESS;
                }
            }
            // Equip a saddle or bunny barding held by the owner.
            if (this.isOwnedBy(player)
                && (this.isEquippableInSlot(stack, EquipmentSlot.SADDLE) || this.isEquippableInSlot(stack, EquipmentSlot.BODY))) {
                return stack.interactLivingEntity(player, this, hand);
            }
            if (this.isOwnedBy(player) && stack.isEmpty()) {
                if (player.isSecondaryUseActive()) {
                    // Sneak toggles the sitting pose (works whether saddled or not).
                    if (!level.isClientSide()) {
                        this.setOrderedToSit(!this.isOrderedToSit());
                        this.setInSittingPose(this.isOrderedToSit());
                        this.navigation.stop();
                        this.setTarget(null);
                    }
                    return InteractionResult.SUCCESS;
                }
                // Plain right-click: ride a standing, saddled, grown Nijntje; otherwise
                // (young/unsaddled, or currently sitting) open its saddlebag inventory.
                if (canBeRidden() && this.isSaddled() && !this.isVehicle() && !this.isInSittingPose()) {
                    if (!level.isClientSide()) {
                        player.startRiding(this);
                    }
                    return InteractionResult.SUCCESS;
                }
                if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                    ServerPlayNetworking.send(serverPlayer, new OpenNijntjeMenuPayload(this.getId()));
                    serverPlayer.openMenu(new SimpleMenuProvider(
                        (id, inv, p) -> new NijntjeMenu(id, inv, this), this.getDisplayName()));
                }
                return InteractionResult.SUCCESS;
            }
        } else if (food) {
            this.usePlayerItem(player, hand, stack);
            if (!level.isClientSide()) {
                // Carrot taming: roughly 1-in-3, like a wolf with a bone.
                if (this.random.nextInt(3) == 0) {
                    this.tame(player);
                    this.setOrderedToSit(true);
                    this.setInSittingPose(true);
                    this.navigation.stop();
                    this.setTarget(null);
                    this.level().broadcastEntityEvent(this, (byte) 7);
                } else {
                    this.level().broadcastEntityEvent(this, (byte) 6);
                }
            }
            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }

    @Override
    public void tame(final Player player) {
        super.tame(player);
        this.lastFedGameTime = this.level().getGameTime();
    }

    @Override
    public @Nullable NijntjeEntity getBreedOffspring(final ServerLevel level, final AgeableMob partner) {
        NijntjeEntity baby = ModEntities.NIJNTJE.create(level, EntitySpawnReason.BREEDING);
        if (baby != null) {
            baby.setGrowthStage(GrowthStage.BABY);
            if (this.getOwnerReference() != null) {
                baby.setOwnerReference(this.getOwnerReference());
                baby.setTame(true, false);
            }
        }
        return baby;
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(final ServerLevelAccessor level, final DifficultyInstance difficulty,
                                                  final EntitySpawnReason reason, final @Nullable SpawnGroupData groupData) {
        applyStageAttributes();
        this.setHealth(this.getMaxHealth());
        return super.finalizeSpawn(level, difficulty, reason, groupData);
    }

    // --- Persistence ---

    @Override
    protected void addAdditionalSaveData(final ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("GrowthStage", this.entityData.get(DATA_GROWTH_STAGE));
        output.putInt("CareProgress", this.careProgress);
        output.putLong("LastFedGameTime", this.lastFedGameTime);
        output.putLong("LastProcessedDay", this.lastProcessedDay);
        output.store("DressColor", DyeColor.LEGACY_ID_CODEC, this.getDressColor());
        output.putBoolean("HasSnuffy", this.hasSnuffy);
        NonNullList<ItemStack> items = NonNullList.withSize(this.inventory.getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < this.inventory.getContainerSize(); i++) {
            items.set(i, this.inventory.getItem(i));
        }
        ContainerHelper.saveAllItems(output, items);
    }

    @Override
    protected void readAdditionalSaveData(final ValueInput input) {
        super.readAdditionalSaveData(input);
        this.careProgress = input.getIntOr("CareProgress", 0);
        this.entityData.set(DATA_CARE_PROGRESS, this.careProgress);
        this.lastFedGameTime = input.getLongOr("LastFedGameTime", 0L);
        this.lastProcessedDay = input.getLongOr("LastProcessedDay", Long.MIN_VALUE);
        this.setDressColor(input.read("DressColor", DyeColor.LEGACY_ID_CODEC).orElse(DEFAULT_DRESS_COLOR));
        this.hasSnuffy = input.getBooleanOr("HasSnuffy", false);
        setGrowthStage(GrowthStage.byId(input.getIntOr("GrowthStage", GrowthStage.ADULT.ordinal())));
        NonNullList<ItemStack> items = NonNullList.withSize(this.inventory.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        for (int i = 0; i < items.size(); i++) {
            this.inventory.setItem(i, items.get(i));
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.NIJNTJE_SAY;
    }

    @Override
    public int getAmbientSoundInterval() {
        // The clip is ~6s, so keep it occasional rather than the chatty default.
        return 600;
    }

    @Override
    protected SoundEvent getHurtSound(final DamageSource source) {
        return SoundEvents.RABBIT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.RABBIT_DEATH;
    }
}
