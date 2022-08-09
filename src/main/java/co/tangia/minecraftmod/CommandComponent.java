package co.tangia.minecraftmod;

import com.mojang.brigadier.Command;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.item.PrimedTnt;
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
  public int delaySeconds;
  private UUID playerUUID;
  private static final Logger LOGGER = LogUtils.getLogger();

  public CommandComponent(String playerName, String displayName, UUID playerUUID, String command, long startTick , int delaySeconds) {
    this.playerUUID = playerUUID;
    this.playerName = playerName;
    this.displayName = displayName;
    this.command = command;
    this.startTick = startTick;
    this.delaySeconds = delaySeconds;
  }

  public String getMessage() {
    return this.command.replaceAll("\\$DISPLAYNAME", this.displayName).replaceAll("\\$PLAYERNAME", this.playerName);
  }

  public void init() {
    MinecraftForge.EVENT_BUS.register(this);
  }

  @SubscribeEvent
  public void onTick(TickEvent.LevelTickEvent event) {
    if (this.stopListening) {
      MinecraftForge.EVENT_BUS.unregister(this);
      return;
    }
    if (event.level.dayTime() > this.startTick + this.delaySeconds * 20) {
      LOGGER.info("Spawning tnt");
      this.stopListening = true;
      var player = event.level.getPlayerByUUID(this.playerUUID);
      if (player != null) {
        event.level.getServer().getCommands().performCommand(
                event.level.getServer().createCommandSourceStack(),
                this.getMessage());
      }
    }
  }
}
