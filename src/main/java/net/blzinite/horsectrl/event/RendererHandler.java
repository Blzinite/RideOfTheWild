package net.blzinite.horsectrl.event;

import net.blzinite.horsectrl.HorseCtrlMod;
import net.blzinite.horsectrl.render.ControlledHorseRenderer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;


@EventBusSubscriber
public class RendererHandler {
    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(HorseCtrlMod.HORSE.get(), ControlledHorseRenderer::new);
    }
}
