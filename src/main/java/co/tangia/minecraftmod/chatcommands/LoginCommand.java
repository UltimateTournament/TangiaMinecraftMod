package co.tangia.minecraftmod.chatcommands;

import java.util.UUID;

import co.tangia.minecraftmod.TangiaMod;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
import org.slf4j.Logger;

public class LoginCommand {
    private static final String codeArg = "code";
    // a random UUID identifying this sender
    private static final UUID sender = UUID.fromString("311989a7-2050-40fb-a6d0-283fcdbcd644");
    private static final Logger LOGGER = LogUtils.getLogger();

    private final TangiaMod mod;

    public LoginCommand(TangiaMod mod) {
        this.mod = mod;
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("tangia")
                .then(Commands.literal("login")
                    .then(Commands.argument(codeArg, StringArgumentType.string())
                        .executes(this::login))));
    }

    private int login(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        if (mod == null)
            return 0;
        var player = ctx.getSource().getPlayerOrException();
        try {
            mod.login(player, StringArgumentType.getString(ctx, codeArg));
            player.sendMessage(new TextComponent("You're logged in now"), sender);
        } catch (Exception ex) {
            LOGGER.warn("failed to login: " + ex);
            player.sendMessage(new TextComponent("We couldn't log you in"), sender);
            return 0;
        }
        return Command.SINGLE_SUCCESS;
    }
}
