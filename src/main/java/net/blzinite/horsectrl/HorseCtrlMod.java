package net.blzinite.horsectrl;

import net.blzinite.horsectrl.entity.ControlledHorseEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.horse.Horse;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;


@Mod(HorseCtrlMod.MODID)
public class HorseCtrlMod {
    public static final String MODID = "horse_ctrl";

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, "minecraft");
    public static final DeferredHolder<EntityType<?>, EntityType<ControlledHorseEntity>> HORSE = ENTITY_TYPES.register("horse", () ->
            EntityType.Builder.of(ControlledHorseEntity::new, MobCategory.CREATURE)
                    .sized(1.3965F, 1.6F)
                    .build("horse"));

    public HorseCtrlMod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::registerAttributes);
        ENTITY_TYPES.register(modEventBus);
    }

    public void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(HORSE.get(), Horse.createBaseHorseAttributes().build());
    }
}
