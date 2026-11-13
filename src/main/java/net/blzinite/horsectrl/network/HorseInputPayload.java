package net.blzinite.horsectrl.network;

import net.blzinite.horsectrl.HorseCtrlMod;
import net.blzinite.horsectrl.config.HorseCtrlConfig;
import net.blzinite.horsectrl.util.HorseControlAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public record HorseInputPayload(float forward, float right, float yaw, boolean sprintPressed) implements CustomPacketPayload {
    public static final Type<HorseInputPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(HorseCtrlMod.MODID, "horse_input"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HorseInputPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT,
            HorseInputPayload::forward,
            ByteBufCodecs.FLOAT,
            HorseInputPayload::right,
            ByteBufCodecs.FLOAT,
            HorseInputPayload::yaw,
            ByteBufCodecs.BOOL,
            HorseInputPayload::sprintPressed,
            HorseInputPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, HorseInputPayload::handle);
    }

    private static void handle(HorseInputPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.getVehicle() instanceof AbstractHorse horse && HorseCtrlConfig.isControlledMount(horse)) {
                ((HorseControlAccess) horse).horseCtrl$receiveInput(payload.forward(), payload.right(), payload.yaw(), payload.sprintPressed());
            }
        });
    }
}
