package net.blzinite.horsectrl;

import com.mojang.logging.LogUtils;
import net.blzinite.horsectrl.config.HorseCtrlConfig;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;


@Mod(HorseCtrlMod.MODID)
public class HorseCtrlMod {
    public static final String MODID = "horse_ctrl";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final Style SPUR_FONT = Style.EMPTY.withFont(ResourceLocation.fromNamespaceAndPath("horse_ctrl", "spurs"));

    public HorseCtrlMod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, HorseCtrlConfig.SPEC);
    }
}
