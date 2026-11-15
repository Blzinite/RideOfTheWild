package net.blzinite.horsectrl.network;

import net.blzinite.horsectrl.config.HorseCtrlConfig;
import net.blzinite.horsectrl.util.HorseControlAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record HorseInputPacket(float forward, float right, float yaw, boolean sprintPressed) {
    public static void encode(HorseInputPacket msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.forward);
        buf.writeFloat(msg.right);
        buf.writeFloat(msg.yaw);
        buf.writeBoolean(msg.sprintPressed);
    }

    public static HorseInputPacket decode(FriendlyByteBuf buf) {
        return new HorseInputPacket(
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readBoolean()
        );
    }

    public static void handle(HorseInputPacket msg, Supplier<NetworkEvent.Context> supplier
    ) {
        NetworkEvent.Context ctx = supplier.get();

        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            if (player.getVehicle() instanceof AbstractHorse horse
                    && horse.getControllingPassenger() == player
                    && HorseCtrlConfig.isControlledMount(horse)) {
                ((HorseControlAccess) horse).horseCtrl$receiveInput(msg.forward(), msg.right(), msg.yaw(), msg.sprintPressed());
            }
        });

        ctx.setPacketHandled(true);
    }
}
