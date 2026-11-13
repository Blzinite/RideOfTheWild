package net.blzinite.horsectrl.event;

import net.blzinite.horsectrl.config.HorseCtrlConfig;
import net.blzinite.horsectrl.network.HorseInputPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber
public class SprintKeyDetect {
    private static boolean wasSprintDown = false;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        boolean down = mc.options.keySprint.isDown();
        boolean sprintPressed = down && !wasSprintDown;
        wasSprintDown = down;

        if (mc.player != null && mc.player.getVehicle() instanceof AbstractHorse horse
                && HorseCtrlConfig.isControlledMount(horse)) {

            PacketDistributor.sendToServer(new HorseInputPayload(
                    mc.player.zza, mc.player.xxa, mc.player.getYRot(),
                    sprintPressed
            ));
        }
    }
}
