package co.tangia.minecraftmod;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.UUID;
        import com.mojang.logging.LogUtils;
        import net.minecraft.world.entity.item.PrimedTnt;
        import net.minecraftforge.common.MinecraftForge;
        import net.minecraftforge.event.TickEvent;
        import net.minecraftforge.eventbus.api.SubscribeEvent;
        import net.minecraftforge.fml.common.Mod;
        import org.slf4j.Logger;

        import java.util.UUID;

@Mod.EventBusSubscriber
public class SoundComponent {
  public String soundID;
  public int delaySeconds;
  private long startTick;
  private boolean stopListening;
  private UUID playerUUID;
  public SoundComponent(UUID playerUUID, String soundID, int delaySeconds, long startTick) {
    this.startTick = startTick;
    this.stopListening = false;
    this.playerUUID = playerUUID;
    this.delaySeconds = delaySeconds;
    this.soundID = soundID;
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
      var player = event.level.getPlayerByUUID(this.playerUUID);
        if (player == null) {
            return;
        }
      BlockPos bp = new BlockPos(player.getX(), player.getY(), player.getZ());
      event.level.playSound(null, bp, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(this.soundID)), SoundSource.AMBIENT, 1f, 1f);
      this.stopListening = true;
    }
  }
}
