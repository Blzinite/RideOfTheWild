package net.blzinite.horsectrl.event;

import net.blzinite.horsectrl.HorseCtrlMod;
import net.blzinite.horsectrl.render.ControlledHorseRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class RendererHandler {
    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(HorseCtrlMod.HORSE.get(), ControlledHorseRenderer::new);
    }
}
