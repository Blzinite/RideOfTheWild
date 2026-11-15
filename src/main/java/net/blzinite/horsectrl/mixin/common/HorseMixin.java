package net.blzinite.horsectrl.mixin.common;

import net.blzinite.horsectrl.HorseCtrlMod;
import net.blzinite.horsectrl.config.HorseCtrlConfig;
import net.blzinite.horsectrl.util.HorseControlAccess;
import net.blzinite.horsectrl.util.Speeds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(AbstractHorse.class)
public abstract class HorseMixin implements HorseControlAccess {

    // ============================================================
    // Circling AI constants
    // ============================================================
    @Unique
    private static final double HORSE_CTRL_ORBIT_RADIUS = 6.0;
    @Unique
    private static final double HORSE_CTRL_ORBIT_GAIN = 0.05;
    @Unique
    private static final float HORSE_CTRL_ORBIT_TURN_RATE = 4.0f;
    @Unique
    private int horseCtrl$orbitDirection = 1;

    // ============================================================
    // Shadows
    // ============================================================
    @Shadow
    protected boolean allowStandSliding;
    @Shadow
    protected int gallopSoundCounter;
    @Shadow
    protected float playerJumpPendingScale;
    @Shadow
    protected abstract void executeRidersJump(float power, Vec3 motion);

    @Shadow
    protected abstract boolean getFlag(int flagId);

    @Shadow
    public abstract boolean isTamed();

    // ============================================================
    // Synced entity data
    // ============================================================
    @Unique
    private static final EntityDataAccessor<Float> HORSE_CTRL_HEAD_ROTATION = SynchedEntityData.defineId(AbstractHorse.class, EntityDataSerializers.FLOAT);
    @Unique
    private static final EntityDataAccessor<Float> HORSE_CTRL_BODY_ROTATION = SynchedEntityData.defineId(AbstractHorse.class, EntityDataSerializers.FLOAT);
    @Unique
    private static final EntityDataAccessor<Integer> HORSE_CTRL_SPEED = SynchedEntityData.defineId(AbstractHorse.class, EntityDataSerializers.INT);
    @Unique
    private static final EntityDataAccessor<Integer> HORSE_CTRL_MAX_SPURS = SynchedEntityData.defineId(AbstractHorse.class, EntityDataSerializers.INT);
    @Unique
    private static final EntityDataAccessor<Float> HORSE_CTRL_SPURS = SynchedEntityData.defineId(AbstractHorse.class, EntityDataSerializers.FLOAT);

    // ============================================================
    // Per-horse state
    // ============================================================
    @Unique
    private float horseCtrl$cruiseSpeed = 0f;
    @Unique
    private float horseCtrl$lean = 0.0F;
    @Unique
    private float horseCtrl$forwardInput = 0f;
    @Unique
    private boolean horseCtrl$sprintRequested = false;

    // ============================================================
    // Accessors
    // ============================================================
    @Override
    public float horseCtrl$getLean() {
        return horseCtrl$lean;
    }
    @Override
    public void horseCtrl$setLean(float lean) {
        horseCtrl$lean = lean;
    }
    @Unique
    private AbstractHorse horseCtrl$self() {
        return (AbstractHorse) (Object) this;
    }
    @Override
    public boolean horseCtrl$hasRealSaddle() {
        return this.getFlag(4);
    }

    // ============================================================
    // Entity data
    // ============================================================
    @Unique
    private float horseCtrl$getHeadRotation() {return horseCtrl$self().getEntityData().get(HORSE_CTRL_HEAD_ROTATION);}
    @Unique
    private void horseCtrl$setHeadRotation(float rotation) {horseCtrl$self().getEntityData().set(HORSE_CTRL_HEAD_ROTATION, rotation);}
    @Unique
    private float horseCtrl$getBodyRotation() {return horseCtrl$self().getEntityData().get(HORSE_CTRL_BODY_ROTATION);}
    @Unique
    private void horseCtrl$setBodyRotation(float rotation) {horseCtrl$self().getEntityData().set(HORSE_CTRL_BODY_ROTATION, rotation);}
    @Unique
    private float horseCtrl$getCruiseSpeed() {return horseCtrl$cruiseSpeed;}
    @Unique
    private void horseCtrl$setCruiseSpeed(float speed) {
        AbstractHorse horse = horseCtrl$self();
        if (horse.level().isClientSide()) return;
        horseCtrl$cruiseSpeed = Mth.clamp(speed, 0f, 4f);
        int gear = Mth.ceil(horseCtrl$cruiseSpeed);
        if (horse.getEntityData().get(HORSE_CTRL_SPEED) != gear) {
            horse.getEntityData().set(HORSE_CTRL_SPEED, gear);
        }
    }
    @Unique
    private int horseCtrl$getGear() {
        return horseCtrl$self().getEntityData().get(HORSE_CTRL_SPEED);
    }
    @Unique
    private int horseCtrl$getMaxSpurs() {return horseCtrl$self().getEntityData().get(HORSE_CTRL_MAX_SPURS);}
    @Unique
    private void horseCtrl$setMaxSpurs(int value) {horseCtrl$self().getEntityData().set(HORSE_CTRL_MAX_SPURS, value);}
    @Unique
    private float horseCtrl$getSpurs() {return horseCtrl$self().getEntityData().get(HORSE_CTRL_SPURS);}
    @Unique
    private void horseCtrl$setSpurs(float value) {horseCtrl$self().getEntityData().set(HORSE_CTRL_SPURS, value);}

    // ============================================================
    // defineSynchedData
    // ============================================================
    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void horseCtrl$defineSynchedData(CallbackInfo ci) {
        SynchedEntityData builder = horseCtrl$self().getEntityData();
        builder.define(HORSE_CTRL_HEAD_ROTATION, 0.0F);
        builder.define(HORSE_CTRL_BODY_ROTATION, 0.0F);
        builder.define(HORSE_CTRL_SPEED, 0);
        builder.define(HORSE_CTRL_MAX_SPURS, 2);
        builder.define(HORSE_CTRL_SPURS, 2.0F);
    }

    // ============================================================
    // Spur logic
    // ============================================================
    @Unique
    private boolean horseCtrl$canSpur() {
        return horseCtrl$getSpurs() >= 1;
    }

    @Unique
    private void horseCtrl$consumeSpur() {
        horseCtrl$setSpurs(Math.max(0f, horseCtrl$getSpurs() - 1f));
    }

    @Unique
    private void horseCtrl$trySpur() {
        if (horseCtrl$getCruiseSpeed() < Speeds.GALLOP.getValue()) return;
        if (!horseCtrl$canSpur()) return;

        horseCtrl$setCruiseSpeed(Speeds.SPRINT.getValue());
        horseCtrl$consumeSpur();
        horseCtrl$playSpurSound();
    }

    @Unique
    private void horseCtrl$rechargeSpur() {
        float spurs = horseCtrl$getSpurs();
        if (spurs < horseCtrl$getMaxSpurs()) {
            horseCtrl$setSpurs(Math.min(
                    horseCtrl$getMaxSpurs(),
                    spurs + HorseCtrlConfig.spurRechargeRate
            ));
        }
    }

    @Unique
    private void horseCtrl$renderSpurs(Player rider) {
        if (rider.level().isClientSide && horseCtrl$getGear() >= Speeds.GALLOP.getValue()) {
            float spurs = horseCtrl$getSpurs();
            String spursDisplay = "\uE000".repeat(Math.max(0, Mth.floor(spurs))) +
                    "\uE001".repeat(Math.max(0, horseCtrl$getMaxSpurs() - Mth.floor(spurs)));
            rider.displayClientMessage(Component.literal(spursDisplay).withStyle(HorseCtrlMod.SPUR_FONT), true);
        }
    }


    // ============================================================
    // Speed control
    // ============================================================
    @Unique
    private void horseCtrl$accelerate(float input) {
        float speed = horseCtrl$getCruiseSpeed();
        if (input == 0f) {
            horseCtrl$setCruiseSpeed(Mth.ceil(speed));
            return;
        }

        if (input > 0f) {
            input *= 0.25f;
        }

        horseCtrl$setCruiseSpeed(Mth.clamp(speed + input, 0f, Speeds.GALLOP.getValue()));
    }

    @Unique
    private void horseCtrl$decelerate(float input) {
        float speed = horseCtrl$getCruiseSpeed();

        if (speed > HorseCtrlConfig.rearSpeedThreshold) {
            horseCtrl$self().makeMad();
            speed = 0;
        }

        horseCtrl$setCruiseSpeed(Mth.clamp(speed + input, 0f, Speeds.SPRINT.getValue()));
    }

    @Unique
    private void horseCtrl$updateCruiseSpeed(float input) {
        float speed = horseCtrl$getCruiseSpeed();

        if (input < 0f) {
            horseCtrl$decelerate(input);
            return;
        }

        if (speed >= Speeds.GALLOP.getValue()) {
            return;
        }

        horseCtrl$accelerate(input);
    }

    @Unique
    private void horseCtrl$updateCoasting() {

        if (horseCtrl$cruiseSpeed <= 0f) {
            horseCtrl$cruiseSpeed = 0;
            return;
        }
        horseCtrl$cruiseSpeed -= 0.25f;
        float speed = (float) (horseCtrl$cruiseSpeed * horseCtrl$self().getAttributeValue(Attributes.MOVEMENT_SPEED));
        horseCtrl$self().setSpeed(speed);
        horseCtrl$self().travel(new Vec3(0, 0, 1));
    }


    @Override
    public void horseCtrl$receiveInput(float forward, float right, float yaw, boolean sprintPressed) {
        AbstractHorse horse = horseCtrl$self();

        if (horse.level().isClientSide()) return;

        horseCtrl$forwardInput = Mth.clamp(forward, -1f, 1f);

        if (sprintPressed) {
            horseCtrl$sprintRequested = true;
        }

        horseCtrl$updateHeadRotation(right, yaw);
    }

    // ============================================================
    // AI Tweaks
    // ============================================================
    @Unique
    private void horseCtrl$updateMobCircling(Mob rider) {
        AbstractHorse horse = horseCtrl$self();
        LivingEntity target = rider.getTarget();

        if (target != null && target.isAlive()) {
            double dx = target.getX() - horse.getX();
            double dz = target.getZ() - horse.getZ();

            double distanceSqr = dx * dx + dz * dz;
            if (distanceSqr < 0.0001) return;

            double distance = Math.sqrt(distanceSqr);

            double tx = dx / distance;
            double tz = dz / distance;

            float yaw = horseCtrl$getBodyRotation() * Mth.DEG_TO_RAD;
            double fx = -Mth.sin(yaw);
            double fz = Mth.cos(yaw);
            double dot = fx * tx + fz * tz;
            double radiusError = distance - HORSE_CTRL_ORBIT_RADIUS;

            double desiredDot = Mth.clamp(
                    radiusError * HORSE_CTRL_ORBIT_GAIN,
                    -0.6,
                    0.6
            );
            double error = desiredDot - dot;

            double cross = fx * tz - fz * tx;

            double side = Math.abs(cross) > 0.05 ? Math.signum(cross) : horseCtrl$orbitDirection;

            float turn = (float) Mth.clamp(
                    error * side * HORSE_CTRL_ORBIT_TURN_RATE,
                    -HORSE_CTRL_ORBIT_TURN_RATE,
                    HORSE_CTRL_ORBIT_TURN_RATE
            );
            if (Math.abs(turn) > 1) {
                horseCtrl$setHeadRotation(horseCtrl$getHeadRotation() + turn);
            }


            horseCtrl$cruiseSpeed = 3f;
            float speed = (float) horseCtrl$self().getAttributeValue(Attributes.MOVEMENT_SPEED);
            horseCtrl$self().setSpeed(speed);
            horseCtrl$self().travel(new Vec3(0, 0, 1));

            horseCtrl$updateBodyRotation();
            horseCtrl$updateRidingRotation();
        } else if (horseCtrl$cruiseSpeed > 0) {
            horseCtrl$updateCoasting();
        }
    }

    // ============================================================
    // Head / body rotation
    // ============================================================
    @Unique
    private void horseCtrl$updateHeadRotation(float right, float yaw) {
        float current = horseCtrl$getHeadRotation();
        float body = horseCtrl$getBodyRotation();

        float target;

        if (horseCtrl$forwardInput > 0f) {
            target = yaw;
        } else {
            target = current - 4f * right;
        }

        float difference = Mth.wrapDegrees(target - body);
        difference = Mth.clamp(difference, -30f, 30f);

        horseCtrl$setHeadRotation(body + difference);
    }

    @Unique
    private void horseCtrl$updateBodyRotation() {
        float speed = horseCtrl$getCruiseSpeed();
        float rate = Mth.square(5f - speed) * 0.8f;
        horseCtrl$setBodyRotation(Mth.approachDegrees(horseCtrl$getBodyRotation(), horseCtrl$getHeadRotation(), rate));
    }

    @Unique
    private void horseCtrl$updateRidingRotation() {
        AbstractHorse horse = horseCtrl$self();
        float headAngle = horseCtrl$getCruiseSpeed() > Speeds.GALLOP.getValue() ? 15f : 0f;
        horse.setYRot(horseCtrl$getBodyRotation());
        horse.setXRot(headAngle);
        horse.yHeadRot = horseCtrl$getHeadRotation();
        horse.yRotO = horse.yBodyRot = horseCtrl$getBodyRotation();
    }


    // ============================================================
    // Sprint
    // ============================================================
    @Unique
    private void horseCtrl$updateSprintState() {
        float speed = horseCtrl$getCruiseSpeed();

        if (speed <= Speeds.GALLOP.getValue()) {
            return;
        }

        horseCtrl$setCruiseSpeed(Math.max(Speeds.GALLOP.getValue(), speed - HorseCtrlConfig.sprintDecayRate));
    }

    @Unique
    private void horseCtrl$trample() {
        AbstractHorse horse = horseCtrl$self();
        if (!horse.level().isClientSide() && horseCtrl$getGear() > Speeds.GALLOP.getValue() && HorseCtrlConfig.trampleDamage > 0) {
            AABB area = horse.getBoundingBox().inflate(1.2D);

            List<LivingEntity> kickedEntities = horse.level().getEntitiesOfClass(
                    LivingEntity.class,
                    area,
                    entity -> !(entity instanceof AbstractHorse) && !horse.getPassengers().contains(entity)
            );

            for (LivingEntity livingEntity : kickedEntities) {
                livingEntity.knockback(2.5, Mth.sin((float) (horse.getYRot() * (Math.PI / 180))), -Mth.cos((float) (horse.getYRot() * (Math.PI / 180))));
                float damage = (float) (HorseCtrlConfig.trampleDamage);
                livingEntity.hurt(livingEntity.damageSources().mobAttack(horse), damage);
            }
        }
    }


    @Unique
    private void horseCtrl$spawnSprintParticles() {
        AbstractHorse horse = horseCtrl$self();

        if (horse.level().isClientSide() && horse.getRandom().nextFloat() < 0.3f && horse.onGround()) {
            double x = horse.getX() + (horse.getRandom().nextDouble() - 0.5) * horse.getBbWidth();
            double y = horse.getY() + 0.1;
            double z = horse.getZ() + (horse.getRandom().nextDouble() - 0.5) * horse.getBbWidth();

            horse.level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    x, y, z,
                    0, 0.05, 0
            );
        }
    }


    // ============================================================
    // Spur sound
    // ============================================================
    @Unique
    private void horseCtrl$playSpurSound() {
        AbstractHorse horse = horseCtrl$self();

        horse.playSound(
                SoundEvents.PLAYER_SMALL_FALL,
                1f,
                0.2f
        );

        horse.playSound(
                SoundEvents.HORSE_AMBIENT,
                1f,
                1f
        );
    }


    // ============================================================
    // Ridden input
    // ============================================================
    @Inject(method = "getRiddenInput", at = @At("HEAD"), cancellable = true)
    private void horseCtrl$getRiddenInput(Player rider, Vec3 travelVector, CallbackInfoReturnable<Vec3> cir) {
        if (!HorseCtrlConfig.isControlledMount(this)) {
            return;
        }

        AbstractHorse horse = horseCtrl$self();
        if (horse.isStanding() && !this.allowStandSliding) {
            cir.setReturnValue(Vec3.ZERO);
            return;
        }

        horseCtrl$renderSpurs(rider);

        float swim = horse.isInWater() && horse.getFluidTypeHeight(ForgeMod.WATER_TYPE.get()) > 0 ? 0.3f : 0f;

        int speed = horseCtrl$getGear();
        if (speed <= 0 && rider.zza < 0) {
            cir.setReturnValue(new Vec3(0, swim, rider.zza * 0.2)); // Backup
        } else {
            cir.setReturnValue(new Vec3(0, swim, speed));
        }
    }

    @Inject(method = "isSaddled", at = @At("HEAD"), cancellable = true)
    private void horseCtrl$isSaddled(CallbackInfoReturnable<Boolean> cir) {
        if (HorseCtrlConfig.isControlledMount(this) && HorseCtrlConfig.allowBareback && this.isTamed()) cir.setReturnValue(true);
    }


    // ============================================================
    // Ridden tick
    // ============================================================
    @Inject(method = "tick", at = @At("HEAD"))
    private void horseCtrl$tick(CallbackInfo ci) {
        if (!HorseCtrlConfig.isControlledMount(this)) {
            return;
        }

        AbstractHorse horse = horseCtrl$self();
        if (!horse.level().isClientSide()) {
            Entity rider = horse.getFirstPassenger();
            if (rider instanceof Mob mob) {
                horseCtrl$updateMobCircling(mob);
            } else if (rider == null && horseCtrl$cruiseSpeed > 0) {
                horseCtrl$updateCoasting();
            }
        }

    }


    @Inject(method = "tickRidden", at = @At("HEAD"), cancellable = true)
    private void horseCtrl$tickRidden(Player player, Vec3 travelVector, CallbackInfo ci) {
        if (!HorseCtrlConfig.isControlledMount(this)) {
            return;
        }

        AbstractHorse horse = horseCtrl$self();

        if (!horse.level().isClientSide()) {
            horseCtrl$updateCruiseSpeed(horseCtrl$forwardInput);

            if (horseCtrl$sprintRequested) {
                horseCtrl$sprintRequested = false;
                horseCtrl$trySpur();
            }
            horseCtrl$updateSprintState();
            horseCtrl$rechargeSpur();
            horseCtrl$trample();
        }

        horseCtrl$updateBodyRotation();
        horseCtrl$updateRidingRotation();


        if (horse.isControlledByLocalInstance()) {
            if (horseCtrl$getGear() > Speeds.GALLOP.getValue()) {
                horseCtrl$spawnSprintParticles();
            }
            if (travelVector.z <= 0) {
                this.gallopSoundCounter = 0;
            }

            if (horse.onGround()) {
                horse.setIsJumping(false);

                if (this.playerJumpPendingScale > 0 && !horse.isJumping()) {
                    this.executeRidersJump(this.playerJumpPendingScale, travelVector);
                }

                this.playerJumpPendingScale = 0;
            }
        }

        ci.cancel();
    }


    // ============================================================
    // Ridden speed
    // ============================================================
    @Inject(method = "getRiddenSpeed", at = @At("HEAD"), cancellable = true)
    private void horseCtrl$getRiddenSpeed(Player player, CallbackInfoReturnable<Float> cir) {
        if (!HorseCtrlConfig.isControlledMount(this)) {
            return;
        }

        int speed = horseCtrl$getGear();

        float speedAttribute = (float) horseCtrl$self().getAttributeValue(Attributes.MOVEMENT_SPEED);

        Speeds gear = Speeds.fromValue(speed);

        float result = switch (gear) {
            case WALK -> 0.2f * speedAttribute;
            case TROT -> 0.5f * speedAttribute;
            case GALLOP -> speedAttribute;
            case SPRINT -> 1.5f * speedAttribute;
        };

        cir.setReturnValue(result);
    }


    // ============================================================
    // Jump
    // ============================================================
    @Inject(method = "executeRidersJump", at = @At("TAIL"))
    private void horseCtrl$executeRidersJump(float power, Vec3 motion, CallbackInfo ci) {
        if (!HorseCtrlConfig.isControlledMount(this)) {
            return;
        }

        AbstractHorse horse = horseCtrl$self();
        Vec3 look = horse.getLookAngle();
        horse.setDeltaMovement(horse.getDeltaMovement().add(look.x * 0.5, 0, look.z * 0.5));
    }


    // ============================================================
    // Player mounting
    // ============================================================
    @Inject(method = "doPlayerRide", at = @At("TAIL"))
    private void horseCtrl$doPlayerRide(Player player, CallbackInfo ci) {
        if (!HorseCtrlConfig.isControlledMount(this)) {
            return;
        }

        AbstractHorse horse = horseCtrl$self();
        horseCtrl$setHeadRotation(horse.getYHeadRot());
        horseCtrl$setBodyRotation(horse.getYRot());
        horseCtrl$setCruiseSpeed(0.0F);
    }


    // ============================================================
    // Standing / kick
    // ============================================================
    @Inject(method = "setStanding", at = @At("TAIL"))
    private void horseCtrl$setStanding(boolean shouldStand, CallbackInfo ci) {
        if (!HorseCtrlConfig.isControlledMount(this)) {
            return;
        }

        AbstractHorse horse = horseCtrl$self();

        if (horse.level().isClientSide() || HorseCtrlConfig.rearDamageMultiplier <= 0) {
            return;
        }
        if (shouldStand) {
            AABB area = horse.getBoundingBox().inflate(2.0D);

            List<LivingEntity> kickedEntities = horse.level().getEntitiesOfClass(
                    LivingEntity.class,
                    area,
                    entity -> !(entity instanceof AbstractHorse) && !horse.getPassengers().contains(entity)
            );

            for (LivingEntity livingEntity : kickedEntities) {
                livingEntity.knockback(2.4f, Mth.sin((float) (horse.getYRot()*(Math.PI/180))), -Mth.cos((float) (horse.getYRot()*(Math.PI/180))));
                float damage = (float) (horse.getAttributeValue(Attributes.MAX_HEALTH) * HorseCtrlConfig.rearDamageMultiplier);
                livingEntity.hurt(livingEntity.damageSources().mobAttack(horse), damage);
            }
        }
    }


    // ============================================================
    // Rearing
    // ============================================================
    @Inject(method = "canPerformRearing", at = @At("HEAD"), cancellable = true)
    private void horseCtrl$canPerformRearing(CallbackInfoReturnable<Boolean> cir) {
        if (!HorseCtrlConfig.isControlledMount(this)) {
            return;
        }

        AbstractHorse horse = horseCtrl$self();

        if (horse.getLastDamageSource() != null) {
            cir.setReturnValue(horse.hurtTime > 0 && horse.getLastDamageSource().getEntity() != null);
        }
    }


    // ============================================================
    // Save data
    // ============================================================

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void horseCtrl$addAdditionalSaveData(CompoundTag tag, CallbackInfo ci) {
        if (!HorseCtrlConfig.isControlledMount(this)) {
            return;
        }
        tag.putInt("MaxSpurs", horseCtrl$getMaxSpurs());
    }


    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void horseCtrl$readAdditionalSaveData(CompoundTag tag, CallbackInfo ci) {
        if (!HorseCtrlConfig.isControlledMount(this)) {
            return;
        }
        AbstractHorse horse = horseCtrl$self();
        int maxSpurs = tag.getInt("MaxSpurs");
        if (maxSpurs <= 0) {
            maxSpurs = horse.getRandom().nextIntBetweenInclusive(2, 5);
        }
        horseCtrl$setMaxSpurs(maxSpurs);
        horseCtrl$setSpurs(maxSpurs);
    }


    // ============================================================
    // Spawn
    // ============================================================

    @Inject(method = "finalizeSpawn", at = @At("RETURN"))
    private void horseCtrl$finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, SpawnGroupData spawnGroupData, CompoundTag compoundTag, CallbackInfoReturnable<SpawnGroupData> cir) {
        if (!HorseCtrlConfig.isControlledMount(this)) {
            return;
        }
        AbstractHorse horse = horseCtrl$self();
        int maxSpurs = horse.getRandom().nextIntBetweenInclusive(2, 5);
        horseCtrl$setMaxSpurs(maxSpurs);
        horseCtrl$setSpurs(maxSpurs);
        horseCtrl$setHeadRotation(horse.getYRot());
        horseCtrl$setBodyRotation(horse.getYRot());
        horseCtrl$setCruiseSpeed(0.0F);
    }
}

