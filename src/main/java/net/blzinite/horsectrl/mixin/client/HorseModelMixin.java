package net.blzinite.horsectrl.mixin.client;

import net.blzinite.horsectrl.config.HorseCtrlConfig;
import net.blzinite.horsectrl.util.HorseControlAccess;
import net.minecraft.client.model.HorseModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HorseModel.class)
public class HorseModelMixin<T extends AbstractHorse>  {
    @Final
    @Shadow
    private ModelPart[] saddleParts;
    @Final
    @Shadow
    private ModelPart[] ridingParts;

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/animal/horse/AbstractHorse;FFFFF)V", at = @At("TAIL"))
    private void horseCtrl$hideBarebackSaddle(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (!HorseCtrlConfig.allowBareback || !HorseCtrlConfig.isControlledMount(entity)) return ;

        if (entity instanceof HorseControlAccess access && !access.horseCtrl$hasRealSaddle()) {
            for (ModelPart modelpart : this.saddleParts) {
                modelpart.visible = false;
            }
        }
    }
}
