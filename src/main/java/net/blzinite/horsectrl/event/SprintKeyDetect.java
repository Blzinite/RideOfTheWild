package net.blzinite.horsectrl.event;

import net.blzinite.horsectrl.config.HorseCtrlConfig;
import net.blzinite.horsectrl.network.HorseCtrlNetwork;
import net.blzinite.horsectrl.network.HorseInputPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SprintKeyDetect {
    private static boolean wasSprintDown = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        boolean down = mc.options.keySprint.isDown();
        boolean sprintPressed = down && !wasSprintDown;
        wasSprintDown = down;

        if (mc.player != null && mc.player.getVehicle() instanceof AbstractHorse horse
                && HorseCtrlConfig.isControlledMount(horse)) {

            HorseCtrlNetwork.sendToServer(new HorseInputPacket(
                    mc.player.zza, mc.player.xxa, mc.player.getYRot(),
                    sprintPressed
            ));
        }
    }
}
