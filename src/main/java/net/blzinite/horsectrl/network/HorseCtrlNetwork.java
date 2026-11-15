package net.blzinite.horsectrl.network;

import net.blzinite.horsectrl.HorseCtrlMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class HorseCtrlNetwork {
    private static final String VERSION = "1";

    public static final SimpleChannel CHANNEL =
            NetworkRegistry.newSimpleChannel(
                    ResourceLocation.fromNamespaceAndPath(HorseCtrlMod.MODID, "main"),
                    () -> VERSION,
                    VERSION::equals,
                    VERSION::equals
            );

    public static void register() {
        CHANNEL.registerMessage(
                0,
                HorseInputPacket.class,
                HorseInputPacket::encode,
                HorseInputPacket::decode,
                HorseInputPacket::handle
        );
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }
}
