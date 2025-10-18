package net.blzinite.horsectrl;

import net.blzinite.horsectrl.entity.ControlledHorseEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(HorseCtrlMod.MODID)
public class HorseCtrlMod {
    public static final String MODID = "horse_ctrl";

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "minecraft");
    public static final RegistryObject<EntityType<ControlledHorseEntity>> HORSE = ENTITY_TYPES.register("horse", () ->
            EntityType.Builder.of(ControlledHorseEntity::new, MobCategory.CREATURE)
                    .sized(1.3965F, 1.6F)
                    .build("horse"));

    public HorseCtrlMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        modEventBus.addListener(this::registerAttributes);

        ENTITY_TYPES.register(modEventBus);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    public void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(HORSE.get(), Horse.createBaseHorseAttributes().build());
    }
}
