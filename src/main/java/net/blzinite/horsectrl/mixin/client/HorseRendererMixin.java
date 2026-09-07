package net.blzinite.horsectrl.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.blzinite.horsectrl.config.HorseCtrlConfig;
import net.blzinite.horsectrl.util.HorseControlAccess;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class HorseRendererMixin<T extends LivingEntity> {
    @Inject(method = "setupRotations", at = @At("TAIL"))
    private void horseCtrl$setupRotations(T entity, PoseStack poseStack, float bob, float yBodyRot, float partialTick, float scale, CallbackInfo ci) {
        if (HorseCtrlConfig.isControlledMount(entity) && entity instanceof AbstractHorse horse) {
            if (horse.getControllingPassenger() instanceof Player rider) {
                if (rider.zza >= 0.0 && horse.getDeltaMovement().horizontalDistanceSqr() > 0) {
                    float yawDiff = horse.getYRot() - horse.yHeadRot;
                    float leanAngle = Mth.clamp(yawDiff * 2, -5.0f, 5.0f);
                    HorseControlAccess access = (HorseControlAccess) horse;
                    float lean = access.horseCtrl$getLean();
                    lean += (leanAngle - lean) * 0.2F;
                    access.horseCtrl$setLean(lean);
                    poseStack.mulPose(Axis.ZP.rotationDegrees(lean));
                }
            }
        }
    }

}
