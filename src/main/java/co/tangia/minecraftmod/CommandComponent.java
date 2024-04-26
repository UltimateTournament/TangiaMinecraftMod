package co.tangia.minecraftmod;

import com.mojang.brigadier.ParseResults;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.ClientCommandSourceStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

import java.util.UUID;

public class CommandComponent implements CommandSource {
  public String command;
  public String playerName;
  public String displayName;
  private final long startTick;
  private boolean stopListening;
  public int delayTicks;
  private final TangiaMod.CommandAckWaiter ackWaiter;
  private final UUID playerUUID;
  private static final Logger LOGGER = LogUtils.getLogger();

  public CommandComponent(String playerName, String displayName, UUID playerUUID, String command, long startTick, int delayTicks, TangiaMod.CommandAckWaiter ackWaiter) {
    this.playerUUID = playerUUID;
    this.playerName = playerName;
    this.displayName = displayName;
    this.command = command;
    this.startTick = startTick;
    this.delayTicks = delayTicks;
    this.ackWaiter = ackWaiter;
    if (ackWaiter != null) {
      ackWaiter.add(this);
    }
  }

  public String getMessage() {
    return this.command
        .replaceAll("\\$DISPLAYNAME", this.displayName)
        .replaceAll("\\$PLAYERNAME", this.playerName);
  }

  public void init() {
    MinecraftForge.EVENT_BUS.register(this);
  }

  @SubscribeEvent
  public void onTick(TickEvent.PlayerTickEvent event) {
    if (this.stopListening) {
      MinecraftForge.EVENT_BUS.unregister(this);
      return;
    }
    if (event.player.level().dayTime() <= this.startTick + this.delayTicks) {
      return;
    }
    if (!this.playerUUID.equals(event.player.getUUID())) {
      return;
    }
    this.stopListening = true;
    var server = event.player.level().getServer();
    boolean cmdSuccess;
    if (server != null) {
      var stack = new CommandSourceStack(this, event.player.position(), Vec2.ZERO, server.getLevel(event.player.level().dimension()), 4, "Server", MutableComponent.create(new LiteralContents("Server")), server, null);
      cmdSuccess = server.getCommands().performPrefixedCommand(stack, this.getMessage()) > 0;
    } else {
      cmdSuccess = ClientCommandHandler.runCommand(this.getMessage());
    }
    LOGGER.info("Ran command: " + this.getMessage() + " res: " + cmdSuccess + " onServer:" + (server != null));
    if (this.ackWaiter != null) {
      if (cmdSuccess) {
        this.ackWaiter.ack(this);
      } else {
        this.ackWaiter.fail(this);
      }
    }
  }

  @Override
  public void sendSystemMessage(Component c) {
    LOGGER.info("command got message: {}", c.getString());
  }

  @Override
  public boolean acceptsSuccess() {
    return true;
  }

  @Override
  public boolean acceptsFailure() {
    return true;
  }

  @Override
  public boolean shouldInformAdmins() {
    return false;
  }
}
