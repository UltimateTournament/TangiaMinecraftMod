package co.tangia.minecraftmod.chatcommands;

import co.tangia.minecraftmod.TowerComponent;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.slf4j.Logger;

public class TowerCommand {
  private static final Logger LOGGER = LogUtils.getLogger();

  public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
            Commands.literal("tangia")
                .then(Commands.literal("tower")
                .executes(this::createTower)));
    dispatcher.register(
            Commands.literal("tangia")
                .then(Commands.literal("template")
                .executes(this::template)));
  }

  private int createTower(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    try {
      var player = ctx.getSource().getPlayerOrException();
      var world = player.getCommandSenderWorld();
      var tower = new TowerComponent(player.getX() + 2, player.getY(), player.getZ() + 2, 20);
      tower.instantiate(world, true);
      return Command.SINGLE_SUCCESS;
    } catch (Exception e) {
      LOGGER.error("exception in command", e);
      return 0;
    }
  }

  private int template(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    try {
      var player = ctx.getSource().getPlayerOrException();
      var world = player.getCommandSenderWorld();
      var tower = new TowerComponent(player.getX() + 2, player.getY(), player.getZ() + 2, 0);
      tower.instantiate(world, false);
      return Command.SINGLE_SUCCESS;
    } catch (Exception e) {
      LOGGER.error("exception in command", e);
      return 0;
    }
  }
}
