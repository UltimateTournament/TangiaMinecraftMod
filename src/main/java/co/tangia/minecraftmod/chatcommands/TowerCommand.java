package co.tangia.minecraftmod.chatcommands;

import co.tangia.minecraftmod.TangiaMod;
import co.tangia.minecraftmod.TowerComponent;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.UUID;

public class TowerCommand {
    private static final Logger LOGGER = LogUtils.getLogger();

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("tangia")
                .then(Commands.literal("tower")
                    .executes(this::createTower)));
    }

    private int createTower(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        var world = player.getCommandSenderWorld();
        new TowerComponent(player.getX() + 2, player.getY(), player.getZ() + 2).setBlockEntities(world);
        return Command.SINGLE_SUCCESS;
    }
}
