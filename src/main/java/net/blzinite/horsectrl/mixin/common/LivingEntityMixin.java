package net.blzinite.horsectrl.mixin.common;

import net.blzinite.horsectrl.config.HorseCtrlConfig;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Inject(method = "getFlyingSpeed", at = @At("RETURN"), cancellable = true)
    private void horseCtrl$getFlyingSpeed(CallbackInfoReturnable<Float> cir) {
        if (HorseCtrlConfig.isControlledMount(this)) {
            LivingEntity livingEntity = (LivingEntity) (Object) this;
            cir.setReturnValue(livingEntity.getSpeed() * 0.2f); // maintains velocity when jumping
        }
    }
}
