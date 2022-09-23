package co.tangia.minecraftmod;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

import java.util.UUID;

public class CommandComponent {
  public String command;
  public String playerName;
  public String displayName;
  private long startTick;
  private boolean stopListening;
  public int delayTicks;
  private UUID playerUUID;
  private static final Logger LOGGER = LogUtils.getLogger();

  public CommandComponent(String playerName, String displayName, UUID playerUUID, String command, long startTick , int delayTicks) {
    this.playerUUID = playerUUID;
    this.playerName = playerName;
    this.displayName = displayName;
    this.command = command;
    this.startTick = startTick;
    this.delayTicks = delayTicks;
  }

  public String getMessage() {
    return this.command.replaceAll("\\$DISPLAYNAME", this.displayName).replaceAll("\\$PLAYERNAME", this.playerName);
  }

  public void init() {
    MinecraftForge.EVENT_BUS.register(this);
  }

  @SubscribeEvent
  public void onTick(TickEvent.WorldTickEvent event) {
    if (this.stopListening) {
      MinecraftForge.EVENT_BUS.unregister(this);
      return;
    }
    if (event.world.dayTime() > this.startTick + this.delayTicks) {
      this.stopListening = true;
      var player = event.world.getPlayerByUUID(this.playerUUID);
      LOGGER.info("Running command: " + this.getMessage());
      if (player != null) {
        event.world.getServer().getCommands().performCommand(
                event.world.getServer().createCommandSourceStack(),
                this.getMessage());
      }
    }
  }
}
