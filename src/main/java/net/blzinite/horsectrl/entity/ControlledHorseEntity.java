package net.blzinite.horsectrl.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ControlledHorseEntity extends Horse {
    // Static Reference
    private static final Style SPUR_FONT = Style.EMPTY.withFont(ResourceLocation.fromNamespaceAndPath("horse_ctrl", "spurs"));

    private static final float WALK_SPEED = 1.0F;
    private static final float TROT_SPEED = 2.0F;
    private static final float GALLOP_SPEED = 3.0F;
    private static final float SPRINT_SPEED = 4.0F;
    private static final float SPUR_RECHARGE_RATE = 0.004F;
    private static final float SPRINT_DECAY_RATE = 0.006F;
    private static final float SPRINT_THRESHOLD = 3.0F;
    private static final float REAR_SPEED_THRESHOLD = 2.0F;

    // Entity Data
    private static final EntityDataAccessor<Float> HEAD_ROTATION = SynchedEntityData.defineId(ControlledHorseEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> BODY_ROTATION = SynchedEntityData.defineId(ControlledHorseEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SPEED = SynchedEntityData.defineId(ControlledHorseEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> MAX_SPURS = SynchedEntityData.defineId(ControlledHorseEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SPURS = SynchedEntityData.defineId(ControlledHorseEntity.class, EntityDataSerializers.FLOAT);

    // Functional
    private boolean spurIsReady;

    public ControlledHorseEntity(EntityType<? extends Horse> entityType, Level level) {
        super(entityType, level);
        spurIsReady = false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HEAD_ROTATION, this.getYRot());
        builder.define(BODY_ROTATION, this.getYRot());
        builder.define(SPEED, 0f);
        builder.define(MAX_SPURS, 2);
        builder.define(SPURS, 2f);
    }

    // Getters & Setters
    public float getHeadRotation() { return this.entityData.get(HEAD_ROTATION); }
    public void setHeadRotation(float headRotation) { this.entityData.set(HEAD_ROTATION, headRotation); }
    public float getBodyRotation() { return this.entityData.get(BODY_ROTATION); }
    public void setBodyRotation(float bodyRotation) { this.entityData.set(BODY_ROTATION, bodyRotation); }
    public float getCruiseSpeed() { return this.entityData.get(SPEED); }
    public void setCruiseSpeed(float cruiseSpeed) { this.entityData.set(SPEED, cruiseSpeed); }
    public int getMaxSpurs() { return this.entityData.get(MAX_SPURS); }
    public void setMaxSpurs(int value) { this.entityData.set(MAX_SPURS, value); }
    public float getSpurs() { return this.entityData.get(SPURS); }
    public void setSpurs(float value) { this.entityData.set(SPURS, value); }


    private boolean canSpur() {
        return spurIsReady && getSpurs() > 1;
    }

    private void consumeSpur() {
        setSpurs(Math.max(0f, getSpurs() - 1));
        spurIsReady = false;
    }

    private void trySpur(float input) {
        if (input <= 0.2f || !canSpur()) {
            return;
        }
        setCruiseSpeed(SPRINT_SPEED);
        consumeSpur();
        playSpurSound();
    }

    private void rechargeSpur() {
        float spurs = getSpurs();
        if (spurs < getMaxSpurs()) {
            setSpurs(Math.min(
                    getMaxSpurs(),
                    spurs + SPUR_RECHARGE_RATE
            ));
        }
    }

    private void accelerate(float input) {
        float speed = getCruiseSpeed();

        if (input == 0f) {
            setCruiseSpeed(Mth.ceil(speed));
            return;
        }

        if (input > 0f) {
            input *= 0.25f; // slower acceleration, faster deceleration
        }

        setCruiseSpeed(Mth.clamp(speed + input, 0f, SPRINT_SPEED));
    }

    private void decelerate(float input) {
        float speed = getCruiseSpeed();
        if (speed > REAR_SPEED_THRESHOLD) {
            spurIsReady = false;
            makeMad();
        }

        setCruiseSpeed(Mth.clamp(speed + input, 0f, SPRINT_SPEED));
    }

    private void updateCruiseSpeed(float input) {
        float speed = getCruiseSpeed();

        if (input < 0f) {
            decelerate(input);
            return;
        }

        if (speed >= GALLOP_SPEED) {
            trySpur(input);
            return;
        }

        accelerate(input);
    }

    private void updateHeadRotation(Player rider){
        float target = getHeadRotation() - 4 * rider.xxa;
        if (Mth.degreesDifferenceAbs(target, getBodyRotation()) < 30f) {
            setHeadRotation(target);
        }
    }

    @Override
    protected Vec3 getRiddenInput(Player rider, Vec3 travelVector) {
        if (this.isStanding() && !this.allowStandSliding) {
            return Vec3.ZERO;
        }
        float input = rider.zza;
        updateCruiseSpeed(input);
        updateHeadRotation(rider);

        return new Vec3(0, 0, getCruiseSpeed());
    }

    private void updateBodyRotation() {
        float speed = getCruiseSpeed();
        float rate = Mth.square(5 - speed)/4;
        setBodyRotation(Mth.approachDegrees(getBodyRotation(), getHeadRotation(), rate));
    }

    private void updateSprintState() {
        float speed = getCruiseSpeed();
        if (speed <= GALLOP_SPEED) {
            return;
        }
        setCruiseSpeed(speed - SPRINT_DECAY_RATE);
        if (level().isClientSide()) {
            spawnSprintParticles();
        }
    }

    private void updateRidingRotation() {
        float headHeight = getCruiseSpeed() > GALLOP_SPEED ? 15f : 0f;

        setRot(getBodyRotation(), headHeight);
        this.yHeadRot = getHeadRotation();
        this.yRotO = this.yBodyRot = this.getYRot();
    }

    private void handleLocalRiderState(Vec3 travelVector) {
        if (travelVector.z <= 0.0) {
            this.gallopSoundCounter = 0;
        }
        if (this.onGround()) {
            this.setIsJumping(false);
            if (this.playerJumpPendingScale > 0.0F && !this.isJumping()) {
                this.executeRidersJump(this.playerJumpPendingScale, travelVector);
            }
            this.playerJumpPendingScale = 0.0F;
        }
    }

    @Override
    protected void tickRidden(Player player, Vec3 travelVector) {
        updateBodyRotation();
        updateSprintState();
        rechargeSpur();
        updateRidingRotation();

        if (this.isControlledByLocalInstance()) {
            handleLocalRiderState(travelVector);
        }
    }

    private void spawnSprintParticles() {
        if (!level().isClientSide() || random.nextFloat() >= 0.3F) {
            return;
        }

        double x = getX() + (random.nextDouble() - 0.5D) * getBbWidth();
        double y = getY() + 0.1D;
        double z = getZ() + (random.nextDouble() - 0.5D) * getBbWidth();

        level().addParticle(
                ParticleTypes.CAMPFIRE_COSY_SMOKE,
                x, y, z,
                0.0D,
                0.05D,
                0.0D
        );
    }

    private void playSpurSound() {
        this.playSound(SoundEvents.PLAYER_SMALL_FALL, 1f, 0.2f);
        this.playSound(SoundEvents.HORSE_AMBIENT, 1f, 1f);
    }

    private void kickNearbyEntities() {
        AABB area = getBoundingBox().inflate(2.0D);

        List<LivingEntity> kickedEntities = level().getEntitiesOfClass(
                LivingEntity.class,
                area,
                entity ->
                        entity != this &&
                                !this.getPassengers().contains(entity)
        );
        for (LivingEntity livingEntity : kickedEntities) {
            livingEntity.knockback(2.4f, Mth.sin((float) (this.getYRot()*(Math.PI/180))), -Mth.cos((float) (this.getYRot()*(Math.PI/180))));
            float dmg = (float) this.getAttributeValue(Attributes.MAX_HEALTH)
                    * (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 2.0f;
            livingEntity.hurt(livingEntity.damageSources().mobAttack(this), dmg);
        }
    }

    @Override
    public void setStanding(boolean shouldStand) {
        super.setStanding(shouldStand);

        if (shouldStand && !level().isClientSide()) {
            kickNearbyEntities();
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        int maxSpurs = tag.getInt("MaxSpurs");
        if (maxSpurs <= 0) {
            setMaxSpurs(this.random.nextIntBetweenInclusive(2, 5));
        } else {
            setMaxSpurs(maxSpurs);
        }
        setSpurs(getMaxSpurs());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("MaxSpurs", getMaxSpurs());
    }

    @Override
    protected boolean canPerformRearing() {
        if (this.getLastDamageSource() != null) {
            return hurtTime > 0 && this.getLastDamageSource().getEntity() != null;
        }
        return super.canPerformRearing();
    }

    @Override
    protected float getFlyingSpeed() {
        return this.getSpeed() * 0.2f;
    }

    @Override
    protected void executeRidersJump(float power, Vec3 motion) {
        super.executeRidersJump(power, motion);
        Vec3 look = this.getLookAngle();
        this.setDeltaMovement(this.getDeltaMovement().add(look.x * 0.3, 0, look.z * 0.3));
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        int speed = Mth.ceil(getCruiseSpeed());
        if (!spurIsReady && speed >= 3 && player.zza < 0.5) {
            spurIsReady = true;
        }
        float speedAttribute = (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED);
        return switch(speed) {
            case 1 -> 0.2f * speedAttribute;
            case 2 -> 0.5f * speedAttribute;
            case 4 -> 1.5f * speedAttribute;
            default -> speedAttribute;
        };
    }

    @Override
    protected void doPlayerRide(Player player) {
        super.doPlayerRide(player);
        setHeadRotation(this.getYHeadRot());
        setBodyRotation(this.getYRot());
        setCruiseSpeed(0f);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        setMaxSpurs(this.random.nextIntBetweenInclusive(2, 5));
        setSpurs(getMaxSpurs());
        return result;
    }
}
