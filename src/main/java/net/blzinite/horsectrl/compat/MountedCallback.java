package net.blzinite.horsectrl.compat;

import com.github.exopandora.shouldersurfing.api.callback.ICameraCouplingCallback;
import net.blzinite.horsectrl.entity.ControlledHorseEntity;
import net.minecraft.client.Minecraft;

public class MountedCallback implements ICameraCouplingCallback {
    @Override
    public boolean isForcingCameraCoupling(Minecraft minecraft) {
        return minecraft.getCameraEntity() == minecraft.player && minecraft.player != null && !minecraft.player.isSpectator() && minecraft.player.isPassenger() && minecraft.player.getVehicle() instanceof ControlledHorseEntity;
    }
}