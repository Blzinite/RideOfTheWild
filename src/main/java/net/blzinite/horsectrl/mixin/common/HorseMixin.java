package net.blzinite.horsectrl.mixin.common;

import net.minecraft.world.entity.animal.horse.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractHorse.class)
public class HorseMixin {
    @Unique
    private int horseCtrl$testValue = 123;

    @Inject(method = "getRiddenInput", at = @At("HEAD"))
    private void horseCtrl$tick(CallbackInfo ci) {
        System.out.println("Horse ticking!");
    }
}
