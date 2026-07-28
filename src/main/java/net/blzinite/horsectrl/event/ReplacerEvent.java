package net.blzinite.horsectrl.event;

import net.blzinite.horsectrl.HorseCtrlMod;
import net.blzinite.horsectrl.entity.ControlledHorseEntity;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class ReplacerEvent {
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Horse vanillaHorse && !(event.getEntity() instanceof ControlledHorseEntity)) {
            Level level = event.getLevel();
            ControlledHorseEntity newHorse = HorseCtrlMod.HORSE.get().create(level);
            if (newHorse != null) {
                newHorse.moveTo(vanillaHorse.position());
                newHorse.setCustomName(vanillaHorse.getCustomName());
                level.addFreshEntity(newHorse);
                vanillaHorse.discard();
            }
        }
    }
}
