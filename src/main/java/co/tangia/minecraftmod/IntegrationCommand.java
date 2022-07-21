package co.tangia.minecraftmod;

import java.util.UUID;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;

public class IntegrationCommand {
    public IntegrationCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("tangia")
                .then(Commands.literal("set")
                    .then(Commands.argument("testarg", StringArgumentType.string()))
                    .executes(this::integrate)));
    }

    private int integrate(CommandContext<CommandSourceStack> sourceStack) throws CommandSyntaxException {
        sourceStack.getSource().getPlayerOrException().sendMessage(new TextComponent("good command " + StringArgumentType.getString(sourceStack, "testarg")), UUID.randomUUID());
        return 0;
    }
}
