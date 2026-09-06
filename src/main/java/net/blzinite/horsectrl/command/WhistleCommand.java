package net.blzinite.horsectrl.command;

import com.mojang.brigadier.CommandDispatcher;
import net.blzinite.horsectrl.entity.ControlledHorseEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;


@EventBusSubscriber
public class WhistleCommand {
    @SubscribeEvent
    public static void onCommandRegister(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("whistle")
                .then(Commands.argument("horse", EntityArgument.entity())
                        .executes(commandContext -> {
                    Entity entity = EntityArgument.getEntity(commandContext, "horse");
                    Player player = commandContext.getSource().getPlayer();
                    if (player != null && entity instanceof ControlledHorseEntity horse) {
                        horse.getNavigation().moveTo(player, 1.5);
                    }
                    return 0;
                })));
    }
}
