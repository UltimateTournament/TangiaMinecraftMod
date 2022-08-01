package co.tangia.minecraftmod.chatcommands;

import co.tangia.minecraftmod.TangiaMod;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.LiteralContents;
import org.slf4j.Logger;

import java.util.UUID;

public class LogoutCommand {
    private static final String codeArg = "code";
    // a random UUID identifying this sender
    private static final UUID sender = UUID.fromString("d0661b9f-f614-449b-a666-dc1a8a7f9935");
    private static final Logger LOGGER = LogUtils.getLogger();

    private final TangiaMod mod;

    public LogoutCommand(TangiaMod mod) {
        this.mod = mod;
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("tangia")
                .then(Commands.literal("logout")
                    .executes(this::logout)));
    }

    private int logout(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        if (mod == null)
            return 0;
        try {
            var player = ctx.getSource().getPlayerOrException();
            mod.logout(player, true);
            player.sendSystemMessage(MutableComponent.create(new LiteralContents("You're logged out now")));
        } catch (Exception e) {
            LOGGER.error("exception in command", e);
        }
        return Command.SINGLE_SUCCESS;
    }
}
