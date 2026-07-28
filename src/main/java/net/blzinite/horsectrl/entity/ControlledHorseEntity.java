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
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ControlledHorseEntity extends Horse {
    // Static Reference
    private static final Style SPUR_FONT = Style.EMPTY.withFont(ResourceLocation.fromNamespaceAndPath("horse_ctrl", "spurs"));

    // Entity Data
    private static final EntityDataAccessor<Float> HEAD_ROTATION = SynchedEntityData.defineId(ControlledHorseEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> BODY_ROTATION = SynchedEntityData.defineId(ControlledHorseEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SPEED = SynchedEntityData.defineId(ControlledHorseEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> MAX_SPURS = SynchedEntityData.defineId(ControlledHorseEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SPURS = SynchedEntityData.defineId(ControlledHorseEntity.class, EntityDataSerializers.FLOAT);

    // Functional
    private boolean allowSpur;

    public ControlledHorseEntity(EntityType<? extends Horse> entityType, Level level) {
        super(entityType, level);
        allowSpur = false;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(HEAD_ROTATION, this.getYRot());
        this.entityData.define(BODY_ROTATION, this.getYRot());
        this.entityData.define(SPEED, 0f);
        this.entityData.define(MAX_SPURS, 2);
        this.entityData.define(SPURS, 2f);
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

    private void spawnSprintParticles() {
        if (this.random.nextFloat() < 0.3F) {
            double x = this.getX() + (this.random.nextDouble() - 0.5D) * this.getBbWidth();
            double y = this.getY() + 0.1D;
            double z = this.getZ() + (this.random.nextDouble() - 0.5D) * this.getBbWidth();

            this.level().addParticle(
                    ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    x, y, z,
                    0.0D,
                    0.05D,
                    0.0D
            );
        }
    }

    private void playSpurSound(Level level) {
        if (!level.isClientSide()) {
            level.playSound(
                    null,
                    this.getX(), this.getY(), this.getZ(),
                    SoundEvents.PLAYER_SMALL_FALL,
                    SoundSource.PLAYERS,
                    1, 0.2f
            );
            level.playSound(
                    null,
                    this.getX(), this.getY(), this.getZ(),
                    SoundEvents.HORSE_AMBIENT, SoundSource.PLAYERS,
                    1, 1
            );
        } else {
            level.playLocalSound(
                    this.getX(), this.getY(), this.getZ(),
                    SoundEvents.PLAYER_SMALL_FALL, SoundSource.PLAYERS,
                    1f, 0.2f,
                    false
            );
            level.playLocalSound(
                    this.getX(), this.getY(), this.getZ(),
                    SoundEvents.HORSE_AMBIENT, SoundSource.PLAYERS,
                    1, 1,
                    false
            );
        }
    }

    @Override
    public void setStanding(boolean shouldStand) {
        super.setStanding(shouldStand);

        if (shouldStand) {
            List<LivingEntity> kickedEntities = level().getEntitiesOfClass(LivingEntity.class, new AABB(this.position(), this.position()).inflate(2), e -> !this.getPassengers().contains(e) && !e.equals(this));
            for (LivingEntity livingEntity : kickedEntities) {
                livingEntity.knockback(2.4f, Mth.sin((float) (this.getYRot()*(Math.PI/180))), -Mth.cos((float) (this.getYRot()*(Math.PI/180))));
                float dmg = (float) this.getAttributeValue(Attributes.MAX_HEALTH)
                        * (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 2.0f;
                livingEntity.hurt(livingEntity.damageSources().mobAttack(this), dmg);
            }
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        int maxSpurs = tag.getInt("MaxSpurs");
        if (maxSpurs == 0) {
            setMaxSpurs(this.random.nextIntBetweenInclusive(2, 5));
        } else {
            setMaxSpurs(maxSpurs);
        }
        setSpurs(getMaxSpurs());
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
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
    protected @NotNull Vec3 getRiddenInput(@NotNull Player rider, @NotNull Vec3 travelVector) {
        if (this.isStanding() && !this.allowStandSliding) {
            return Vec3.ZERO;
        } else {
            float speed = getCruiseSpeed();
            float z_input = rider.zza;
            if (z_input > 0.0F) {
                z_input *= 0.25F;
            }
            float spurs = getSpurs();

            if (z_input < 0.0F) {
                if (speed > 2) {
                    allowSpur = false;
                    this.makeMad();
                }
                speed = Mth.clamp(speed+z_input, 0F, 3.0F);
            } else if (Mth.ceil(speed) >= 3) {
                if (level().isClientSide) {
                    String spursDisplay = "\uE000".repeat(Math.max(0, Mth.floor(spurs))) +
                            "\uE001".repeat(Math.max(0, getMaxSpurs() - Mth.floor(spurs)));
                    rider.displayClientMessage(Component.literal(spursDisplay).withStyle(SPUR_FONT), true);
                }
                if (allowSpur && z_input > 0.2 && spurs >= 1) {
                    speed = 4;
                    allowSpur = false;
                    playSpurSound(level());

                    setSpurs(spurs - 1);
                }
            } else {
                if (z_input == 0) {
                    speed = Mth.ceil(speed);
                } else {
                    speed = Mth.clamp(speed+z_input, 0F, 3.0F);
                }
            }
            setCruiseSpeed(speed);

            float x_input = getHeadRotation() - 4*rider.xxa;
            if (Mth.degreesDifferenceAbs(x_input, getBodyRotation()) < 30) {
                setHeadRotation(x_input);
            }

            if (getCruiseSpeed() <= 0.0F && rider.zza <= 0.0F) {
                return new Vec3(0, 0, rider.zza*0.25);
            }
            return new Vec3(0, 0, getCruiseSpeed());
        }
    }

    @Override
    protected void executeRidersJump(float power, Vec3 motion) {
        super.executeRidersJump(power, motion);
        Vec3 look = this.getLookAngle();
        this.setDeltaMovement(this.getDeltaMovement().add(look.x * 0.3, 0, look.z * 0.3));
    }

    @Override
    protected void tickRidden(@NotNull Player player, @NotNull Vec3 travelVector) {
        float speed = getCruiseSpeed();
        float rate = Mth.square(5 - speed)/4;
        setBodyRotation(Mth.approachDegrees(getBodyRotation(), getHeadRotation(), rate));

        float headHeight = 0;
        if (speed > 3) {
            setCruiseSpeed(speed - 0.006f);
            spawnSprintParticles();
            headHeight = 15;
        }
        this.setRot(getBodyRotation(), headHeight);
        this.yHeadRot = getHeadRotation();
        this.yRotO = this.yBodyRot = this.getYRot();

        if (getSpurs() < getMaxSpurs()) {
            setSpurs(getSpurs() + 0.004f);
        }

        if (this.isControlledByLocalInstance()) {
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
    }

    @Override
    protected float getRiddenSpeed(@NotNull Player player) {
        int speed = Mth.ceil(getCruiseSpeed());
        if (!allowSpur && speed >= 3 && player.zza < 0.5) {
            allowSpur = true;
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
    protected void doPlayerRide(@NotNull Player player) {
        super.doPlayerRide(player);
        setHeadRotation(this.getYHeadRot());
        setBodyRotation(this.getBodyRotation());
        setCruiseSpeed(0f);
    }

    @Override
    public SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor level,
                                        @NotNull DifficultyInstance difficulty,
                                        @NotNull MobSpawnType reason, SpawnGroupData data, CompoundTag tag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data, tag);
        setMaxSpurs(this.random.nextIntBetweenInclusive(2, 5));
        setSpurs(getMaxSpurs());
        return result;
    }
}
