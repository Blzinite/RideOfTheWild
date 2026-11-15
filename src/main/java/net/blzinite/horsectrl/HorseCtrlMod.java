package net.blzinite.horsectrl;

import net.blzinite.horsectrl.config.HorseCtrlConfig;
import net.blzinite.horsectrl.network.HorseCtrlNetwork;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;


@Mod(HorseCtrlMod.MODID)
public class HorseCtrlMod {
    public static final String MODID = "horse_ctrl";

    public static final Style SPUR_FONT = Style.EMPTY.withFont(ResourceLocation.fromNamespaceAndPath("horse_ctrl", "spurs"));

    public HorseCtrlMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        context.registerConfig(ModConfig.Type.COMMON, HorseCtrlConfig.SPEC);
        HorseCtrlNetwork.register();
    }

//    private void registerPayloads(RegisterPayloadHandlersEvent event) {
//        HorseInputPacket.register(event.registrar("1"));
//    }
}
